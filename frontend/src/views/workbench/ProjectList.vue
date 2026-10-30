<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchProjects, type ProjectSummary } from '@/api/workbench'

const router = useRouter()
const projects = ref<ProjectSummary[]>([])
const total = ref(0)
const loading = ref(false)
const page = reactive({ number: 1, size: 20 })

async function load() {
  loading.value = true
  try {
    const result = await fetchProjects(page.number, page.size)
    projects.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function createProject() {
  const title = window.prompt('请输入项目标题')
  if (!title) return
  await router.push({ path: '/agent/analyze', query: { title } })
}

onMounted(load)
</script>

<template>
  <main class="projects-shell">
    <header>
      <div>
        <span class="eyebrow">PROJECT PORTFOLIO</span>
        <h1>项目工作台</h1>
        <p>统一查看报告、方案、生成任务、申报检查与 AI 轨迹。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="createProject">新建分析项目</el-button>
    </header>

    <section class="project-grid" v-loading="loading">
      <article
        v-for="project in projects"
        :key="project.id"
        @click="router.push(`/workbench/projects/${project.id}`)"
      >
        <div class="project-meta">
          <span>#{{ project.id }}</span>
          <el-tag :type="project.status === 'ANALYZED' ? 'success' : 'info'">{{ project.status }}</el-tag>
        </div>
        <h2>{{ project.title }}</h2>
        <p>{{ project.description || '暂无项目描述' }}</p>
        <footer>
          <span>更新于 {{ new Date(project.updatedAt).toLocaleString() }}</span>
          <el-icon><Search /></el-icon>
        </footer>
      </article>
    </section>
    <el-empty v-if="!loading && projects.length === 0" description="还没有项目，请先完成一次选题分析" />
    <el-pagination
      v-model:current-page="page.number"
      v-model:page-size="page.size"
      :total="total"
      layout="total, prev, pager, next"
      @change="load"
    />
  </main>
</template>

<style scoped>
.projects-shell {
  width: min(1180px, calc(100% - 40px));
  margin: 0 auto;
  padding: 48px 0 70px;
}

header {
  display: flex;
  gap: 20px;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 24px;
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

.project-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.project-grid article {
  min-height: 190px;
  padding: 22px;
  cursor: pointer;
  border: 1px solid rgba(32, 68, 56, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  transition: transform 0.2s, box-shadow 0.2s;
}

.project-grid article:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 40px rgba(31, 68, 55, 0.1);
}

.project-meta,
footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.project-meta span,
footer span {
  color: #87948d;
  font-size: 12px;
}

h2 {
  margin: 24px 0 10px;
  font-family: "STSong", "SimSun", serif;
  font-size: 22px;
}

.project-grid p {
  min-height: 46px;
  color: #607269;
  line-height: 1.6;
}

footer {
  margin-top: 20px;
  color: #b45a39;
}

.el-pagination {
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 850px) {
  .project-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 580px) {
  .project-grid {
    grid-template-columns: 1fr;
  }

  header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>

