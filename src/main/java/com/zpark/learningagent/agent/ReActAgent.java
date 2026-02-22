package com.zpark.learningagent.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ReAct (Reasoning and Acting) 模式的代理抽象类  
 * 实现了思考-行动的循环模式
 */  
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ReActAgent extends BaseAgent {
    /**
     * 处理当前的状态并决定下一步行动
     * @return
     */
   public abstract boolean think();
   /**
     * 执行当前步骤
     * @return 行动执行结果
     */
   public abstract String act();

   @Override
    public  String step() {
       try {
           // 思考
           boolean thinkResult = think();
           if (!thinkResult) {
               // 思考失败
               return "Thinking failed";
           }
           // 执行
           String actResult = act();
           return actResult;
       } catch (Exception e) {
           //记录异常
           e.printStackTrace();
           return "Error running agent: " + e.getMessage();
       }
   }
}
