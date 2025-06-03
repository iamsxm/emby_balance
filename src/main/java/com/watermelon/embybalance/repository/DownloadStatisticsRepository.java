package com.watermelon.embybalance.repository;

import com.watermelon.embybalance.entity.DownloadStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DownloadStatisticsRepository extends JpaRepository<DownloadStatistics, Long> {
    
    /**
     * 根据线路ID查找统计记录
     */
    List<DownloadStatistics> findByRouteId(Long routeId);
    
    /**
     * 根据客户端IP查找统计记录
     */
    List<DownloadStatistics> findByClientIp(String clientIp);
    
    /**
     * 查找指定时间范围内的统计记录
     */
    List<DownloadStatistics> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找指定线路在指定时间范围内的统计记录
     */
    List<DownloadStatistics> findByRouteIdAndCreatedAtBetween(Long routeId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 计算指定线路在指定时间范围内的平均带宽
     */
    @Query("SELECT AVG(ds.bandwidth) FROM DownloadStatistics ds WHERE ds.routeId = :routeId AND ds.success = true AND ds.createdAt >= :startTime")
    Double calculateAverageBandwidthByRoute(@Param("routeId") Long routeId, @Param("startTime") LocalDateTime startTime);
    
    /**
     * 计算指定线路在指定时间范围内的平均响应时间
     */
    @Query("SELECT AVG(ds.responseTime) FROM DownloadStatistics ds WHERE ds.routeId = :routeId AND ds.success = true AND ds.createdAt >= :startTime")
    Double calculateAverageResponseTimeByRoute(@Param("routeId") Long routeId, @Param("startTime") LocalDateTime startTime);
    
    /**
     * 获取指定线路的成功率
     */
    @Query("SELECT (COUNT(ds) * 100.0 / (SELECT COUNT(ds2) FROM DownloadStatistics ds2 WHERE ds2.routeId = :routeId AND ds2.createdAt >= :startTime)) " +
           "FROM DownloadStatistics ds WHERE ds.routeId = :routeId AND ds.success = true AND ds.createdAt >= :startTime")
    Double calculateSuccessRateByRoute(@Param("routeId") Long routeId, @Param("startTime") LocalDateTime startTime);
    
    /**
     * 获取所有线路的性能统计（最近24小时）
     */
    @Query("SELECT ds.routeId, AVG(ds.bandwidth) as avgBandwidth, AVG(ds.responseTime) as avgResponseTime, " +
           "(COUNT(CASE WHEN ds.success = true THEN 1 END) * 100.0 / COUNT(ds)) as successRate, COUNT(ds) as totalRequests " +
           "FROM DownloadStatistics ds WHERE ds.createdAt >= :startTime " +
           "GROUP BY ds.routeId " +
           "ORDER BY avgBandwidth DESC")
    List<Object[]> getRoutePerformanceStatistics(@Param("startTime") LocalDateTime startTime);
    
    /**
     * 删除指定时间之前的统计记录（用于数据清理）
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffTime);
    
    /**
     * 统计指定IP地址的下载次数
     */
    @Query("SELECT COUNT(ds) FROM DownloadStatistics ds WHERE ds.clientIp = :clientIp AND ds.createdAt >= :startTime")
    Long countDownloadsByClientIp(@Param("clientIp") String clientIp, @Param("startTime") LocalDateTime startTime);
}