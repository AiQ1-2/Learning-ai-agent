package com.zpark.learningagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 创建一个向量数据库的配置类
 * 作用：创建一个向量数据库，用于存储学习资料文档向量
 */
@Configuration
public class LearningVectorStoreConfig {
    @Resource
    private LearningDocumentLoader learningDocumentLoader;


    @Resource
    private MyKeywordEnricher myKeywordEnricher;
    
    @Bean
    VectorStore learningVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        //加载文档
        // 调用文档加载器加载所有Markdown格式的学习资料文档
        // 返回包含文档内容、元数据的Document对象列表
        List<Document> documents = learningDocumentLoader.loadMarkdowns();
        // 将加载的文档添加到向量存储中
        // 此过程会自动对文档进行向量化处理，将文本转换为向量表示
        // 便于后续的相似度检索和RAG问答功能
//        simpleVectorStore.add(documents);
        // 自动补充关键词元信息
        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documents);
        simpleVectorStore.add(enrichedDocuments);
        // 返回配置完成的向量存储实例供Spring容器管理
        return simpleVectorStore;

    }
}
