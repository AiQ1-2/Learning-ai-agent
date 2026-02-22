package com.zpark.learningagent.tools;

import com.zpark.learningagent.tools.FileOperationTool;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class FileOperationToolTest {

    @Test
    public void testReadFile() {
        // 创建FileOperationTool实例，作用：文件操作工具类
        FileOperationTool tool = new FileOperationTool();
        String fileName = "流浪学习日志.txt";
        String result = tool.readFile(fileName);
        assertNotNull(result);
    }

    @Test
    public void testWriteFile() {
        FileOperationTool tool = new FileOperationTool();
        String fileName = "流浪学习日志.txt";
        String content = "不要偷窥我的学习日志，请你自己离开";
        String result = tool.writeFile(fileName, content);
        assertNotNull(result);
    }
}
