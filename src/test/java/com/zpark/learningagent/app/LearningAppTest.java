package com.zpark.learningagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LearningAppTest {
@Resource
private LearningApp learningApp;
    @Test
    void doTest() {
        String chatId = UUID.randomUUID().toString();
        //第一轮对话
        String message = "你好，我是小明";
        String result = learningApp.doChat(message, chatId);
        System.out.println("第一轮回复：" + result);
        Assertions.assertFalse(result.isEmpty());
        //第二轮对话
        message = "我想学习Java后端开发，你有什么好的学习建议吗？";
        result = learningApp.doChat(message, chatId);
        System.out.println("第二轮回复：" + result);
        Assertions.assertFalse(result.isEmpty());
        //第三轮对话
        message = "请问我是谁，我想学什么？";
        result = learningApp.doChat(message, chatId);
        System.out.println("第三轮回复：" + result);
        Assertions.assertFalse(result.isEmpty());
    }
    @Test
    void doTestWithReport() {
        String chatId = UUID.randomUUID().toString();
        //第一轮对话
        String message = "你好，我是小明，我想学习Python数据分析，但是不知道从哪里开始";
        LearningApp.LearningReport report = learningApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(report);
    }
    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经学了一段时间编程，但是感觉进步很慢，怎么办？";
        String answer =  learningApp.doChatWithRAG(message, chatId);
        Assertions.assertNotNull(answer);
    }
    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        testMessage("推荐几个适合初学者的Java学习网站");

        // 测试网页抓取：学习资源分析
        testMessage("看看编程导航网站（codefather.cn）有哪些适合新手的学习路线？");

        // 测试资源下载：图片下载
        testMessage("下载一张Java学习路线图保存为文件");

        // 测试终端操作：执行代码
        testMessage("执行 Python3 脚本来生成学习数据分析报告");

        // 测试文件操作：保存学习档案
        testMessage("保存我的学习档案为文件");

        // 测试 PDF 生成
        testMessage("生成一份'Java后端学习计划'PDF，包含学习路线、资源推荐和时间安排");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = learningApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }
    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        // 测试地图 MCP
        String message = "我住在上海静安区，请帮我找附近5公里内适合学习的咖啡馆或图书馆";
        String answer =  learningApp.doChatWithMcp(message, chatId);
    }


}
