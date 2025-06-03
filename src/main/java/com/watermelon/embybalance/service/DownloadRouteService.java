package com.watermelon.embybalance.service;

import com.watermelon.embybalance.entity.DownloadRoute;
import com.watermelon.embybalance.repository.DownloadRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadRouteService {
    
    private final DownloadRouteRepository downloadRouteRepository;
    private final DownloadStatisticsService downloadStatisticsService;
    
    /**
     * 获取所有下载线路
     */
    public List<DownloadRoute> getAllRoutes() {
        return downloadRouteRepository.findAllOrderByCreatedAtDesc();
    }
    
    /**
     * 根据ID获取下载线路
     */
    public Optional<DownloadRoute> getRouteById(Long id) {
        return downloadRouteRepository.findById(id);
    }
    
    /**
     * 创建新的下载线路
     */
    @Transactional
    public DownloadRoute createRoute(DownloadRoute route) {
        // 检查域名和端口组合是否已存在
        if (downloadRouteRepository.existsByDomainAndPort(route.getDomain(), route.getPort())) {
            throw new IllegalArgumentException("域名和端口组合已存在: " + route.getDomain() + ":" + route.getPort());
        }
        
        // 验证权重
        validateRoute(route);
        
        log.info("创建新的下载线路: {}", route.getFullUrl());
        return downloadRouteRepository.save(route);
    }
    
    /**
     * 更新下载线路
     */
    @Transactional
    public DownloadRoute updateRoute(Long id, DownloadRoute updatedRoute) {
        return downloadRouteRepository.findById(id)
                .map(existingRoute -> {
                    // 检查更新后的域名和端口组合是否与其他记录冲突
                    if (!existingRoute.getDomain().equals(updatedRoute.getDomain()) || 
                        !existingRoute.getPort().equals(updatedRoute.getPort())) {
                        if (downloadRouteRepository.existsByDomainAndPort(updatedRoute.getDomain(), updatedRoute.getPort())) {
                            throw new IllegalArgumentException("域名和端口组合已存在: " + updatedRoute.getDomain() + ":" + updatedRoute.getPort());
                        }
                    }
                    
                    existingRoute.setDomain(updatedRoute.getDomain());
                    existingRoute.setProtocol(updatedRoute.getProtocol());
                    existingRoute.setPort(updatedRoute.getPort());
                    
                    log.info("更新下载线路: ID={}, URL={}", id, existingRoute.getFullUrl());
                    return downloadRouteRepository.save(existingRoute);
                })
                .orElseThrow(() -> new IllegalArgumentException("下载线路不存在: ID=" + id));
    }
    
    /**
     * 删除下载线路
     */
    @Transactional
    public void deleteRoute(Long id) {
        if (!downloadRouteRepository.existsById(id)) {
            throw new IllegalArgumentException("下载线路不存在: ID=" + id);
        }
        
        log.info("删除下载线路: ID={}", id);
        downloadRouteRepository.deleteById(id);
    }
    
    /**
     * 根据域名搜索下载线路
     */
    public List<DownloadRoute> searchByDomain(String domain) {
        return downloadRouteRepository.findByDomainContainingIgnoreCase(domain);
    }
    
    /**
     * 根据协议筛选下载线路
     */
    public List<DownloadRoute> getRoutesByProtocol(DownloadRoute.Protocol protocol) {
        return downloadRouteRepository.findByProtocol(protocol);
    }
    
    /**
     * 根据权重随机选择一个下载线路
     */
    public Optional<DownloadRoute> selectRouteByWeight() {
        List<DownloadRoute> routes = getAllRoutes();
        if (routes.isEmpty()) {
            return Optional.empty();
        }
        
        // 计算总权重
        int totalWeight = routes.stream()
                .mapToInt(DownloadRoute::getWeight)
                .sum();
        
        if (totalWeight == 0) {
            return Optional.empty();
        }
        
        // 生成随机数
        Random random = new Random();
        int randomWeight = random.nextInt(totalWeight);
        
        // 根据权重选择线路
        int currentWeight = 0;
        for (DownloadRoute route : routes) {
            currentWeight += route.getWeight();
            if (randomWeight < currentWeight) {
                log.info("根据权重选择线路: {} (权重: {})", route.getFullUrl(), route.getWeight());
                return Optional.of(route);
            }
        }
        
        // 如果没有选中任何线路，返回第一个
        return Optional.empty();
    }
    
    /**
     * 基于性能统计智能选择下载线路
     * 优先选择带宽高、响应时间短、成功率高的线路
     */
    public Optional<DownloadRoute> selectRouteByPerformance() {
        List<DownloadRoute> routes = getAllRoutes();
        if (routes.isEmpty()) {
            return Optional.empty();
        }
        
        // 获取所有线路的性能统计
        List<Map<String, Object>> performanceStats = downloadStatisticsService.getAllRoutePerformanceStatistics();
        Map<Long, Map<String, Object>> statsMap = performanceStats.stream()
                .collect(Collectors.toMap(
                    stats -> (Long) stats.get("routeId"),
                    stats -> stats
                ));
        
        // 计算每个线路的综合评分
        Map<DownloadRoute, Double> routeScores = routes.stream()
                .collect(Collectors.toMap(
                    route -> route,
                    route -> {
                        Map<String, Object> stats = statsMap.get(route.getId());
                        if (stats == null || (Long) stats.get("totalRequests") < 5) {
                            // 如果没有足够的统计数据，使用基础权重
                            return route.getWeight().doubleValue();
                        }
                        
                        // 计算性能评分
                        Double avgBandwidth = (Double) stats.get("avgBandwidth");
                        Double avgResponseTime = (Double) stats.get("avgResponseTime");
                        Double successRate = (Double) stats.get("successRate");
                        
                        // 标准化处理并计算综合评分
                        double bandwidthScore = Math.min(avgBandwidth / 10.0, 1.0) * 100;
                        double responseTimeScore = Math.max(0, 100 - (avgResponseTime / 100.0));
                        double successRateScore = successRate;
                        
                        // 加权计算：带宽40% + 响应时间30% + 成功率30%
                        double performanceScore = (bandwidthScore * 0.4) + (responseTimeScore * 0.3) + (successRateScore * 0.3);
                        
                        // 结合原始权重
                        return performanceScore * (route.getWeight() / 100.0);
                    }
                ));
        
        // 使用加权随机选择，性能好的线路被选中概率更高
        double totalScore = routeScores.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalScore <= 0) {
            // 如果所有评分都为0，回退到基于权重的选择
            return selectRouteByWeight();
        }
        
        Random random = new Random();
        double randomScore = random.nextDouble() * totalScore;
        
        double currentScore = 0;
        for (Map.Entry<DownloadRoute, Double> entry : routeScores.entrySet()) {
            currentScore += entry.getValue();
            if (randomScore <= currentScore) {
                DownloadRoute selectedRoute = entry.getKey();
                log.info("基于性能选择线路: {} (评分: {:.2f})", selectedRoute.getFullUrl(), entry.getValue());
                return Optional.of(selectedRoute);
            }
        }
        
        // 如果没有选中任何线路，返回评分最高的
        return routeScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }
    
    /**
     * 获取线路性能报告
     */
    public Map<String, Object> getRoutePerformanceReport(Long routeId) {
        Map<String, Object> report = new HashMap<>();
        
        Optional<DownloadRoute> routeOpt = getRouteById(routeId);
        if (routeOpt.isEmpty()) {
            return report;
        }
        
        DownloadRoute route = routeOpt.get();
        report.put("route", route);
        report.put("avgBandwidth", downloadStatisticsService.getAverageBandwidth(routeId));
        report.put("avgResponseTime", downloadStatisticsService.getAverageResponseTime(routeId));
        report.put("successRate", downloadStatisticsService.getSuccessRate(routeId));
        report.put("performanceScore", downloadStatisticsService.calculateRouteScore(routeId));
        
        return report;
    }
    
    /**
     * 获取所有线路的性能排名
     */
    public List<Map<String, Object>> getRoutePerformanceRanking() {
        List<DownloadRoute> routes = getAllRoutes();
        
        return routes.stream()
                .map(route -> {
                    Map<String, Object> routeInfo = new HashMap<>();
                    routeInfo.put("route", route);
                    routeInfo.put("avgBandwidth", downloadStatisticsService.getAverageBandwidth(route.getId()));
                    routeInfo.put("avgResponseTime", downloadStatisticsService.getAverageResponseTime(route.getId()));
                    routeInfo.put("successRate", downloadStatisticsService.getSuccessRate(route.getId()));
                    routeInfo.put("performanceScore", downloadStatisticsService.calculateRouteScore(route.getId()));
                    return routeInfo;
                })
                .sorted((a, b) -> Double.compare(
                    (Double) b.get("performanceScore"), 
                    (Double) a.get("performanceScore")
                ))
                .toList();
    }
    
    /**
     * 验证下载线路数据
     */
    public void validateRoute(DownloadRoute route) {
        if (route.getDomain() == null || route.getDomain().trim().isEmpty()) {
            throw new IllegalArgumentException("域名不能为空");
        }
        
        if (route.getPort() == null || route.getPort() < 1 || route.getPort() > 65535) {
            throw new IllegalArgumentException("端口必须在1-65535之间");
        }
        
        if (route.getProtocol() == null) {
            throw new IllegalArgumentException("协议不能为空");
        }
        
        if (route.getWeight() == null || route.getWeight() < 1 || route.getWeight() > 100) {
            throw new IllegalArgumentException("权重必须在1-100之间");
        }
    }
}