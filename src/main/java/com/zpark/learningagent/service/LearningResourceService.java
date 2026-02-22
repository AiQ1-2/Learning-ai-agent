package com.zpark.learningagent.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 学习资源推荐服务
 * 作用：根据学习者的需求描述，从学习者画像库中找到最匹配的学习伙伴
 */
@Service  // 标记为服务层组件
public class LearningResourceService {
  private static final Logger log = LoggerFactory.getLogger(LearningResourceService.class);
    // 学习者画像向量存储
    // @Qualifier 指定注入名为 "learnerProfileVectorStore" 的Bean
    // 因为项目中可能有多个 VectorStore，需要明确指定使用哪一个
    private final VectorStore learnerProfileVectorStore;

    // 构造函数注入
    public LearningResourceService(@Qualifier("learnerProfileVectorStore") VectorStore learnerProfileVectorStore) {
        this.learnerProfileVectorStore = learnerProfileVectorStore;
    }

    /**
     * 根据学习需求推荐匹配的学习伙伴
     * @param query 学习者的需求描述，例如："我想找一个一起学习Java的伙伴"
     * @param topK 返回前K 个最匹配的结果
     * @return 推荐的学习者画像列表
     */
    public List<Document> recommendMatches(String query, int topK) {
        // 记录日志：开始推荐
        log.info("开始推荐学习伙伴，查询条件: {}, topK: {}", query, topK);

        // 执行相似度搜索
        // 使用最简单的方法：直接传入查询文本和返回数量
        // 原理：将 query 转换为向量，然后在向量数据库中找到最相似的文档
        List<Document> results = learnerProfileVectorStore.similaritySearch(query);

        // 记录日志：推荐完成
        log.info("推荐完成，找到 {} 个学习伙伴", results.size());

        // 返回搜索结果
        return results;
    }

    /**
     * 格式化推荐结果，将 Document 列表转换为易读的字符串
     * @param documents 推荐的文档列表
     * @return 格式化后的字符串
     */
    public String formatRecommendations(List<Document> documents) {
        // 如果没有找到学习伙伴
        if (documents.isEmpty()) {
            return "抱歉，暂时没有找到合适的学习伙伴。";
        }

        // 创建字符串构建器，用于拼接结果
        StringBuilder result = new StringBuilder("为您推荐以下学习伙伴：\n\n");

        // 遍历每个推荐结果
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);

            // 添加序号
            result.append(i + 1).append(". ");

            // 添加文档内容（学习者画像信息）
            result.append(doc.getText());
            result.append("\n");

            // 添加文件名信息（如果有）
            if (doc.getMetadata().containsKey("filename")) {
                result.append("来源: ").append(doc.getMetadata().get("filename"));
                result.append("\n");
            }

            result.append("\n");
        }

        // 返回格式化后的字符串
        return result.toString();
    }
}
