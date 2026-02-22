package com.zpark.learningagent.utils;

/**
 * 用于存储apiKey
 * 注意：请在环境变量或配置文件中设置API_KEY，不要硬编码
 */
public class ApiKey {
    // 从环境变量获取，或在application.yml中配置
    String API_KEY = System.getenv("DASHSCOPE_API_KEY");
}
