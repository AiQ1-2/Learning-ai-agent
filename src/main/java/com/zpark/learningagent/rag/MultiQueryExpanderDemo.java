package com.zpark.learningagent.rag;


import org.antlr.v4.runtime.ListTokenSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 多查询扩展器演示类
 * 用于将单一查询扩展为多个相关查询，提升RAG检索效果
 */
@Component  // 注册为Spring组件，交给Spring容器管理
public class MultiQueryExpanderDemo {
    /**
     * ChatClient构建器实例
     * 用于构建聊天客户端，供多查询扩展器使用
     */
    private final ChatClient.Builder chatClientBuilder;

    /**
     * 构造函数 - 通过构造函数注入ChatClient.Builder
     *
     * @param chatClientBuilder ChatClient构建器实例
     */
    public MultiQueryExpanderDemo(ChatClient.Builder chatClientBuilder) {
        // 将注入的构建器实例赋值给成员变量
        this.chatClientBuilder = chatClientBuilder;
    }

    /**
     * 扩展查询方法
     * 将输入的查询语句扩展为多个相关的查询语句
     *
     * @param query 输入的原始查询字符串（注意：当前实现中未使用此参数）
     * @return 扩展后的查询列表
     */
    public List<Query> expand(String query){
        // 创建多查询扩展器构建器实例
        MultiQueryExpander queryExpander = MultiQueryExpander.builder()
                // 设置聊天客户端构建器，用于调用大语言模型生成相关查询
                .chatClientBuilder(chatClientBuilder)
                // 设置要生成的相关查询数量为3个
                .numberOfQueries(3)
                // 构建并返回配置好的多查询扩展器实例
                .build();

        // 执行查询扩展操作
        // 注意：这里硬编码了一个固定的查询"谁是流浪啊？"，而不是使用传入的query参数
        // 这可能是代码的一个bug，应该使用传入的query参数
        List<Query> queries = queryExpander.expand(new Query("谁是流浪啊？"));

        // 返回扩展后的查询列表
        return queries;
    }
}
