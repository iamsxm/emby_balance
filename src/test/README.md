# Emby代理服务集成测试说明

## 概述

本项目包含了对`EmbyProxyService`的集成测试，用于验证与真实Emby服务器的交互功能，特别是路径修改功能的正确性。

## 测试配置

集成测试使用`application-test.properties`配置文件，该文件位于`src/test/resources`目录下。在运行集成测试前，请确保以下配置项已正确设置：

```properties
# Emby服务器配置
emby.server.url=http://your-emby-server-url
emby.api.key=your-emby-api-key

# Path修改前缀
path.modification.prefix=/modified
```

请将`emby.server.url`和`emby.api.key`替换为您实际环境中的值。

## 运行测试

### 准备工作

1. 确保Emby服务器处于可访问状态
2. 在`EmbyProxyServiceIntegrationTest.java`文件中，将测试用例中的媒体项ID替换为实际存在的ID：

```java
// 将此处替换为实际的媒体项ID
String itemId = "替换为实际的媒体项ID";
```

### 执行测试

可以通过以下方式运行集成测试：

1. 使用IDE（如IntelliJ IDEA或Eclipse）直接运行测试类
2. 使用Maven命令行：

```bash
./mvnw test -Dtest=EmbyProxyServiceIntegrationTest
```

## 测试内容

集成测试包含以下测试用例：

1. `testRealEmbyServerInteraction`：测试与真实Emby服务器的交互，验证路径修改功能
2. `testInvalidItemId`：测试系统对无效媒体项ID的处理

## 注意事项

- 集成测试依赖于外部Emby服务器，请确保网络连接正常
- 测试可能会受到Emby服务器性能和网络延迟的影响
- 如果测试失败，请检查Emby服务器状态和配置参数是否正确