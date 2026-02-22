package com.zpark.learningagent.controller;

import com.zpark.learningagent.agent.LearningManus;
import com.zpark.learningagent.app.LearningApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LearningApp learningApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;
    
    // 保存正在运行的Agent实例，用于中断
    private final Map<String, LearningManus> runningAgents = new ConcurrentHashMap<>();

    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return learningApp.doChat(message, chatId);
    }
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return learningApp.doChatByStream(message, chatId);
    }
    @GetMapping(value = "/love_app/chat/sse/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithLoveAppSSEServerSentEvent(String message, String chatId) {
        return learningApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }
    @GetMapping("/love_app/chat/sse/emitter")
    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter emitter = new SseEmitter(180000L); // 3分钟超时
        // 获取 Flux 数据流并直接订阅
        learningApp.doChatByStream(message, chatId)
                .subscribe(
                        // 处理每条消息
                        chunk -> {
                            try {
                                emitter.send(chunk);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        // 处理错误
                        emitter::completeWithError,
                        // 处理完成
                        emitter::complete
                );
        // 返回emitter
        return emitter;
    }


    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message 用户消息
     * @param sessionId 会话ID（可选，用于中断）
     * @return SSE 流式响应
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message, @RequestParam(required = false) String sessionId) {
        // 生成或使用传入的sessionId
        String sid = (sessionId != null && !sessionId.isEmpty()) ? sessionId : UUID.randomUUID().toString();
        
        // 每次请求创建新的 LearningManus 实例，避免状态污染
        LearningManus learningManus = new LearningManus(allTools, dashscopeChatModel);
        
        // 保存到运行中的Agent Map
        runningAgents.put(sid, learningManus);
        
        SseEmitter emitter = learningManus.runStream(message);
        
        // 完成时清理
        emitter.onCompletion(() -> runningAgents.remove(sid));
        emitter.onTimeout(() -> runningAgents.remove(sid));
        emitter.onError(e -> runningAgents.remove(sid));
        
        return emitter;
    }
    
    /**
     * 中断正在运行的Agent
     *
     * @param sessionId 会话ID
     * @return 中断结果
     */
    @PostMapping("/manus/stop")
    public ResponseEntity<Map<String, Object>> stopManus(@RequestParam String sessionId) {
        LearningManus agent = runningAgents.get(sessionId);
        if (agent != null) {
            agent.interrupt();
            runningAgents.remove(sessionId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Agent已中断"));
        }
        return ResponseEntity.ok(Map.of("success", false, "message", "未找到对应的Agent会话"));
    }

}
