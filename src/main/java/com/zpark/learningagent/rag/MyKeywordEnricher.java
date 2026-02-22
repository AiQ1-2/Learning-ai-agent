package com.zpark.learningagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 关键词增强器组件
 * 用于为文档自动提取和添加关键词元数据，提升向量检索的准确性
 */
@Component  // 注册为Spring组件，交给Spring容器管理
class MyKeywordEnricher {
    /**
     * DashScope聊天模型实例
     * 用于调用大语言模型来分析文档内容并提取关键词
     */
    @Resource  // 自动注入DashScope聊天模型Bean
    private ChatModel dashscopeChatModel;

    /**
     * 为文档列表添加关键词元数据
     *
     * @param documents 原始文档列表
     * @return 添加了关键词元数据的文档列表
     */
    List<Document> enrichDocuments(List<Document> documents) {
        // 创建关键词元数据增强器实例
        // 参数1：使用的聊天模型（dashscopeChatModel）
        // 参数2：要提取的关键词数量（5个）
        KeywordMetadataEnricher enricher = new KeywordMetadataEnricher(this.dashscopeChatModel, 5);

        // 对文档列表应用关键词增强处理
        // 该方法会调用大语言模型分析每个文档的内容，自动提取5个最相关的关键词
        // 并将这些关键词作为元数据添加到文档中
        return enricher.apply(documents);
    }
}
