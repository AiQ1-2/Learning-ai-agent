package com.zpark.learningagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class LearningManusTest {

    @Resource
    private LearningManus learningManus;

    @Test
    void run() {
        String userPrompt = """  
                我想学习Java后端开发，请帮我找一些适合初学者的学习资源，
                并结合网络上的教程，制定一份详细的学习计划，
                并以 PDF 格式输出""";
        String answer = learningManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}
