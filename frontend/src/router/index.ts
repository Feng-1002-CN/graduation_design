import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/Home.vue'),
    },
    {
      path: '/competitions',
      name: 'competitions',
      component: () => import('@/views/competition/CompetitionList.vue'),
    },
    {
      path: '/knowledge',
      name: 'knowledge',
      component: () => import('@/views/knowledge/KnowledgeBase.vue'),
    },
    {
      path: '/rules/qa',
      name: 'rule-qa',
      component: () => import('@/views/rule/RuleQa.vue'),
    },
    {
      path: '/search/recommend',
      name: 'recommend',
      component: () => import('@/views/search/Recommend.vue'),
    },
    {
      path: '/compare/past-works',
      name: 'past-works',
      component: () => import('@/views/compare/CompareWorks.vue'),
    },
    {
      path: '/agent/analyze',
      name: 'agent-analyze',
      component: () => import('@/views/agent/Analyze.vue'),
    },
    {
      path: '/plans',
      name: 'plans',
      component: () => import('@/views/plan/PlanView.vue'),
    },
    {
      path: '/ppt',
      name: 'ppt',
      component: () => import('@/views/ppt/PptGenerate.vue'),
    },
    {
      path: '/video',
      name: 'video',
      component: () => import('@/views/video/VideoGenerate.vue'),
    },
    {
      path: '/apply/check',
      name: 'apply-check',
      component: () => import('@/views/apply/ApplyCheck.vue'),
    },
    {
      path: '/workbench/projects',
      name: 'workbench-projects',
      component: () => import('@/views/workbench/ProjectList.vue'),
    },
    {
      path: '/workbench/projects/:projectId',
      name: 'workbench-project',
      component: () => import('@/views/workbench/Workbench.vue'),
    },
  ],
})

export default router

