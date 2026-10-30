<script setup lang="ts">
import { CircleCheck, DocumentChecked, UploadFilled, Warning } from '@element-plus/icons-vue'
import { ElMessage, type UploadFile } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import {
  checkApplication,
  uploadApplyFile,
  type ApplyCheckResponse,
  type ApplyFile,
} from '@/api/apply'
import { fetchCompetitions, type Competition } from '@/api/competition'

const competitions = ref<Competition[]>([])
const files = ref<File[]>([])
const uploaded = ref<ApplyFile[]>([])
const loading = ref(false)
const report = ref<ApplyCheckResponse>()
const form = reactive({ competitionId: undefined as number | undefined })

function onFileChange(file: UploadFile) {
  if (file.raw) files.value.push(file.raw)
}

async function check() {
  if (!form.competitionId) {
    ElMessage.warning('请先选择竞赛')
    return
  }
  loading.value = true
  try {
    uploaded.value = []
    for (const file of files.value) uploaded.value.push(await uploadApplyFile(file))
    report.value = await checkApplication(form.competitionId, uploaded.value)
  } finally {
    loading.value = false
  }
}

function statusType(status: string) {
  return status === 'OK' ? 'success' : status === 'MISMATCH' ? 'warning' : 'danger'
}

onMounted(async () => {
  competitions.value = (await fetchCompetitions({ page: 1, size: 100 })).records
})
</script>

<template>
  <main class="apply-shell">
    <header>
      <span class="eyebrow">RULE-DRIVEN CHECKLIST</span>
      <h1>申报材料检查</h1>
      <p>依据有效竞赛规则提取材料清单，对照文件名与类型，明确列出通过、缺失或不匹配项。</p>
    </header>

    <section class="check-card">
      <div class="mark"><el-icon><DocumentChecked /></el-icon></div>
      <div>
        <el-form label-position="top">
          <el-form-item label="目标竞赛">
            <el-select v-model="form.competitionId" filterable placeholder="选择竞赛">
              <el-option
                v-for="item in competitions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-upload
            drag
            multiple
            accept=".pdf,.docx,.zip,.png,.jpg,.jpeg"
            :auto-upload="false"
            :on-change="onFileChange"
          >
            <el-icon><UploadFilled /></el-icon>
            <span>拖入报名表、作品说明或附件</span>
          </el-upload>
          <div class="actions">
            <small>已选择 {{ files.length }} 个文件</small>
            <el-button type="primary" :loading="loading" @click="check">开始对照检查</el-button>
          </div>
        </el-form>
      </div>
    </section>

    <section v-if="report" class="report-card">
      <div class="report-head">
        <div>
          <span>检查报告 #{{ report.id }}</span>
          <h2>材料完整性结果</h2>
        </div>
        <el-tag :type="report.result.some((item) => item.status === 'MISSING') ? 'warning' : 'success'">
          {{ report.result.filter((item) => item.status === 'OK').length }}/{{ report.result.length }} 通过
        </el-tag>
      </div>
      <div class="result-list">
        <article v-for="item in report.result" :key="item.item">
          <el-icon v-if="item.status === 'OK'"><CircleCheck /></el-icon>
          <el-icon v-else><Warning /></el-icon>
          <div>
            <h3>{{ item.item }}</h3>
            <p>{{ item.fileName || '未找到对应文件' }}</p>
            <small v-if="item.advice">{{ item.advice }}</small>
            <a v-if="item.source" :href="item.source" target="_blank" rel="noreferrer">
              规则依据 · {{ item.sourceDate ?? '日期未提供' }}
            </a>
          </div>
          <el-tag :type="statusType(item.status)">{{ item.status }}</el-tag>
        </article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.apply-shell {
  width: min(1060px, calc(100% - 40px));
  margin: 0 auto;
  padding: 48px 0 70px;
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
  margin: 0 0 24px;
  color: #65756d;
}

.check-card {
  display: grid;
  grid-template-columns: 250px 1fr;
  gap: 26px;
  padding: 28px;
  border-radius: 22px;
  background: #173d33;
}

.mark {
  display: grid;
  place-items: center;
  color: #f3b08f;
  font-size: 90px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.035);
}

.check-card :deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.72);
}

.check-card :deep(.el-select) {
  width: 100%;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.report-card {
  margin-top: 16px;
  padding: 24px;
  border: 1px solid rgba(32, 68, 56, 0.1);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.74);
}

.report-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.report-head span {
  color: #849089;
  font-size: 12px;
}

.report-head h2 {
  margin: 5px 0 0;
  font-family: "STSong", "SimSun", serif;
}

.result-list {
  display: grid;
  gap: 10px;
}

.result-list article {
  display: grid;
  grid-template-columns: 28px 1fr auto;
  gap: 12px;
  align-items: start;
  padding: 14px;
  border-radius: 12px;
  background: #f6f4ed;
}

.result-list h3 {
  margin: 0;
  font-size: 15px;
}

.result-list p,
.result-list small {
  margin: 5px 0 0;
  color: #718078;
}

.result-list a {
  display: block;
  margin-top: 8px;
  color: #9e4a2f;
  font-size: 12px;
}

@media (max-width: 680px) {
  .check-card {
    grid-template-columns: 1fr;
  }

  .mark {
    min-height: 120px;
  }
}
</style>

