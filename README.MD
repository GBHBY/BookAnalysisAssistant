# Book Analysis Assistant

基于 Spring Boot 的武侠小说知识库问答：入库向量化、检索、多轮对话、Agent 自动决定是否查库。

## 演示视频

项目示例视频：[`示例.mp4`](./示例.mp4)

如果当前平台无法直接预览，可点击链接后下载查看。

## 主要功能

- **问题改写与检索增强**：结合当前会话历史，先用模型对用户问题做 Query Rewrite，将“他/她/那本书/上次说的”等指代补全为明确的人物、作品、门派、武功或情节关键词，生成更适合向量检索的「推荐查询」，再交给 Agent 决定是否调用知识库工具，提升多轮追问场景下的召回准确率；实现见 `QueryService`、`SmartChatController`。
- **Agent + RAG 检索链路**：基于 Spring AI / Spring AI Alibaba Agent 构建可调用工具的对话链路，Agent 通过 `searchKnowledgeBase` 工具按需触发知识库检索，对用户问题做相似向量搜索，并将命中的文本片段注入上下文后生成回答，减少模型脱离知识库自由发挥；实现见 `KnowledgeSearchTool`、`SmartChatController`、`EmbeddingService`。
- **检索过滤与精准召回**：检索阶段支持按 `domain`、`category`、`keywords` 等元数据构建过滤表达式，结合 pgvector 做“语义相似度 + 元数据条件”混合检索，适合区分不同书名、人物主题和剧情类型，降低跨作品误召回；实现见 `FilterExpressionBuilder`、`KnowledgeSearchTool`、`EmbeddingService`。
- **历史压缩与上下文控制**：针对长会话先异步摘要历史对话，保留已讨论作品、人物关系、关键结论与未完成追问，再把摘要而非完整历史送入 Prompt，在维持上下文连续性的同时节省 token；实现见 `MemeryService`、`SmartChatController`。
- **流式对话与思考态输出**：提供 `GET /api/smart-chat/stream-chat` 流式接口，以 NDJSON 形式连续返回“问题改写结果、历史摘要、最终答案、引用信息”等节点，方便前端实时展示思考过程与回答生成过程，优化交互体验；实现见 `SmartChatController`。
- **工具调用配额控制**：为单轮对话维护工具调用上下文和调用上限，限制知识库搜索次数，避免 Agent 在不必要场景下重复检索导致延迟上升和成本浪费；实现见 `ToolCallQuotaService`、`KnowledgeSearchTool`。
- **引用收集与可追溯回答**：对单轮工具命中的文档片段进行统一收集，输出来源文档名、片段摘要与相关分数，支持前端展示引用来源，增强回答可解释性和可追溯性；实现见 `TurnCitationCollector`。
- **PDF 入库与自动元数据抽取**：支持 PDF 上传导入，完成文档读取、清洗、分句切分、Embedding 计算与向量写库；同时基于模型自动抽取书名/领域、内容类别、关键词、摘要等元数据，为后续过滤检索提供结构化支撑；实现见 `EmbeddingController`、`EmbeddingService`、`DocumentMetadataService`。
- **多模型接入与嵌入能力**：项目同时接入通义、智谱等模型能力，用于聊天、问题改写、历史摘要、文档元数据提取和向量化，可按场景灵活选择模型服务，方便后续扩展和替换；相关依赖与配置见 `pom.xml`、`application-*.yml`。
- **意图识别与能力扩展**：提供独立的意图识别服务，可将用户输入划分为知识问答、订单查询、闲聊、常识问题等类型，并提取关键参数，为后续接入客服、业务工单等多场景 Agent 流程预留扩展点；实现见 `IntentRecognitionService`。
- **会话持久化与多轮记忆**：多轮对话历史持久化到 MySQL，并结合历史加载、摘要压缩、问题改写共同支撑连续追问体验；前端静态页位于 `src/main/resources/static/front`，可直接联调用于演示完整对话链路。

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
