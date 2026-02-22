package com.zpark.learningagent.agent;

import com.zpark.learningagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class LearningManus extends ToolCallAgent {  


    /**
     * 构造函数，初始化LearningManus学习助手实例
     *
     * @param allTools 可用的工具回调数组，用于执行各种功能操作
     * @param dashscopeChatModel 通义千问聊天模型实例，用于处理对话请求
     */
    public LearningManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("learningManus");
        // 设置系统提示词，定义AI助手的角色和能力
        String SYSTEM_PROMPT = """  
                You are LearningManus, an AI learning assistant designed to help students plan their studies and find learning resources efficiently.
                
                IMPORTANT RULES:
                1. If the user asks a simple question (like "who are you", "hello", "how are you"), answer briefly and IMMEDIATELY call the `terminate` tool.
                2. Only use tools when the task requires external actions (search, file operations, resource recommendations, etc.).
                3. For conversational queries without action requirements, respond and terminate.
                4. Focus on learning-related tasks: study planning, resource recommendations, learning partner matching.
                5. Never repeat the same response multiple times.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        // 设置下一步操作提示词，指导AI如何选择和使用工具
        String NEXT_STEP_PROMPT = """  
                Based on learning needs, proactively select the most appropriate tool or combination of tools.  
                For complex learning tasks, you can break down the problem and use different tools step by step to solve it.  
                After using each tool, clearly explain the execution results and suggest the next steps.  
                If you want to stop the interaction at any point, use the `terminate` tool/function call.  
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        // 设置最大步骤数，防止无限循环
        this.setMaxSteps(20);
        // 初始化客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }


}
