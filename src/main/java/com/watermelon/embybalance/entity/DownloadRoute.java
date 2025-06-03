package com.watermelon.embybalance.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "download_routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadRoute {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String domain;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Protocol protocol;
    
    @Column(nullable = false)
    private Integer port;
    
    @Column(nullable = false)
    private Integer weight = 1; // 权重，默认为1
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
    
    public enum Protocol {
        HTTP, HTTPS
    }
    
    /**
     * 获取完整的URL
     */
    public String getFullUrl() {
        return protocol.name().toLowerCase() + "://" + domain + ":" + port;
    }
}