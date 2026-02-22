package com.zpark.learningagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * 自定义顾问工厂类
 * 用于创建带有过滤条件的RAG检索增强顾问
 */
@Slf4j
public class LearningRagAdvisorFactory {

    /**
     * 创建自定义的RAG检索增强顾问
     * 根据学习分类过滤文档，只检索符合条件的学习资料文档
     *
     * @param vectorStore 向量存储实例，用于文档检索
     * @param category 学习分类标识，用于过滤文档（如"学习方法"、"学科知识"等）
     * @return 配置好的检索增强顾问实例
     */
    public static Advisor createLearningRagAdvisor(VectorStore vectorStore, String category) {
        // 构建过滤表达式，根据category字段过滤文档
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("category", category)  // 等于指定分类的文档
                .build();

        // 创建向量存储文档检索器
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)           // 设置向量存储
                .filterExpression(expression)       // 设置过滤条件
                .similarityThreshold(0.5)          // 设置相似度阈值为0.5
                .topK(3)                           // 最多返回3个最相关的文档
                .build();

        // 创建并返回检索增强顾问
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)  // 设置文档检索器
                .queryAugmenter(LearningQueryAugmenterFactory.createInstance())  // 设置空上下文处理器
                .build();
    }
}
