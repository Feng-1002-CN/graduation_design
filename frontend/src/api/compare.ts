import http from './http'

export interface PastWorkResult {
  documentId: number
  title: string
  workYear?: string
  topicOverlap: string
  techOverlap: string
  overlapDesc: string
  source: string
  sourceDate?: string
  score: number
}

export async function comparePastWorks(payload: {
  topic: string
  competitionId?: number
  topK: number
}) {
  const response = await http.post('/compare/past-works', payload)
  return response.data.data.works as PastWorkResult[]
}

