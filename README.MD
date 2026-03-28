# Book Analysis Assistant

基于 Spring Boot 的图书 / 知识库问答助手：支持文本入库、向量检索（RAG）、多轮对话与智能对话（Agent）。

## 功能概览

- **RAG**：文档嵌入写入 PostgreSQL（pgvector），检索增强生成。
- **对话**：多轮会话与历史持久化（JDBC Chat Memory）。
- **模型**：集成阿里云 DashScope（通义）、智谱等（见 `pom.xml` 与配置）。
- **前端**：静态页面对话界面（`src/main/resources/static/front`）。

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
