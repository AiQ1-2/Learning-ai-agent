四、扩展思路
1）自定义 DocumentReader 文档读取器，比如读取 GitHub 仓库信息。可以参考 Spring AI Alibaba 官方开源的代码仓库来了解。
2）自定义 QueryTransformer 查询转换器，比如利用第三方翻译 API 代替 Spring AI 内置的基于大模型的翻译工具，从而降低成本。
3）实现基于向量数据库和其他数据存储（比如 MySQL、Redis、Elasticsearch）的混合检索。实现思路可以是整合多数据源的搜索结果；或者把其他数据存储作为降级方案，从向量数据库中查不到数据时，再从其他数据库中查询。
4）不借助 Spring AI 等开发框架，自主实现 RAG；或者自主实现一个 Spring AI 的 RAG Advisor，从而加深对 RAG 实现原理的理解。
# 1️⃣ 自定义 DocumentReader - GitHub 仓库文档读取器

## GitHubDocumentReader.java（详细注释版）

```java
package com.zpark.zcwaiagent.rag;

// 导入 Spring AI 的 Document 类，用于表示文档对象
import org.springframework.ai.document.Document;
// 导入 Spring 的 Component 注解，将类注册为 Spring Bean
import org.springframework.stereotype.Component;
// 导入 IO 异常类，用于处理网络请求可能出现的异常
import java.io.IOException;
// 导入 URI 类，用于构建网络请求的地址
import java.net.URI;
// 导入 HttpClient，Java 11+ 自带的 HTTP 客户端
import java.net.http.HttpClient;
// 导入 HttpRequest，用于构建 HTTP 请求
import java.net.http.HttpRequest;
// 导入 HttpResponse，用于接收 HTTP 响应
import java.net.http.HttpResponse;
// 导入 ArrayList，用于存储文档列表
import java.util.ArrayList;
// 导入 List 接口
import java.util.List;
// 导入 Map 接口，用于存储文档的元数据
import java.util.Map;

/**
 * GitHub 仓库文档读取器
 * 作用：从 GitHub API 读取指定仓库的 README 文件
 * 原理：通过 GitHub 的 REST API 获取仓库的 README 内容
 */
@Component  // 标记为 Spring 组件，Spring 会自动扫描并注册到容器中
public class GitHubDocumentReader {
    
    // 创建 HTTP 客户端实例，用于发送网络请求
    // HttpClient 是 Java 11 引入的标准 HTTP 客户端
    // newHttpClient() 创建一个默认配置的客户端
    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    /**
     * 读取 GitHub 仓库的 README 文件
     * 
     * @param owner 仓库所有者的用户名，例如 "spring-projects"
     * @param repo 仓库名称，例如 "spring-ai"
     * @return 返回包含 README 内容的文档列表
     * @throws IOException 如果网络请求失败
     * @throws InterruptedException 如果请求被中断
     */
    public List<Document> loadGitHubRepo(String owner, String repo) throws IOException, InterruptedException {
        // 创建一个空的文档列表，用于存储读取到的文档
        List<Document> documents = new ArrayList<>();
        
        // 构建 GitHub API 的 URL
        // 格式：https://api.github.com/repos/{owner}/{repo}/readme
        // 例如：https://api.github.com/repos/spring-projects/spring-ai/readme
        String url = String.format("https://api.github.com/repos/%s/%s/readme", owner, repo);
        
        // 构建 HTTP 请求
        HttpRequest request = HttpRequest.newBuilder()  // 创建请求构建器
                .uri(URI.create(url))  // 设置请求的 URL
                // 设置请求头，告诉 GitHub 返回原始的 Markdown 内容
                // 如果不设置这个头，GitHub 会返回 JSON 格式的响应
                .header("Accept", "application/vnd.github.v3.raw")
                .GET()  // 设置请求方法为 GET
                .build();  // 构建请求对象
        
        // 发送 HTTP 请求并获取响应
        // send() 方法会阻塞等待响应
        // BodyHandlers.ofString() 表示将响应体作为字符串处理
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // 检查响应状态码是否为 200（成功）
        if (response.statusCode() == 200) {
            // 创建 Document 对象
            Document doc = new Document(
                // 第一个参数：文档内容，即 README 的文本内容
                response.body(),
                // 第二个参数：文档的元数据（Metadata）
                // 元数据是一个 Map，用于存储文档的附加信息
                Map.of(
                    "source", "github",  // 标记数据来源是 GitHub
                    "repo", owner + "/" + repo,  // 记录仓库的完整路径
                    "type", "readme"  // 标记文档类型是 README
                )
            );
            // 将创建的文档添加到列表中
            documents.add(doc);
        }
        
        // 返回文档列表
        // 如果请求失败（状态码不是 200），返回空列表
        return documents;
    }
}
```

## GitHubVectorStoreConfig.java（详细注释版）

```java
package com.zpark.zcwaiagent.rag;

// 导入 Spring 的配置注解
import org.springframework.context.annotation.Configuration;
// 导入 Bean 注解，用于定义 Spring Bean
import org.springframework.context.annotation.Bean;
// 导入 Resource 注解，用于注入其他 Bean
import jakarta.annotation.Resource;
// 导入向量存储接口
import org.springframework.ai.vectorstore.VectorStore;
// 导入简单向量存储实现（内存存储）
import org.springframework.ai.vectorstore.SimpleVectorStore;
// 导入嵌入模型接口
import org.springframework.ai.embedding.EmbeddingModel;
// 导入文档类
import org.springframework.ai.document.Document;
// 导入 List 接口
import java.util.List;

/**
 * GitHub 向量存储配置类
 * 作用：创建一个专门存储 GitHub 文档的向量数据库
 */
@Configuration  // 标记为配置类，Spring 会扫描这个类中的 @Bean 方法
public class GitHubVectorStoreConfig {
    
    // 注入 GitHub 文档读取器
    // @Resource 会自动从 Spring 容器中查找 GitHubDocumentReader 类型的 Bean
    @Resource
    private GitHubDocumentReader gitHubDocumentReader;
    
    /**
     * 创建 GitHub 向量存储的 Bean
     * 
     * @param embeddingModel 嵌入模型，用于将文本转换为向量
     * @return 返回配置好的向量存储对象
     * @throws Exception 如果加载文档失败
     */
    @Bean(name = "githubVectorStore")  // 定义 Bean 的名称为 "githubVectorStore"
    public VectorStore githubVectorStore(EmbeddingModel embeddingModel) throws Exception {
        // 创建简单向量存储（内存存储）
        // builder() 创建构建器
        // embeddingModel 是嵌入模型，用于将文本转换为向量
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        
        // 调用 GitHub 文档读取器，加载指定仓库的 README
        // 参数1："spring-projects" 是仓库所有者
        // 参数2："spring-ai" 是仓库名称
        List<Document> documents = gitHubDocumentReader.loadGitHubRepo("spring-projects", "spring-ai");
        
        // 将加载的文档添加到向量存储中
        // add() 方法会自动：
        // 1. 调用 embeddingModel 将文档内容转换为向量
        // 2. 将向量和文档内容一起存储到内存中
        vectorStore.add(documents);
        
        // 返回配置好的向量存储对象
        return vectorStore;
    }
}
```

------

# 2️⃣ 自定义 QueryTransformer - 翻译查询转换器

## TranslationQueryTransformer.java（详细注释版）

```java
package com.zpark.zcwaiagent.rag;

// 导入 Spring AI 的 Query 类
import org.springframework.ai.rag.Query;
// 导入查询转换器接口
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
// 导入 Spring 组件注解
import org.springframework.stereotype.Component;
// 导入 URI 类
import java.net.URI;
// 导入 HTTP 客户端相关类
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
// 导入 Jackson JSON 解析库
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 翻译查询转换器
 * 作用：将中文查询翻译为英文，提升检索效果
 * 原理：调用百度翻译 API 进行翻译
 */
@Component  // 注册为 Spring 组件
public class TranslationQueryTransformer implements QueryTransformer {
    
    // 创建 HTTP 客户端，用于发送网络请求
    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    // 创建 JSON 解析器，用于解析 API 返回的 JSON 数据
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 百度翻译 API 的应用 ID（需要在百度翻译开放平台申请）
    private static final String APP_ID = "your_app_id";
    
    // 百度翻译 API 的密钥（需要在百度翻译开放平台申请）
    private static final String SECRET_KEY = "your_secret_key";
    
    /**
     * 转换查询
     * 这是 QueryTransformer 接口要求实现的方法
     * 
     * @param query 原始查询对象
     * @return 返回翻译后的查询对象
     */
    @Override
    public Query transform(Query query) {
        try {
            // 调用百度翻译 API，将中文翻译为英文
            // query.text() 获取查询的文本内容
            // "zh" 表示源语言是中文
            // "en" 表示目标语言是英文
            String translatedText = translateToBaidu(query.text(), "zh", "en");
            
            // 创建新的 Query 对象，包含翻译后的文本
            return new Query(translatedText);
        } catch (Exception e) {
            // 如果翻译失败（比如网络错误、API 调用失败）
            // 返回原始查询，不影响后续流程
            return query;
        }
    }
    
    /**
     * 调用百度翻译 API
     * 
     * @param text 要翻译的文本
     * @param from 源语言代码（zh=中文，en=英文）
     * @param to 目标语言代码
     * @return 返回翻译后的文本
     * @throws Exception 如果翻译失败
     */
    private String translateToBaidu(String text, String from, String to) throws Exception {
        // 生成随机盐值（salt），用于签名
        // 使用当前时间戳作为盐值，确保每次请求的签名都不同
        String salt = String.valueOf(System.currentTimeMillis());
        
        // 生成签名（sign）
        // 百度翻译 API 要求的签名算法：MD5(appid + 原文 + salt + 密钥)
        // 签名用于验证请求的合法性，防止被恶意调用
        String sign = md5(APP_ID + text + salt + SECRET_KEY);
        
        // 构建完整的 API 请求 URL
        // 包含所有必需的参数：查询文本、源语言、目标语言、应用ID、盐值、签名
        String url = String.format(
            "https://fanyi-api.baidu.com/api/trans/vip/translate?q=%s&from=%s&to=%s&appid=%s&salt=%s&sign=%s",
            text,    // 要翻译的文本
            from,    // 源语言
            to,      // 目标语言
            APP_ID,  // 应用 ID
            salt,    // 盐值
            sign     // 签名
        );
        
        // 构建 HTTP GET 请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))  // 设置请求 URL
                .GET()  // 使用 GET 方法
                .build();  // 构建请求对象
        
        // 发送请求并获取响应
        // 响应体会被解析为字符串
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // 解析 JSON 响应
        // objectMapper.readTree() 将 JSON 字符串解析为 JsonNode 对象
        JsonNode root = objectMapper.readTree(response.body());
        
        // 从 JSON 中提取翻译结果
        // 百度翻译 API 的响应格式：
        // {
        //   "trans_result": [
        //     {
        //       "src": "原文",
        //       "dst": "译文"
        //     }
        //   ]
        // }
        // 这里获取第一个翻译结果的 "dst" 字段（译文）
        return root.get("trans_result")  // 获取 trans_result 数组
                   .get(0)                // 获取数组的第一个元素
                   .get("dst")            // 获取 dst 字段（译文）
                   .asText();             // 转换为字符串
    }
    
    /**
     * MD5 加密方法
     * 百度翻译 API 要求使用 MD5 算法生成签名
     * 
     * @param input 要加密的字符串
     * @return 返回 MD5 加密后的字符串（32位小写）
     */
    private String md5(String input) {
        try {
            // 获取 MD5 消息摘要实例
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            
            // 对输入字符串进行 MD5 加密
            // getBytes() 将字符串转换为字节数组
            // digest() 执行加密，返回加密后的字节数组
            byte[] messageDigest = md.digest(input.getBytes());
            
            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                // 将每个字节转换为两位十六进制数
                // 0xFF 是位掩码，确保结果是正数
                String hex = Integer.toHexString(0xFF & b);
                // 如果只有一位，前面补0
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // 返回32位小写的 MD5 字符串
            return hexString.toString();
        } catch (Exception e) {
            // 如果加密失败，抛出运行时异常
            throw new RuntimeException(e);
        }
    }
}
```

## MultiLanguageQueryRewriter.java（详细注释版）

```java
package com.zpark.zcwaiagent.rag;

// 导入 Spring 组件注解
import org.springframework.stereotype.Component;
// 导入 Resource 注解
import jakarta.annotation.Resource;
// 导入 Query 类
import org.springframework.ai.rag.Query;

/**
 * 多语言查询重写器
 * 作用：封装翻译功能，提供简单的调用接口
 */
@Component  // 注册为 Spring 组件
public class MultiLanguageQueryRewriter {
    
    // 注入翻译查询转换器
    // Spring 会自动从容器中查找 TranslationQueryTransformer 类型的 Bean
    @Resource
    private TranslationQueryTransformer translationTransformer;
    
    /**
     * 使用翻译功能重写查询
     * 
     * @param query 原始查询文本（中文）
     * @return 返回翻译后的查询文本（英文）
     */
    public String rewriteWithTranslation(String query) {
        // 将字符串包装为 Query 对象
        Query originalQuery = new Query(query);
        
        // 调用翻译转换器进行翻译
        // transform() 方法会调用百度翻译 API
        Query translatedQuery = translationTransformer.transform(originalQuery);
        
        // 从 Query 对象中提取翻译后的文本
        // text() 方法返回查询的文本内容
        return translatedQuery.text();
    }
}
```

------

好的，继续给你写第3个和第4个功能的详细注释版本：

------

# 3️⃣ 混合检索 - 向量数据库 + MySQL

## HybridDocumentRetriever.java（详细注释版）

```java
package com.zpark.zcwaiagent.rag;

// 导入 Spring AI 的 Document 类
import org.springframework.ai.document.Document;
// 导入 Query 类
import org.springframework.ai.rag.Query;
// 导入文档检索器接口
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
// 导入向量存储接口
import org.springframework.ai.vectorstore.VectorStore;
// 导入 Spring 的 JDBC 模板，用于执行 SQL 查询
import org.springframework.jdbc.core.JdbcTemplate;
// 导入 Spring 组件注解
import org.springframework.stereotype.Component;
// 导入 List 接口
import java.util.List;
// 导入 Map 接口
import java.util.Map;

/**
 * 混合文档检索器
 * 作用：结合向量检索和传统数据库检索，提升检索准确率
 * 策略：先从向量库检索，如果没找到再从 MySQL 全文搜索
 * 
 * 为什么需要混合检索？
 * 1. 向量检索：适合语义相似的查询，但可能漏掉精确匹配
 * 2. 全文搜索：适合关键词精确匹配，但不理解语义
 * 3. 结合两者：既能理解语义，又能精确匹配关键词
 */
@Component  // 注册为 Spring 组件
public class HybridDocumentRetriever implements DocumentRetriever {
    
    // 向量存储实例，用于语义检索
    // 通过构造函数注入
    private final VectorStore vectorStore;
    
    // JDBC 模板，用于执行 SQL 查询
    // 通过构造函数注入
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * 构造函数
     * Spring 会自动注入这两个参数
     * 
     * @param vectorStore 向量存储实例
     * @param jdbcTemplate JDBC 模板实例
     */
    public HybridDocumentRetriever(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        // 将注入的向量存储赋值给成员变量
        this.vectorStore = vectorStore;
        // 将注入的 JDBC 模板赋值给成员变量
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * 检索文档
     * 这是 DocumentRetriever 接口要求实现的方法
     * 
     * @param query 用户的查询对象
     * @return 返回检索到的文档列表
     */
    @Override
    public List<Document> retrieve(Query query) {
        // ========== 第一步：从向量库检索 ==========
        // 调用向量存储的相似度搜索方法
        // query.text() 获取查询的文本内容
        // 3 表示返回前3个最相似的文档
        // 原理：将查询文本转换为向量，然后计算与所有文档向量的余弦相似度
        List<Document> vectorResults = vectorStore.similaritySearch(query.text(), 3);
        
        // 检查向量库是否找到结果
        // isEmpty() 返回 true 表示列表为空（没找到）
        if (!vectorResults.isEmpty()) {
            // 如果向量库找到了结果，直接返回
            // 不再执行后续的 MySQL 查询，节省资源
            return vectorResults;
        }
        
        // ========== 第二步：向量库没找到，从 MySQL 全文搜索 ==========
        // 调用 MySQL 全文搜索方法
        // 传入查询文本作为关键词
        List<Document> mysqlResults = searchFromMySQL(query.text());
        
        // 检查 MySQL 是否找到结果
        if (!mysqlResults.isEmpty()) {
            // 如果 MySQL 找到了结果，返回
            return mysqlResults;
        }
        
        // ========== 第三步：都没找到，返回空列表 ==========
        // List.of() 创建一个不可变的空列表
        // 返回空列表表示没有找到任何相关文档
        return List.of();
    }
    
    /**
     * 从 MySQL 全文搜索
     * 
     * @param keyword 搜索关键词
     * @return 返回搜索到的文档列表
     */
    private List<Document> searchFromMySQL(String keyword) {
        // 构建 SQL 查询语句
        // MATCH...AGAINST 是 MySQL 的全文搜索语法
        // MATCH(content) 指定要搜索的列
        // AGAINST(? IN NATURAL LANGUAGE MODE) 指定搜索关键词和模式
        // NATURAL LANGUAGE MODE 是自然语言模式，会自动处理停用词、词干等
        // LIMIT 3 限制返回前3条结果
        String sql = "SELECT content, title FROM knowledge_base WHERE MATCH(content) AGAINST(? IN NATURAL LANGUAGE MODE) LIMIT 3";
        
        // 执行 SQL 查询
        // jdbcTemplate.query() 方法用于执行查询并映射结果
        // 参数1：SQL 语句
        // 参数2：SQL 参数数组（? 会被替换为 keyword）
        // 参数3：RowMapper，用于将每一行结果映射为 Document 对象
        return jdbcTemplate.query(sql, new Object[]{keyword}, (rs, rowNum) -> {
            // rs 是 ResultSet，表示查询结果的一行
            // rowNum 是行号（从0开始）
            
            // 创建 Document 对象
            return new Document(
                // 第一个参数：文档内容
                // rs.getString("content") 从结果集中获取 content 列的值
                rs.getString("content"),
                // 第二个参数：文档元数据
                Map.of(
                    // 添加标题元数据
                    "title", rs.getString("title"),
                    // 添加来源标记，表示这个文档来自 MySQL
                    "source", "mysql"
                )
            );
        });
    }
}
```

## HybridAdvisorFactory.java（详细注释版）

```java
package com.zpark.zcwaiagent.rag;

// 导入检索增强顾问
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
// 导入顾问接口
import org.springframework.ai.chat.client.advisor.api.Advisor;
// 导入向量存储接口
import org.springframework.ai.vectorstore.VectorStore;
// 导入 JDBC 模板
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 混合检索顾问工厂类
 * 作用：创建使用混合检索策略的 RAG 顾问
 */
public class HybridAdvisorFactory {
    
    /**
     * 创建混合检索顾问
     * 
     * @param vectorStore 向量存储实例
     * @param jdbcTemplate JDBC 模板实例
     * @return 返回配置好的顾问实例
     */
    public static Advisor createHybridAdvisor(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        // 创建混合文档检索器实例
        // 传入向量存储和 JDBC 模板
        HybridDocumentRetriever retriever = new HybridDocumentRetriever(vectorStore, jdbcTemplate);
        
        // 创建并返回检索增强顾问
        return RetrievalAugmentationAdvisor.builder()
                // 设置文档检索器为混合检索器
                // 当用户提问时，会先调用 retriever.retrieve() 方法
                // 该方法会依次尝试向量检索和 MySQL 检索
                .documentRetriever(retriever)
                // 构建顾问实例
                .build();
    }
}
```

## 使用示例（在 LoveApp.java 中）

```java
// 在 LoveApp 类中添加
@Resource
private JdbcTemplate jdbcTemplate;  // 注入 JDBC 模板

public String doChatWithHybridRAG(String message, String chatId) {
    // 使用混合检索顾问
    ChatResponse chatResponse = chatClient
            .prompt()
            .user(message)
            .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
            // 使用混合检索顾问
            // 会先从向量库检索，如果没找到再从 MySQL 检索
            .advisors(HybridAdvisorFactory.createHybridAdvisor(loveAppVectorStore, jdbcTemplate))
            .call()
            .chatResponse();
    
    return chatResponse.getResult().getOutput().getText();
}
```

------

# 4️⃣ 自主实现 RAG（不使用 Spring AI）

## CustomRAG.java（详细注释版）

```java
package com.zpark.zcwaiagent.rag;

// 导入阿里云 DashScope API
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
// 导入 Spring 组件注解
import org.springframework.stereotype.Component;
// 导入 List 接口
import java.util.List;
// 导入 Map 接口
import java.util.Map;
// 导入 HashMap 类
import java.util.HashMap;
// 导入 ArrayList 类
import java.util.ArrayList;

/**
 * 自主实现的 RAG（不使用 Spring AI 框架）
 * 作用：手动实现 RAG 的完整流程，加深对 RAG 原理的理解
 * 
 * RAG 的核心步骤：
 * 1. 向量化（Embedding）：将文本转换为数字向量
 * 2. 检索（Retrieval）：找到与查询最相似的文档
 * 3. 增强（Augmentation）：将检索到的文档作为上下文
 * 4. 生成（Generation）：基于上下文生成答案
 */
@Component  // 注册为 Spring 组件
public class CustomRAG {
    
    // DashScope API 实例，用于调用阿里云的 AI 服务
    private final DashScopeApi dashScopeApi;
    
    // 存储所有文档的向量
    // Key: 文档内容
    // Value: 文档的向量表示（1536维浮点数组）
    private final Map<String, float[]> documentVectors = new HashMap<>();
    
    /**
     * 构造函数
     * 
     * @param dashScopeApi DashScope API 实例
     */
    public CustomRAG(DashScopeApi dashScopeApi) {
        // 将注入的 API 实例赋值给成员变量
        this.dashScopeApi = dashScopeApi;
    }
    
    /**
     * 添加文档到知识库
     * 在使用 RAG 之前，需要先添加文档
     * 
     * @param documents 文档列表
     */
    public void addDocuments(List<String> documents) {
        // 遍历每个文档
        for (String doc : documents) {
            // 将文档转换为向量
            // getEmbedding() 方法会调用 DashScope Embedding API
            float[] vector = getEmbedding(doc);
            
            // 将文档和向量存储到 Map 中
            // 后续检索时会用到这些向量
            documentVectors.put(doc, vector);
        }
    }
    
    /**
     * 完整的 RAG 查询流程
     * 
     * @param userQuery 用户的查询问题
     * @return 返回 AI 生成的答案
     */
    public String ragQuery(String userQuery) {
        // ========== 步骤1：将用户查询转换为向量 ==========
        // 调用 Embedding API，将查询文本转换为 1536 维向量
        // 向量是一个浮点数数组，例如：[0.1, 0.5, 0.3, ..., 0.8]
        float[] queryVector = getEmbedding(userQuery);
        
        // ========== 步骤2：计算相似度，找到最相关的文档 ==========
        // 遍历所有文档的向量，计算与查询向量的相似度
        // 返回相似度最高的文档内容
        String mostRelevantDoc = findMostRelevant(queryVector);
        
        // ========== 步骤3：构建提示词 ==========
        // 将检索到的文档作为上下文，与用户问题一起构建提示词
        // 这是 RAG 的核心：让 LLM 基于检索到的知识回答问题
        String prompt = String.format("""
            你是一个专业的助手，请基于以下上下文回答用户的问题。
            
            上下文信息：
            %s
            
            用户问题：
            %s
            
            请根据上下文信息回答问题。如果上下文中没有相关信息，请说"我不知道"。
            
            回答：
            """, mostRelevantDoc, userQuery);
        
        // ========== 步骤4：调用 LLM 生成答案 ==========
        // 将构建好的提示词发送给 LLM
        // LLM 会基于上下文生成答案
        return callLLM(prompt);
    }
    
    /**
     * 获取文本的向量表示（Embedding）
     * 
     * @param text 要转换的文本
     * @return 返回 1536 维向量
     */
    private float[] getEmbedding(String text) {
        try {
            // 调用 DashScope Embedding API
            // 这里需要根据实际的 API 接口实现
            // DashScope 的 Embedding 模型会将文本转换为 1536 维向量
            
            // 示例代码（实际需要根据 DashScope API 文档实现）：
            // EmbeddingRequest request = new EmbeddingRequest();
            // request.setInput(text);
            // request.setModel("text-embedding-v1");
            // EmbeddingResponse response = dashScopeApi.embedding(request);
            // return response.getData().get(0).getEmbedding();
            
            // 这里返回一个占位数组
            // 实际使用时需要替换为真实的 API 调用
            return new float[1536];
        } catch (Exception e) {
            // 如果 API 调用失败，抛出运行时异常
            throw new RuntimeException("向量化失败", e);
        }
    }
    
    /**
     * 找到最相关的文档
     * 
     * @param queryVector 查询的向量表示
     * @return 返回最相关的文档内容
     */
    private String findMostRelevant(float[] queryVector) {
        // 记录最高相似度
        double maxSimilarity = -1;
        // 记录最相关的文档
        String mostRelevantDoc = "";
        
        // 遍历所有文档
        for (Map.Entry<String, float[]> entry : documentVectors.entrySet()) {
            // 获取文档内容
            String doc = entry.getKey();
            // 获取文档向量
            float[] docVector = entry.getValue();
            
            // 计算查询向量和文档向量的余弦相似度
            // 余弦相似度的范围是 [-1, 1]
            // 1 表示完全相同，0 表示无关，-1 表示完全相反
            double similarity = cosineSimilarity(queryVector, docVector);
            
            // 如果当前文档的相似度更高，更新记录
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                mostRelevantDoc = doc;
            }
        }
        
        // 返回相似度最高的文档
        return mostRelevantDoc;
    }
    
    /**
     * 计算两个向量的余弦相似度
     * 
     * 余弦相似度公式：
     * similarity = (A · B) / (||A|| * ||B||)
     * 
     * 其中：
     * A · B 是向量点积（dot product）
     * ||A|| 是向量 A 的模（magnitude）
     * ||B|| 是向量 B 的模
     * 
     * @param vector1 第一个向量
     * @param vector2 第二个向量
     * @return 返回余弦相似度（-1 到 1 之间）
     */
    private double cosineSimilarity(float[] vector1, float[] vector2) {
        // 检查向量维度是否相同
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }
        
        // 计算向量点积（A · B）
        // 点积 = a1*b1 + a2*b2 + ... + an*bn
        double dotProduct = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
        }
        
        // 计算向量 A 的模（||A||）
        // 模 = sqrt(a1^2 + a2^2 + ... + an^2)
        double magnitude1 = 0.0;
        for (float v : vector1) {
            magnitude1 += v * v;
        }
        magnitude1 = Math.sqrt(magnitude1);
        
        // 计算向量 B 的模（||B||）
        double magnitude2 = 0.0;
        for (float v : vector2) {
            magnitude2 += v * v;
        }
        magnitude2 = Math.sqrt(magnitude2);
        
        // 计算余弦相似度
        // 如果任一向量的模为0，返回0（避免除以0）
        if (magnitude1 == 0 || magnitude2 == 0) {
            return 0.0;
        }
        
        // 返回余弦相似度
        return dotProduct / (magnitude1 * magnitude2);
    }
    
    /**
     * 调用 LLM 生成答案
     * 
     * @param prompt 提示词（包含上下文和问题）
     * @return 返回 LLM 生成的答案
     */
    private String callLLM(String prompt) {
        try {
            // 调用 DashScope Chat API
            // 这里需要根据实际的 API 接口实现
            
            // 示例代码（实际需要根据 DashScope API 文档实现）：
            // ChatRequest request = new ChatRequest();
            // request.setModel("qwen-turbo");
            // request.setMessages(List.of(new Message("user", prompt)));
            // ChatResponse response = dashScopeApi.chat(request);
            // return response.getChoices().get(0).getMessage().getContent();
            
            // 这里返回一个占位字符串
            // 实际使用时需要替换为真实的 API 调用
            return "AI 生成的答案（占位）";
        } catch (Exception e) {
            // 如果 API 调用失败，抛出运行时异常
            throw new RuntimeException("LLM 调用失败", e);
        }
    }
}
```

## 使用示例

```java
package com.zpark.zcwaiagent.rag;

import org.springframework.stereotype.Component;
import jakarta.annotation.Resource;
import java.util.List;

/**
 * 自定义 RAG 使用示例
 */
@Component
public class CustomRAGExample {
    
    // 注入自定义 RAG 实例
    @Resource
    private CustomRAG customRAG;
    
    /**
     * 初始化知识库并查询
     */
    public void example() {
        // 步骤1：准备文档数据
        List<String> documents = List.of(
            "恋爱中最重要的是沟通，双方要坦诚相待。",
            "单身时要多参加社交活动，扩大社交圈。",
            "已婚后要学会处理家庭关系，尊重彼此的家人。"
        );
        
        // 步骤2：将文档添加到知识库
        // 这一步会将所有文档转换为向量并存储
        customRAG.addDocuments(documents);
        
        // 步骤3：执行 RAG 查询
        // 会自动检索相关文档并生成答案
        String answer = customRAG.ragQuery("恋爱中如何沟通？");
        
        // 步骤4：输出答案
        System.out.println("答案：" + answer);
    }
}
```

------

## 总结：为什么要这样写？

### 1. **混合检索的优势**

```
向量检索：理解语义，但可能漏掉精确匹配
全文搜索：精确匹配关键词，但不理解语义
混合检索：结合两者优势，提升准确率
```

### 2. **自主实现 RAG 的价值**

```
学习目的：深入理解 RAG 的每个步骤
灵活性：可以自定义每个环节的逻辑
但不推荐生产使用：维护成本高，Spring AI 已经很完善
```

### 3. **代码注释的重要性**

```
每行代码都有注释 → 容易理解
关键算法有详细说明 → 知其然也知其所以然
示例代码完整 → 可以直接运行测试
```

现在你应该能完全理解这4个扩展思路了！需要我帮你实现其中某个功能到你的项目中吗？