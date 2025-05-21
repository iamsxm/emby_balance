package com.watermelon.embybalance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbyProxyService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${emby.server.url}")
    private String embyServerUrl;

    @Value("${emby.api.key}")
    private String embyApiKey;
    
    @Value("${path.modification.prefix:}")
    private String pathModificationPrefix;

    public ResponseEntity<Object> processPlaybackInfoRequest(String itemId, 
                                                      Map<String, Object> requestBody, 
                                                      Map<String, String> queryParams, 
                                                      Map<String, String> headers) {
        try {
            // 构建请求URL
            String url = embyServerUrl + "/emby/Items/" + itemId + "/PlaybackInfo";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            
            // 添加所有查询参数
            queryParams.forEach(builder::queryParam);
            
            // 确保API密钥被添加
            if (!queryParams.containsKey("api_key")) {
                builder.queryParam("api_key", embyApiKey);
            }
            
            // 记录完整URL
            String fullUrl = builder.build().toUriString();
            log.info("向Emby服务器发送请求: {}", fullUrl);
            
            // 准备请求头
            HttpHeaders requestHeaders = new HttpHeaders();
            headers.forEach((key, value) -> {
                if (!key.equalsIgnoreCase("host") && !key.equalsIgnoreCase("content-length")) {
                    requestHeaders.add(key, value);
                }
            });
            
            // 设置内容类型为application/json
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            log.info("发送到Emby的请求头: {}", requestHeaders);
            
            // 创建请求实体
            HttpEntity<String> requestEntity;
            if (requestBody != null && !requestBody.isEmpty()) {
                // 将Map转换为JSON字符串
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                log.info("发送到Emby的请求体: {}", jsonBody);
                requestEntity = new HttpEntity<>(jsonBody, requestHeaders);
            } else {
                log.info("发送到Emby的请求体为空");
                requestEntity = new HttpEntity<>(requestHeaders);
            }
            
            // 发送请求到Emby服务器
            log.info("开始向Emby服务器发送请求...");
            ResponseEntity<String> response = restTemplate.exchange(
                    builder.build().toUri(),
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            
            // 记录Emby服务器响应
            log.info("Emby服务器响应状态码: {}", response.getStatusCode());
            log.info("Emby服务器响应头: {}", response.getHeaders());
            
            // 记录原始响应体（可能很大，考虑是否需要完整记录）
            String responseBody = response.getBody();
            if (responseBody != null) {
                if (responseBody.length() > 1000) {
                    log.info("Emby服务器原始响应体(截断): {}...", responseBody.substring(0, 1000));
                } else {
                    log.info("Emby服务器原始响应体: {}", responseBody);
                }
            }
            
            // 修改响应中的Path
            log.info("开始修改响应中的Path...");
            String modifiedResponse = modifyPathInResponse(responseBody);
            
            // 记录修改后的响应体
            if (modifiedResponse != null && modifiedResponse.length() > 1000) {
                log.info("修改后的响应体(截断): {}...", modifiedResponse.substring(0, 1000));
            } else {
                log.info("修改后的响应体: {}", modifiedResponse);
            }
            
            // 创建新的响应头，不直接使用原始响应头
            HttpHeaders responseHeaders = new HttpHeaders();
            
            // 复制原始响应头中的重要信息，但不包括Content-Length
            response.getHeaders().forEach((key, values) -> {
                if (!key.equalsIgnoreCase("Content-Length")) {
                    responseHeaders.put(key, values);
                }
            });
            
            // 设置正确的Content-Type
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            // 返回修改后的响应，让Spring自动计算Content-Length
            Object newResponseBody = objectMapper.readValue(modifiedResponse, Object.class);
            return new ResponseEntity<>(newResponseBody, responseHeaders, response.getStatusCode());
            
        } catch (Exception e) {
            log.error("处理PlaybackInfo请求时出错", e);
            // 记录详细的异常堆栈
            log.error("异常详情: ", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    private String modifyPathInResponse(String responseBody) throws JsonProcessingException {
        if (responseBody == null || responseBody.isEmpty()) {
            return responseBody;
        }
        
        JsonNode rootNode = objectMapper.readTree(responseBody);
        
        // 修改MediaSources数组中的Path
        if (rootNode.has("MediaSources") && rootNode.get("MediaSources").isArray()) {
            ArrayNode mediaSources = (ArrayNode) rootNode.get("MediaSources");
            
            for (int i = 0; i < mediaSources.size(); i++) {
                JsonNode mediaSource = mediaSources.get(i);
                //修改协议为Http，
                ((ObjectNode) mediaSource).put("Protocol", "Http");
                //修改IsRemote,SupportsDirectPlay,SupportsDirectStream为true
                ((ObjectNode) mediaSource).put("IsRemote", true);
                ((ObjectNode) mediaSource).put("SupportsDirectPlay", true);
                ((ObjectNode) mediaSource).put("SupportsDirectStream", false);
                //修改SupportsTranscoding为false
                ((ObjectNode) mediaSource).put("SupportsTranscoding", false);
                if (mediaSource.has("Path")) {
                    String originalPath = mediaSource.get("Path").asText();
//                    String modifiedPath = pathModificationPrefix + originalPath;
                    String modifiedPath = "http://46.38.242.30:9090/d/cnd2dkrpqsse7d993?/72小时黄金行动 (2023) {tmdb-1162650} - 1080p.mp4";
                    ((ObjectNode) mediaSource).put("Path", modifiedPath);
                }
                if (mediaSource.has("DirectStreamUrl")) {
                    String originalPath = mediaSource.get("DirectStreamUrl").asText();
//                    String modifiedPath = pathModificationPrefix + originalPath;
                    String modifiedPath = "http://46.38.242.30:9090/d/cnd2dkrpqsse7d993?/72小时黄金行动 (2023) {tmdb-1162650} - 1080p.mp4";
                    ((ObjectNode) mediaSource).put("DirectStreamUrl", modifiedPath);
                }
            }
        }
        
        return objectMapper.writeValueAsString(rootNode);
    }
}