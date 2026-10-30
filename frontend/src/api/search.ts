import http from './http'

export interface Recommendation {
  competitionId: number
  name: string
  level?: string
  reason: string
  matchedKeywords: string[]
  source: string
  sourceDate?: string
  score: number
}

export async function recommendCompetitions(payload: {
  grade?: string
  direction?: string
  keywords: string[]
  limit: number
}) {
  const response = await http.post('/search/recommend', payload)
  return response.data.data.recommendations as Recommendation[]
}

