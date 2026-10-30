import http from './http'

export interface PlanModule {
  name: string
  description: string
}

export interface ProjectPlan {
  id: number
  projectId: number
  title: string
  background: string
  requirements: string
  modules: PlanModule[]
  techRoute: string
  innovation: string
  schedule: string
  createdAt: string
  updatedAt: string
}

export async function generatePlan(projectId: number) {
  const response = await http.post('/plans/generate', { projectId })
  return response.data.data as ProjectPlan
}

export async function fetchPlan(projectId: number) {
  const response = await http.get(`/plans/${projectId}`)
  return response.data.data as ProjectPlan
}

export async function updatePlan(plan: ProjectPlan) {
  const response = await http.put(`/plans/${plan.id}`, plan)
  return response.data.data as ProjectPlan
}

