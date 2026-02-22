package com.zpark.learningagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class PgVectorVectorStoreConfigTest {

    /**
     * 注入的向量存储实例，用于测试PgVector向量存储配置
     * 该字段通过@Resource注解自动注入Spring容器中配置的VectorStore bean
     */
    @Resource
    VectorStore pgVectorVectorStore;
    @Test
    void test() {
        List<Document> documents = List.of(
                new Document("学习编程的方法，这个到底是什么用，当然是为了学编程做项目了！", Map.of("meta1", "meta1")),
                new Document("认真学习编程中，请你认真的学习"),
                new Document("你的编程学的怎么样了，有没有学到很棒的东西？", Map.of("meta2", "meta2")));
        // 添加文档
        pgVectorVectorStore.add(documents);
        // 相似度查询
        List<Document> results = pgVectorVectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(5).build());
         Assertions.assertNotNull(results);
    }
}
