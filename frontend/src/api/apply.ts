import http from './http'

export interface ApplyFile {
  name: string
  type: string
  url: string
}

export interface RequiredItem {
  item: string
  required: boolean
  source?: string
  sourceDate?: string
}

export interface CheckItemResult {
  item: string
  status: 'OK' | 'MISSING' | 'MISMATCH'
  fileName?: string
  advice: string
  source?: string
  sourceDate?: string
}

export interface ApplyCheckResponse {
  id: number
  competitionId: number
  required: RequiredItem[]
  result: CheckItemResult[]
  createdAt: string
}

export async function uploadApplyFile(file: File) {
  const data = new FormData()
  data.append('file', file)
  const response = await http.post('/apply/files', data, { timeout: 120_000 })
  return response.data.data as ApplyFile
}

export async function checkApplication(competitionId: number, files: ApplyFile[]) {
  const response = await http.post('/apply/check', { competitionId, files })
  return response.data.data as ApplyCheckResponse
}

