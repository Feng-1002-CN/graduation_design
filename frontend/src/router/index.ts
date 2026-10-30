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
  ],
})

export default router

