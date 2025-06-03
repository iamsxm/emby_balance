package com.watermelon.embybalance.repository;

import com.watermelon.embybalance.entity.DownloadRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DownloadRouteRepository extends JpaRepository<DownloadRoute, Long> {
    
    /**
     * 根据域名查找下载线路
     */
    List<DownloadRoute> findByDomainContainingIgnoreCase(String domain);
    
    /**
     * 根据协议查找下载线路
     */
    List<DownloadRoute> findByProtocol(DownloadRoute.Protocol protocol);
    
    /**
     * 根据域名和端口查找下载线路
     */
    Optional<DownloadRoute> findByDomainAndPort(String domain, Integer port);
    
    /**
     * 查找所有可用的下载线路，按创建时间排序
     */
    @Query("SELECT dr FROM DownloadRoute dr ORDER BY dr.createdAt DESC")
    List<DownloadRoute> findAllOrderByCreatedAtDesc();
    
    /**
     * 检查域名和端口组合是否已存在
     */
    boolean existsByDomainAndPort(String domain, Integer port);
}