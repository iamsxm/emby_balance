package com.watermelon.embybalance.controller;

import com.watermelon.embybalance.service.LinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LinkController {

    @Autowired
    private LinkService linkService;

    @GetMapping("/getLink")
    public ResponseEntity<?> getLink(@RequestParam String path) {
        try {
            String link = linkService.getOrCreateLink(path);
            Map<String, String> response = new HashMap<>();
            response.put("url", link);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}