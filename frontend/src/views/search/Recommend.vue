<script setup lang="ts">
import { Compass, Search } from '@element-plus/icons-vue'
import { reactive, ref } from 'vue'

import { recommendCompetitions, type Recommendation } from '@/api/search'

const loading = ref(false)
const results = ref<Recommendation[]>([])
const form = reactive({
  grade: '大二',
  direction: '',
  keywordsText: '',
  limit: 10,
})

async function search() {
  loading.value = true
  try {
    results.value = await recommendCompetitions({
      grade: form.grade,
      direction: form.direction,
      keywords: form.keywordsText
        .split(/[,，\s]+/)
        .map((item) => item.trim())
        .filter(Boolean),
      limit: form.limit,
    })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="recommend-shell">
    <header>
      <span class="eyebrow">PROFILE-AWARE DISCOVERY</span>
      <h1>智能竞赛检索</h1>
      <p>基于年级、技术方向与兴趣关键词匹配开放赛事，推荐理由保留官方来源。</p>
    </header>

    <section class="profile-card">
      <div class="profile-mark"><el-icon><Compass /></el-icon></div>
      <el-form label-position="top">
        <div class="profile-grid">
          <el-form-item label="当前年级">
            <el-select v-model="form.grade">
              <el-option v-for="grade in ['大一', '大二', '大三', '大四', '研究生']" :key="grade" :value="grade" />
            </el-select>
          </el-form-item>
          <el-form-item label="专业方向">
            <el-input v-model="form.direction" placeholder="例如：计算机视觉" />
          </el-form-item>
          <el-form-item label="兴趣关键词">
            <el-input v-model="form.keywordsText" placeholder="机器学习，图像识别" />
          </el-form-item>
          <el-form-item label="推荐数量">
            <el-input-number v-model="form.limit" :min="1" :max="30" />
          </el-form-item>
        </div>
        <el-button type="primary" :icon="Search" :loading="loading" @click="search">
          发现适合的竞赛
        </el-button>
      </el-form>
    </section>

    <section v-if="results.length" class="results">
      <article v-for="(item, index) in results" :key="item.competitionId">
        <div class="rank">{{ String(index + 1).padStart(2, '0') }}</div>
        <div class="result-body">
          <div class="result-head">
            <div>
              <span class="level">{{ item.level ?? '未标注级别' }}</span>
              <h2>{{ item.name }}</h2>
            </div>
            <strong>{{ item.score.toFixed(2) }}</strong>
          </div>
          <p>{{ item.reason }}</p>
          <div class="tags">
            <el-tag v-for="keyword in item.matchedKeywords" :key="keyword" effect="plain">
              {{ keyword }}
            </el-tag>
          </div>
          <div class="source">
            <a :href="item.source" target="_blank" rel="noreferrer">{{ item.source }}</a>
            <span>来源日期 {{ item.sourceDate ?? '未提供' }}</span>
          </div>
        </div>
      </article>
    </section>
    <el-empty v-else description="填写画像后查看匹配赛事" />
  </main>
</template>

<style scoped>
.recommend-shell {
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

.profile-card {
  position: relative;
  padding: 28px;
  overflow: hidden;
  border: 1px solid rgba(32, 68, 56, 0.1);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.75);
}

.profile-mark {
  position: absolute;
  right: 24px;
  top: 18px;
  color: rgba(183, 84, 51, 0.15);
  font-size: 90px;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  padding-right: 80px;
}

.profile-grid :deep(.el-select) {
  width: 100%;
}

.results {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}

.results article {
  display: grid;
  grid-template-columns: 74px 1fr;
  overflow: hidden;
  border: 1px solid rgba(32, 68, 56, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.68);
}

.rank {
  display: grid;
  place-items: center;
  color: #f4e4d6;
  font-family: "Georgia", serif;
  font-size: 28px;
  background: #214f41;
}

.result-body {
  padding: 20px 22px;
}

.result-head {
  display: flex;
  justify-content: space-between;
}

.result-head strong {
  color: #b85d38;
  font-size: 20px;
}

.level {
  color: #76867d;
  font-size: 12px;
}

h2 {
  margin: 6px 0 0;
  font-family: "STSong", "SimSun", serif;
  font-size: 23px;
}

.result-body p {
  color: #53655c;
  line-height: 1.7;
}

.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.source {
  display: flex;
  gap: 16px;
  margin-top: 14px;
  font-size: 12px;
}

.source a {
  color: #9c4a2e;
  overflow-wrap: anywhere;
}

.source span {
  color: #86938c;
}

.el-empty {
  margin-top: 18px;
}

@media (max-width: 860px) {
  .profile-grid {
    grid-template-columns: 1fr 1fr;
    padding-right: 0;
  }
}

@media (max-width: 560px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .results article {
    grid-template-columns: 45px 1fr;
  }
}
</style>

