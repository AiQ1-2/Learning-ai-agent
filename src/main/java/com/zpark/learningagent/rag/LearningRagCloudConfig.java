package com.zpark.learningagent.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
class LearningRagCloudConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Bean
    // 创建基于云端RAG的顾问Bean
    // 该顾问用于从DashScope云端知识库检索相关信息
    public Advisor learningRagAdvisor() {
        // 初始化DashScope API客户端，使用配置的API密钥
        // DashScopeApi是阿里云百炼平台的API客户端
        DashScopeApi dashScopeApi  = new DashScopeApi(dashScopeApiKey);
        // 定义要检索的知识库名称
        // "学习助手"是在DashScope平台上创建的知识库索引名称
        final String KNOWLEDGE_BASE = "学习助手";
        // 创建文档检索器实例
        // DashScopeDocumentRetriever负责从指定的知识库中检索相关文档
        DashScopeDocumentRetriever dashScopeDocumentRetriever = new DashScopeDocumentRetriever(dashScopeApi, DashScopeDocumentRetrieverOptions.builder()
                // 配置要检索的知识库索引名称
                .withIndexName(KNOWLEDGE_BASE)
                // 构建检索器配置选项
                .build());
        // 构建并返回检索增强顾问
        // RetrievalAugmentationAdvisor会在AI对话前自动检索相关文档
        return RetrievalAugmentationAdvisor.builder()
                // 设置文档检索器，用于获取相关知识
                .documentRetriever(dashScopeDocumentRetriever)
                // 构建最终的顾问实例
                .build();
    }

}
