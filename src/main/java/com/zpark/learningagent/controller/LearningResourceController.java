package com.zpark.learningagent.controller;

import com.zpark.learningagent.service.LearningResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学习资源推荐控制器
 * 作用：提供HTTP 接口，让前端或其他系统可以调用推荐功能
 */
@RestController  // 标记为REST 控制器，返回 JSON 数据
@RequestMapping("/api/learning-resource")  // 设置基础路径为/api/learning-resource
@Tag(name = "学习资源推荐", description = "基于RAG的学习伙伴推荐API")  // Swagger 文档标签
public class LearningResourceController {

    // 注入推荐服务
    @Resource
    private LearningResourceService learningResourceService;

    /**
     * 推荐学习伙伴的接口
     * 访问地址：POST http://localhost:8080/api/learning-resource/recommend
     * @param query 学习需求描述（必填参数）
     * @param topK 返回前几个结果（可选参数，默认为3）
     * @return 格式化后的推荐结果字符串
     */
    @PostMapping("/recommend")  // 处理 POST 请求
    @Operation(summary = "推荐学习伙伴", description = "根据学习需求推荐合适的学习伙伴")  // Swagger 文档说明
    public String recommend(
            @RequestParam String query,  // 从请求参数中获取 query
            @RequestParam(defaultValue = "3") int topK) {  // 从请求参数中获取 topK，默认值为3
        
        // 调用服务层的推荐方法
        List<Document> matches = learningResourceService.recommendMatches(query, topK);
        
        // 格式化结果并返回
        return learningResourceService.formatRecommendations(matches);
    }

    /**
     * 测试接口
     * 访问地址：GET http://localhost:8080/api/learning-resource/test
     * @return 测试推荐结果
     */
    @GetMapping("/test")  // 处理 GET 请求
    @Operation(summary = "测试推荐功能")  // Swagger 文档说明
    public String test() {
        // 使用固定的查询条件进行测试
        String query = "我想找一个一起学习Java后端开发的伙伴";
        
        // 调用推荐服务，返回前2个结果
        List<Document> matches = learningResourceService.recommendMatches(query, 2);
        
        // 格式化并返回结果
        return learningResourceService.formatRecommendations(matches);
    }
}
