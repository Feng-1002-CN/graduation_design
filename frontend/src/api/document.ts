import http from './http'

export interface KnowledgeDocument {
  id: number
  competitionId?: number
  title: string
  fileType: string
  fileUrl: string
  sourceUrl?: string
  sourceDate?: string
  status: 'PENDING' | 'PROCESSING' | 'READY' | 'FAILED'
  chunkCount: number
  errorMsg?: string
  documentType: string
  workYear?: string
  createdAt: string
}

export interface SearchHit {
  documentId: number
  chunkIndex: number
  pageNumber?: number
  content: string
  source: string
  sourceDate?: string
  score: number
  documentType: string
  workYear?: string
}

export interface DocumentFilters {
  page: number
  size: number
  competitionId?: number
  status?: string
}

export async function fetchDocuments(filters: DocumentFilters) {
  const response = await http.get('/documents', { params: filters })
  return response.data.data as {
    records: KnowledgeDocument[]
    total: number
    page: number
    size: number
  }
}

export async function uploadDocument(formData: FormData) {
  const response = await http.post('/documents/upload', formData, {
    timeout: 120_000,
  })
  return response.data.data as { documentId: number; title: string; status: string }
}

export async function searchKnowledge(payload: {
  query: string
  topK: number
  competitionId?: number
  documentType?: string
}) {
  const response = await http.post('/documents/search', payload)
  return response.data.data.hits as SearchHit[]
}

export async function deleteDocument(id: number) {
  await http.delete(`/documents/${id}`)
}

