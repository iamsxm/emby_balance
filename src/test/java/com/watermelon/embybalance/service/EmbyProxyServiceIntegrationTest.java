package com.watermelon.embybalance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class EmbyProxyServiceIntegrationTest {

    @Autowired
    private EmbyProxyService embyProxyService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${emby.server.url}")
    private String embyServerUrl;
    
    @Value("${emby.api.key}")
    private String embyApiKey;
    
    @Value("${path.modification.prefix}")
    private String pathModificationPrefix;
    
    /**
     * 集成测试：测试与真实Emby服务器的交互
     * 注意：此测试需要Emby服务器处于可访问状态
     * 测试配置文件中的emby.server.url和emby.api.key需要配置为有效值
     */
    @Test
    void testRealEmbyServerInteraction() throws Exception {
        // 准备测试数据 - 使用一个已知存在的媒体项ID
        // 注意：需要替换为实际环境中存在的媒体项ID
        String itemId = "311994";
        
        // 准备请求参数
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        
        // 执行请求
        ResponseEntity<Object> response = embyProxyService.processPlaybackInfoRequest(
                itemId, requestBody, queryParams, headers);
        
        // 基本验证
        assertNotNull(response, "响应不应为空");
        assertNotNull(response.getBody(), "响应体不应为空");
        
        // 将响应体转换为JsonNode以便检查
        String responseJson = objectMapper.writeValueAsString(response.getBody());
        System.out.println(responseJson);
        JsonNode rootNode = objectMapper.readTree(responseJson);
        
        // 验证响应中包含MediaSources
        assertTrue(rootNode.has("MediaSources"), "响应应包含MediaSources字段");
        assertTrue(rootNode.get("MediaSources").isArray(), "MediaSources应为数组");
        
        // 验证Path是否被正确修改
        JsonNode mediaSources = rootNode.get("MediaSources");
        if (!mediaSources.isEmpty()) {
            for (int i = 0; i < mediaSources.size(); i++) {
                JsonNode mediaSource = mediaSources.get(i);
                if (mediaSource.has("Path")) {
                    String path = mediaSource.get("Path").asText();
                    assertTrue(path.startsWith(pathModificationPrefix), 
                            "Path应以配置的前缀开头: " + pathModificationPrefix);
                }
            }
        }
    }
    
    /**
     * 集成测试：测试无效的媒体项ID
     * 验证系统对错误情况的处理
     */
    @Test
    void testInvalidItemId() {
        // 使用一个无效的媒体项ID
        String invalidItemId = "invalid-id-that-does-not-exist";
        
        // 准备请求参数
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        
        // 执行请求
        ResponseEntity<Object> response = embyProxyService.processPlaybackInfoRequest(
                invalidItemId, requestBody, queryParams, headers);
        
        // 验证响应
        assertNotNull(response, "即使是错误情况，响应也不应为空");
        // 注意：具体的状态码取决于Emby服务器对无效ID的响应方式
        // 可能是404 Not Found或其他错误码
    }
}