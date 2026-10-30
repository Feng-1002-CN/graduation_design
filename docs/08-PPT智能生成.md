# 阶段 08：PPT 智能生成

> 基于预设模板（Apache POI XSLF）填入项目数据，生成**可编辑** .pptx 文件，现场交付可修改的演示文稿。

## 1. 目标
上传/内置 PPT 模板，将项目方案、选题分析结论填入模板占位符，生成可编辑的 .pptx，存储到 MinIO 并支持下载/预览。

## 2. 前置依赖
阶段 00、06、07。

## 3. 验收标准
- [ ] 内置至少 1 套 PPT 模板（含占位符）。
- [ ] 根据 project 数据生成 .pptx，占位符被正确替换。
- [ ] 生成文件可下载，且用 PowerPoint/WPS 打开仍可编辑。
- [ ] 长耗时生成走异步任务，返回任务状态。

## 4. 范围

### In Scope
- 模板加载与占位符替换（Apache POI XSLF）。
- 异步生成任务（Spring Task）。
- 文件存储 MinIO + 下载接口。

### Out of Scope
- 不实现 PPT 在线编辑。
- 不生成图片型 PPT（保持可编辑）。
- 不自研布局引擎。

## 5. 数据模型
```sql
CREATE TABLE generation_task (
    id          BIGSERIAL PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES project(id),
    task_type   VARCHAR(16) NOT NULL,              -- PPT / VIDEO
    status      VARCHAR(16) DEFAULT 'PENDING',     -- PENDING/RUNNING/SUCCESS/FAILED
    file_url    VARCHAR(512),                      -- MinIO 路径
    error_msg   TEXT,
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now()
);
```

## 6. API 契约

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/ppt/generate` | 提交 PPT 生成任务 |
| GET | `/api/v1/tasks/{taskId}` | 查询任务状态 |
| GET | `/api/v1/ppt/{taskId}/download` | 下载生成的 PPT |

请求/响应：
```json
// 请求
{ "projectId": 1, "templateId": "default" }
// 任务查询响应 data
{ "taskId": 1, "status": "SUCCESS", "fileUrl": "ppt/project_1.pptx" }
```

## 7. 核心流程
1. 校验 project 与 plan 存在，创建 generation_task（PENDING）。
2. 异步执行：
   - 加载模板 `templates/default.pptx`；
   - 遍历 slide/text，替换占位符（如 `{{title}}`、`{{background}}`、`{{modules}}`、`{{techRoute}}`）；
   - 模块清单渲染为列表文本；
   - 保存到临时文件 → 上传 MinIO → 更新任务 SUCCESS + fileUrl。
3. 失败置 FAILED + errorMsg。
4. 下载接口按 fileUrl 从 MinIO 读取返回。

## 8. 文件结构
```
backend/.../gen/ppt/
├── PptGenerator.java           # POI 占位符替换
├── PptTemplateService.java     # 模板管理
backend/.../task/
├── entity/ GenerationTask.java
├── repository/ GenerationTaskRepository.java
├── service/ TaskService.java, PptGenerateTask.java
└── controller/ PptController.java, TaskController.java
backend/src/main/resources/templates/default.pptx   # 内置模板
frontend/src/
├── api/ppt.ts
└── views/ppt/ PptGenerate.vue
```

## 9. 关键实现要点与约束
- 占位符约定 `{{key}}`；模板内支持段落级与整文本框替换。
- 生成必须**可编辑**：只写文本到 shape，不转图片。
- 异步 + 任务状态轮询（前端可用定时器或 SSE 查询）。
- 模板缺失时给出明确错误（不静默失败）。

## 10. AI 实现提示词

```text
请实现「PPT 智能生成」模块（Spring Boot 3 + Java 17，根包名 com.graduation.competition，
已有 project/project_plan 实体、MinIO、异步线程池）。

【技术约束】
- 使用 Apache POI XSLF 操作 .pptx，占位符格式 {{key}}。
- 生成走 @Async 异步任务，状态存 generation_task 表。

【必须实现】
1. 实体 GenerationTask（projectId/taskType PPT/VIDEO/status PENDING-RUNNING-SUCCESS-FAILED/
   fileUrl/errorMsg/createdAt/updatedAt）+ Repository。
2. PptGenerator：
   - 加载模板文件（classpath:templates/default.pptx）；
   - 遍历 Slide 的 Shape/TextParagraph，将 {{title}}/{{background}}/{{requirements}}/
     {{modules}}/{{techRoute}}/{{innovation}} 等占位符替换为 plan 数据；
   - modules 渲染为多行文本；
   - 输出临时 .pptx → 上传 MinIO → 返回 fileUrl。
3. PptGenerateTask（@Async）：创建任务→生成→更新状态（含失败 errorMsg）。
4. PptController：
   - POST /api/v1/ppt/generate（projectId/templateId，返回 taskId）；
   - GET /api/v1/ppt/{taskId}/download（MinIO 流式下载）。
5. TaskController：GET /api/v1/tasks/{taskId} 查询状态。
6. 前端 PptGenerate.vue：按钮提交生成、轮询任务状态、成功后下载按钮。

【输出要求】
- 代码带中文专业注释；生成文件必须保持可编辑（仅写文本，不转图片）；
- 提供一份 default.pptx 模板的占位符说明。
```
