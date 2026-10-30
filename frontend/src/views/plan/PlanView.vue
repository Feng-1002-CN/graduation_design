<script setup lang="ts">
import { EditPen, MagicStick, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import {
  fetchPlan,
  generatePlan,
  updatePlan,
  type ProjectPlan,
} from '@/api/plan'

const route = useRoute()
const projectId = ref(Number(route.query.projectId || 1))
const plan = ref<ProjectPlan>()
const loading = ref(false)
const editing = ref(false)

async function load() {
  if (!projectId.value) return
  try {
    plan.value = await fetchPlan(projectId.value)
  } catch {
    plan.value = undefined
  }
}

async function generate() {
  loading.value = true
  try {
    plan.value = await generatePlan(projectId.value)
    ElMessage.success('结构化方案已生成')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!plan.value) return
  plan.value = await updatePlan(plan.value)
  editing.value = false
  ElMessage.success('方案已保存')
}

onMounted(load)
</script>

<template>
  <main class="plan-shell">
    <header>
      <span class="eyebrow">STRUCTURED PROJECT BLUEPRINT</span>
      <h1>项目方案辅助</h1>
      <p>以分析结果为上下文生成结构化方案，模块清单可继续用于 PPT 和视频。</p>
    </header>

    <section class="toolbar">
      <el-input-number v-model="projectId" :min="1" controls-position="right" />
      <el-button @click="load">读取方案</el-button>
      <el-button type="primary" :icon="MagicStick" :loading="loading" @click="generate">
        生成结构化方案
      </el-button>
      <el-button v-if="plan" :icon="EditPen" @click="editing = !editing">
        {{ editing ? '取消编辑' : '编辑方案' }}
      </el-button>
      <el-button v-if="editing" type="success" @click="save">保存修改</el-button>
    </section>

    <article v-if="plan" class="plan-document">
      <template v-if="editing">
        <el-form label-position="top">
          <el-form-item label="方案标题"><el-input v-model="plan.title" /></el-form-item>
          <div class="two-column">
            <el-form-item label="项目背景">
              <el-input v-model="plan.background" type="textarea" :rows="5" />
            </el-form-item>
            <el-form-item label="需求分析">
              <el-input v-model="plan.requirements" type="textarea" :rows="5" />
            </el-form-item>
          </div>
          <div class="section-head">
            <h2>功能模块</h2>
            <el-button :icon="Plus" text @click="plan.modules.push({ name: '', description: '' })">
              添加模块
            </el-button>
          </div>
          <div v-for="(module, index) in plan.modules" :key="index" class="module-edit">
            <el-input v-model="module.name" placeholder="模块名称" />
            <el-input v-model="module.description" placeholder="模块职责" />
          </div>
          <el-form-item label="技术路线">
            <el-input v-model="plan.techRoute" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="创新点">
            <el-input v-model="plan.innovation" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="开发计划">
            <el-input v-model="plan.schedule" type="textarea" :rows="3" />
          </el-form-item>
        </el-form>
      </template>
      <template v-else>
        <div class="doc-head">
          <span>PROJECT PLAN</span>
          <h2>{{ plan.title }}</h2>
          <small>项目 #{{ plan.projectId }}</small>
        </div>
        <section>
          <h3>背景</h3>
          <p>{{ plan.background }}</p>
        </section>
        <section>
          <h3>需求分析</h3>
          <p>{{ plan.requirements }}</p>
        </section>
        <section>
          <h3>功能模块</h3>
          <div class="module-grid">
            <article v-for="(module, index) in plan.modules" :key="index">
              <span>{{ String(index + 1).padStart(2, '0') }}</span>
              <h4>{{ module.name }}</h4>
              <p>{{ module.description }}</p>
            </article>
          </div>
        </section>
        <div class="two-column">
          <section>
            <h3>技术路线</h3>
            <p>{{ plan.techRoute }}</p>
          </section>
          <section>
            <h3>创新点</h3>
            <p>{{ plan.innovation }}</p>
          </section>
        </div>
        <section>
          <h3>开发计划</h3>
          <p>{{ plan.schedule }}</p>
        </section>
      </template>
    </article>
    <el-empty v-else description="输入项目 ID 生成或读取方案" />
  </main>
</template>

<style scoped>
.plan-shell {
  width: min(1100px, calc(100% - 40px));
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

.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.plan-document {
  padding: 36px 42px;
  border: 1px solid rgba(32, 68, 56, 0.1);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 20px 60px rgba(43, 67, 56, 0.08);
}

.doc-head {
  padding-bottom: 24px;
  border-bottom: 2px solid #214f41;
}

.doc-head span {
  color: #b45a39;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.15em;
}

.doc-head h2 {
  margin: 10px 0;
  font-family: "STSong", "SimSun", serif;
  font-size: 34px;
}

.doc-head small {
  color: #849089;
}

.plan-document section {
  margin-top: 24px;
}

.plan-document h3 {
  margin-bottom: 10px;
  color: #b45a39;
  font-size: 14px;
  letter-spacing: 0.08em;
}

.plan-document p {
  margin: 0;
  color: #45594f;
  line-height: 1.85;
  white-space: pre-wrap;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.module-grid article {
  padding: 16px;
  border-radius: 12px;
  background: #f4f2ea;
}

.module-grid span {
  color: #c77855;
  font-family: "Georgia", serif;
  font-size: 22px;
}

.module-grid h4 {
  margin: 8px 0;
  font-size: 15px;
}

.module-grid p {
  font-size: 13px;
}

.two-column {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
}

.section-head h2 {
  font-size: 17px;
}

.module-edit {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 10px;
  margin-bottom: 10px;
}

@media (max-width: 680px) {
  .plan-document {
    padding: 24px;
  }

  .module-grid,
  .two-column,
  .module-edit {
    grid-template-columns: 1fr;
  }
}
</style>

