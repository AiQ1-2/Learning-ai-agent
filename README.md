# 学习规划智能体 (Learning AI Agent)

基于 Spring AI 和通义千问的智能学习助手，支持 RAG 知识库问答和 Agent 工具调用。

## ✨ 功能特性

- 🤖 **智能问答助手**：基于知识库的智能问答，快速解答学习疑惑
- 🚀 **全能学习助理**：支持网络搜索、PDF生成、资源下载等工具调用
- 📚 **RAG 知识增强**：结合向量数据库实现精准的知识检索
- 💬 **流式对话**：支持 SSE 流式输出，实时展示思考过程
- 🛑 **中断控制**：支持随时中断 AI 生成

## 🛠️ 技术栈

### 后端
- Java 17+
- Spring Boot 3.x
- Spring AI
- 通义千问 (DashScope)
- PostgreSQL + pgvector
- MCP (Model Context Protocol)

### 前端
- Vue 3
- Vite
- Font Awesome

## 🚀 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- PostgreSQL 15+ (需安装 pgvector 扩展)
- Maven 3.8+

### 后端配置

1. 复制配置文件模板：
```bash
cd zcw-ai-agent/src/main/resources
cp application-example.yml application.yml
cp mcp-servers-example.json mcp-servers.json
```

2. 编辑 `application.yml`，填入你的配置：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/zcw_ai_agent
    username: your_username
    password: your_password
  ai:
    dashscope:
      api-key: your_dashscope_api_key  # 通义千问 API Key

search-api:
  api-key: your_search_api_key  # SearchAPI Key (用于网络搜索)
```

3. 编辑 `mcp-servers.json`，配置 MCP 服务：
```json
{
  "mcpServers": {
    "amap-maps": {
      "env": {
        "AMAP_MAPS_API_KEY": "your_amap_api_key"
      }
    }
  }
}
```

4. 启动后端：
```bash
cd zcw-ai-agent
./mvnw spring-boot:run
```

### 前端配置

1. 安装依赖：
```bash
cd zcw-ai-agent-frontend
npm install
```

2. 启动开发服务器：
```bash
npm run dev
```

3. 访问 http://localhost:5173

## 📁 项目结构

```
zcw-ai-agent/
├── src/main/java/com/zpark/learningagent/
│   ├── agent/          # Agent 智能体实现
│   ├── controller/     # API 控制器
│   ├── rag/            # RAG 知识增强
│   ├── tools/          # 工具调用实现
│   └── ...
├── src/main/resources/
│   ├── document/       # 知识库文档
│   └── ...
└── ...

zcw-ai-agent-frontend/
├── src/
│   ├── views/          # 页面组件
│   └── ...
└── ...
```

## 🔑 API Key 获取

- **通义千问 API Key**: https://dashscope.console.aliyun.com/
- **SearchAPI Key**: https://www.searchapi.io/
- **高德地图 API Key**: https://console.amap.com/

## 📝 开源协议

本项目采用 [MIT License](LICENSE) 开源协议。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！
