# 学科竞赛智能辅助系统

基于 RAG 与多模态内容生成的计算机类学科竞赛项目助手。系统围绕“竞赛知识库 + 四步选题分析智能体”建设，并提供方案、PPT、视频、申报检查与项目工作台能力。

## 项目边界

- 分析报告只基于可检索证据评价规则匹配、技术可行性、项目完整性、相似项目、选题风险和优化建议。
- 系统不预测获奖概率，不输出任何获奖率字段。
- 视频仅采用“脚本生成 + TTS 配音 + 用户素材合成”，不调用视频生成模型。
- 竞赛资料必须保留来源与发布日期，已过期规则不参与检索和问答。

## 技术栈

- 后端：Spring Boot 3、Java 17、Spring MVC、Spring Data JPA、Spring Data Redis
- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、Axios
- 基础设施：PostgreSQL、Redis、MinIO、Milvus
- 内容生成：Apache POI、PDFBox、OpenAI 兼容模型接口、TTS、FFmpeg

## 本地启动

### 1. 基础服务

```powershell
docker compose -f backend/docker-compose.yml up -d
```

该命令默认只启动 PostgreSQL、Redis、MinIO 与 Milvus，不要求预先打包后端。

### 2. 后端

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

本地联调可为 H2 模式，无需先启动 PostgreSQL。连接 Docker 中基础设施可使用默认的 PostgreSQL 配置：

```powershell
mvn spring-boot:run
```

后端默认地址为 `http://localhost:8080`，Swagger 地址为
`http://localhost:8080/swagger-ui.html`。

使用 `local` profile 时，数据库使用 H2，文件使用本地目录，向量检索使用数据库余弦
降级，Embedding 使用确定性本地实现，未配置 TTS 时使用本地演示音轨。生产模式默认连接
PostgreSQL、MinIO、Milvus 与远端 Embedding/LLM/TTS 服务。

也可以使用 Docker 多阶段构建一次启动完整系统：

```powershell
docker compose -f backend/docker-compose.yml --profile full up -d --build
```

### 3. 前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址为 `http://localhost:5173`，开发服务器会将 `/api` 转发到后端。

## 测试

```powershell
cd backend
mvn test

cd ..\frontend
npm run build
```

本机未安装 FFmpeg 时，真实 MP4 集成测试会明确跳过；安装 FFmpeg 后可完整验证视频流
与音频流。Docker 运行镜像已内置 FFmpeg。

## 配置

数据库、Redis、MinIO、Milvus、Embedding、LLM、TTS 与 FFmpeg 地址均通过环境变量覆盖，默认值见
`backend/src/main/resources/application.yml`。模型与 TTS 未配置时会返回明确的不可用状态，核心知识检索仍可使用本地向量降级存储。

常用的生产配置包括：

- `AI_BASE_URL`、`AI_API_KEY`、`AI_MODEL`：OpenAI 兼容模型。
- `EMBEDDING_BASE_URL`、`EMBEDDING_API_KEY`、`EMBEDDING_MODEL`：BGE/M3E 向量服务。
- `TTS_BASE_URL`、`TTS_API_KEY`、`TTS_VOICE`：TTS 服务。
- `FFMPEG_EXECUTABLE`：FFmpeg 可执行文件路径。
- `VECTOR_PROVIDER=milvus` 或 `database`，`EMBEDDING_PROVIDER=remote` 或 `local`。

