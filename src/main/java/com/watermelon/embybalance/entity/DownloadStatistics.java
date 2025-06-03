package com.watermelon.embybalance.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "download_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long routeId; // 关联的下载线路ID
    
    @Column(nullable = false)
    private String clientIp; // 客户端IP地址
    
    @Column(nullable = false)
    private Long downloadSize; // 下载文件大小（字节）
    
    @Column(nullable = false)
    private Long downloadDuration; // 下载耗时（毫秒）
    
    @Column(nullable = false)
    private Double bandwidth; // 计算出的带宽（MB/s）
    
    @Column(nullable = false)
    private Long responseTime; // 响应时间（毫秒）
    
    @Column(nullable = false)
    private Boolean success; // 下载是否成功
    
    @Column(name = "file_path")
    private String filePath; // 下载的文件路径
    
    @Column(name = "user_agent")
    private String userAgent; // 用户代理
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // 计算带宽：文件大小(MB) / 下载时间(秒)
        if (downloadDuration > 0) {
            double fileSizeMB = downloadSize / (1024.0 * 1024.0);
            double durationSeconds = downloadDuration / 1000.0;
            this.bandwidth = fileSizeMB / durationSeconds;
        } else {
            this.bandwidth = 0.0;
        }
    }
    
    /**
     * 手动计算带宽
     */
    public void calculateBandwidth() {
        if (downloadDuration > 0) {
            double fileSizeMB = downloadSize / (1024.0 * 1024.0);
            double durationSeconds = downloadDuration / 1000.0;
            this.bandwidth = fileSizeMB / durationSeconds;
        } else {
            this.bandwidth = 0.0;
        }
    }
}