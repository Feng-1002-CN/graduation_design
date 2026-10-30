import http from './http'

export interface CompetitionGroup {
  id?: number
  name: string
  eligibility?: string
}

export interface CompetitionRule {
  id?: number
  groupId?: number
  groupName?: string
  title: string
  content: string
  sourceUrl: string
  sourceDate: string
  publishDate?: string
  status?: string
}

export interface Competition {
  id: number
  name: string
  organizer?: string
  category?: string
  level?: string
  officialWebsite?: string
  registerStart?: string
  registerEnd?: string
  contestStart?: string
  contestEnd?: string
  description?: string
  sourceUrl: string
  sourceDate: string
  status: string
  createdAt: string
  updatedAt: string
  groups: CompetitionGroup[]
  rules: CompetitionRule[]
}

export interface CompetitionPayload {
  name: string
  organizer?: string
  category?: string
  level?: string
  officialWebsite?: string
  registerStart?: string
  registerEnd?: string
  contestStart?: string
  contestEnd?: string
  description?: string
  sourceUrl: string
  sourceDate: string
  status?: string
  groups: CompetitionGroup[]
  rules: CompetitionRule[]
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface CompetitionFilters {
  page: number
  size: number
  name?: string
  organizer?: string
  category?: string
  level?: string
  status?: string
  dateFrom?: string
  dateTo?: string
}

export async function fetchCompetitions(filters: CompetitionFilters) {
  const response = await http.get('/competitions', { params: filters })
  return response.data.data as PageResult<Competition>
}

export async function fetchCompetition(id: number) {
  const response = await http.get(`/competitions/${id}`)
  return response.data.data as Competition
}

export async function createCompetition(payload: CompetitionPayload) {
  const response = await http.post('/competitions', payload)
  return response.data.data as Competition
}

export async function updateCompetition(id: number, payload: CompetitionPayload) {
  const response = await http.put(`/competitions/${id}`, payload)
  return response.data.data as Competition
}

export async function deleteCompetition(id: number) {
  await http.delete(`/competitions/${id}`)
}

