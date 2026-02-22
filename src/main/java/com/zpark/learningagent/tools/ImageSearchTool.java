package com.zpark.learningagent.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图片搜索工具
 * 使用 Pexels API 搜索图片
 */
public class ImageSearchTool {

    // Pexels API 密钥
    private static final String API_KEY = "u2ys1m826uNVg5fZiILKNUVNOXv0oEnnaqL5pQ6jEJzmGQdCBtKnDe06";

    // Pexels 搜索接口
    private static final String API_URL = "https://api.pexels.com/v1/search";

    /**
     * 搜索图片
     * @param query 搜索关键词
     * @return 图片 URL 列表（逗号分隔）
     */
    @Tool(description = "Search images from web using Pexels API")
    public String searchImage(
            @ToolParam(description = "Search query keyword") String query) {
        try {
            List<String> imageUrls = searchMediumImages(query);
            return String.join(",", imageUrls);
        } catch (Exception e) {
            return "Error searching image: " + e.getMessage();
        }
    }

    /**
     * 搜索中等尺寸的图片列表
     * @param query 搜索关键词
     * @return 图片 URL 列表
     */
    private List<String> searchMediumImages(String query) {
        // 设置请求头（包含 API 密钥）
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", API_KEY);

        // 设置请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("per_page", 5);  // 限制返回数量

        // 发送 GET 请求
        String response = HttpUtil.createGet(API_URL)
                .addHeaders(headers)
                .form(params)
                .execute()
                .body();

        // 解析响应 JSON
        return JSONUtil.parseObj(response)
                .getJSONArray("photos")
                .stream()
                .map(photoObj -> (JSONObject) photoObj)
                .map(photoObj -> photoObj.getJSONObject("src"))
                .map(photo -> photo.getStr("medium"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}
