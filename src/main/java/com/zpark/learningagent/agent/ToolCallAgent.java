package com.zpark.learningagent.agent;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.zpark.learningagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现 think 和 act 方法
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ToolCallAgent extends ReActAgent {
    private static final Logger log = LoggerFactory.getLogger(ToolCallAgent.class);
    
    // 可用的工具列表
    private final ToolCallback[] availableTools;

    // 保存LLM返回的、包含工具调用指令的响应
    private ChatResponse toolCallChatResponse;
    
    // 保存最新的AI文本回复（用于流式输出给前端）
    private String latestAiResponse = "";
    
    // 保存思考过程信息（用于流式输出）
    private String thinkingProcess = "";

    // 工具调用管理器
    private final ToolCallingManager toolCallingManager;

    // 禁用Spring AI内置工具调用的配置项
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
                .withProxyToolCalls(true)
                .build();
    }

    /**
     * 处理当前状态并决定下一步行动
     */
    @Override
    public boolean think() {
        // 处理下一步提示词
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }

        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, chatOptions);

        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .tools(availableTools)
                    .call()
                    .chatResponse();

            this.toolCallChatResponse = chatResponse;

            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            String result = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            
            // 保存AI的文本回复
            this.latestAiResponse = (result != null) ? result : "";
            
            // 构建思考过程信息（JSON格式，前端解析）
            if (!toolCallList.isEmpty()) {
                StringBuilder thinking = new StringBuilder();
                thinking.append("[THINKING]");
                for (int i = 0; i < toolCallList.size(); i++) {
                    if (i > 0) thinking.append(",");
                    thinking.append(toolCallList.get(i).name());
                }
                thinking.append("[/THINKING]");
                this.thinkingProcess = thinking.toString();
            } else {
                this.thinkingProcess = "";
            }

            log.info(getName() + " 的思考: " + result);
            log.info(getName() + " 选择了 " + toolCallList.size() + " 个工具来使用");
            
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称: %s, 参数: %s",
                            toolCall.name(),
                            toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);

            if (toolCallList.isEmpty()) {
                getMessageList().add(assistantMessage);
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + " 的思考过程遇到了问题: " + e.getMessage());
            this.latestAiResponse = "处理时遇到错误: " + e.getMessage();
            getMessageList().add(new AssistantMessage("处理时遇到错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并处理结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具调用";
        }

        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);

        setMessageList(toolExecutionResult.conversationHistory());

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 执行完成")
                .collect(Collectors.joining("\n"));
        
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));
        
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
        }

        log.info(results);
        return results;
    }
    
    /**
     * 获取最新的AI回复内容（用于流式输出）
     */
    public String getLatestAiResponse() {
        return this.latestAiResponse;
    }
    
    /**
     * 重写父类方法，返回AI的文本回复给前端
     */
    @Override
    protected String getStreamOutput() {
        StringBuilder output = new StringBuilder();
        
        // 先输出思考过程
        if (this.thinkingProcess != null && !this.thinkingProcess.isEmpty()) {
            output.append(this.thinkingProcess);
            this.thinkingProcess = "";
        }
        
        // 再输出AI回复
        if (this.latestAiResponse != null && !this.latestAiResponse.isEmpty()) {
            output.append(this.latestAiResponse);
            this.latestAiResponse = "";
        }
        
        return output.toString();
    }
}
