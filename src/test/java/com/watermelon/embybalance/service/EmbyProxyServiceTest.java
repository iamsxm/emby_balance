package com.watermelon.embybalance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.profiles.active=test")
class EmbyProxyServiceTest {

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private ObjectMapper objectMapper;

    @Autowired
    private EmbyProxyService embyProxyService;
    
    @Value("${emby.server.url}")
    private String embyServerUrl;
    
    @Value("${emby.api.key}")
    private String embyApiKey;
    
    @Value("${path.modification.prefix}")
    private String pathModificationPrefix;

    @Test
    void processPlaybackInfoRequest_success() throws Exception {
        // 准备测试数据
        String itemId = "123456";
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        // 模拟Emby服务器响应
        String responseBody = "{\"MediaSources\":[{\"Path\":\"/movies/test.mp4\"}]}";
        ResponseEntity<String> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

        // 模拟修改后的响应
        String modifiedResponseBody = "{\"MediaSources\":[{\"Path\":\"/modified/movies/test.mp4\"}]}";
        JsonNode mockRootNode = Mockito.mock(JsonNode.class);
        ArrayNode mockArrayNode = Mockito.mock(ArrayNode.class);
        ObjectNode mockObjectNode = Mockito.mock(ObjectNode.class);
        JsonNode mockPathNode = Mockito.mock(JsonNode.class);

        // 配置Mock行为
        when(restTemplate.exchange(any(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(mockResponse);
        
        when(objectMapper.readTree(responseBody)).thenReturn(mockRootNode);
        when(mockRootNode.has("MediaSources")).thenReturn(true);
        when(mockRootNode.get("MediaSources")).thenReturn(mockArrayNode);
        when(mockArrayNode.isArray()).thenReturn(true);
        when(mockArrayNode.size()).thenReturn(1);
        when(mockArrayNode.get(0)).thenReturn(mockObjectNode);
        when(mockObjectNode.has("Path")).thenReturn(true);
        when(mockObjectNode.get("Path")).thenReturn(mockPathNode);
        when(mockPathNode.asText()).thenReturn("/movies/test.mp4");
        when(objectMapper.writeValueAsString(mockRootNode)).thenReturn(modifiedResponseBody);
        when(objectMapper.readValue(modifiedResponseBody, Object.class)).thenReturn(new HashMap<String, Object>());

        // 执行测试
        ResponseEntity<Object> result = embyProxyService.processPlaybackInfoRequest(itemId, requestBody, queryParams, headers);

        // 验证结果
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        
        // 验证RestTemplate被正确调用
        Mockito.verify(restTemplate).exchange(any(), eq(HttpMethod.POST), any(), eq(String.class));
        
        // 验证Path修改逻辑被调用
        Mockito.verify(objectMapper).readTree(responseBody);
        Mockito.verify(mockObjectNode).put(eq("Path"), eq(pathModificationPrefix + "/movies/test.mp4"));
        Mockito.verify(objectMapper).writeValueAsString(mockRootNode);
    }

    @Test
    void processPlaybackInfoRequest_no_response() throws Exception {
        // 准备测试数据
        String itemId = "123456";
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> headers = new HashMap<>();

        // 模拟Emby服务器返回空响应
        ResponseEntity<String> mockResponse = new ResponseEntity<>("", HttpStatus.OK);
        when(restTemplate.exchange(any(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(mockResponse);
        when(objectMapper.readValue("", Object.class)).thenReturn(new HashMap<String, Object>());

        // 执行测试
        ResponseEntity<Object> result = embyProxyService.processPlaybackInfoRequest(itemId, requestBody, queryParams, headers);

        // 验证结果
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        
        // 验证没有进行Path修改
        Mockito.verify(objectMapper, Mockito.never()).readTree(anyString());
    }

    @Test
    void processPlaybackInfoRequest_error() throws Exception {
        // 准备测试数据
        String itemId = "123456";
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> headers = new HashMap<>();

        // 模拟异常
        when(restTemplate.exchange(any(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenThrow(new RuntimeException("测试异常"));

        // 执行测试
        ResponseEntity<Object> result = embyProxyService.processPlaybackInfoRequest(itemId, requestBody, queryParams, headers);

        // 验证结果
        assertNotNull(result);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, String> errorBody = (Map<String, String>) result.getBody();
        assertNotNull(errorBody);
        assertEquals("测试异常", errorBody.get("error"));
    }
}