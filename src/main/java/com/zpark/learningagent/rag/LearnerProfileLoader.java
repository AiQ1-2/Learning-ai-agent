package com.zpark.learningagent.rag;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class LearnerProfileLoader {
    private static final Logger log = LoggerFactory.getLogger(LearnerProfileLoader.class);

    //资源解析器，用于查找和加载文件资源
    private final ResourcePatternResolver resourcePatternResolver;
    //构造函数注入
    public LearnerProfileLoader(ResourcePatternResolver resourcePatternResolver){
        this.resourcePatternResolver = resourcePatternResolver;
    }
    /**
     * 加载所有的学习者画像文档
     */
    public List<Document> loadLearnerProfiles(){
        //创建一个空列表，用于存储所有的学习者画像文档
        List<Document> allDocuments = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:user_profile/*.md");
            for (Resource resource : resources){
                String fileName = resource.getFilename();
                //创建一个Markdown文档读取器，并设置参数
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        // 遇到水平线（---）时创建新文档，这样可以将一个文件分成多个文档块
                        .withHorizontalRuleCreateDocument(true)
                        // 不包含代码块内容（学习者画像不需要代码）
                        .withIncludeCodeBlock(false)
                        // 不包含引用块内容
                        .withIncludeBlockquote(false)
                        // 给每个文档添加元数据：文件名
                        .withAdditionalMetadata("filename", fileName)
                        // 给每个文档添加元数据：类型标记为学习者画像
                        .withAdditionalMetadata("type", "learner-profile")
                        // 构建配置对象
                        .build();
                //创建一个Markdown文档读取器，并传入资源文件和配置对象
                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource,config);
                // reader.get() 返回该文件解析出的所有 Document 对象
                allDocuments.addAll(reader.get());

                // 记录日志：成功加载了哪个文件，以及解析出多少个文档
                log.info("成功加载学习者画像文件: {}, 文档数量: {}", fileName, reader.get().size());
            }
        }catch (IOException e){
            log.error("学习者画像文档加载失败",e);
        }
        return allDocuments;
    }
}
