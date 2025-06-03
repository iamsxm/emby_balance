package com.watermelon.embybalance.controller;

import com.watermelon.embybalance.service.DownloadStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {
    
    private final DownloadStatisticsService downloadStatisticsService;
    
    /**
     * 记录下载统计信息（由Nginx调用）
     */
    @PostMapping("/record")
    public ResponseEntity<String> recordDownloadStatistics(
            @RequestBody Map<String, Object> statisticsData,
            HttpServletRequest request) {
        
        try {
            // 从请求体中提取统计数据
            Long routeId = getLongValue(statisticsData, "routeId");
            String clientIp = getStringValue(statisticsData, "clientIp");
            String filePath = getStringValue(statisticsData, "filePath");
            Long downloadSize = getLongValue(statisticsData, "downloadSize");
            Long downloadDuration = getLongValue(statisticsData, "downloadDuration");
            Long responseTime = getLongValue(statisticsData, "responseTime");
            Boolean success = getBooleanValue(statisticsData, "success");
            String userAgent = getStringValue(statisticsData, "userAgent");
            
            // 如果clientIp为空，从请求头中获取
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = getClientIpAddress(request);
            }
            
            // 记录统计信息
            downloadStatisticsService.recordDownloadStatistics(
                routeId != null ? routeId : 0L,
                clientIp,
                filePath,
                downloadSize != null ? downloadSize : 0L,
                downloadDuration != null ? downloadDuration : 0L,
                responseTime != null ? responseTime : 0L,
                success != null ? success : false,
                userAgent
            );
            
            log.debug("记录下载统计: routeId={}, clientIp={}, filePath={}, size={}, duration={}", 
                     routeId, clientIp, filePath, downloadSize, downloadDuration);
            
            return ResponseEntity.ok("统计信息记录成功");
            
        } catch (Exception e) {
            log.error("记录下载统计信息失败", e);
            return ResponseEntity.status(500).body("记录统计信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量记录下载统计信息
     */
    @PostMapping("/record/batch")
    public ResponseEntity<String> recordBatchStatistics(
            @RequestBody Map<String, Object> batchData,
            HttpServletRequest request) {
        
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> statisticsList = 
                (java.util.List<Map<String, Object>>) batchData.get("statistics");
            
            if (statisticsList == null || statisticsList.isEmpty()) {
                return ResponseEntity.badRequest().body("统计数据列表不能为空");
            }
            
            int successCount = 0;
            int failCount = 0;
            
            for (Map<String, Object> statisticsData : statisticsList) {
                try {
                    Long routeId = getLongValue(statisticsData, "routeId");
                    String clientIp = getStringValue(statisticsData, "clientIp");
                    String filePath = getStringValue(statisticsData, "filePath");
                    Long downloadSize = getLongValue(statisticsData, "downloadSize");
                    Long downloadDuration = getLongValue(statisticsData, "downloadDuration");
                    Long responseTime = getLongValue(statisticsData, "responseTime");
                    Boolean success = getBooleanValue(statisticsData, "success");
                    String userAgent = getStringValue(statisticsData, "userAgent");
                    
                    if (clientIp == null || clientIp.isEmpty()) {
                        clientIp = getClientIpAddress(request);
                    }
                    
                    downloadStatisticsService.recordDownloadStatistics(
                        routeId != null ? routeId : 0L,
                        clientIp,
                        filePath,
                        downloadSize != null ? downloadSize : 0L,
                        downloadDuration != null ? downloadDuration : 0L,
                        responseTime != null ? responseTime : 0L,
                        success != null ? success : false,
                        userAgent
                    );
                    
                    successCount++;
                } catch (Exception e) {
                    log.error("批量记录统计信息时出错", e);
                    failCount++;
                }
            }
            
            String message = String.format("批量记录完成: 成功 %d 条, 失败 %d 条", successCount, failCount);
            log.info(message);
            
            return ResponseEntity.ok(message);
            
        } catch (Exception e) {
            log.error("批量记录下载统计信息失败", e);
            return ResponseEntity.status(500).body("批量记录统计信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 安全地从Map中获取String值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 安全地从Map中获取Long值
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法解析Long值: key={}, value={}", key, value);
            return null;
        }
    }
    
    /**
     * 安全地从Map中获取Boolean值
     */
    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        
        return Boolean.parseBoolean(value.toString());
    }
}