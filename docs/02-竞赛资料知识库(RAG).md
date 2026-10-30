# 阶段 02：竞赛资料知识库（RAG 底座）

> 全系统 RAG 核心底座。实现「上传 → 解析 → 切分 → 向量化 → 入向量库」，供规则解读、检索、往届对比、选题分析复用。

## 1. 目标
支持上传 PDF/Word 竞赛资料（通知、标准、作品等），完成文档解析、切片、向量化（远端 BGE/M3E），并写入向量库（带 Metadata：来源/日期/页码）；提供文档与切片管理。

## 2. 前置依赖
阶段 00、01（资料可关联竞赛）。

## 3. 验收标准
- [ ] 上传一份 PDF 后，可查询到文档记录及切片数量。
- [ ] 切片已向量化并写入 Milvus/Chroma，`GET /api/v1/documents/{id}/chunks` 返回切片。
- [ ] 可用 `POST /api/v1/documents/search` 按语义命中相关切片（含出处信息）。
- [ ] 支持 Word(.docx) 解析；文件存储于 MinIO 并可回读。

## 4. 范围

### In Scope
- PDF（Apache PDFBox）、Word（Apache POI）解析。
- TokenTextSplitter 切分。
- 远端 Embedding（HTTP 调用 BGE/M3E）向量化。
- Milvus/Chroma 写入与检索（含 Metadata 过滤）。
- 文档/切片管理接口。

### Out of Scope
- 不实现本地 Embedding 模型推理。
- 不实现 OCR（扫描件暂不支持，可后续扩展）。
- 不实现复杂混合检索 BM25/ES（可选，非本期必需；混合检索在阶段 04/05 增强）。

## 5. 数据模型

```sql
-- 文档
CREATE TABLE document (
    id             BIGSERIAL PRIMARY KEY,
    competition_id BIGINT REFERENCES competition(id), -- 可空（通用资料）
    title          VARCHAR(255) NOT NULL,
    file_type      VARCHAR(16) NOT NULL,              -- PDF / DOCX
    file_url       VARCHAR(512) NOT NULL,             -- MinIO 对象路径
    source_url     VARCHAR(512),
    source_date    DATE,
    status         VARCHAR(16) DEFAULT 'PENDING',     -- PENDING/PROCESSING/READY/FAILED
    chunk_count    INT DEFAULT 0,
    error_msg      TEXT,
    created_at     TIMESTAMPTZ DEFAULT now()
);

-- 切片
CREATE TABLE document_chunk (
    id             BIGSERIAL PRIMARY KEY,
    document_id    BIGINT NOT NULL REFERENCES document(id),
    chunk_index    INT NOT NULL,
    content        TEXT NOT NULL,
    page_number    INT,                                -- 页码（PDF）
    token_count    INT,
    vector_id      VARCHAR(128),                       -- 向量库主键
    created_at     TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_document_competition ON document(competition_id);
CREATE INDEX idx_chunk_document ON document_chunk(document_id, chunk_index);
```

向量库 Collection：`competition_knowledge`
- 字段：`id`(str)、`vector`(float[])、`document_id`(int)、`competition_id`(int)、`source`(str)、`source_date`(str)、`page_number`(int)、`content`(str, 可选冗余用于快速返回)。

## 6. API 契约

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/documents/upload` | multipart 上传（file + competitionId + title 可选） |
| GET | `/api/v1/documents` | 分页查询文档 |
| GET | `/api/v1/documents/{id}` | 文档详情 |
| GET | `/api/v1/documents/{id}/chunks` | 切片列表 |
| DELETE | `/api/v1/documents/{id}` | 删除文档及切片/向量 |
| POST | `/api/v1/documents/search` | 语义检索切片（topK） |

上传响应：
```json
{ "code": 0, "message": "success", "data": { "documentId": 1, "title": "计算机设计大赛通知.pdf", "status": "PROCESSING" } }
```

检索请求/响应：
```json
// 请求
{ "query": "计算机视觉中药识别评分标准", "topK": 5, "competitionId": 1 }
// 响应 data 内
{ "hits": [
  { "documentId": 2, "chunkIndex": 3, "pageNumber": 4, "content": "……",
    "source": "https://example.com", "sourceDate": "2025-12-01", "score": 0.91 }
] }
```

## 7. 核心流程（上传 → 入库）
1. 接收文件 → 校验类型（pdf/docx）与大小（≤50MB）。
2. 上传 MinIO，返回 `fileUrl`；创建 document 记录（status=PENDING）。
3. 异步处理（`@Async`）：
   - 解析：PDFBox 逐页提取文本 / POI 提取 Word 段落；
   - 切分：TokenTextSplitter（chunkSize≈512 token、overlap≈64）；
   - 向量化：调用远端 Embedding HTTP 接口（批量）；
   - 写向量库：批量 upsert，携带 metadata；
   - 写 `document_chunk` 表，回填 `vector_id`；
   - 更新 document status=READY、chunkCount。
4. 失败则 status=FAILED 并记录 errorMsg。
5. 语义检索：query → Embedding → 向量库 topK（可按 competitionId/source_date 过滤）→ 组装出处信息返回。

## 8. 文件结构
```
backend/.../rag/
├── parser/ PdfParser.java, WordParser.java, DocumentParserFactory.java
├── splitter/ TextChunkSplitter.java
├── embedding/ EmbeddingClient.java, RemoteEmbeddingClient.java
├── vector/ VectorStoreService.java, MilvusVectorStoreService.java
└── DocumentParseService.java (异步编排)
backend/.../document/
├── entity/ DocumentEntity.java, DocumentChunk.java
├── repository/ DocumentRepository.java, DocumentChunkRepository.java
├── dto/ DocumentUploadResult.java, SearchRequest.java, SearchHit.java
├── service/ DocumentService.java
└── controller/ DocumentController.java
```

## 9. 关键实现要点与约束
- 解析与向量化必须**异步**，避免阻塞上传接口。
- Embedding 批量调用需限流/重试（HTTP 超时、指数退避）。
- 向量库操作需幂等：同一文档重复入库前先删除旧向量。
- 检索结果必须携带 `source`/`sourceDate`/`pageNumber`（出处证据链，供阶段 03/06 使用）。
- 维度必须与所选模型一致（如 BGE 768 维），配置化。

## 10. AI 实现提示词

```text
请基于已搭建工程（Spring Boot 3 + Java 17，根包名 com.graduation.competition，已有
ApiResponse/异步线程池/MinIO/Redis 配置，JPA + PostgreSQL），实现「竞赛资料知识库(RAG)」模块。

【技术约束】
- 文档解析：Apache PDFBox（PDF 逐页提取）、Apache POI（Word .docx 段落提取）。
- 切分：TokenTextSplitter（chunkSize≈512 token、overlap≈64，可简单按字符近似，注释说明）。
- 向量化：远端 Embedding HTTP 接口（OkHttp 调用 BGE/M3E，URL 走配置，返回 float[]，维度配置化）。
- 向量库：Milvus（milvus-sdk-java），Collection 名 competition_knowledge，
  字段 id(str)/vector(float[])/document_id(long)/competition_id(long)/source(str)/
  source_date(str)/page_number(int)。

【必须实现】
1. 实体：DocumentEntity、DocumentChunk（JPA，含 status/chunkCount 字段）。
2. rag 包：
   - parser 工厂：按扩展名选择 PdfParser/WordParser，返回 List<ParsePage/段落文本+页码>；
   - TextChunkSplitter：按 token 近似切分，返回带页码/序号的分片；
   - RemoteEmbeddingClient：OkHttp 批量向量化，带超时与重试；
   - MilvusVectorStoreService：connect、upsert(带metadata)、search(topK+过滤)、deleteByDocumentId。
   - DocumentParseService（@Async）：编排 解析→切分→向量化→写库→更新状态。
3. document 包：DocumentService（上传→存 MinIO→建记录→触发异步处理；删除文档级联删切片与向量；
   语义检索：query→embedding→search→组装 SearchHit 含 source/sourceDate/pageNumber/score）。
4. DocumentController：
   - POST /api/v1/documents/upload（multipart，返回 documentId+status=PROCESSING）；
   - GET /api/v1/documents（分页）；GET /{id}；GET /{id}/chunks；DELETE /{id}；
   - POST /api/v1/documents/search（topK）。
5. application.yml 增加 minio/embedding/milvus 配置项。

【输出要求】
- 代码带中文专业注释，符合企业级规范；
- 检索结果必须带出处（source/sourceDate/pageNumber）；
- 不实现 BM25/ES 混合检索与重排（后续阶段增强）；
- 不实现本地模型推理与 OCR。
```
