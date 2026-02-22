# AI Agent 扩展功能实现指南

## 一、自定义 Advisor（权限校验、违禁词校验）

### 1.1 权限校验 Advisor

创建 `AuthorizationAdvisor.java`：

```java
package com.zpark.zcwaiagent.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;

public class AuthorizationAdvisor implements CallAroundAdvisor {
    
    private static final Logger log = LoggerFactory.getLogger(AuthorizationAdvisor.class);

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        // 从请求中获取用户信息
        String userId = (String) request.adviseContext().get("userId");
        
        log.info("权限校验 - 用户ID: {}", userId);
        
        // 权限校验逻辑
        if (userId == null || !hasPermission(userId)) {
            throw new SecurityException("用户无权限访问 AI 服务");
        }
        
        // 通过校验，继续执行
        return chain.nextAroundCall(request);
    }
    
    private boolean hasPermission(String userId) {
        // 实现你的权限校验逻辑
        // 可以查询数据库、Redis 等
        return true;
    }

    @Override
    public String getName() {
        return "AuthorizationAdvisor";
    }

    @Override
    public int getOrder() {
        return 100; // 优先级，数字越小越先执行
    }
}
```

### 1.2 违禁词校验 Advisor

创建 `SensitiveWordAdvisor.java`：

```java
package com.zpark.zcwaiagent.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;

import java.util.Arrays;
import java.util.List;

public class SensitiveWordAdvisor implements CallAroundAdvisor {
    
    private static final Logger log = LoggerFactory.getLogger(SensitiveWordAdvisor.class);
    
    // 违禁词列表（实际应该从配置文件或数据库加载）
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
        "暴力", "色情", "赌博"
    );

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        String userText = request.userText();
        
        log.info("违禁词校验 - 用户输入: {}", userText);
        
        // 检查违禁词
        for (String word : SENSITIVE_WORDS) {
            if (userText.contains(word)) {
                log.warn("检测到违禁词: {}", word);
                throw new IllegalArgumentException("输入内容包含违禁词: " + word);
            }
        }
        
        // 通过校验，继续执行
        return chain.nextAroundCall(request);
    }

    @Override
    public String getName() {
        return "SensitiveWordAdvisor";
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
```

### 1.3 在 LoveApp 中使用

```java
chatClient = ChatClient.builder(chatModel)
    .defaultSystem(SYSTEM_PROMPT)
    .defaultAdvisors(
        new AuthorizationAdvisor(),      // 权限校验
        new SensitiveWordAdvisor(),      // 违禁词校验
        new MessageChatMemoryAdvisor(chatMemory),
        new MyLoggerAdvisor()
    )
    .build();
```

---

## 二、自定义对话记忆（MySQL/Redis 持久化）

### 2.1 MySQL 实现

创建 `MySQLChatMemory.java`：

```java
package com.zpark.zcwaiagent.chatmemory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class MySQLChatMemory implements ChatMemory {
    
    private final JdbcTemplate jdbcTemplate;
    
    public MySQLChatMemory(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        // 将消息序列化并保存到 MySQL
        String sql = "INSERT INTO chat_history (conversation_id, messages, created_at) VALUES (?, ?, NOW())";
        String messagesJson = serializeMessages(messages);
        jdbcTemplate.update(sql, conversationId, messagesJson);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        // 从 MySQL 读取最近 N 条消息
        String sql = "SELECT messages FROM chat_history WHERE conversation_id = ? ORDER BY created_at DESC LIMIT ?";
        String messagesJson = jdbcTemplate.queryForObject(sql, String.class, conversationId, lastN);
        return deserializeMessages(messagesJson);
    }

    @Override
    public void clear(String conversationId) {
        String sql = "DELETE FROM chat_history WHERE conversation_id = ?";
        jdbcTemplate.update(sql, conversationId);
    }
    
    private String serializeMessages(List<Message> messages) {
        // 使用 Jackson 或其他工具序列化
        return ""; // 实现序列化逻辑
    }
    
    private List<Message> deserializeMessages(String json) {
        // 反序列化
        return List.of(); // 实现反序列化逻辑
    }
}
```

**数据库表结构**：

```sql
CREATE TABLE chat_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    messages TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id)
);
```

### 2.2 Redis 实现

创建 `RedisChatMemory.java`：

```java
package com.zpark.zcwaiagent.chatmemory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class RedisChatMemory implements ChatMemory {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final long expireTime = 24; // 24小时过期
    
    public RedisChatMemory(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = "chat:history:" + conversationId;
        redisTemplate.opsForList().rightPushAll(key, messages.toArray());
        redisTemplate.expire(key, expireTime, TimeUnit.HOURS);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        String key = "chat:history:" + conversationId;
        long size = redisTemplate.opsForList().size(key);
        long start = Math.max(0, size - lastN);
        return (List<Message>) (List<?>) redisTemplate.opsForList().range(key, start, -1);
    }

    @Override
    public void clear(String conversationId) {
        String key = "chat:history:" + conversationId;
        redisTemplate.delete(key);
    }
}
```

### 2.3 配置切换

在 `application.yml` 中配置：

```yaml
chat:
  memory:
    type: redis  # 可选: memory, mysql, redis
```

---

## 三、Prompt 模板管理

### 3.1 创建模板文件

在 `src/main/resources/prompts/` 目录下创建模板文件：

**love-expert.txt**:
```
你是一位专业的恋爱心理专家，名字叫{expertName}。
用户信息：
- 姓名：{userName}
- 情感状态：{relationshipStatus}
- 问题描述：{problem}

请根据用户的情况，提供专业的建议。
```

### 3.2 模板加载器

创建 `PromptTemplateLoader.java`：

```java
package com.zpark.zcwaiagent.prompt;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Component
public class PromptTemplateLoader {
    
    private final Map<String, String> templateCache = new HashMap<>();
    
    public String loadTemplate(String templateName) throws IOException {
        if (templateCache.containsKey(templateName)) {
            return templateCache.get(templateName);
        }
        
        ClassPathResource resource = new ClassPathResource("prompts/" + templateName + ".txt");
        String content = new String(Files.readAllBytes(resource.getFile().toPath()));
        templateCache.put(templateName, content);
        return content;
    }
    
    public String fillTemplate(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
```

### 3.3 使用示例

```java
@Resource
private PromptTemplateLoader templateLoader;

public String doChatWithTemplate(String userName, String problem, String chatId) {
    // 加载模板
    String template = templateLoader.loadTemplate("love-expert");
    
    // 填充变量
    Map<String, String> variables = new HashMap<>();
    variables.put("expertName", "李心理");
    variables.put("userName", userName);
    variables.put("relationshipStatus", "恋爱中");
    variables.put("problem", problem);
    
    String systemPrompt = templateLoader.fillTemplate(template, variables);
    
    // 使用填充后的 prompt
    return chatClient.prompt()
        .system(systemPrompt)
        .user(problem)
        .call()
        .content();
}
```

---

## 四、多模态对话助手（图片解析）

### 4.1 添加依赖

在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.11.0</version>
</dependency>
```

### 4.2 创建多模态助手

创建 `MultimodalApp.java`：

```java
package com.zpark.zcwaiagent.app;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.util.Base64;

@Component
public class MultimodalApp {
    
    private final ChatClient chatClient;
    
    public MultimodalApp(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }
    
    /**
     * 解析图片内容
     */
    public String analyzeImage(String imagePath, String question) throws IOException {
        // 读取图片并转换为 Base64
        ClassPathResource resource = new ClassPathResource(imagePath);
        byte[] imageBytes = resource.getInputStream().readAllBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        
        // 构建多模态请求
        return chatClient.prompt()
            .user(userSpec -> userSpec
                .text(question)
                .media(new Media(MimeTypeUtils.IMAGE_PNG, base64Image))
            )
            .call()
            .content();
    }
    
    /**
     * 通过 URL 解析图片
     */
    public String analyzeImageByUrl(String imageUrl, String question) {
        return chatClient.prompt()
            .user(userSpec -> userSpec
                .text(question)
                .media(new Media(MimeTypeUtils.IMAGE_PNG, imageUrl))
            )
            .call()
            .content();
    }
}
```

### 4.3 创建 REST 接口

创建 `MultimodalController.java`：

```java
package com.zpark.zcwaiagent.controller;

import com.zpark.zcwaiagent.app.MultimodalApp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/multimodal")
@Tag(name = "多模态对话")
public class MultimodalController {
    
    @Resource
    private MultimodalApp multimodalApp;
    
    @PostMapping("/analyze-image")
    @Operation(summary = "上传图片并分析")
    public String analyzeImage(
        @RequestParam("file") MultipartFile file,
        @RequestParam("question") String question
    ) throws IOException {
        byte[] imageBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        
        // 调用 AI 分析
        return multimodalApp.analyzeImage(base64Image, question);
    }
    
    @PostMapping("/analyze-by-url")
    @Operation(summary = "通过 URL 分析图片")
    public String analyzeByUrl(
        @RequestParam("imageUrl") String imageUrl,
        @RequestParam("question") String question
    ) {
        return multimodalApp.analyzeImageByUrl(imageUrl, question);
    }
}
```

---

## 五、配置示例

### 5.1 application.yml 完整配置

```yaml
spring:
  application:
    name: zcw-ai-agent
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-vl-plus  # 支持多模态的模型
  
  # MySQL 配置
  datasource:
    url: jdbc:mysql://localhost:3306/ai_agent?useUnicode=true&characterEncoding=utf8
    username: root
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # Redis 配置
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD}
    database: 0

# 自定义配置
chat:
  memory:
    type: redis  # memory, mysql, redis
  advisor:
    auth-enabled: true
    sensitive-word-enabled: true
  prompt:
    template-path: classpath:prompts/
```

---

## 六、测试示例

### 6.1 测试 Advisor

```java
@Test
void testAdvisorChain() {
    String chatId = UUID.randomUUID().toString();
    
    // 测试违禁词拦截
    assertThrows(IllegalArgumentException.class, () -> {
        loveApp.doChat("我想了解暴力内容", chatId);
    });
    
    // 正常对话
    String result = loveApp.doChat("你好", chatId);
    assertNotNull(result);
}
```

### 6.2 测试多模态

```java
@Test
void testImageAnalysis() throws IOException {
    String result = multimodalApp.analyzeImage(
        "images/test.png", 
        "这张图片里有什么？"
    );
    System.out.println("AI 分析结果: " + result);
    assertFalse(result.isEmpty());
}
```

---

## 七、部署建议

1. **开发环境**: 使用 InMemoryChatMemory
2. **测试环境**: 使用 Redis
3. **生产环境**: 使用 MySQL + Redis（MySQL 做持久化，Redis 做缓存）

## 八、注意事项

1. 违禁词列表应该从配置中心或数据库动态加载
2. 图片上传需要限制大小（建议 5MB 以内）
3. 对话历史需要定期清理，避免数据库膨胀
4. 敏感信息（API Key、密码）使用环境变量
5. 生产环境建议使用对象存储（OSS）保存图片

---

完成！这份文档涵盖了你提到的四个扩展功能的完整实现方案。
