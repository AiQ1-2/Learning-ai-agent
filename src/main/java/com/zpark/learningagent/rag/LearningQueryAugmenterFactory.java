package com.zpark.learningagent.rag;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

/**
 * 学习应用上下文查询增强器工厂类
 * 用于创建当上下文为空时的默认响应处理机制
 */
public class LearningQueryAugmenterFactory {
    /**
     * 创建上下文查询增强器实例
     * 当检索不到相关文档时，返回预定义的友好提示信息
     *
     * @return 配置好的上下文查询增强器实例
     */
    public static ContextualQueryAugmenter createInstance() {
        // 创建空上下文时的提示模板
        // 当无法检索到相关文档时，使用此模板生成友好的错误提示
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate("""
                你应该输出下面的内容：
                抱歉，我只能回答学习规划相关的问题，别的没办法帮到您哦，
                有问题可以联系管理员
                """);

        // 构建并返回上下文查询增强器实例
        return ContextualQueryAugmenter.builder()
                // 不允许空上下文，强制使用预定义的提示模板
                .allowEmptyContext(false)
                // 设置空上下文时使用的提示模板
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                // 构建最终的增强器实例
                .build();
    }
}
