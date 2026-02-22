package com.zpark.learningagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import com.zpark.learningagent.constant.FileConstant;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 文件操作工具类
 */
public class FileOperationTool {

    private final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/file";

    /**
     * 读取文件内容
     *
     * @param fileName 文件名
     * @return 文件内容
     */
    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of the file to read") String fileName) {
       String filePath = FILE_DIR+ "/" + fileName;
       try{
           return FileUtil.readUtf8String(filePath);
       } catch (IORuntimeException e) {

           return "Eroor reading file :"+e.getMessage();
       }
    }
    /**
     * 写入文件内容
     *
     * @param fileName 文件名
     * @param content 文件内容
     * @return 文件写入结果
     */
  @Tool(description = "Write content to a file")
    public String writeFile(
        @ToolParam(description = "Name of the file to write") String fileName,
        @ToolParam(description = "Content to write to the file") String content) {
       String filePath = FILE_DIR+ "/" + fileName;
       try{
           // 创建目录
           FileUtil.mkdir(FILE_DIR);
           // 写入文件
           FileUtil.writeUtf8String(content,filePath);
           return "File written successfully"+filePath;
       } catch (IORuntimeException e) {
           return "Error writing file :"+e.getMessage();
       }
    }
}
