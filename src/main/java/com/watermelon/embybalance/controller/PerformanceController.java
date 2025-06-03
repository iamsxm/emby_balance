package com.watermelon.embybalance.controller;

import com.watermelon.embybalance.service.DownloadRouteService;
import com.watermelon.embybalance.service.DownloadStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@Slf4j
public class PerformanceController {
    
    private final DownloadRouteService downloadRouteService;
    private final DownloadStatisticsService downloadStatisticsService;
    
    /**
     * 获取所有线路的性能排名
     */
    @GetMapping("/ranking")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getPerformanceRanking() {
        try {
            List<Map<String, Object>> ranking = downloadRouteService.getRoutePerformanceRanking();
            return ResponseEntity.ok(ranking);
        } catch (Exception e) {
            log.error("获取性能排名失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取指定线路的性能报告
     */
    @GetMapping("/route/{routeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getRoutePerformance(@PathVariable Long routeId) {
        try {
            Map<String, Object> report = downloadRouteService.getRoutePerformanceReport(routeId);
            if (report.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("获取线路性能报告失败: routeId={}", routeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取所有线路的性能统计概览
     */
    @GetMapping("/overview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getPerformanceOverview() {
        try {
            List<Map<String, Object>> overview = downloadStatisticsService.getAllRoutePerformanceStatistics();
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            log.error("获取性能概览失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 手动清理过期统计数据
     */
    @PostMapping("/cleanup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> cleanupStatistics() {
        try {
            downloadStatisticsService.cleanupOldStatistics();
            return ResponseEntity.ok(Map.of("message", "统计数据清理完成"));
        } catch (Exception e) {
            log.error("清理统计数据失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "清理失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取客户端下载统计
     */
    @GetMapping("/client/{clientIp}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getClientStatistics(@PathVariable String clientIp, 
                                                @RequestParam(defaultValue = "24") int hours) {
        try {
            var statistics = downloadStatisticsService.getClientStatistics(clientIp, hours);
            return ResponseEntity.ok(Map.of(
                "clientIp", clientIp,
                "hours", hours,
                "statistics", statistics,
                "totalDownloads", statistics.size(),
                "successfulDownloads", statistics.stream().mapToLong(stat -> stat.getSuccess() ? 1 : 0).sum(),
                "averageBandwidth", statistics.stream()
                        .filter(stat -> stat.getSuccess())
                        .mapToDouble(stat -> stat.getBandwidth())
                        .average().orElse(0.0)
            ));
        } catch (Exception e) {
            log.error("获取客户端统计失败: clientIp={}", clientIp, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "获取统计失败: " + e.getMessage()));
        }
    }
}