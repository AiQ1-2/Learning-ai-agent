package com.zpark.learningagent.app;

import com.zpark.learningagent.advisor.MyLoggerAdvisor;
import com.zpark.learningagent.chatmemory.FileBasedChatMemory;
import com.zpark.learningagent.rag.LearningRagAdvisorFactory;
import com.zpark.learningagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
public class LearningApp {
    
    private static final Logger log = LoggerFactory.getLogger(LearningApp.class);

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "你是一位专业的学习规划导师，擅长为不同背景的学习者制定个性化学习计划。" +
            "开场向用户表明身份，告知用户可以咨询学习规划相关问题。" +
            "围绕学习目标、当前水平、可用时间三个维度提问：" +
            "学习目标方面询问是考研、考证还是技能提升；" +
            "当前水平方面询问基础知识掌握情况和学习困难点；" +
            "可用时间方面询问每天可投入的学习时长和学习偏好。" +
            "引导用户详述学习目标、当前困难和期望效果，以便给出个性化学习方案。";

    public LearningApp(ChatModel chatModel){
  /**
 * 初始化聊天客户端，配置系统提示和基于内存的聊天记忆。
 *
 * 该方法创建了一个基于内存的聊天记忆实例，并将其与聊天客户端绑定，
 * 同时设置了默认的系统提示和消息顾问。
 */
//初始化基于内存的聊天记忆
//ChatMemory chatMemory = new InMemoryChatMemory();
  //初始化基于文件的聊天记忆
  String fileDir = System.getProperty("user.dir")+"/tmp/chat-memory";
  ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
/**
 * 构建聊天客户端实例。
 *
 * 使用指定的聊天模型、系统提示和聊天记忆顾问来构建聊天客户端。
 *
 * @param chatModel 聊天模型，用于处理聊天逻辑
 * @param SYSTEM_PROMPT 系统提示，定义聊天的默认行为或上下文
 * @param chatMemory 聊天记忆实例，用于存储和管理聊天历史
 * @return 配置完成的聊天客户端实例
 */
chatClient = ChatClient.builder(chatModel)
        .defaultSystem(SYSTEM_PROMPT)
        .defaultAdvisors(
                new MessageChatMemoryAdvisor(chatMemory),
                //自定义日志拦截器
                new MyLoggerAdvisor()
                //自定义推理增强Advisor
//                ,new SimpleLoggerAdvisor()
        )
        .build();
    }

    /**
     * 编写对话方法
     */
    public String doChat(String message, String chatId) {
        log.info("用户：{}", message);
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("AI回复：{}", content);
        return content;
    }
  /**
 * LearningReport 是一个记录类（record），用于封装学习报告的相关信息。
 *
 * @param title       报告的标题，表示该学习报告的主题或名称。
 * @param suggestions 建议列表，包含针对该学习报告提出的具体建议内容。
 */
record LearningReport(String title, List<String> suggestions) {
}

    /**
     *Ai 报告功能
     */
    public LearningReport doChatWithReport(String message, String chatId) {
        log.info("用户：{}", message);
        LearningReport response = chatClient
                .prompt()
                .system  (SYSTEM_PROMPT+"每次对话后都要生成学习报告，标题为{用户名}的学习报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LearningReport.class);
        log.info("response{}", response);
        return response;
    }
    //-----------------------RAG 功能---------------------------------
    @Resource
    private VectorStore learningVectorStore;

    @Resource
    private Advisor learningRagAdvisor;
    @Resource
    private VectorStore  pgVectorVectorStore;;

    @Resource
    private QueryRewriter queryRewriter;

      // RAG问答功能实现方法
    // 结合向量检索和大语言模型，提供基于知识库的智能问答
    public String doChatWithRAG(String message, String chatId) {
        //重写用户提问信息
        String rewriteMessage = queryRewriter.doQueryRewrite(message);
        // 构建聊天请求并获取响应
        // 使用链式调用方式构建完整的聊天流程
        ChatResponse chatResponse = chatClient
                // 创建新的聊天提示
                .prompt()
                // 设置用户输入的消息内容
                .user(rewriteMessage)//使用重写过后的信息
                // 配置聊天记忆参数，关联当前对话ID
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                // 添加自定义日志记录顾问，用于监控请求和响应
                .advisors(new MyLoggerAdvisor())
                // 添加RAG问答顾问，基于向量存储进行相似文档检索
                // 该顾问会自动检索与用户问题最相关的学习知识文档
//                .advisors(new QuestionAnswerAdvisor(learningVectorStore))
                //基于阿里云知识服务
//                .advisors(learningRagAdvisor)
                //基于RAG增强检索服务（pgVector）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                //自定义RAG检索增强服务
                .advisors(LearningRagAdvisorFactory.createLearningRagAdvisor(
                        learningVectorStore,
                        "学习方法"
                ))
                // 执行聊天请求并获取响应
                .call()
                // 提取聊天响应对象
                .chatResponse();
        // 从响应中提取AI生成的文本内容
        String content = chatResponse.getResult().getOutput().getText();
        // 记录AI回复内容到日志
         log.info("content:" , content);
        // 返回AI生成的回答内容
        return content;
    }
    //--------集中调用Ai工具类-----------------------------------
    @Resource
    private ToolCallback[] allTools;
    /**
     * 带工具调用的聊天方法
     * 支持AI调用各种工具来完成复杂任务
     *
     * @param message 用户输入的消息内容
     * @param chatId 会话ID，用于区分不同用户的对话历史
     * @return AI生成的回复内容
     */
    public String doChatWithTools(String message, String chatId) {
        // 构建聊天请求并获取响应
        ChatResponse response = chatClient
                // 创建新的聊天提示
                .prompt()
                // 设置用户输入的消息内容
                .user(message)
                // 配置聊天记忆参数
                .advisors(spec -> spec
                        // 关联当前对话ID，用于检索相关历史记录
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        // 设置检索的历史消息数量为10条
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 注册所有可用的工具回调，使AI能够调用这些工具
                .tools(allTools)
                // 执行聊天请求并获取响应
                .call()
                // 提取聊天响应对象
                .chatResponse();
        // 从响应结果中提取AI生成的文本内容
        String content = response.getResult().getOutput().getText();
        // 记录AI回复内容到日志，便于调试和监控
        log.info("content: {}", content);
        // 返回AI生成的回答内容
        return content;
    }

    /**
     * 调用MCP工具
     */
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
    /**
     * 带流式输出的聊天方法
     * 支持AI的流式输出，实时返回结果
     *
     * @param message 用户输入的消息内容
     * @param chatId 会话ID，用于区分不同用户的对话历史
     * @return AI生成的回复内容
     */
    public Flux<String> doChatByStream(String message, String chatId) {
    return chatClient
            .prompt()
            .user(message)
            .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                    .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
            .stream()
            .content();
}





}

