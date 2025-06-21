package com.watermelon.embybalance.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class Cd2Service {

    private static final String CD2_API_URL = "http://127.0.0.1:19798/api/fs/list";
    private final RestTemplate restTemplate = new RestTemplate();

    public String getFileIdByPath(String path) {
        Map<String, String> req = new HashMap<>();
        req.put("path", path);

        ResponseEntity<Map> resp = restTemplate.postForEntity(CD2_API_URL, req, Map.class);
        if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) resp.getBody().get("items");
            if (items != null && !items.isEmpty()) {
                Map<String, Object> file = items.get(0);
                return (String) file.get("file_id");
            }
        }
        return null;
    }
}