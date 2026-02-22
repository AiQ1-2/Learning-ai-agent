package com.zpark.learningagent.agent;

import com.zpark.learningagent.agent.model.AgentState;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础代理类，用于管理代理的状态和执行步骤
 */
@Getter
@Setter
public abstract class BaseAgent {
    private static final Logger log = LoggerFactory.getLogger(BaseAgent.class);

    private String name;
    private String systemPrompt;
    private String nextStepPrompt;
    private AgentState state = AgentState.IDLE;
    private int maxSteps = 10;
    private int ccurrentStep = 0;
    
    // 中断标志
    private volatile boolean interrupted = false;

    private ChatClient chatClient;
    private List<Message> messageList = new ArrayList<>();

    /**
     * 运行代理
     */
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("cannot run agent from state: " + this.state);
        }
        if (StringUtil.isBlank(userPrompt)) {
            throw new RuntimeException("cannot run agent with empty user prompt");
        }
        
        state = AgentState.RUNNING;
        messageList.add(new UserMessage(userPrompt));
        List<String> results = new ArrayList<>();
        
        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                ccurrentStep = stepNumber;
                log.info("Executing step " + stepNumber + "/" + maxSteps);
                
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            
            if (ccurrentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return StringUtils.join(results, "\n");
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error running agent: " + e.getMessage(), e);
            return "执行错误: " + e.getMessage();
        } finally {
            this.cleanup();
        }
    }

    /**
     * 运行代理（流式输出）
     */
    public SseEmitter runStream(String userPrompt) {
        SseEmitter emitter = new SseEmitter(300000L);

        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentState.IDLE) {
                    log.warn("Agent state is {}, resetting to IDLE", this.state);
                    this.cleanup();
                }
                
                if (StringUtil.isBlank(userPrompt)) {
                    emitter.send("错误：不能使用空提示词运行代理");
                    emitter.complete();
                    return;
                }

                state = AgentState.RUNNING;
                messageList.add(new UserMessage(userPrompt));

                try {
                    for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                        if (interrupted) {
                            state = AgentState.FINISHED;
                            emitter.send("执行已被用户中断");
                            break;
                        }
                        
                        int stepNumber = i + 1;
                        ccurrentStep = stepNumber;
                        log.info("Executing step " + stepNumber + "/" + maxSteps);

                        String stepResult = step();
                        
                        String outputContent = getStreamOutput();
                        if (outputContent != null && !outputContent.isEmpty()) {
                            emitter.send(outputContent);
                        }
                    }
                    
                    if (ccurrentStep >= maxSteps) {
                        state = AgentState.FINISHED;
                        emitter.send("执行结束: 达到最大步骤 (" + maxSteps + ")");
                    }
                    emitter.complete();
                } catch (Exception e) {
                    state = AgentState.ERROR;
                    log.error("执行智能体失败", e);
                    try {
                        emitter.send("执行错误: " + e.getMessage());
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    this.cleanup();
                }
            } catch (Exception e) {
                log.error("SSE stream error", e);
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timed out");
        });

        emitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        return emitter;
    }

    /**
     * 执行单个步骤
     */
    public abstract String step();
    
    /**
     * 获取要发送给前端的流式输出内容
     */
    protected String getStreamOutput() {
        return "";
    }
    
    /**
     * 中断代理执行
     */
    public void interrupt() {
        this.interrupted = true;
        log.info("Agent {} interrupted", this.name);
    }
    
    /**
     * 检查是否被中断
     */
    public boolean isInterrupted() {
        return this.interrupted;
    }
    
    /**
     * 清理资源
     */
    protected void cleanup() {
        if (this.state != AgentState.IDLE) {
            this.state = AgentState.IDLE;
        }
        this.interrupted = false;
        this.ccurrentStep = 0;
        if (this.messageList != null) {
            this.messageList.clear();
        }
        log.debug("Agent cleanup completed");
    }
}
