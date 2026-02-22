# 学习规划智能体改造方案（细粒度执行版）

## 一、改造原则

### 1.1 核心原则
- **只做改造，不加新功能**：保持现有功能结构，只替换业务领域
- **最小化改动**：优先使用重命名和内容替换，避免架构调整  
- **保持可运行**：每个步骤完成后项目都能正常编译运行
- **细粒度执行**：每次只改一个文件或一组强相关文件

### 1.2 改造范围
✅ 包名和类名重命名  
✅ 业务文本替换（恋爱 → 学习）  
✅ 知识库内容替换  
✅ Prompt内容调整  
❌ 不添加新工具  
❌ 不修改架构  
❌ 不改变数据库结构（仅改表名）

---

## 二、详细改造清单（共73个文件）

### 阶段1：项目基础配置（3个文件）

#### 1.1 pom.xml
**改动内容**：
- `<artifactId>zcw-ai-agent</artifactId>` → `<artifactId>learning-ai-agent</artifactId>`
- `<name>zcw-ai-agent</name>` → `<name>learning-ai-agent</name>`
- `<description>zcw-ai-agent</description>` → `<description>learning-ai-agent</description>`

**风险**：低  
**验证**：`mvn clean compile`

#### 1.2 application.yml
**改动内容**：
- `name: zcw-ai-agent` → `name: learning-ai-agent`
- `packages-to-scan: com.zpark.zcwaiagent.controller` → `packages-to-scan: com.zpark.learningagent.controller`
- 数据库名：`zcw_ai_agent` → `learning_ai_agent`（可选，暂不改）

**风险**：低  
**验证**：启动应用检查配置加载

#### 1.3 .gitignore / README等文档
**改动内容**：更新项目描述文本  
**风险**：无  
**验证**：无需验证

---

### 阶段2：包名重命名（自动化处理）

#### 2.1 包名重命名
**改动内容**：
```
com.zpark.zcwaiagent → com.zpark.learningagent
```

**涉及目录**：
- `src/main/java/com/zpark/zcwaiagent/` → `src/main/java/com/zpark/learningagent/`
- `src/test/java/com/zpark/zcwaiagent/` → `src/test/java/com/zpark/learningagent/`

**文件数量**：所有Java文件（约50+个）

**执行方式**：使用IDE的Refactor > Rename Package功能  
**风险**：中等（需要IDE自动处理import）  
**验证**：`mvn clean compile`

---

### 阶段3：核心业务类重命名（18个文件）

#### 3.1 应用层（app包）

**文件**：`LoveApp.java` → `LearningApp.java`  
**改动**：
- 类名：`LoveApp` → `LearningApp`
- 内部record：`LoveReport` → `LearningReport`
- 变量名：`loveAppVectorStore` → `learningVectorStore`
- 变量名：`loveAppRagCloudAdvisor` → `learningRagAdvisor`
- 系统Prompt：完整替换（见下文）

**风险**：中  
**验证**：编译通过 + 单元测试

---

#### 3.2 服务层（service包）

**文件**：`MatchmakingService.java` → `LearningResourceService.java`  
**改动**：
- 类名：`MatchmakingService` → `LearningResourceService`
- 方法名：`recommendMatches` → `recommendResources`
- 变量名：`userProfileVectorStore` → `learnerProfileVectorStore`
- 日志文本：`推荐匹配对象` → `推荐学习资源`
- 返回文本：`匹配对象` → `学习资源`

**风险**：低  
**验证**：编译通过

---

#### 3.3 RAG层（rag包）- 6个文件

**文件1**：`LoveAppDocumentLoader.java` → `LearningDocumentLoader.java`  
**改动**：
- 类名重命名
- 注释：`加载LoveApp文档` → `加载学习资料文档`
- 日志：`恋爱相关文档` → `学习资料文档`
- 路径：`classpath:document/*.md`（保持不变）

**文件2**：`LoveAppVectorStoreConfig.java` → `LearningVectorStoreConfig.java`  
**改动**：
- 类名重命名
- Bean名：`loveAppVectorStore` → `learningVectorStore`
- 注入变量：`loveAppDocumentLoader` → `learningDocumentLoader`

**文件3**：`LoveAppRagCustomAdvisorFactory.java` → `LearningRagAdvisorFactory.java`  
**改动**：
- 类名重命名
- 方法名：`createLoveAppRagCustomAdvisor` → `createLearningRagAdvisor`

**文件4**：`LoveAppRagCloudAdvisorConfig.java` → `LearningRagCloudConfig.java`  
**改动**：
- 类名重命名
- Bean名：`loveAppRagCloudAdvisor` → `learningRagAdvisor`

**文件5**：`LoveAppContextualQueryAugmenterFactory.java` → `LearningQueryAugmenterFactory.java`  
**改动**：
- 类名重命名

**文件6**：`UserProfileDocumentLoader.java` → `LearnerProfileLoader.java`  
**改动**：
- 类名重命名
- 路径：`classpath:user_profile/*.md` → `classpath:learner_profile/*.md`
- 注释：`用户画像` → `学习者画像`

**文件7**：`UserProfileVectorStoreConfig.java` → `LearnerProfileVectorConfig.java`  
**改动**：
- 类名重命名
- Bean名：`userProfileVectorStore` → `learnerProfileVectorStore`

**风险**：中  
**验证**：编译通过 + RAG功能测试

---

#### 3.4 控制器层（controller包）- 2个文件

**文件1**：`AiController.java`  
**改动**：
- 注入变量：`LoveApp loveApp` → `LearningApp learningApp`
- 接口路径：`/ai/love_app/chat/*` → `/ai/learning/chat/*`
- 方法名：`doChatWithLoveAppSync` → `doChatWithLearningSync`
- 方法名：`doChatWithLoveAppSSE` → `doChatWithLearningSSE`
- 方法调用：`loveApp.doChat()` → `learningApp.doChat()`

**文件2**：`MatchmakingController.java` → `LearningResourceController.java`  
**改动**：
- 类名重命名
- 注入服务：`MatchmakingService` → `LearningResourceService`
- 接口路径：`/matchmaking/*` → `/learning-resource/*`
- 方法名和返回文本调整

**风险**：低  
**验证**：启动应用 + 接口测试

---

#### 3.5 Agent层（agent包）- 1个文件

**文件**：`YuManus.java` → `LearningManus.java`  
**改动**：
- 类名：`YuManus` → `LearningManus`
- 名称：`yuManus` → `learningManus`
- 系统Prompt：完整替换（见下文）

**风险**：低  
**验证**：编译通过 + Agent测试

---

### 阶段4：测试文件重命名（10个文件）

#### 4.1 测试类重命名
- `ZcwAiAgentApplicationTests.java` → `LearningAiAgentApplicationTests.java`
- `LoveAppTest.java` → `LearningAppTest.java`
- `YuManusTest.java` → `LearningManusTest.java`
- `LoveAppDocumentLoaderTest.java` → `LearningDocumentLoaderTest.java`
- 其他测试文件保持不变（工具类测试）

**改动内容**：
- 类名重命名
- 测试方法中的变量名调整
- 断言文本调整

**风险**：低  
**验证**：`mvn test`

---

### 阶段5：资源文件替换（16个文件）

#### 5.1 知识库文档（document目录）

**删除文件**：
- `恋爱常见问题和回答 - 单身篇.md`
- `恋爱常见问题和回答 - 恋爱篇.md`
- `恋爱常见问题和回答 - 已婚篇.md`

**新建文件**：
- `学习方法 - 时间管理.md`
- `学习方法 - 记忆技巧.md`
- `学习方法 - 考试准备.md`

**内容示例**（时间管理.md）：
```markdown
# 时间管理方法

## 番茄工作法
番茄工作法是一种时间管理方法，将工作分为25分钟的专注时段...

## 时间块规划
将一天划分为不同的时间块，每个时间块专注于特定任务...

## 优先级矩阵
使用艾森豪威尔矩阵区分任务的重要性和紧急性...
```

**风险**：低  
**验证**：RAG检索测试

---

#### 5.2 用户画像文件（user_profile → learner_profile）

**目录重命名**：
- `src/main/resources/user_profile/` → `src/main/resources/learner_profile/`

**文件重命名**：
- `user-001.md` → `learner-001.md`
- `user-002.md` → `learner-002.md`
- ... (共10个文件)

**内容替换示例**（learner-001.md）：
```markdown
# 学习者画像 - 001

## 基本信息
- 姓名：张三
- 年龄：22岁
- 身份：大学生

## 学习目标
- 准备考研，目标计算机专业
- 需要提升数学和专业课水平

## 学习偏好
- 喜欢视频教程
- 每天可学习4-6小时
- 擅长逻辑思维，记忆力一般

## 当前困难
- 时间管理困难
- 数学基础薄弱
- 缺乏系统学习计划
```

**风险**：低  
**验证**：向量检索测试

---

### 阶段6：Prompt内容替换

#### 6.1 LearningApp系统Prompt

**原内容**：
```java
private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
        "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
        "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
        "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";
```

**新内容**：
```java
private static final String SYSTEM_PROMPT = "你是一位专业的学习规划导师，擅长为不同背景的学习者制定个性化学习计划。" +
        "开场向用户表明身份，告知用户可以咨询学习规划相关问题。" +
        "围绕学习目标、当前水平、可用时间三个维度提问：" +
        "学习目标方面询问是考研、考证还是技能提升；" +
        "当前水平方面询问基础知识掌握情况和学习困难点；" +
        "可用时间方面询问每天可投入的学习时长和学习偏好。" +
        "引导用户详述学习目标、当前困难和期望效果，以便给出个性化学习方案。";
```

---

#### 6.2 LearningManus Agent Prompt

**原内容**：
```java
String SYSTEM_PROMPT = """  
        You are YuManus, an all-capable AI assistant designed to solve tasks efficiently using available tools.
        
        IMPORTANT RULES:
        1. If the user asks a simple question (like "who are you", "hello", "how are you"), answer briefly and IMMEDIATELY call the `terminate` tool.
        2. Only use tools when the task requires external actions (search, file operations, etc.).
        3. For conversational queries without action requirements, respond and terminate.
        4. Never repeat the same response multiple times.
        """;
```

**新内容**：
```java
String SYSTEM_PROMPT = """  
        You are LearningManus, an AI learning assistant designed to help users achieve their learning goals efficiently.
        
        CAPABILITIES:
        1. Create personalized learning plans based on user goals
        2. Search for learning resources and materials
        3. Generate learning reports and summaries
        4. Answer questions about various subjects
        
        IMPORTANT RULES:
        1. If the user asks a simple question (like "who are you", "hello"), answer briefly and IMMEDIATELY call the `terminate` tool.
        2. Always understand user's learning goals before creating plans
        3. Use tools when needed (search for resources, save plans to files, etc.)
        4. For conversational queries without action requirements, respond and terminate.
        5. Never repeat the same response multiple times.
        """;
```

---

## 三、执行顺序和验证点

### 执行顺序
```
阶段1（配置文件）→ 阶段2（包名）→ 阶段3（核心类）→ 阶段4（测试）→ 阶段5（资源）→ 阶段6（Prompt）
```

### 每阶段验证点
1. **阶段1完成**：`mvn clean compile` 通过
2. **阶段2完成**：`mvn clean compile` 通过，无import错误
3. **阶段3完成**：`mvn clean compile` 通过，启动应用成功
4. **阶段4完成**：`mvn test` 通过
5. **阶段5完成**：RAG检索返回学习相关内容
6. **阶段6完成**：对话测试符合学习场景

---

## 四、风险控制

### 4.1 回滚策略
- 每个阶段完成后提交Git
- 出现问题立即回滚到上一个稳定版本

### 4.2 测试策略
- 每个阶段完成后运行编译测试
- 核心功能完成后进行集成测试
- 最终进行完整的端到端测试

### 4.3 注意事项
- 包名重命名必须使用IDE的Refactor功能，避免手动修改
- 所有字符串常量中的"恋爱"、"love"等关键词都需要替换
- 日志输出文本也需要同步更新
- 注释和文档也要同步更新

---

## 五、预期结果

改造完成后，项目将：
- ✅ 包名变更为 `com.zpark.learningagent`
- ✅ 核心类名符合学习场景
- ✅ 接口路径变更为 `/ai/learning/*`
- ✅ 知识库包含学习方法和学科知识
- ✅ 学习者画像替换用户画像
- ✅ Prompt适配学习规划场景
- ✅ 所有测试通过
- ✅ 应用正常启动和运行

**不会改变**：
- 技术架构（Agent、RAG、工具调用）
- 工具类（保持原有9个工具）
- 数据库结构（仅表名可选修改）
- 核心业务逻辑流程

---

## 六、总结

本方案采用细粒度、分阶段的改造策略，只做业务领域替换，不添加新功能。共涉及73个文件的修改，分6个阶段执行，每个阶段都有明确的验证点，风险可控，可随时回滚。

**核心改造**：恋爱咨询 → 学习规划  
**保持不变**：技术架构、工具体系、业务流程  
**预计耗时**：2-3天（取决于测试和调试时间）
