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

@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadRouteService {
    
    private final DownloadRouteRepository downloadRouteRepository;
    
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
        return Optional.of(routes.get(0));
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