# Book Analysis Assistant

基于 Spring Boot 的武侠小说知识库问答：入库向量化、检索、多轮对话、Agent 自动决定是否查库。

## 主要功能

- **问题改写**：结合当前会话历史，用模型把用户问句改写成适合向量检索的一句话（补全指代、突出人名/门派/武功等关键词），作为「推荐查询」交给 Agent；实现见 `QueryService`、`SmartChatController`。
- **RAG 检索**：Agent 可调用工具 `searchKnowledgeBase`，对查询做相似向量检索（pgvector），把命中文本片段拼进上下文再回答；实现见 `KnowledgeSearchTool`、`EmbeddingService`。
- **历史压缩**：长对话可先摘要再进 Prompt，减轻上下文长度；实现见 `MemeryService`。
- **工具与配额**：每轮对话限制知识库搜索次数（避免反复调工具）；实现见 `ToolCallQuotaService`。
- **引用收集**：工具返回的文档片段可汇总，便于前端展示来源；实现见 `TurnCitationCollector`。
- **流式对话**：`GET /api/smart-chat/stream-chat`，NDJSON 流式返回（含思考类节点与最终回答）。
- **向量入库**：文本/文件走嵌入写入向量库；另有 `GET /api/embedding/import/file` 按路径导入。文档上传与元数据见 `RAGService`（部分检索链路仍可按 DashScope 能力扩展）。
- **意图识别**：`IntentRecognitionService` 可按用户说法归类（订单/知识/闲聊等），供其它流程接入。
- **会话持久化**：多轮历史存 MySQL，Spring AI JDBC Chat Memory；前端静态页在 `src/main/resources/static/front`（与仓库根目录 `front` 对应资源一致时可同步）。

**模型**：通义（DashScope）、智谱等，依赖与版本见 `pom.xml`。

## 技术栈

| 类别 | 说明 |
|------|------|
| 运行时 | Java 17 |
| 框架 | Spring Boot 3.5.x、Spring Web / WebFlux |
| AI | Spring AI Alibaba Agent、DashScope、Zhipu 等 Starter |
| 关系库 | MySQL（业务与对话记忆等） |
| 向量库 | PostgreSQL + pgvector |

## 环境要求

- JDK 17+
- Maven 3.8+
- 可访问的 **MySQL**、**PostgreSQL（含 pgvector 扩展）**
- 各模型厂商的 **API Key**（在配置中填写，勿提交到仓库）

## 快速启动

```bash
mvn -q -DskipTests spring-boot:run
```

默认端口见配置（一般为 `8080`）。浏览器访问根路径或静态前端入口即可使用对话界面。

使用不同环境时，通过 Spring Profile 切换，例如：

```bash
mvn -q -DskipTests spring-boot:run -Dspring-boot.run.profiles=home
```

（具体 profile 名称以项目中 `application-*.yml` 为准。）

## 配置说明

在对应的 `application-*.yml` 中配置：

- MySQL 主数据源（`spring.datasource.primary`）
- PostgreSQL 向量数据源（`spring.datasource.vector`）
- `spring.ai.dashscope`、`spring.ai.zhipuai` 等 API Key 与模型名

**请勿将含真实密钥的配置文件提交到公开仓库**；本地可使用 `application-local.yml`（已 gitignore）或环境变量覆盖。

## 项目结构（简要）

```
src/main/java/com/book/analysis/assistant/
├── web/           # REST 接口（对话、嵌入等）
├── service/       # RAG、对话、意图、配额等业务
├── emdedding/     # 嵌入与向量写入
├── config/        # 数据源、Chat、Agent 等配置
├── entity/ dto/ mapper/  # 持久化与传输
└── BookAnalysisAssistantApplication.java
```

## 构建

```bash
mvn -q clean package -DskipTests
```

产物为 `target/BookAnalysisAssistant-0.0.1-SNAPSHOT.jar`。

## 许可证

未特别声明时，以仓库内 LICENSE 或团队约定为准。
