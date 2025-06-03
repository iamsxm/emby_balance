package com.watermelon.embybalance.controller;

import com.watermelon.embybalance.entity.DownloadRoute;
import com.watermelon.embybalance.service.DownloadRouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/download-routes")
@RequiredArgsConstructor
@Slf4j
public class DownloadRouteController {
    
    private final DownloadRouteService downloadRouteService;
    
    /**
     * 获取所有下载线路
     */
    @GetMapping
    public ResponseEntity<List<DownloadRoute>> getAllRoutes() {
        List<DownloadRoute> routes = downloadRouteService.getAllRoutes();
        return ResponseEntity.ok(routes);
    }
    
    /**
     * 根据ID获取下载线路
     */
    @GetMapping("/{id}")
    public ResponseEntity<DownloadRoute> getRouteById(@PathVariable Long id) {
        return downloadRouteService.getRouteById(id)
                .map(route -> ResponseEntity.ok(route))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 创建新的下载线路
     */
    @PostMapping
    public ResponseEntity<?> createRoute(@RequestBody DownloadRoute route) {
        try {
            downloadRouteService.validateRoute(route);
            DownloadRoute createdRoute = downloadRouteService.createRoute(route);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRoute);
        } catch (IllegalArgumentException e) {
            log.warn("创建下载线路失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("创建下载线路时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "服务器内部错误"));
        }
    }
    
    /**
     * 更新下载线路
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoute(@PathVariable Long id, @RequestBody DownloadRoute route) {
        try {
            downloadRouteService.validateRoute(route);
            DownloadRoute updatedRoute = downloadRouteService.updateRoute(id, route);
            return ResponseEntity.ok(updatedRoute);
        } catch (IllegalArgumentException e) {
            log.warn("更新下载线路失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("更新下载线路时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "服务器内部错误"));
        }
    }
    
    /**
     * 删除下载线路
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoute(@PathVariable Long id) {
        try {
            downloadRouteService.deleteRoute(id);
            return ResponseEntity.ok(Map.of("message", "下载线路删除成功"));
        } catch (IllegalArgumentException e) {
            log.warn("删除下载线路失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("删除下载线路时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "服务器内部错误"));
        }
    }
    
    /**
     * 根据域名搜索下载线路
     */
    @GetMapping("/search")
    public ResponseEntity<List<DownloadRoute>> searchRoutes(@RequestParam String domain) {
        List<DownloadRoute> routes = downloadRouteService.searchByDomain(domain);
        return ResponseEntity.ok(routes);
    }
    
    /**
     * 根据协议筛选下载线路
     */
    @GetMapping("/filter")
    public ResponseEntity<List<DownloadRoute>> filterRoutes(@RequestParam DownloadRoute.Protocol protocol) {
        List<DownloadRoute> routes = downloadRouteService.getRoutesByProtocol(protocol);
        return ResponseEntity.ok(routes);
    }
}