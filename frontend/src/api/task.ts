import http from './http'
import type { ProjectPlan } from './plan'

export interface GenerationTask {
  taskId: number
  projectId: number
  taskType: 'PPT' | 'VIDEO'
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'
  stage?: string
  progress: number
  fileUrl?: string
  errorMsg?: string
  createdAt: string
  updatedAt: string
}

export async function createPptTask(projectId: number, templateId = 'default') {
  const response = await http.post('/ppt/generate', { projectId, templateId })
  return response.data.data as { taskId: number; status: string }
}

export async function fetchTask(taskId: number) {
  const response = await http.get(`/tasks/${taskId}`)
  return response.data.data as GenerationTask
}

export function pptDownloadUrl(taskId: number) {
  return `/api/v1/ppt/${taskId}/download`
}

export type { ProjectPlan }

