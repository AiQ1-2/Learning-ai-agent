package com.zpark.learningagent.rag;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import jakarta.annotation.Resource;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 学习者画像向量存储配置类
 * 作用：创建一个专门存储学习者画像的向量数据库
 */
@Configuration  // 标记为配置类，Spring 会扫描这个类中的 @Bean 方法
public class LearnerProfileVectorConfig {

    // 注入学习者画像文档加载器
    @Resource
    private LearnerProfileLoader learnerProfileLoader;

    /**
     * 创建学习者画像向量存储的 Bean
     * @param embeddingModel 嵌入模型，用于将文本转换为向量
     * @return 返回配置好的向量存储对象
     */
    @Bean(name = "learnerProfileVectorStore")  // 指定 Bean 的名称为 learnerProfileVectorStore
    public VectorStore learnerProfileVectorStore(DashScopeEmbeddingModel embeddingModel) {
      //创建一个向量存储对象，并传入嵌入模型
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        //调用文档加载器加载学习者画像文档
        List<Document> documents = learnerProfileLoader.loadLearnerProfiles();
        //将加载的文档添加到向量存储中
        simpleVectorStore.add(documents);
        return simpleVectorStore;
    }
}
