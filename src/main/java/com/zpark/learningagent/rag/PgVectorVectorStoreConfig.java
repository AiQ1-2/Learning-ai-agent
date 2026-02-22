package com.zpark.learningagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
public class PgVectorVectorStoreConfig {

    @Autowired
    private LearningDocumentLoader learningDocumentLoader;

    @Bean
    public VectorStore pgVectorVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        // 创建PgVector向量存储实例，传入数据库连接模板和嵌入模型
        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                // 设置向量维度为1536（对应DashScope嵌入模型的输出维度），需要根据实际使用的嵌入模型调整
                .dimensions(1536)
                // 设置距离计算类型为余弦距离，适用于文本相似度计算
                .distanceType(COSINE_DISTANCE)
                // 设置索引类型为HNSW（Hierarchical Navigable Small World），适合高维向量的近似最近邻搜索
                .indexType(HNSW)
                // 初始化数据库表结构，设为true会在首次启动时自动创建所需的表和索引
                .initializeSchema(true)
                // 指定数据库模式名称，默认为"public"
                .schemaName("public")
                // 指定向量表名称，默认为"vector_store"
                .vectorTableName("vector_store")
                // 设置批量处理文档的最大数量，避免一次性处理过多数据导致内存溢出
                .maxDocumentBatchSize(10000)
                // 构建并返回配置好的向量存储实例
                .build();
        //加载文档
        List<Document> documents = learningDocumentLoader.loadMarkdowns();
        vectorStore.add(documents);
        return vectorStore;
    }
}
