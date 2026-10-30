<script setup lang="ts">
import { ChatDotRound, Link } from '@element-plus/icons-vue'
import { onMounted, reactive, ref } from 'vue'

import { fetchCompetitions, type Competition } from '@/api/competition'
import { askRules, type QaResponse } from '@/api/rule'

const loading = ref(false)
const competitions = ref<Competition[]>([])
const response = ref<QaResponse>()
const form = reactive({
  question: '',
  competitionId: undefined as number | undefined,
  topK: 5,
})

async function ask() {
  if (!form.question.trim()) return
  loading.value = true
  try {
    response.value = await askRules(form)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  competitions.value = (await fetchCompetitions({ page: 1, size: 100 })).records
})
</script>

<template>
  <main class="qa-shell">
    <header>
      <span class="eyebrow">EVIDENCE-GROUNDED ANSWERS</span>
      <h1>竞赛规则解读</h1>
      <p>每个回答均来自已入库资料，并附来源、发布日期和页码；证据不足时明确降级。</p>
    </header>

    <section class="question-card">
      <div class="ask-row">
        <el-input
          v-model="form.question"
          type="textarea"
          :rows="3"
          placeholder="例如：本科生组报名需要提交哪些材料？"
          @keydown.ctrl.enter="ask"
        />
        <el-button type="primary" :icon="ChatDotRound" :loading="loading" @click="ask">
          提问
        </el-button>
      </div>
      <div class="ask-options">
        <el-select v-model="form.competitionId" clearable placeholder="全部竞赛">
          <el-option
            v-for="item in competitions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <span>引用数量</span>
        <el-input-number v-model="form.topK" :min="1" :max="20" />
      </div>
    </section>

    <section v-if="response" class="answer-card" :class="{ fallback: response.fallback }">
      <div class="answer-label">
        {{ response.fallback ? '证据不足' : '基于证据的回答' }}
      </div>
      <p class="answer-text">{{ response.answer }}</p>

      <div v-if="response.references.length" class="references">
        <h2><el-icon><Link /></el-icon> 引用依据</h2>
        <article v-for="item in response.references" :key="`${item.documentId}-${item.chunkIndex}`">
          <strong>[{{ item.index }}] 文档 #{{ item.documentId }} · 第 {{ item.pageNumber ?? '—' }} 页</strong>
          <a :href="item.source" target="_blank" rel="noreferrer">{{ item.source }}</a>
          <span>发布日期 {{ item.sourceDate ?? '未提供' }} · 相关度 {{ item.score.toFixed(3) }}</span>
        </article>
      </div>
    </section>

    <section v-else class="empty">
      <el-empty description="提交问题后，系统将先检索证据，再生成回答" />
    </section>
  </main>
</template>

<style scoped>
.qa-shell {
  width: min(1040px, calc(100% - 40px));
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
  margin: 0 0 28px;
  color: #65756d;
}

.question-card,
.answer-card,
.empty {
  padding: 26px;
  border: 1px solid rgba(32, 68, 56, 0.1);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.74);
}

.ask-row {
  display: grid;
  grid-template-columns: 1fr 110px;
  gap: 12px;
}

.ask-row .el-button {
  height: auto;
}

.ask-options {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 14px;
  color: #77867e;
  font-size: 13px;
}

.ask-options .el-select {
  width: 320px;
}

.answer-card {
  margin-top: 16px;
  border-left: 4px solid #3f8068;
}

.answer-card.fallback {
  border-left-color: #d28b44;
}

.answer-label {
  color: #3f8068;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.fallback .answer-label {
  color: #b66d2c;
}

.answer-text {
  margin: 18px 0;
  font-family: "STSong", "SimSun", serif;
  font-size: 18px;
  line-height: 1.9;
  white-space: pre-wrap;
}

.references {
  padding-top: 16px;
  border-top: 1px solid #e2e7e3;
}

.references h2 {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 15px;
}

.references article {
  display: grid;
  gap: 5px;
  padding: 12px;
  border-radius: 10px;
  background: #f6f4ee;
}

.references article + article {
  margin-top: 8px;
}

.references a {
  color: #a34f32;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.references span {
  color: #7b8981;
  font-size: 12px;
}

.empty {
  margin-top: 16px;
}

@media (max-width: 640px) {
  .ask-row {
    grid-template-columns: 1fr;
  }

  .ask-row .el-button {
    height: 44px;
  }
}
</style>

