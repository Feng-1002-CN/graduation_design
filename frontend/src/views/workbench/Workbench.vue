<script setup lang="ts">
import { Download, Refresh } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchTask } from '@/api/task'
import {
  fetchWorkbench,
  fetchWorkbenchPlan,
  fetchWorkbenchReport,
  type Workbench,
} from '@/api/workbench'

const route = useRoute()
const router = useRouter()
const projectId = Number(route.params.projectId)
const loading = ref(false)
const data = ref<Workbench>()
const reportContent = ref('')
const plan = ref<{ title?: string; modules?: unknown; techRoute?: string; background?: string }>()
const activeTab = ref('report')

const reportHtml = computed(() => marked.parse(reportContent.value, { async: false }))

async function load() {
  loading.value = true
  try {
    data.value = await fetchWorkbench(projectId)
    if (data.value.latestReport) {
      reportContent.value = (await fetchWorkbenchReport(projectId)).content
    }
    if (data.value.plan) {
      plan.value = await fetchWorkbenchPlan(projectId)
    }
  } finally {
    loading.value = false
  }
}

function taskUrl(taskId: number) {
  return `/api/v1/tasks/${taskId}`
}

async function refreshTask(taskId: number) {
  await fetchTask(taskId)
  load()
}

function modulesText(value?: string) {
  try {
    return value ? JSON.parse(value).map((item: { name: string }) => item.name).join(' · ') : '—'
  } catch {
    return value ?? '—'
  }
}

onMounted(load)
</script>

<template>
  <main class="workbench-shell" v-loading="loading">
    <header v-if="data">
      <button class="back" @click="router.push('/workbench/projects')">← 项目列表</button>
      <div class="title-row">
        <div>
          <span>PROJECT #{{ data.project.id }}</span>
          <h1>{{ data.project.title }}</h1>
          <p>{{ data.project.description }}</p>
        </div>
        <el-button :icon="Refresh" @click="load">刷新工作台</el-button>
      </div>
    </header>

    <section v-if="data" class="metric-grid">
      <article><span>分析报告</span><strong>{{ data.latestReport ? '已生成' : '待分析' }}</strong></article>
      <article><span>项目方案</span><strong>{{ data.plan ? '已生成' : '待生成' }}</strong></article>
      <article><span>内容任务</span><strong>{{ data.tasks.length }}</strong></article>
      <article><span>AI 工具轨迹</span><strong>{{ data.traces.length }}</strong></article>
    </section>

    <el-tabs v-if="data" v-model="activeTab" class="workspace-tabs">
      <el-tab-pane label="分析报告" name="report">
        <article v-if="reportContent" class="markdown" v-html="reportHtml" />
        <el-empty v-else description="尚未生成分析报告" />
      </el-tab-pane>
      <el-tab-pane label="项目方案" name="plan">
        <article v-if="plan" class="plan-panel">
          <h2>{{ plan.title }}</h2>
          <p>{{ plan.background }}</p>
          <div class="plan-row">
            <span>技术路线</span>
            <p>{{ plan.techRoute }}</p>
          </div>
          <div class="plan-row">
            <span>功能模块</span>
            <p>{{ Array.isArray(plan.modules) ? plan.modules.map((item: { name: string }) => item.name).join(' · ') : plan.modules }}</p>
          </div>
        </article>
        <el-empty v-else description="尚未生成项目方案" />
      </el-tab-pane>
      <el-tab-pane label="PPT / 视频" name="tasks">
        <div class="task-list">
          <article v-for="task in data.tasks" :key="task.taskId">
            <div>
              <span>{{ task.taskType }} · 任务 #{{ task.taskId }}</span>
              <h3>{{ task.status }} / {{ task.stage ?? 'QUEUED' }}</h3>
              <small v-if="task.errorMsg">{{ task.errorMsg }}</small>
            </div>
            <el-progress :percentage="task.progress" :stroke-width="8" />
            <div class="task-actions">
              <a
                v-if="task.status === 'SUCCESS' && task.taskType === 'PPT'"
                :href="`/api/v1/ppt/${task.taskId}/download`"
              >
                <el-button :icon="Download">下载 PPT</el-button>
              </a>
              <a
                v-if="task.status === 'SUCCESS' && task.taskType === 'VIDEO'"
                :href="`/api/v1/video/${task.taskId}/download`"
              >
                <el-button :icon="Download">下载视频</el-button>
              </a>
            </div>
            <video
              v-if="task.status === 'SUCCESS' && task.taskType === 'VIDEO'"
              :src="`/api/v1/video/${task.taskId}/download`"
              controls
            />
          </article>
        </div>
        <el-empty v-if="data.tasks.length === 0" description="尚未生成 PPT 或视频" />
      </el-tab-pane>
      <el-tab-pane label="申报检查" name="apply">
        <article v-if="data.applyCheck" class="apply-summary">
          <h2>检查报告 #{{ data.applyCheck.id }}</h2>
          <pre>{{ JSON.stringify(data.applyCheck.result, null, 2) }}</pre>
        </article>
        <el-empty v-else description="尚未执行申报检查" />
      </el-tab-pane>
      <el-tab-pane label="生成日志" name="logs">
        <div class="timeline">
          <article v-for="event in data.timeline" :key="`${event.type}-${event.time}-${event.title}`">
            <time>{{ new Date(event.time).toLocaleString() }}</time>
            <div>
              <span>{{ event.type }}</span>
              <h3>{{ event.title }}</h3>
              <p>{{ event.status }}</p>
            </div>
          </article>
        </div>
        <el-empty v-if="data.timeline.length === 0" description="暂无生成日志" />
      </el-tab-pane>
    </el-tabs>
  </main>
</template>

<style scoped>
.workbench-shell {
  width: min(1180px, calc(100% - 40px));
  margin: 0 auto;
  padding: 38px 0 70px;
}

.back {
  padding: 0;
  color: #7a8981;
  cursor: pointer;
  border: 0;
  background: none;
}

.title-row {
  display: flex;
  gap: 20px;
  align-items: flex-end;
  justify-content: space-between;
  margin: 18px 0 24px;
}

.title-row span {
  color: #b45a39;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.title-row h1 {
  margin: 8px 0;
  font-family: "STSong", "SimSun", serif;
  font-size: 40px;
}

.title-row p {
  max-width: 720px;
  margin: 0;
  color: #66776e;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 18px;
}

.metric-grid article {
  display: grid;
  gap: 10px;
  padding: 18px;
  border-radius: 16px;
  background: #173d33;
}

.metric-grid span {
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
}

.metric-grid strong {
  color: #f3b08f;
  font-family: "STSong", "SimSun", serif;
  font-size: 22px;
}

.workspace-tabs {
  padding: 22px;
  border: 1px solid rgba(32, 68, 56, 0.1);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.74);
}

.markdown {
  max-width: 900px;
  line-height: 1.85;
}

.markdown :deep(h2) {
  padding-bottom: 8px;
  border-bottom: 1px solid #e2e8e4;
}

.plan-panel h2 {
  font-family: "STSong", "SimSun", serif;
  font-size: 28px;
}

.plan-row {
  margin-top: 18px;
  padding: 16px;
  border-radius: 12px;
  background: #f5f3eb;
}

.plan-row span {
  color: #b45a39;
  font-size: 12px;
  font-weight: 700;
}

.plan-row p {
  margin: 8px 0 0;
  line-height: 1.7;
}

.task-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.task-list article {
  display: grid;
  gap: 12px;
  padding: 16px;
  border-radius: 14px;
  background: #f5f3eb;
}

.task-list span,
.task-list small {
  color: #7b8981;
  font-size: 12px;
}

.task-list h3 {
  margin: 5px 0 0;
}

.task-actions a {
  text-decoration: none;
}

.task-list video {
  width: 100%;
  border-radius: 10px;
  background: #0d1914;
}

.apply-summary pre {
  max-height: 520px;
  padding: 16px;
  overflow: auto;
  border-radius: 12px;
  background: #f5f3eb;
}

.timeline {
  position: relative;
  display: grid;
  gap: 12px;
}

.timeline article {
  display: grid;
  grid-template-columns: 150px 1fr;
  gap: 18px;
  padding: 12px 0;
  border-bottom: 1px solid #edf0ed;
}

.timeline time {
  color: #85928b;
  font-size: 12px;
}

.timeline span {
  color: #b45a39;
  font-size: 10px;
  font-weight: 700;
}

.timeline h3 {
  margin: 5px 0;
  font-size: 15px;
}

.timeline p {
  margin: 0;
  color: #6c7c73;
}

@media (max-width: 760px) {
  .metric-grid,
  .task-list {
    grid-template-columns: 1fr 1fr;
  }

  .title-row {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 520px) {
  .metric-grid,
  .task-list {
    grid-template-columns: 1fr;
  }

  .timeline article {
    grid-template-columns: 1fr;
  }
}
</style>

