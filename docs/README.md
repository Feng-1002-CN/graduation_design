# 学科竞赛智能辅助系统 — 总体规划与阶段路线图

> 本文档是项目的「总纲」。所有阶段文档（`00` ~ `11`）均遵循本文件定义的定位、架构、通用约定与阶段顺序。
> 每个阶段文档末尾都附有可直接粘贴给 AI 的实现提示词，用于驱动 AI 生成对应代码。

---

## 1. 项目概述

面向**计算机类学科竞赛**参赛学生与指导教师的「竞赛项目助手」，覆盖**赛事查询 → 规则解读 → 选题分析 → 申报材料与展示内容生成**全流程。

- **核心主线**（必须重点实现）：竞赛知识库 + 选题分析智能体。
- **配套能力**：PPT 模板生成（可编辑）、视频「脚本 + 配音 + 素材合成」（可播放，不自研视频模型）。
- **验收标准**：录入 ≥3 类竞赛资料，完成多模拟项目选题分析，现场生成可编辑 PPT 与可播放视频。

## 2. 核心定位与边界（防膨胀约束，全阶段强制遵守）

1. **AI 绝不预测获奖概率**，只能用「规则匹配、技术可行性、项目完整性、相似项目、选题风险、优化建议」六个客观维度评估。
2. 竞赛信息必须记录**来源与发布日期**，禁用过期规则（检索时按日期过滤）。
3. 视频仅做「脚本 + 配音 + 素材合成」，**不碰视频生成大模型**。
4. 聚焦核心主线，任何阶段不得擅自扩展超出本阶段「范围（In/Out）」的功能。

## 3. 系统功能模块总览

| 编号 | 模块名称 | 一句话定位 | 优先级 | 依赖阶段 |
|------|----------|-----------|--------|----------|
| 00 | 基础设施与工程脚手架 | 工程、中间件、通用框架、鉴权与统一返回 | 前置 | — |
| 01 | 竞赛信息管理 | 竞赛/组别/规则的结构化 CRUD，带来源与日期元数据 | P0 | 00 |
| 02 | 竞赛资料知识库 | PDF/Word 上传→解析→切片→向量化→入库（RAG 底座） | P0 | 00,01 |
| 03 | 竞赛规则解读 | 基于 RAG 回答报名/评分要求，强制标注出处 | P0 | 02 |
| 04 | 智能竞赛检索 | 基于用户画像（年级/方向）的语义推荐 | P0 | 01,02 |
| 05 | 往届作品对比 | 向量相似度检索，分析主题/技术路线重合度 | P0 | 02 |
| 06 | 智能选题分析 | **核心智能体**：4 步编排生成分析报告（SSE 流式） | P0 | 01,02,03,04,05 |
| 07 | 项目方案辅助 | 基于 LLM 生成需求分析、模块清单、技术路线 | P1 | 06 |
| 08 | PPT 智能生成 | 基于模板（Apache POI）生成可编辑 .pptx | P1 | 06 |
| 09 | 视频辅助制作 | LLM 脚本 + TTS + FFmpeg 合成 MP4（异步） | P2 | 06,07 |
| 10 | 申报材料检查 | 规则对照校验报名表/作品说明/附件完整性 | P2 | 01 |
| 11 | 项目工作台 | 版本、材料、任务进度、AI 生成日志汇总 | P2 | 全部 |

## 4. 技术架构

### 4.1 后端（Spring Boot 3.x + Java 17，Maven）

| 能力 | 选型 |
|------|------|
| 核心框架 | Spring Boot 3.x、Spring MVC、Spring Data JPA |
| AI / RAG | Spring AI（OpenAI 兼容接口，支持国产/本地模型 Tool Calling） |
| 文档解析 | Spring AI DocumentReader + Apache PDFBox（PDF）/ Apache POI（Word） |
| 切分/嵌入 | TokenTextSplitter + 远端 Embedding（BGE/M3E，HTTP 调用，Java 本地不跑模型） |
| 向量库 | Milvus（推荐）或 Chroma，存向量 + Metadata（来源/日期/页码） |
| 混合检索 | 向量召回 + BM25（可接 Elasticsearch），重排可选 |
| 智能体编排 | Spring AI Tool Calling / Function Calling，编排 4 步选题分析 |
| PPT 生成 | Apache POI XSLF（加载模板、替换占位符） |
| TTS | Edge-TTS / 云 TTS（REST/WebSocket） |
| 视频合成 | FFmpeg（ProcessBuilder / ffmpeg-cli-wrapper） |
| 流式输出 | Spring WebFlux SseEmitter / Flux\<ServerSentEvent\> |
| 异步任务 | Spring Task / 消息队列（RabbitMQ / Redis Stream） |
| 业务库 | PostgreSQL |
| 缓存 | Redis |
| 文件存储 | MinIO |
| 工程化 | Docker Compose、Swagger/OpenAPI |

### 4.2 前端（Vue3 + Vite，HBuilder X）

| 能力 | 选型 |
|------|------|
| 框架 | Vue3（Composition API / Setup 语法糖）+ Vite |
| UI | Element Plus + UnoCSS/Tailwind |
| 状态/请求 | Pinia + Axios（RESTful）+ EventSource（SSE 流式） |
| 富文本 | Markdown 渲染（选题分析报告）+ PPTX 在线预览 |
| 视频播放 | Video.js / 原生 \<video\> |

---

## 5. 阶段路线图

实现顺序 = 阶段编号顺序（依赖优先）。每一阶段产出**可独立验收**的增量。

| 阶段 | 模块 | 优先级 | 交付物（后端/前端） | 验收要点 |
|------|------|--------|---------------------|----------|
| 00 | 基础设施 | 前置 | 工程骨架、Docker Compose、通用返回/异常、鉴权、MinIO/Redis/Milvus 连通 | 启动成功、健康检查通过 |
| 01 | 竞赛信息管理 | P0 | Competition CRUD + 组别/规则字段 | 能录入并查询 ≥3 类竞赛 |
| 02 | 资料知识库 | P0 | 上传→解析→切片→Embedding→入向量库 | 上传 PDF 后可被检索 |
| 03 | 规则解读 | P0 | RAG 问答接口 + 出处标注 | 回答带页码/来源 |
| 04 | 智能检索 | P0 | 语义推荐接口 | 按画像推荐竞赛 |
| 05 | 往届对比 | P0 | 相似度检索接口 | 返回相似作品+重合度 |
| 06 | 选题分析 | P0 核心 | Agent 编排 + SSE 流式报告 | 输入设想→流式输出 4 步报告 |
| 07 | 方案辅助 | P1 | LLM 生成方案接口 | 生成需求/模块/技术路线 |
| 08 | PPT 生成 | P1 | 模板填充生成 .pptx | 现场生成可编辑 PPT |
| 09 | 视频制作 | P2 | 脚本+TTS+FFmpeg 异步任务 | 生成可播放 MP4 |
| 10 | 申报检查 | P2 | 规则对照校验接口 | 校验报名表完整性 |
| 11 | 工作台 | P2 | 项目聚合页面 | 展示进度/材料/日志 |

---

## 6. 通用约定（所有阶段一致遵循）

### 6.1 基础信息
- 后端根包名：`com.graduation.competition`
- 前端工程目录：`frontend/`；后端工程目录：`backend/`
- 统一 API 前缀：`/api/v1`（核心智能体接口 `/api/v1/agent/analyze` 保持一致语义）
- 认证：MVP 采用**单用户/免登录 + 预留 JWT 过滤器**；`@PreAuthorize` 后续可启用。

### 6.2 统一返回结构
```json
{ "code": 0, "message": "success", "data": {} }
```
- `code = 0` 成功；非 0 为业务错误码（见各阶段）。
- 分页返回：`{ "records": [], "total": 100, "page": 1, "size": 10 }`

### 6.3 后端分层与包结构
```
backend/src/main/java/com/graduation/competition/
├── common/        # 统一返回 ApiResponse、异常、错误码、常量
├── config/        # Spring AI、Milvus、MinIO、FFmpeg、Async、Swagger 配置
├── entity/        # JPA 实体
├── repository/    # JPA Repository
├── dto/           # 请求/响应 DTO
├── service/       # 业务 Service 接口与实现
├── controller/    # REST Controller
├── agent/         # 智能体 Tool 定义与编排（ToolCalling）
├── rag/           # 文档解析、切分、Embedding、向量库访问
├── gen/           # PPT / TTS / FFmpeg 生成工具
├── task/          # 异步任务
└── security/      # 鉴权过滤器/配置
```

### 6.4 前端目录结构
```
frontend/src/
├── api/           # Axios 封装，按模块拆分
├── views/         # 页面
├── components/    # 公共组件
├── stores/        # Pinia
├── router/        # 路由
├── utils/         # SSE、markdown、预览等工具
└── styles/        # UnoCSS/Tailwind
```

### 6.5 数据规范
- 所有时间统一 UTC 存储、接口返回 ISO-8601。
- 竞赛资料与规则必须携带 `sourceUrl`（来源）、`sourceDate`（发布日期），用于「禁用过期规则」过滤。
- 向量 Metadata 统一字段：`source`、`sourceDate`、`pageNumber`、`documentId`、`competitionId`。

### 6.6 代码规范
- Java 代码带中文专业注释，符合企业级规范；DTO 使用 Lombok。
- 前端使用 TypeScript + `<script setup lang="ts">`。

---

## 7. 阶段文档索引

| 文档 | 内容 |
|------|------|
| [00-基础设施与工程脚手架.md](./00-基础设施与工程脚手架.md) | 工程、中间件、通用框架 |
| [01-竞赛信息管理.md](./01-竞赛信息管理.md) | 竞赛 CRUD |
| [02-竞赛资料知识库(RAG).md](./02-竞赛资料知识库(RAG).md) | RAG 底座 |
| [03-竞赛规则解读.md](./03-竞赛规则解读.md) | RAG 问答 |
| [04-智能竞赛检索.md](./04-智能竞赛检索.md) | 语义推荐 |
| [05-往届作品对比.md](./05-往届作品对比.md) | 相似度检索 |
| [06-智能选题分析(核心).md](./06-智能选题分析(核心).md) | 核心智能体 |
| [07-项目方案辅助.md](./07-项目方案辅助.md) | LLM 方案生成 |
| [08-PPT智能生成.md](./08-PPT智能生成.md) | PPT 模板填充 |
| [09-视频辅助制作.md](./09-视频辅助制作.md) | 脚本+TTS+FFmpeg |
| [10-申报材料检查.md](./10-申报材料检查.md) | 规则校验 |
| [11-项目工作台.md](./11-项目工作台.md) | 聚合工作台 |
