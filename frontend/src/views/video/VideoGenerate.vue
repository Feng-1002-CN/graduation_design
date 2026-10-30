<script setup lang="ts">
import { Download, UploadFilled, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage, type UploadFile } from 'element-plus'
import { onBeforeUnmount, reactive, ref } from 'vue'

import { fetchTask, type GenerationTask } from '@/api/task'
import { createVideoTask, uploadVideoMaterial, videoUrl } from '@/api/video'

const form = reactive({ projectId: 1 })
const files = ref<File[]>([])
const creating = ref(false)
const task = ref<GenerationTask>()
let timer: number | undefined

function onFileChange(file: UploadFile) {
  if (file.raw) files.value.push(file.raw)
}

async function generate() {
  creating.value = true
  try {
    const imageUrls: string[] = []
    for (const file of files.value) {
      imageUrls.push(await uploadVideoMaterial(file))
    }
    const created = await createVideoTask(form.projectId, imageUrls)
    task.value = await fetchTask(created.taskId)
    ElMessage.success('视频任务已提交')
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
  <main class="video-shell">
    <header>
      <span class="eyebrow">NARRATION + MATERIALS</span>
      <h1>视频辅助制作</h1>
      <p>模型生成分镜旁白，TTS 配音，再用你的截图或演示片段合成 MP4。全程不使用视频生成大模型。</p>
    </header>

    <section class="create-card">
      <div class="visual">
        <el-icon><VideoPlay /></el-icon>
        <span>1280 × 720 · MP4</span>
      </div>
      <div class="controls">
        <el-form-item label="项目 ID">
          <el-input-number v-model="form.projectId" :min="1" />
        </el-form-item>
        <el-upload
          drag
          multiple
          accept=".png,.jpg,.jpeg,.webp"
          :auto-upload="false"
          :on-change="onFileChange"
        >
          <el-icon><UploadFilled /></el-icon>
          <span>上传项目截图或演示图片（可选）</span>
        </el-upload>
        <small>已选择 {{ files.length }} 个素材；不上传时使用内置演示占位图。</small>
        <el-button type="primary" size="large" :loading="creating" @click="generate">
          生成演示视频
        </el-button>
      </div>
    </section>

    <section v-if="task" class="task-card">
      <div class="task-head">
        <div>
          <span>任务 #{{ task.taskId }}</span>
          <h2>{{ task.status }}</h2>
        </div>
        <el-tag>{{ task.stage ?? 'QUEUED' }}</el-tag>
      </div>
      <el-progress
        :percentage="task.progress"
        :status="task.status === 'FAILED' ? 'exception' : task.status === 'SUCCESS' ? 'success' : undefined"
      />
      <el-alert v-if="task.errorMsg" :title="task.errorMsg" type="error" :closable="false" />
      <template v-if="task.status === 'SUCCESS'">
        <video :src="videoUrl(task.taskId)" controls preload="metadata" />
        <a :href="videoUrl(task.taskId)" download>
          <el-button :icon="Download">下载 MP4</el-button>
        </a>
      </template>
    </section>
  </main>
</template>

<style scoped>
.video-shell {
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
  max-width: 760px;
  margin: 0 0 24px;
  color: #65756d;
  line-height: 1.7;
}

.create-card {
  display: grid;
  grid-template-columns: 360px 1fr;
  overflow: hidden;
  border-radius: 22px;
  background: #183f35;
}

.visual {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 16px;
  min-height: 330px;
  color: #f5b28f;
  background:
    radial-gradient(circle at center, rgba(229, 126, 86, 0.2), transparent 45%),
    repeating-linear-gradient(90deg, transparent 0 42px, rgba(255, 255, 255, 0.035) 42px 43px);
}

.visual .el-icon {
  font-size: 96px;
}

.visual span {
  color: rgba(255, 255, 255, 0.55);
  font-size: 12px;
  letter-spacing: 0.1em;
}

.controls {
  display: grid;
  gap: 14px;
  padding: 28px;
  color: #f8f4e8;
}

.controls small {
  color: rgba(255, 255, 255, 0.58);
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

video {
  width: 100%;
  max-height: 620px;
  border-radius: 14px;
  background: #0c1713;
}

.task-card a {
  justify-self: start;
  text-decoration: none;
}

@media (max-width: 700px) {
  .create-card {
    grid-template-columns: 1fr;
  }

  .visual {
    min-height: 180px;
  }
}
</style>

