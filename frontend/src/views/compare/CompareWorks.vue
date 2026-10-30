<script setup lang="ts">
import { Files, Search } from '@element-plus/icons-vue'
import { onMounted, reactive, ref } from 'vue'

import { fetchCompetitions, type Competition } from '@/api/competition'
import { comparePastWorks, type PastWorkResult } from '@/api/compare'

const loading = ref(false)
const searched = ref(false)
const works = ref<PastWorkResult[]>([])
const competitions = ref<Competition[]>([])
const form = reactive({
  topic: '',
  competitionId: undefined as number | undefined,
  topK: 5,
})

async function compare() {
  if (!form.topic.trim()) return
  loading.value = true
  searched.value = true
  try {
    works.value = await comparePastWorks(form)
  } finally {
    loading.value = false
  }
}

function tagType(level: string) {
  return level === '高' ? 'danger' : level === '中' ? 'warning' : 'info'
}

onMounted(async () => {
  competitions.value = (await fetchCompetitions({ page: 1, size: 100 })).records
})
</script>

<template>
  <main class="compare-shell">
    <header>
      <span class="eyebrow">PRIOR WORK LANDSCAPE</span>
      <h1>往届作品对比</h1>
      <p>检索已标记的往届作品，观看主题与技术路线的相似情况，并保留原始出处。</p>
    </header>

    <section class="query-card">
      <el-input
        v-model="form.topic"
        type="textarea"
        :rows="3"
        placeholder="描述你的选题，例如：基于计算机视觉的中药材图像识别系统"
      />
      <div class="query-actions">
        <el-select v-model="form.competitionId" clearable placeholder="全部竞赛">
          <el-option
            v-for="item in competitions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <span>返回数量</span>
        <el-input-number v-model="form.topK" :min="1" :max="10" />
        <el-button type="primary" :icon="Search" :loading="loading" @click="compare">
          与往届作品对比
        </el-button>
      </div>
    </section>

    <section v-if="works.length" class="works">
      <article v-for="work in works" :key="work.documentId">
        <div class="work-icon"><el-icon><Files /></el-icon></div>
        <div class="work-main">
          <div class="work-head">
            <div>
              <span>{{ work.workYear ?? '年份未标注' }} · 相似度 {{ work.score.toFixed(3) }}</span>
              <h2>{{ work.title }}</h2>
            </div>
            <strong>{{ work.topicOverlap }}度重合</strong>
          </div>
          <div class="overlap-row">
            <el-tag :type="tagType(work.topicOverlap)">主题重合度 {{ work.topicOverlap }}</el-tag>
            <el-tag :type="tagType(work.techOverlap)">技术路线重合度 {{ work.techOverlap }}</el-tag>
          </div>
          <p>{{ work.overlapDesc }}</p>
          <div class="work-source">
            <a :href="work.source" target="_blank" rel="noreferrer">{{ work.source }}</a>
            <span>来源日期 {{ work.sourceDate ?? '未提供' }}</span>
          </div>
        </div>
      </article>
    </section>
    <el-empty
      v-else
      :description="searched ? '未找到可比较的往届作品' : '输入选题后在知识库中进行相似检索'"
    />
  </main>
</template>

<style scoped>
.compare-shell {
  width: min(1080px, calc(100% - 40px));
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

.query-card {
  padding: 24px;
  border: 1px solid rgba(32, 68, 56, 0.1);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.75);
}

.query-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 14px;
  color: #7b8881;
  font-size: 13px;
}

.query-actions .el-select {
  width: 300px;
}

.query-actions .el-button {
  margin-left: auto;
}

.works {
  display: grid;
  gap: 14px;
  margin-top: 20px;
}

.works article {
  display: grid;
  grid-template-columns: 64px 1fr;
  overflow: hidden;
  border: 1px solid rgba(32, 68, 56, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.7);
}

.work-icon {
  display: grid;
  place-items: center;
  color: #f0d5c5;
  font-size: 28px;
  background: #69442e;
}

.work-main {
  padding: 20px 22px;
}

.work-head {
  display: flex;
  gap: 16px;
  justify-content: space-between;
}

.work-head span {
  color: #7b8981;
  font-size: 12px;
}

.work-head strong {
  align-self: flex-start;
  color: #b65c37;
}

h2 {
  margin: 7px 0 0;
  font-family: "STSong", "SimSun", serif;
  font-size: 22px;
}

.overlap-row {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.work-main p {
  color: #55675d;
  line-height: 1.75;
}

.work-source {
  display: flex;
  gap: 16px;
  font-size: 12px;
}

.work-source a {
  color: #98482e;
  overflow-wrap: anywhere;
}

.work-source span {
  color: #849089;
}

.el-empty {
  margin-top: 20px;
}

@media (max-width: 640px) {
  .query-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .query-actions .el-select {
    width: 100%;
  }

  .query-actions .el-button {
    margin-left: 0;
  }
}
</style>

