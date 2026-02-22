package com.zpark.learningagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LearningDocumentLoaderTest {
    @Resource
    private LearningDocumentLoader loader;
    @Test
    void loadMarkdowns() {
        loader.loadMarkdowns();
    }
}