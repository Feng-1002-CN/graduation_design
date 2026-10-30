import http from './http'

export interface ProjectSummary {
  id: number
  title: string
  description?: string
  targetCompetitionId?: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface WorkbenchTask {
  taskId: number
  taskType: 'PPT' | 'VIDEO'
  status: string
  stage?: string
  progress: number
  fileUrl?: string
  errorMsg?: string
  updatedAt: string
}

export interface TraceItem {
  id: number
  step: number
  toolName: string
  summary: string
  startedAt: string
  endedAt?: string
}

export interface TimelineEvent {
  time: string
  type: string
  title: string
  status: string
}

export interface Workbench {
  project: ProjectSummary
  latestReport?: {
    reportId: number
    summary: string
    createdAt: string
  }
  plan?: {
    id: number
    title: string
    modules: string
    techRoute: string
    updatedAt: string
  }
  tasks: WorkbenchTask[]
  applyCheck?: {
    id: number
    result: unknown
    createdAt: string
  }
  traces: TraceItem[]
  timeline: TimelineEvent[]
}

export async function fetchProjects(page = 1, size = 20) {
  const response = await http.get('/workbench/projects', { params: { page, size } })
  return response.data.data as {
    records: ProjectSummary[]
    total: number
    page: number
    size: number
  }
}

export async function fetchWorkbench(projectId: number) {
  const response = await http.get(`/workbench/projects/${projectId}`)
  return response.data.data as Workbench
}

export async function fetchWorkbenchReport(projectId: number) {
  const response = await http.get(`/agent/projects/${projectId}/report`)
  return response.data.data as {
    reportId: number
    projectId: number
    content: string
    refs: unknown[]
    createdAt: string
  }
}

export async function fetchWorkbenchPlan(projectId: number) {
  const response = await http.get(`/plans/${projectId}`)
  return response.data.data
}

