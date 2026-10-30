<script setup lang="ts">
import { Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { reactive, ref, watch } from 'vue'

import {
  createCompetition,
  updateCompetition,
  type Competition,
  type CompetitionPayload,
} from '@/api/competition'

const props = defineProps<{
  modelValue: boolean
  competition?: Competition
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)

function emptyPayload(): CompetitionPayload {
  return {
    name: '',
    organizer: '',
    category: '',
    level: '',
    officialWebsite: '',
    sourceUrl: '',
    sourceDate: '',
    status: 'ACTIVE',
    groups: [{ name: '', eligibility: '' }],
    rules: [
      {
        title: '',
        content: '',
        sourceUrl: '',
        sourceDate: '',
        status: 'ACTIVE',
      },
    ],
  }
}

const form = reactive<CompetitionPayload>(emptyPayload())

const rules: FormRules<CompetitionPayload> = {
  name: [{ required: true, message: '请输入竞赛名称', trigger: 'blur' }],
  sourceUrl: [{ required: true, message: '请输入来源 URL', trigger: 'blur' }],
  sourceDate: [{ required: true, message: '请选择来源发布日期', trigger: 'change' }],
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    const source = props.competition
    Object.assign(form, source ? structuredClone(source) : emptyPayload())
    if (form.groups.length === 0) form.groups.push({ name: '', eligibility: '' })
    if (form.rules.length === 0) {
      form.rules.push({ title: '', content: '', sourceUrl: '', sourceDate: '', status: 'ACTIVE' })
    }
  },
)

function close() {
  emit('update:modelValue', false)
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (props.competition?.id) {
      await updateCompetition(props.competition.id, form)
      ElMessage.success('竞赛已更新')
    } else {
      await createCompetition(form)
      ElMessage.success('竞赛已创建')
    }
    emit('saved')
    close()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    width="min(920px, 94vw)"
    destroy-on-close
    @close="close"
  >
    <template #header>
      <div class="dialog-title">{{ competition ? '编辑竞赛' : '录入竞赛' }}</div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="form-grid">
        <el-form-item label="竞赛名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：中国大学生计算机设计大赛" />
        </el-form-item>
        <el-form-item label="主办方">
          <el-input v-model="form.organizer" />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="form.category" allow-create filterable placeholder="选择或输入类别">
            <el-option label="软件" value="软件" />
            <el-option label="硬件" value="硬件" />
            <el-option label="AI" value="AI" />
            <el-option label="综合" value="综合" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="form.level" allow-create filterable placeholder="选择或输入级别">
            <el-option label="校级" value="校级" />
            <el-option label="省级" value="省级" />
            <el-option label="国家级" value="国家级" />
            <el-option label="国际级" value="国际级" />
          </el-select>
        </el-form-item>
        <el-form-item label="官网">
          <el-input v-model="form.officialWebsite" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="有效" value="ACTIVE" />
            <el-option label="停用" value="EXPIRED" />
          </el-select>
        </el-form-item>
        <el-form-item label="报名开始">
          <el-date-picker v-model="form.registerStart" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="报名截止">
          <el-date-picker v-model="form.registerEnd" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="比赛开始">
          <el-date-picker v-model="form.contestStart" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="比赛结束">
          <el-date-picker v-model="form.contestEnd" value-format="YYYY-MM-DD" />
        </el-form-item>
      </div>
      <el-form-item label="简介">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>

      <div class="form-grid source-grid">
        <el-form-item label="来源 URL" prop="sourceUrl">
          <el-input v-model="form.sourceUrl" placeholder="官方通知或规则页面" />
        </el-form-item>
        <el-form-item label="来源发布日期" prop="sourceDate">
          <el-date-picker v-model="form.sourceDate" value-format="YYYY-MM-DD" />
        </el-form-item>
      </div>

      <section class="dynamic-section">
        <div class="section-heading">
          <h3>参赛组别</h3>
          <el-button :icon="Plus" text @click="form.groups.push({ name: '', eligibility: '' })">
            添加组别
          </el-button>
        </div>
        <div v-for="(group, index) in form.groups" :key="index" class="dynamic-row">
          <el-input v-model="group.name" placeholder="组别名称" />
          <el-input v-model="group.eligibility" placeholder="参赛资格" />
          <el-button
            :icon="Delete"
            circle
            text
            type="danger"
            @click="form.groups.splice(index, 1)"
          />
        </div>
      </section>

      <section class="dynamic-section">
        <div class="section-heading">
          <h3>结构化规则</h3>
          <el-button
            :icon="Plus"
            text
            @click="form.rules.push({ title: '', content: '', sourceUrl: '', sourceDate: '', status: 'ACTIVE' })"
          >
            添加规则
          </el-button>
        </div>
        <div v-for="(rule, index) in form.rules" :key="index" class="rule-editor">
          <div class="rule-heading">
            <strong>规则 {{ index + 1 }}</strong>
            <el-button
              :icon="Delete"
              circle
              text
              type="danger"
              @click="form.rules.splice(index, 1)"
            />
          </div>
          <el-input v-model="rule.title" placeholder="规则标题" />
          <el-input v-model="rule.content" type="textarea" :rows="3" placeholder="规则正文" />
          <div class="form-grid">
            <el-input v-model="rule.sourceUrl" placeholder="规则来源 URL" />
            <el-date-picker
              v-model="rule.sourceDate"
              value-format="YYYY-MM-DD"
              placeholder="规则来源日期"
            />
          </div>
        </div>
      </section>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-title {
  font-family: "STSong", "SimSun", serif;
  font-size: 24px;
  font-weight: 600;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.source-grid {
  padding: 16px;
  border-radius: 14px;
  background: #f4f8f5;
}

.dynamic-section {
  margin-top: 18px;
  padding: 18px;
  border: 1px solid #e1e7e2;
  border-radius: 16px;
}

.section-heading,
.rule-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-heading h3 {
  margin: 0;
  font-size: 16px;
}

.dynamic-row {
  display: grid;
  grid-template-columns: 1fr 1.5fr auto;
  gap: 10px;
  margin-top: 12px;
}

.rule-editor {
  display: grid;
  gap: 10px;
  margin-top: 12px;
  padding: 14px;
  border-radius: 12px;
  background: #f8f5ef;
}

.rule-editor .form-grid {
  grid-template-columns: 2fr 1fr;
}

@media (max-width: 680px) {
  .form-grid,
  .rule-editor .form-grid,
  .dynamic-row {
    grid-template-columns: 1fr;
  }
}
</style>

