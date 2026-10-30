<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  deleteCompetition,
  fetchCompetition,
  fetchCompetitions,
  type Competition,
  type CompetitionFilters,
} from '@/api/competition'
import CompetitionForm from '@/components/CompetitionForm.vue'

const router = useRouter()
const loading = ref(false)
const competitions = ref<Competition[]>([])
const total = ref(0)
const formVisible = ref(false)
const editing = ref<Competition>()

const filters = reactive<CompetitionFilters>({
  page: 1,
  size: 10,
  name: '',
  organizer: '',
  category: '',
  level: '',
  status: '',
})

async function load() {
  loading.value = true
  try {
    const result = await fetchCompetitions(filters)
    competitions.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(filters, {
    page: 1,
    name: '',
    organizer: '',
    category: '',
    level: '',
    status: '',
  })
  load()
}

async function edit(row: Competition) {
  editing.value = await fetchCompetition(row.id)
  formVisible.value = true
}

function create() {
  editing.value = undefined
  formVisible.value = true
}

async function remove(row: Competition) {
  await ElMessageBox.confirm(`确认停用并删除“${row.name}”？`, '逻辑删除', {
    type: 'warning',
  })
  await deleteCompetition(row.id)
  ElMessage.success('已逻辑删除')
  load()
}

function formatDate(value?: string) {
  return value || '—'
}

onMounted(load)
</script>

<template>
  <main class="page-shell">
    <header class="page-head">
      <div>
        <span class="eyebrow">COMPETITION CATALOG</span>
        <h1>竞赛信息管理</h1>
        <p>维护赛事、组别和规则的结构化资料，来源与发布日期随记录强制保存。</p>
      </div>
      <div class="head-actions">
        <el-button @click="router.push('/')">返回首页</el-button>
        <el-button type="primary" :icon="Plus" @click="create">录入竞赛</el-button>
      </div>
    </header>

    <section class="filter-panel">
      <el-form :inline="true" :model="filters">
        <el-form-item label="名称">
          <el-input v-model="filters.name" clearable placeholder="竞赛名称" />
        </el-form-item>
        <el-form-item label="主办方">
          <el-input v-model="filters.organizer" clearable placeholder="主办方" />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="filters.category" clearable placeholder="全部">
            <el-option v-for="item in ['软件', '硬件', 'AI', '综合']" :key="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="filters.level" clearable placeholder="全部">
            <el-option v-for="item in ['校级', '省级', '国家级', '国际级']" :key="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部">
            <el-option label="有效" value="ACTIVE" />
            <el-option label="过期" value="EXPIRED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="filters.page = 1; load()">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-panel">
      <el-table v-loading="loading" :data="competitions" row-key="id">
        <el-table-column prop="name" label="竞赛名称" min-width="230" show-overflow-tooltip />
        <el-table-column prop="organizer" label="主办方" min-width="180" show-overflow-tooltip />
        <el-table-column prop="category" label="类别" width="90" />
        <el-table-column prop="level" label="级别" width="100" />
        <el-table-column label="报名截止" width="120">
          <template #default="{ row }">{{ formatDate(row.registerEnd) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '有效' : '过期' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="edit(row)">编辑</el-button>
            <el-button text type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="filters.page"
        v-model:page-size="filters.size"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @change="load"
      />
    </section>

    <CompetitionForm
      v-model="formVisible"
      :competition="editing"
      @saved="load"
    />
  </main>
</template>

<style scoped>
.page-shell {
  width: min(1280px, calc(100% - 40px));
  margin: 0 auto;
  padding: 48px 0;
}

.page-head {
  display: flex;
  gap: 30px;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 28px;
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

.page-head p {
  margin: 0;
  color: #65756d;
}

.head-actions {
  display: flex;
  gap: 10px;
}

.filter-panel,
.table-panel {
  padding: 22px;
  border: 1px solid rgba(32, 68, 56, 0.1);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.7);
}

.table-panel {
  margin-top: 16px;
}

.el-pagination {
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 720px) {
  .page-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>

