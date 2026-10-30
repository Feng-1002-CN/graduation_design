<script setup lang="ts">
import { Delete, Search, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'

import { fetchCompetitions, type Competition } from '@/api/competition'
import {
  deleteDocument,
  fetchDocuments,
  searchKnowledge,
  uploadDocument,
  type KnowledgeDocument,
  type SearchHit,
} from '@/api/document'

const loading = ref(false)
const uploading = ref(false)
const searching = ref(false)
const documents = ref<KnowledgeDocument[]>([])
const total = ref(0)
const hits = ref<SearchHit[]>([])
const competitionOptions = ref<Competition[]>([])
let timer: number | undefined

const filters = reactive({ page: 1, size: 10, competitionId: undefined as number | undefined })
const searchForm = reactive({
  query: '',
  topK: 5,
  competitionId: undefined as number | undefined,
  documentType: '',
})
const uploadForm = reactive({
  competitionId: undefined as number | undefined,
  title: '',
  sourceUrl: '',
  sourceDate: '',
  documentType: 'MATERIAL',
  workYear: '',
})
const selectedFile = ref<File>()

async function load() {
  loading.value = true
  try {
    const result = await fetchDocuments(filters)
    documents.value = result.records
    total.value = result.total
    if (documents.value.some((item) => item.status === 'PENDING' || item.status === 'PROCESSING')) {
      scheduleRefresh()
    }
  } finally {
    loading.value = false
  }
}

function scheduleRefresh() {
  window.clearTimeout(timer)
  timer = window.setTimeout(load, 1500)
}

function onFileChange(file: UploadFile) {
  selectedFile.value = file.raw
  if (!uploadForm.title && file.name) uploadForm.title = file.name.replace(/\.(pdf|docx)$/i, '')
}

async function submitUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择 PDF 或 DOCX 文件')
    return
  }
  const data = new FormData()
  data.append('file', selectedFile.value)
  Object.entries(uploadForm).forEach(([key, value]) => {
    if (value !== '' && value !== undefined) data.append(key, String(value))
  })
  uploading.value = true
  try {
    await uploadDocument(data)
    ElMessage.success('文档已上传，正在后台解析')
    selectedFile.value = undefined
    uploadForm.title = ''
    load()
  } finally {
    uploading.value = false
  }
}

async function search() {
  if (!searchForm.query.trim()) return
  searching.value = true
  try {
    hits.value = await searchKnowledge({
      query: searchForm.query,
      topK: searchForm.topK,
      competitionId: searchForm.competitionId,
      documentType: searchForm.documentType || undefined,
    })
  } finally {
    searching.value = false
  }
}

async function remove(row: KnowledgeDocument) {
  await ElMessageBox.confirm(`删除“${row.title}”及其全部切片？`, '删除文档', { type: 'warning' })
  await deleteDocument(row.id)
  load()
}

onMounted(async () => {
  const competitions = await fetchCompetitions({ page: 1, size: 100 })
  competitionOptions.value = competitions.records
  load()
})

onBeforeUnmount(() => window.clearTimeout(timer))
</script>

<template>
  <main class="knowledge-shell">
    <header>
      <span class="eyebrow">RETRIEVAL-AUGMENTED KNOWLEDGE</span>
      <h1>竞赛资料知识库</h1>
      <p>PDF 与 DOCX 将异步解析、切分并写入向量库，所有片段保留证据出处。</p>
    </header>

    <div class="workspace-grid">
      <section class="upload-panel">
        <h2>上传资料</h2>
        <el-upload
          drag
          :auto-upload="false"
          :limit="1"
          accept=".pdf,.docx"
          :on-change="onFileChange"
          :on-remove="() => (selectedFile = undefined)"
        >
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div>拖入 PDF / DOCX，或点击选择</div>
          <small>单文件不超过 50MB，扫描件 OCR 暂不支持</small>
        </el-upload>
        <el-form label-position="top" class="compact-form">
          <el-form-item label="关联竞赛">
            <el-select v-model="uploadForm.competitionId" clearable placeholder="可选">
              <el-option
                v-for="item in competitionOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="资料标题">
            <el-input v-model="uploadForm.title" />
          </el-form-item>
          <div class="two-column">
            <el-form-item label="来源 URL">
              <el-input v-model="uploadForm.sourceUrl" placeholder="用于证据链" />
            </el-form-item>
            <el-form-item label="来源日期">
              <el-date-picker v-model="uploadForm.sourceDate" value-format="YYYY-MM-DD" />
            </el-form-item>
          </div>
          <div class="two-column">
            <el-form-item label="资料类型">
              <el-select v-model="uploadForm.documentType">
                <el-option label="竞赛资料" value="MATERIAL" />
                <el-option label="往届作品" value="PAST_WORK" />
              </el-select>
            </el-form-item>
            <el-form-item label="作品年份">
              <el-input v-model="uploadForm.workYear" :disabled="uploadForm.documentType !== 'PAST_WORK'" />
            </el-form-item>
          </div>
          <el-button type="primary" :loading="uploading" @click="submitUpload">开始上传</el-button>
        </el-form>
      </section>

      <section class="search-panel">
        <h2>语义检索</h2>
        <div class="search-line">
          <el-input
            v-model="searchForm.query"
            size="large"
            placeholder="例如：本科组需要提交哪些报名材料？"
            @keyup.enter="search"
          />
          <el-button type="primary" size="large" :icon="Search" :loading="searching" @click="search">
            检索
          </el-button>
        </div>
        <div class="search-options">
          <el-select v-model="searchForm.competitionId" clearable placeholder="全部竞赛">
            <el-option
              v-for="item in competitionOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-select v-model="searchForm.documentType" clearable placeholder="全部资料">
            <el-option label="竞赛资料" value="MATERIAL" />
            <el-option label="往届作品" value="PAST_WORK" />
          </el-select>
          <el-input-number v-model="searchForm.topK" :min="1" :max="20" />
        </div>
        <div v-if="hits.length" class="hits">
          <article v-for="hit in hits" :key="`${hit.documentId}-${hit.chunkIndex}`">
            <div class="hit-head">
              <span>文档 #{{ hit.documentId }} · 第 {{ hit.pageNumber ?? '—' }} 页</span>
              <el-tag effect="plain">{{ hit.score.toFixed(3) }}</el-tag>
            </div>
            <p>{{ hit.content }}</p>
            <a :href="hit.source" target="_blank" rel="noreferrer">{{ hit.source }}</a>
            <small>来源日期：{{ hit.sourceDate ?? '未提供' }}</small>
          </article>
        </div>
        <el-empty v-else description="输入问题后检索证据片段" />
      </section>
    </div>

    <section class="document-panel">
      <div class="panel-title">
        <h2>已入库文档</h2>
        <el-pagination
          v-model:current-page="filters.page"
          v-model:page-size="filters.size"
          :total="total"
          layout="total, prev, pager, next"
          @change="load"
        />
      </div>
      <el-table v-loading="loading" :data="documents">
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="fileType" label="格式" width="80" />
        <el-table-column prop="documentType" label="类型" width="110" />
        <el-table-column prop="chunkCount" label="切片" width="80" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tooltip :content="row.errorMsg || ''" :disabled="!row.errorMsg">
              <el-tag :type="row.status === 'READY' ? 'success' : row.status === 'FAILED' ? 'danger' : 'warning'">
                {{ row.status }}
              </el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="sourceDate" label="来源日期" width="120" />
        <el-table-column width="70" fixed="right">
          <template #default="{ row }">
            <el-button :icon="Delete" circle text type="danger" @click="remove(row)" />
          </template>
        </el-table-column>
      </el-table>
    </section>
  </main>
</template>

<style scoped>
.knowledge-shell {
  width: min(1320px, calc(100% - 40px));
  margin: 0 auto;
  padding: 48px 0 64px;
}

header {
  margin-bottom: 26px;
}

.eyebrow {
  color: #b45a39;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

h1 {
  margin: 10px 0;
  font-family: "STSong", "SimSun", serif;
  font-size: 44px;
}

header p {
  margin: 0;
  color: #65756d;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(300px, 0.8fr) minmax(420px, 1.2fr);
  gap: 16px;
}

.upload-panel,
.search-panel,
.document-panel {
  padding: 24px;
  border: 1px solid rgba(32, 68, 56, 0.1);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
}

h2 {
  margin: 0 0 18px;
  font-family: "STSong", "SimSun", serif;
}

.upload-icon {
  margin-bottom: 10px;
  color: #c76742;
  font-size: 44px;
}

.compact-form {
  margin-top: 18px;
}

.two-column {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.two-column :deep(.el-date-editor),
.two-column :deep(.el-select),
.compact-form :deep(.el-select) {
  width: 100%;
}

.search-line {
  display: flex;
  gap: 10px;
}

.search-options {
  display: flex;
  gap: 10px;
  margin: 12px 0 20px;
}

.search-options :deep(.el-select) {
  flex: 1;
}

.hits {
  display: grid;
  gap: 12px;
  max-height: 520px;
  overflow: auto;
}

.hits article {
  padding: 16px;
  border-left: 3px solid #d47a55;
  border-radius: 0 12px 12px 0;
  background: #f8f5ee;
}

.hit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #6e7e75;
  font-size: 12px;
}

.hits p {
  margin: 12px 0;
  line-height: 1.75;
}

.hits a {
  color: #9e4b2f;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.hits small {
  display: block;
  margin-top: 8px;
  color: #87948d;
}

.document-panel {
  margin-top: 16px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

@media (max-width: 900px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .two-column,
  .search-line,
  .search-options {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>

