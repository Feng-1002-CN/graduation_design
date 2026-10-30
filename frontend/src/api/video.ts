import http from './http'
import type { GenerationTask } from './task'

export async function uploadVideoMaterial(file: File) {
  const data = new FormData()
  data.append('file', file)
  const response = await http.post('/video/materials', data, { timeout: 120_000 })
  return response.data.data.fileUrl as string
}

export async function createVideoTask(projectId: number, images: string[]) {
  const response = await http.post('/video/generate', { projectId, images })
  return response.data.data as { taskId: number; status: string }
}

export function videoUrl(taskId: number) {
  return `/api/v1/video/${taskId}/download`
}

export type { GenerationTask }

