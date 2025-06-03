package com.watermelon.embybalance.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.watermelon.embybalance.service.EmbyProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class EmbyProxyController {

    private final ObjectMapper objectMapper;
    private final EmbyProxyService embyProxyService;

    @PostMapping(value = "/emby/Items/{itemId}/PlaybackInfo", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<Object> handlePlaybackInfo(
            @PathVariable("itemId") String itemId,
            @RequestBody(required = false)  String requestBodyString,
            @RequestParam Map<String, String> queryParams,
            @RequestHeader Map<String, String> headers) {
        log.info("收到PlaybackInfo请求,itemId={}", itemId);
        
        // 记录请求头信息
        log.info("请求头信息: {}", headers);
        
        // 记录查询参数
        log.info("查询参数: {}", queryParams);
        
        // 记录请求体
        log.info("原始请求体: {}", requestBodyString);
        
        Map<String, Object> requestBody = new HashMap<>();
        if (requestBodyString != null && !requestBodyString.isEmpty()) {
            try {
                // 尝试将请求体解析为JSON
                requestBody = objectMapper.readValue(requestBodyString, new TypeReference<Map<String, Object>>() {
                });
                log.info("解析后的请求体: {}", requestBody);
            } catch (Exception e) {
                // 如果解析失败，将原始文本作为"content"字段
                requestBody.put("content", requestBodyString);
                log.warn("请求体解析失败，作为content字段处理: {}", e.getMessage());
            }
        }

        ResponseEntity<Object> response = embyProxyService.processPlaybackInfoRequest(itemId, requestBody, queryParams, headers);
        
        // 记录响应状态和头信息
        log.info("响应状态码: {}", response.getStatusCode());
        log.info("响应头信息: {}", response.getHeaders());
        
        // 记录响应体（注意可能很大，考虑是否需要完整记录）
        if (response.getBody() != null) {
            try {
                String responseBodyStr = objectMapper.writeValueAsString(response.getBody());
                // 如果响应体太大，可以只记录一部分
                if (responseBodyStr.length() > 1000) {
                    log.info("响应体(截断): {}...", responseBodyStr.substring(0, 1000));
                } else {
                    log.info("响应体: {}", responseBodyStr);
                }
            } catch (Exception e) {
                log.warn("响应体序列化失败: {}", e.getMessage());
            }
        }
        
        return response;
    }

    @GetMapping(value = "/emby/videos/{itemId}/original.{container}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<Object> handleVideosDownload(
            @PathVariable("itemId") String itemId,
            @PathVariable("container") String container,
            @RequestBody(required = false)  String requestBodyString,
            @RequestParam Map<String, String> queryParams,
            @RequestHeader Map<String, String> headers) {

        log.info("收到videos请求,itemId={},container={}", itemId, container);
        //返回302  重定向
        return ResponseEntity.status(302).header("Location", "https://www.w3schools.com/html/movie.mp4").build();
    }
}