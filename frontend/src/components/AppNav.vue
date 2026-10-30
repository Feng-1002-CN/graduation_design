<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
const items = [
  { path: '/', label: '首页' },
  { path: '/competitions', label: '竞赛' },
  { path: '/knowledge', label: '知识库' },
  { path: '/rules/qa', label: '规则' },
  { path: '/search/recommend', label: '推荐' },
  { path: '/compare/past-works', label: '往届' },
  { path: '/agent/analyze', label: '选题' },
  { path: '/plans', label: '方案' },
  { path: '/ppt', label: 'PPT' },
  { path: '/video', label: '视频' },
  { path: '/apply/check', label: '申报' },
  { path: '/workbench/projects', label: '工作台' },
]
</script>

<template>
  <nav class="app-nav">
    <span class="brand">竞赛项目助手</span>
    <div class="links">
      <RouterLink
        v-for="item in items"
        :key="item.path"
        :to="item.path"
        :class="{ active: route.path === item.path || (item.path !== '/' && route.path.startsWith(item.path)) }"
      >
        {{ item.label }}
      </RouterLink>
    </div>
  </nav>
</template>

<style scoped>
.app-nav {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  gap: 24px;
  align-items: center;
  width: 100%;
  padding: 11px max(20px, calc((100vw - 1320px) / 2));
  border-bottom: 1px solid rgba(28, 64, 53, 0.1);
  background: rgba(247, 245, 238, 0.9);
  backdrop-filter: blur(16px);
}

.brand {
  flex: none;
  color: #173d33;
  font-family: "STSong", "SimSun", serif;
  font-weight: 700;
}

.links {
  display: flex;
  gap: 4px;
  overflow-x: auto;
}

.links a {
  flex: none;
  padding: 7px 10px;
  color: #677970;
  font-size: 13px;
  text-decoration: none;
  border-radius: 7px;
}

.links a:hover,
.links a.active {
  color: #fff;
  background: #234f42;
}
</style>

