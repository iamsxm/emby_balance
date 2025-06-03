package com.watermelon.embybalance.controller;

import com.watermelon.embybalance.entity.DownloadRoute;
import com.watermelon.embybalance.service.DownloadRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {
    
    private final DownloadRouteService downloadRouteService;
    
    /**
     * 首页 - 重定向到下载线路管理页面
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/routes";
    }
    
    /**
     * 下载线路管理页面
     */
    @GetMapping("/routes")
    public String routesPage(Model model, @RequestParam(required = false) String search) {
        List<DownloadRoute> routes;
        
        if (search != null && !search.trim().isEmpty()) {
            routes = downloadRouteService.searchByDomain(search.trim());
            model.addAttribute("search", search.trim());
        } else {
            routes = downloadRouteService.getAllRoutes();
        }
        
        model.addAttribute("routes", routes);
        model.addAttribute("protocols", DownloadRoute.Protocol.values());
        return "routes";
    }
    
    /**
     * 新增下载线路页面
     */
    @GetMapping("/routes/new")
    @PreAuthorize("isAuthenticated()")
    public String newRoutePage(Model model) {
        model.addAttribute("route", new DownloadRoute());
        model.addAttribute("protocols", DownloadRoute.Protocol.values());
        model.addAttribute("isEdit", false);
        return "route-form";
    }
    
    /**
     * 编辑下载线路页面
     */
    @GetMapping("/routes/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String editRoutePage(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return downloadRouteService.getRouteById(id)
                .map(route -> {
                    model.addAttribute("route", route);
                    model.addAttribute("protocols", DownloadRoute.Protocol.values());
                    model.addAttribute("isEdit", true);
                    return "route-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "下载线路不存在");
                    return "redirect:/routes";
                });
    }
    
    /**
     * 保存下载线路（新增或更新）
     */
    @PostMapping("/routes/save")
    @PreAuthorize("isAuthenticated()")
    public String saveRoute(@ModelAttribute DownloadRoute route, 
                           @RequestParam(required = false) Long id,
                           RedirectAttributes redirectAttributes) {
        try {
            downloadRouteService.validateRoute(route);
            
            if (id != null) {
                // 更新
                downloadRouteService.updateRoute(id, route);
                redirectAttributes.addFlashAttribute("success", "下载线路更新成功");
            } else {
                // 新增
                downloadRouteService.createRoute(route);
                redirectAttributes.addFlashAttribute("success", "下载线路创建成功");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (id != null) {
                return "redirect:/routes/" + id + "/edit";
            } else {
                return "redirect:/routes/new";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "操作失败：" + e.getMessage());
        }
        
        return "redirect:/routes";
    }
    
    /**
     * 删除下载线路
     */
    @PostMapping("/routes/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    public String deleteRoute(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            downloadRouteService.deleteRoute(id);
            redirectAttributes.addFlashAttribute("success", "下载线路删除成功");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "删除失败：" + e.getMessage());
        }
        
        return "redirect:/routes";
    }
}