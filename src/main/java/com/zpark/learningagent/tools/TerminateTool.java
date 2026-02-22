package com.zpark.learningagent.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * 终止工具类，用于结束AI助手的交互任务
 * 提供终止当前工作流程的功能
 */
public class TerminateTool {

    /**
     * 当请求得到满足或助手无法继续执行任务时终止交互
     * 当所有任务完成后调用此工具结束工作
     * @return 包含任务结束信息的字符串
     */
    @Tool(description = """  
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.  
            "When you have finished all the tasks, call this tool to end the work.  
            """)
    public String doTerminate() {
        return "任务结束";
    }
}

