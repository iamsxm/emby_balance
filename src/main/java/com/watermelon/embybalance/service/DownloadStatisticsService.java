package com.watermelon.embybalance.service;

import com.watermelon.embybalance.entity.DownloadStatistics;
import com.watermelon.embybalance.repository.DownloadStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadStatisticsService {
    
    private final DownloadStatisticsRepository statisticsRepository;
    
    /**
     * 异步记录下载统计信息
     */
    @Async
    @Transactional
    public void recordDownloadStatistics(Long routeId, String clientIp, String filePath, 
                                        Long downloadSize, Long downloadDuration, 
                                        Long responseTime, Boolean success, String userAgent) {
        try {
            DownloadStatistics statistics = new DownloadStatistics();
            statistics.setRouteId(routeId);
            statistics.setClientIp(clientIp);
            statistics.setFilePath(filePath);
            statistics.setDownloadSize(downloadSize);
            statistics.setDownloadDuration(downloadDuration);
            statistics.setResponseTime(responseTime);
            statistics.setSuccess(success);
            statistics.setUserAgent(userAgent);
            
            // 计算带宽会在@PrePersist中自动执行
            statisticsRepository.save(statistics);
            
            log.info("记录下载统计: 线路ID={}, 客户端IP={}, 带宽={:.2f}MB/s, 成功={}", 
                    routeId, clientIp, statistics.getBandwidth(), success);
        } catch (Exception e) {
            log.error("记录下载统计失败", e);
        }
    }
    
    /**
     * 获取指定线路的平均带宽（最近24小时）
     */
    public Double getAverageBandwidth(Long routeId) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        Double avgBandwidth = statisticsRepository.calculateAverageBandwidthByRoute(routeId, startTime);
        return avgBandwidth != null ? avgBandwidth : 0.0;
    }
    
    /**
     * 获取指定线路的平均响应时间（最近24小时）
     */
    public Double getAverageResponseTime(Long routeId) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        Double avgResponseTime = statisticsRepository.calculateAverageResponseTimeByRoute(routeId, startTime);
        return avgResponseTime != null ? avgResponseTime : 0.0;
    }
    
    /**
     * 获取指定线路的成功率（最近24小时）
     */
    public Double getSuccessRate(Long routeId) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        Double successRate = statisticsRepository.calculateSuccessRateByRoute(routeId, startTime);
        return successRate != null ? successRate : 0.0;
    }
    
    /**
     * 获取所有线路的性能统计
     */
    public List<Map<String, Object>> getAllRoutePerformanceStatistics() {
        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        List<Object[]> results = statisticsRepository.getRoutePerformanceStatistics(startTime);
        
        return results.stream().map(result -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("routeId", result[0]);
            stats.put("avgBandwidth", result[1] != null ? result[1] : 0.0);
            stats.put("avgResponseTime", result[2] != null ? result[2] : 0.0);
            stats.put("successRate", result[3] != null ? result[3] : 0.0);
            stats.put("totalRequests", result[4] != null ? result[4] : 0L);
            return stats;
        }).toList();
    }
    
    /**
     * 计算线路的综合性能评分
     * 评分算法：带宽权重40% + 响应时间权重30% + 成功率权重30%
     */
    public Double calculateRouteScore(Long routeId) {
        Double avgBandwidth = getAverageBandwidth(routeId);
        Double avgResponseTime = getAverageResponseTime(routeId);
        Double successRate = getSuccessRate(routeId);
        
        // 标准化处理
        double bandwidthScore = Math.min(avgBandwidth / 10.0, 1.0) * 100; // 假设10MB/s为满分
        double responseTimeScore = Math.max(0, 100 - (avgResponseTime / 100.0)); // 响应时间越低分数越高
        double successRateScore = successRate;
        
        // 加权计算
        double totalScore = (bandwidthScore * 0.4) + (responseTimeScore * 0.3) + (successRateScore * 0.3);
        
        log.debug("线路{}性能评分: 带宽={:.2f}, 响应时间={:.2f}, 成功率={:.2f}, 总分={:.2f}", 
                routeId, bandwidthScore, responseTimeScore, successRateScore, totalScore);
        
        return totalScore;
    }
    
    /**
     * 获取客户端IP的下载统计
     */
    public List<DownloadStatistics> getClientStatistics(String clientIp, int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        return statisticsRepository.findByClientIp(clientIp).stream()
                .filter(stat -> stat.getCreatedAt().isAfter(startTime))
                .toList();
    }
    
    /**
     * 清理过期的统计数据（保留最近7天的数据）
     */
    @Transactional
    public void cleanupOldStatistics() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
        try {
            statisticsRepository.deleteByCreatedAtBefore(cutoffTime);
            log.info("清理了{}之前的统计数据", cutoffTime);
        } catch (Exception e) {
            log.error("清理统计数据失败", e);
        }
    }
    
    /**
     * 检查客户端是否频繁下载（防止滥用）
     */
    public boolean isClientAbusing(String clientIp, int maxRequestsPerHour) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        Long requestCount = statisticsRepository.countDownloadsByClientIp(clientIp, startTime);
        return requestCount > maxRequestsPerHour;
    }
}