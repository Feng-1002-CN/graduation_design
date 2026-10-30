import http from './http'

export interface Reference {
  index: number
  documentId: number
  chunkIndex: number
  source: string
  pageNumber?: number
  sourceDate?: string
  score: number
}

export interface QaResponse {
  answer: string
  references: Reference[]
  fallback: boolean
}

export async function askRules(payload: {
  question: string
  competitionId?: number
  topK: number
}) {
  const response = await http.post('/rules/qa', payload)
  return response.data.data as QaResponse
}

