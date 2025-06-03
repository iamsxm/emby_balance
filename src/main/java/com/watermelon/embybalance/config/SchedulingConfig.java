package com.watermelon.embybalance.config;

import com.watermelon.embybalance.service.DownloadStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulingConfig {
    
    private final DownloadStatisticsService downloadStatisticsService;
    
    /**
     * 每天凌晨2点清理过期的统计数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldStatistics() {
        log.info("开始执行定时清理统计数据任务");
        try {
            downloadStatisticsService.cleanupOldStatistics();
            log.info("定时清理统计数据任务完成");
        } catch (Exception e) {
            log.error("定时清理统计数据任务失败", e);
        }
    }
}