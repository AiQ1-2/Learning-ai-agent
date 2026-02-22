package com.zpark.learningagent.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration  // 标记为Spring配置类，用于注册Bean
public class ToolRegistration {

    @Value("${search-api.api-key}")  // 从配置文件中注入搜索API的密钥
    private String searchApiKey;

    @Bean  // 注册为Spring Bean，返回所有工具的回调数组
    public ToolCallback[] allTools() {
        // 创建文件操作工具实例，用于读写文件操作
        FileOperationTool fileOperationTool = new FileOperationTool();
        // 创建网络搜索工具实例，使用注入的API密钥进行百度搜索
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        // 创建网页抓取工具实例，用于抓取网页内容
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        // 创建资源下载工具实例，用于下载网络资源
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        // 创建终端操作工具实例，用于执行系统命令
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        // 创建PDF生成工具实例，用于生成PDF文档
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        // 创建图片搜索工具实例，用于搜索网络图片
        ImageSearchTool imageSearchTool = new ImageSearchTool();
        //创建终止工具实例，用于终止当前任务
        TerminateTool terminateTool = new TerminateTool();
        // 使用ToolCallbacks工具类将所有工具实例转换为回调数组并返回
        return ToolCallbacks.from(
            fileOperationTool,      // 文件操作工具回调
            webSearchTool,          // 网络搜索工具回调
            webScrapingTool,        // 网页抓取工具回调
            resourceDownloadTool,   // 资源下载工具回调
            terminalOperationTool,  // 终端操作工具回调
            pdfGenerationTool,      // PDF生成工具回调
            imageSearchTool,         // 图片搜索工具回调
             terminateTool            // 终止工具回调
        );
    }
}
