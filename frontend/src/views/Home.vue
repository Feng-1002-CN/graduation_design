<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Connection, Document, MagicStick, Monitor } from '@element-plus/icons-vue'

import { getHealth, type HealthData } from '@/api/system'

const health = ref<HealthData>()
const loading = ref(false)
const online = computed(() => health.value?.status === 'UP')

async function checkHealth() {
  loading.value = true
  try {
    health.value = await getHealth()
  } finally {
    loading.value = false
  }
}

onMounted(checkHealth)
</script>

<template>
  <main class="shell">
    <header class="hero">
      <div class="eyebrow">COMPETITION PROJECT ASSISTANT</div>
      <h1>学科竞赛智能辅助系统</h1>
      <p>从竞赛资料到选题分析、方案与演示内容，让每一步建议都有据可查。</p>
      <div class="status" :class="{ online }">
        <span class="dot" />
        后端服务 {{ health?.status ?? '正在检查' }}
      </div>
    </header>

    <section class="feature-grid">
      <article>
        <el-icon><Document /></el-icon>
        <h2>竞赛知识库</h2>
        <p>解析通知、规则与往届作品，保留来源、日期和页码。</p>
      </article>
      <article>
        <el-icon><MagicStick /></el-icon>
        <h2>选题分析智能体</h2>
        <p>四步检索与推理，形成六维度可溯源分析报告。</p>
      </article>
      <article>
        <el-icon><Monitor /></el-icon>
        <h2>可编辑内容</h2>
        <p>基于方案生成可编辑 PPT，并用用户素材合成演示视频。</p>
      </article>
    </section>

    <footer>
      <el-icon><Connection /></el-icon>
      <span>{{ health ? `服务时间 ${new Date(health.timestamp).toLocaleString()}` : '正在连接本地服务' }}</span>
      <el-button text :loading="loading" @click="checkHealth">重新检查</el-button>
    </footer>
  </main>
</template>

