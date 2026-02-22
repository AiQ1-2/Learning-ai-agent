# RAG 知识扩展功能实现思路

## 1. 自定义 DocumentReader - GitHub 仓库文档读取器

### 实现思路
通过 GitHub REST API 读取仓库的 README 文件，将其转换为 Spring AI 的 Document 对象，存储到向量数据库中用于 RAG 检索。

### 核心步骤
1. 使用 Java 11+ 的 HttpClient 调用 GitHub API
2. 解析 API 返回的 README 内容
3. 创建 Document 对象并添加元数据
4. 存储到向量数据库

### 关键代码
```java
@Component
public class GitHubDocumentReader {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    public List<Document> loadGitHubRepo(String owner, String repo) throws IOException, InterruptedException {
        // 构建 GitHub API URL
        String url = String.format("https://api.github.com/repos/%s/%s/readme", owner, repo);
        
        // 发送 HTTP 请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3.raw")  // 获取原始 Markdown
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // 创建 Document 对象
        if (response.statusCode() == 200) {
            Document doc = new Document(
                response.body(),
                Map.of(
                    "source", "github",
                    "repo", owner + "/" + repo,
                    "type", "readme"
                )
            );
            return List.of(doc);
        }
        
        return List.of();
    }
}
```

### 向量存储配置
```java
@Configuration
public class GitHubVectorStoreConfig {
    
    @Resource
    private GitHubDocumentReader gitHubDocumentReader;
    
    @Bean(name = "githubVectorStore")
    public VectorStore githubVectorStore(EmbeddingModel embeddingModel) throws Exception {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        
        // 加载 GitHub 仓库文档
        List<Document> documents = gitHubDocumentReader.loadGitHubRepo("spring-projects", "spring-ai");
        
        // 添加到向量库
        vectorStore.add(documents);
        
        return vectorStore;
    }
}
```

### 扩展方向
- 支持读取多个文件（不仅是 README）
- 支持读取 Issues、Pull Requests
- 定期更新仓库内容（定时任务）

---

## 2. 自定义 QueryTransformer - 翻译查询转换器

### 实现思路
在 RAG 检索前，将中文查询翻译为英文，提升对英文文档的检索效果。使用第三方翻译 API（如百度翻译）替代 LLM 翻译，降低成本。

### 为什么需要翻译？
- 知识库中有大量英文文档
- 中文查询直接检索英文文档效果差
- 翻译后的英文查询能更准确匹配英文文档

### 核心步骤
1. 实现 `QueryTransformer` 接口
2. 调用百度翻译 API
3. 生成 MD5 签名（API 要求）
4. 返回翻译后的查询

### 关键代码
```java
@Component
public class TranslationQueryTransformer implements QueryTransformer {
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String APP_ID = "your_app_id";
    private static final String SECRET_KEY = "your_secret_key";
    
    @Override
    public Query transform(Query query) {
        try {
            // 调用翻译 API
            String translatedText = translateToBaidu(query.text(), "zh", "en");
            return new Query(translatedText);
        } catch (Exception e) {
            // 翻译失败，返回原查询
            return query;
        }
    }
    
    private String translateToBaidu(String text, String from, String to) throws Exception {
        // 生成签名
        String salt = String.valueOf(System.currentTimeMillis());
        String sign = md5(APP_ID + text + salt + SECRET_KEY);
        
        // 构建 API URL
        String url = String.format(
            "https://fanyi-api.baidu.com/api/trans/vip/translate?q=%s&from=%s&to=%s&appid=%s&salt=%s&sign=%s",
            text, from, to, APP_ID, salt, sign
        );
        
        // 发送请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // 解析 JSON 响应
        JsonNode root = objectMapper.readTree(response.body());
        return root.get("trans_result").get(0).get("dst").asText();
    }
    
    private String md5(String input) {
        // MD5 加密实现
        // ...
    }
}
```

### 使用方式
```java
@Component
public class MultiLanguageQueryRewriter {
    
    @Resource
    private TranslationQueryTransformer translationTransformer;
    
    public String rewriteWithTranslation(String query) {
        Query originalQuery = new Query(query);
        Query translatedQuery = translationTransformer.transform(originalQuery);
        return translatedQuery.text();
    }
}
```

### 成本对比
- LLM 翻译：每次调用消耗 tokens，成本较高
- 百度翻译 API：按字符计费，成本低
- 适用场景：高频查询、预算有限

---

## 3. 混合检索 - 向量数据库 + MySQL

### 实现思路
结合向量检索（语义搜索）和传统数据库检索（关键词匹配），提升检索准确率和召回率。

### 检索策略
```
策略1：优先向量检索
├─ 向量库找到结果 → 直接返回
└─ 向量库未找到 → 降级到 MySQL 全文搜索

策略2：并行检索合并
├─ 同时查询向量库和 MySQL
└─ 合并结果并去重
```

### 核心代码（策略1：降级方案）
```java
@Component
public class HybridDocumentRetriever implements DocumentRetriever {
    
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    
    public HybridDocumentRetriever(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public List<Document> retrieve(Query query) {
        // 步骤1：向量检索
        List<Document> vectorResults = vectorStore.similaritySearch(query.text(), 3);
        
        if (!vectorResults.isEmpty()) {
            return vectorResults;  // 找到结果，直接返回
        }
        
        // 步骤2：向量库未找到，降级到 MySQL
        List<Document> mysqlResults = searchFromMySQL(query.text());
        
        if (!mysqlResults.isEmpty()) {
            return mysqlResults;
        }
        
        // 步骤3：都未找到，返回空列表
        return List.of();
    }
    
    private List<Document> searchFromMySQL(String keyword) {
        // MySQL 全文搜索
        String sql = "SELECT content, title FROM knowledge_base " +
                     "WHERE MATCH(content) AGAINST(? IN NATURAL LANGUAGE MODE) LIMIT 3";
        
        return jdbcTemplate.query(sql, new Object[]{keyword}, (rs, rowNum) -> {
            return new Document(
                rs.getString("content"),
                Map.of(
                    "title", rs.getString("title"),
                    "source", "mysql"
                )
            );
        });
    }
}
```

### 创建混合检索顾问
```java
public class HybridAdvisorFactory {
    
    public static Advisor createHybridAdvisor(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        HybridDocumentRetriever retriever = new HybridDocumentRetriever(vectorStore, jdbcTemplate);
        
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .build();
    }
}
```

### 使用示例
```java
public String doChatWithHybridRAG(String message, String chatId) {
    ChatResponse chatResponse = chatClient
            .prompt()
            .user(message)
            .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
            .advisors(HybridAdvisorFactory.createHybridAdvisor(loveAppVectorStore, jdbcTemplate))
            .call()
            .chatResponse();
    
    return chatResponse.getResult().getOutput().getText();
}
```

### MySQL 全文索引配置
```sql
-- 创建全文索引
ALTER TABLE knowledge_base ADD FULLTEXT INDEX idx_content(content);

-- 查询示例
SELECT * FROM knowledge_base 
WHERE MATCH(content) AGAINST('恋爱技巧' IN NATURAL LANGUAGE MODE);
```

### 优势分析
- 向量检索：理解语义，适合模糊查询
- 全文搜索：精确匹配，适合关键词查询
- 混合检索：互补优势，提升整体效果

---

## 4. 自主实现 RAG（不使用 Spring AI）

### 实现思路
手动实现 RAG 的完整流程，深入理解其工作原理。包括向量化、检索、增强、生成四个步骤。

### RAG 核心流程
```
1. Embedding（向量化）
   ├─ 将文档转换为向量
   └─ 将查询转换为向量

2. Retrieval（检索）
   ├─ 计算查询向量与文档向量的相似度
   └─ 返回最相似的文档

3. Augmentation（增强）
   └─ 将检索到的文档作为上下文

4. Generation（生成）
   └─ LLM 基于上下文生成答案
```

### 核心代码
```java
@Component
public class CustomRAG {
    
    private final DashScopeApi dashScopeApi;
    private final Map<String, float[]> documentVectors = new HashMap<>();
    
    public CustomRAG(DashScopeApi dashScopeApi) {
        this.dashScopeApi = dashScopeApi;
    }
    
    // 添加文档到知识库
    public void addDocuments(List<String> documents) {
        for (String doc : documents) {
            float[] vector = getEmbedding(doc);  // 向量化
            documentVectors.put(doc, vector);     // 存储
        }
    }
    
    // 完整的 RAG 查询流程
    public String ragQuery(String userQuery) {
        // 步骤1：查询向量化
        float[] queryVector = getEmbedding(userQuery);
        
        // 步骤2：检索最相关文档
        String mostRelevantDoc = findMostRelevant(queryVector);
        
        // 步骤3：构建提示词（增强）
        String prompt = String.format("""
            你是一个专业的助手，请基于以下上下文回答用户的问题。
            
            上下文信息：
            %s
            
            用户问题：
            %s
            
            请根据上下文信息回答问题。如果上下文中没有相关信息，请说"我不知道"。
            
            回答：
            """, mostRelevantDoc, userQuery);
        
        // 步骤4：调用 LLM 生成答案
        return callLLM(prompt);
    }
    
    // 获取文本的向量表示
    private float[] getEmbedding(String text) {
        // 调用 DashScope Embedding API
        // 返回 1536 维向量
        // ...
    }
    
    // 找到最相关的文档
    private String findMostRelevant(float[] queryVector) {
        double maxSimilarity = -1;
        String mostRelevantDoc = "";
        
        for (Map.Entry<String, float[]> entry : documentVectors.entrySet()) {
            String doc = entry.getKey();
            float[] docVector = entry.getValue();
            
            // 计算余弦相似度
            double similarity = cosineSimilarity(queryVector, docVector);
            
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                mostRelevantDoc = doc;
            }
        }
        
        return mostRelevantDoc;
    }
    
    // 计算余弦相似度
    private double cosineSimilarity(float[] vector1, float[] vector2) {
        // 公式：similarity = (A · B) / (||A|| * ||B||)
        
        // 计算点积
        double dotProduct = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
        }
        
        // 计算模
        double magnitude1 = 0.0;
        for (float v : vector1) {
            magnitude1 += v * v;
        }
        magnitude1 = Math.sqrt(magnitude1);
        
        double magnitude2 = 0.0;
        for (float v : vector2) {
            magnitude2 += v * v;
        }
        magnitude2 = Math.sqrt(magnitude2);
        
        // 返回余弦相似度
        if (magnitude1 == 0 || magnitude2 == 0) {
            return 0.0;
        }
        
        return dotProduct / (magnitude1 * magnitude2);
    }
    
    // 调用 LLM 生成答案
    private String callLLM(String prompt) {
        // 调用 DashScope Chat API
        // ...
    }
}
```

### 使用示例
```java
@Component
public class CustomRAGExample {
    
    @Resource
    private CustomRAG customRAG;
    
    public void example() {
        // 准备文档
        List<String> documents = List.of(
            "恋爱中最重要的是沟通，双方要坦诚相待。",
            "单身时要多参加社交活动，扩大社交圈。",
            "已婚后要学会处理家庭关系，尊重彼此的家人。"
        );
        
        // 添加到知识库
        customRAG.addDocuments(documents);
        
        // 执行查询
        String answer = customRAG.ragQuery("恋爱中如何沟通？");
        
        System.out.println("答案：" + answer);
    }
}
```

### 余弦相似度原理
```
向量 A = [0.1, 0.5, 0.3]
向量 B = [0.2, 0.4, 0.3]

点积 = 0.1*0.2 + 0.5*0.4 + 0.3*0.3 = 0.31
模 A = sqrt(0.1² + 0.5² + 0.3²) = 0.583
模 B = sqrt(0.2² + 0.4² + 0.3²) = 0.538

相似度 = 0.31 / (0.583 * 0.538) = 0.988

相似度越接近 1，表示两个向量越相似
```

### 为什么要自己实现？
- 学习目的：深入理解 RAG 原理
- 灵活性：可以自定义每个环节
- 调试方便：清楚每一步的输入输出
- 不推荐生产使用：Spring AI 已经很完善

---

## 总结

### 实现优先级建议

**第一阶段（基础扩展）**
1. GitHub 文档读取器 - 扩展知识来源
2. 翻译查询转换器 - 提升跨语言检索

**第二阶段（增强检索）**
3. 混合检索 - 提升检索准确率

**第三阶段（深入学习）**
4. 自主实现 RAG - 理解底层原理

### 关键技术点

| 功能 | 核心技术 | 难度 | 价值 |
|------|---------|------|------|
| GitHub 读取器 | HTTP API 调用 | ⭐⭐ | 扩展知识源 |
| 翻译转换器 | 第三方 API 集成 | ⭐⭐ | 降低成本 |
| 混合检索 | 多数据源整合 | ⭐⭐⭐ | 提升准确率 |
| 自主实现 RAG | 向量计算、相似度 | ⭐⭐⭐⭐ | 深入理解 |

### 扩展方向
- 支持更多文档源（Notion、Confluence、Wiki）
- 实现多语言翻译（支持更多语言对）
- 引入 Elasticsearch 做混合检索
- 实现 Re-ranking（重排序）提升精度
- 添加缓存机制减少 API 调用
