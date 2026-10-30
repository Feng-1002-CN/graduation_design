<script setup lang="ts">
import { Download, MagicStick, Monitor } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onBeforeUnmount, reactive, ref } from 'vue'

import {
  createPptTask,
  fetchTask,
  pptDownloadUrl,
  type GenerationTask,
} from '@/api/task'

const form = reactive({ projectId: 1, templateId: 'default' })
const creating = ref(false)
const task = ref<GenerationTask>()
let timer: number | undefined

async function generate() {
  creating.value = true
  try {
    const created = await createPptTask(form.projectId, form.templateId)
    task.value = await fetchTask(created.taskId)
    ElMessage.success('PPT 生成任务已提交')
    poll()
  } finally {
    creating.value = false
  }
}

function poll() {
  window.clearTimeout(timer)
  timer = window.setTimeout(async () => {
    if (!task.value) return
    task.value = await fetchTask(task.value.taskId)
    if (task.value.status === 'PENDING' || task.value.status === 'RUNNING') poll()
  }, 900)
}

onBeforeUnmount(() => window.clearTimeout(timer))
</script>

<template>
  <main class="ppt-shell">
    <header>
      <span class="eyebrow">EDITABLE PRESENTATION</span>
      <h1>PPT 智能生成</h1>
      <p>基于项目方案填充内置模板，输出 PowerPoint 与 WPS 均可编辑的 PPTX。</p>
    </header>

    <section class="generator-card">
      <div class="preview-icon"><el-icon><Monitor /></el-icon></div>
      <div class="form-zone">
        <h2>默认演示模板</h2>
        <p>5 页结构：封面、背景与需求、功能模块、技术路线、开发计划。</p>
        <div class="inputs">
          <el-form-item label="项目 ID">
            <el-input-number v-model="form.projectId" :min="1" />
          </el-form-item>
          <el-form-item label="模板">
            <el-select v-model="form.templateId">
              <el-option label="默认模板" value="default" />
            </el-select>
          </el-form-item>
          <el-button type="primary" :icon="MagicStick" :loading="creating" @click="generate">
            生成可编辑 PPT
          </el-button>
        </div>
      </div>
    </section>

    <section v-if="task" class="task-card">
      <div class="task-head">
        <div>
          <span>任务 #{{ task.taskId }}</span>
          <h2>{{ task.status }}</h2>
        </div>
        <el-tag :type="task.status === 'SUCCESS' ? 'success' : task.status === 'FAILED' ? 'danger' : 'warning'">
          {{ task.stage ?? task.status }}
        </el-tag>
      </div>
      <el-progress
        :percentage="task.progress"
        :status="task.status === 'FAILED' ? 'exception' : task.status === 'SUCCESS' ? 'success' : undefined"
      />
      <el-alert v-if="task.errorMsg" :title="task.errorMsg" type="error" :closable="false" />
      <a v-if="task.status === 'SUCCESS'" :href="pptDownloadUrl(task.taskId)">
        <el-button type="success" :icon="Download">下载 PPTX</el-button>
      </a>
    </section>
  </main>
</template>

<style scoped>
.ppt-shell {
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

.generator-card {
  display: grid;
  grid-template-columns: 280px 1fr;
  overflow: hidden;
  border-radius: 22px;
  background: #173d33;
}

.preview-icon {
  display: grid;
  place-items: center;
  min-height: 280px;
  color: #f2b394;
  font-size: 100px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.03), rgba(225, 116, 76, 0.14)),
    repeating-linear-gradient(45deg, transparent 0 18px, rgba(255, 255, 255, 0.03) 18px 19px);
}

.form-zone {
  padding: 32px;
  color: #f5f1e6;
}

.form-zone h2 {
  margin: 0;
  font-family: "STSong", "SimSun", serif;
  font-size: 28px;
}

.form-zone p {
  color: rgba(245, 241, 230, 0.65);
  line-height: 1.7;
}

.inputs {
  display: flex;
  gap: 14px;
  align-items: flex-end;
  margin-top: 32px;
}

.inputs .el-select {
  width: 150px;
}

.task-card {
  display: grid;
  gap: 18px;
  margin-top: 16px;
  padding: 24px;
  border: 1px solid rgba(32, 68, 56, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.74);
}

.task-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.task-head span {
  color: #849089;
  font-size: 12px;
}

.task-head h2 {
  margin: 5px 0 0;
}

.task-card a {
  text-decoration: none;
}

@media (max-width: 700px) {
  .generator-card {
    grid-template-columns: 1fr;
  }

  .preview-icon {
    min-height: 150px;
  }

  .inputs {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>

