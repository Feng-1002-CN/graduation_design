<script setup lang="ts">
import { CircleCheck, Loading, MagicStick } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { computed, onMounted, reactive, ref } from 'vue'

import { analyzeTopic, type AgentEvent } from '@/api/agent'
import { fetchCompetitions, type Competition } from '@/api/competition'

const competitions = ref<Competition[]>([])
const running = ref(false)
const report = ref('')
const events = ref<AgentEvent[]>([])
const activeStep = ref(0)
const finished = ref(false)
const form = reactive({
  title: '',
  description: '',
  competitionId: undefined as number | undefined,
})

const stepCards = computed(() => [
  { step: 1, title: '检索赛事规则', tool: 'searchRules', state: stateOf(1) },
  { step: 2, title: '匹配参赛组别', tool: 'matchGroup', state: stateOf(2) },
  { step: 3, title: '检索往届作品', tool: 'searchPastWorks', state: stateOf(3) },
  { step: 4, title: '生成六维报告', tool: 'generateReport', state: stateOf(4) },
])

const reportHtml = computed(() => marked.parse(report.value, { async: false }))

function stateOf(step: number) {
  if (events.value.some((event) => event.type === 'step_end' && event.step === step)) return 'done'
  if (activeStep.value === step) return 'running'
  return 'waiting'
}

async function start() {
  if (!form.title.trim()) return
  report.value = ''
  events.value = []
  activeStep.value = 1
  finished.value = false
  running.value = true
  try {
    await analyzeTopic(
      {
        title: form.title,
        description: form.description,
        competitionId: form.competitionId,
      },
      (event) => {
        events.value.push(event)
        if (event.type === 'step_start' && event.step) activeStep.value = event.step
        if (event.type === 'delta' && event.content) report.value += event.content
        if (event.type === 'done') {
          activeStep.value = 0
          finished.value = true
        }
        if (event.type === 'error') {
          activeStep.value = 0
        }
      },
    )
  } finally {
    running.value = false
  }
}

onMounted(async () => {
  competitions.value = (await fetchCompetitions({ page: 1, size: 100 })).records
})
</script>

<template>
  <main class="agent-shell">
    <header>
      <span class="eyebrow">EVIDENCE-FIRST AGENT</span>
      <h1>智能选题分析</h1>
      <p>四步检索与推理，所有结论可追溯；不预测任何评奖结果。</p>
    </header>

    <section class="input-panel">
      <div class="title-line">
        <el-input v-model="form.title" size="large" placeholder="选题标题，例如：基于计算机视觉的中药材识别系统" />
        <el-select v-model="form.competitionId" clearable size="large" placeholder="自动匹配竞赛">
          <el-option
            v-for="item in competitions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </div>
      <el-input
        v-model="form.description"
        type="textarea"
        :rows="4"
        placeholder="描述核心问题、目标用户、数据、算法与预期作品形态"
      />
      <el-button type="primary" size="large" :icon="MagicStick" :loading="running" @click="start">
        启动四步分析
      </el-button>
    </section>

    <section class="steps">
      <article v-for="card in stepCards" :key="card.step" :class="card.state">
        <span class="step-number">0{{ card.step }}</span>
        <div>
          <h3>{{ card.title }}</h3>
          <small>{{ card.tool }}</small>
        </div>
        <el-icon v-if="card.state === 'done'"><CircleCheck /></el-icon>
        <el-icon v-else-if="card.state === 'running'" class="spin"><Loading /></el-icon>
      </article>
    </section>

    <section class="workspace">
      <aside>
        <h2>过程日志</h2>
        <div v-for="(event, index) in events" :key="index" class="event">
          <span>{{ event.type }}</span>
          <p>{{ event.message || event.summary || event.data?.source || event.content?.slice(0, 80) }}</p>
          <small v-if="event.data?.sourceDate">
            {{ event.data.sourceDate }} · 第 {{ event.data.pageNumber ?? '—' }} 页
          </small>
        </div>
        <el-empty v-if="events.length === 0" description="等待分析任务" :image-size="80" />
      </aside>

      <article class="report-pane">
        <div class="report-head">
          <h2>分析报告</h2>
          <el-tag v-if="finished" type="success">已生成</el-tag>
        </div>
        <div v-if="report" class="markdown" v-html="reportHtml" />
        <el-empty v-else description="报告内容将随步骤实时出现" />
      </article>
    </section>
  </main>
</template>

<style scoped>
.agent-shell {
  width: min(1380px, calc(100% - 40px));
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
  font-size: 46px;
}

header p {
  margin: 0 0 24px;
  color: #65756d;
}

.input-panel {
  display: grid;
  gap: 12px;
  padding: 24px;
  border-radius: 22px;
  background: #173d33;
}

.title-line {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 12px;
}

.input-panel .el-button {
  justify-self: end;
}

.steps {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin: 16px 0;
}

.steps article {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 16px;
  color: #7d8982;
  border: 1px solid rgba(32, 68, 56, 0.1);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.64);
}

.steps article.running {
  color: #9c4a2e;
  border-color: #d98561;
  box-shadow: 0 8px 30px rgba(184, 93, 59, 0.12);
}

.steps article.done {
  color: #31715b;
  border-color: #b8d6c7;
}

.step-number {
  color: #c3cbc6;
  font-family: "Georgia", serif;
  font-size: 24px;
}

.steps h3 {
  margin: 0 0 4px;
  font-size: 14px;
}

.steps small {
  font-size: 10px;
}

.steps .el-icon {
  margin-left: auto;
}

.spin {
  animation: rotate 1.2s linear infinite;
}

@keyframes rotate {
  to { transform: rotate(360deg); }
}

.workspace {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 16px;
}

.workspace aside,
.report-pane {
  padding: 22px;
  border: 1px solid rgba(32, 68, 56, 0.1);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
}

.workspace h2 {
  margin: 0 0 16px;
  font-family: "STSong", "SimSun", serif;
}

.event {
  padding: 10px 0;
  border-bottom: 1px solid #edf0ed;
}

.event span {
  color: #b45a39;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.event p {
  margin: 5px 0;
  color: #52665c;
  font-size: 13px;
  line-height: 1.55;
}

.event small {
  color: #8d9992;
  font-size: 11px;
}

.report-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.markdown {
  color: #273c33;
  line-height: 1.8;
}

.markdown :deep(h1) {
  font-family: "STSong", "SimSun", serif;
  font-size: 32px;
}

.markdown :deep(h2) {
  margin-top: 28px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e2e8e4;
}

.markdown :deep(blockquote) {
  margin: 18px 0;
  padding: 12px 16px;
  color: #6c7972;
  border-left: 3px solid #d47a55;
  background: #f7f3ec;
}

.markdown :deep(li) {
  margin: 7px 0;
}

@media (max-width: 900px) {
  .steps {
    grid-template-columns: 1fr 1fr;
  }

  .workspace {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .title-line,
  .steps {
    grid-template-columns: 1fr;
  }
}
</style>

