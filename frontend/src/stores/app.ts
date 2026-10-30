import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const backendStatus = ref<'UP' | 'DOWN' | 'CHECKING'>('CHECKING')
  return { backendStatus }
})

