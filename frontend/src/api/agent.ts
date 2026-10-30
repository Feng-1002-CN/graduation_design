export interface AgentEvent {
  type: 'step_start' | 'evidence' | 'step_end' | 'delta' | 'done' | 'error'
  step?: number
  tool?: string
  message?: string
  data?: {
    referenceId?: string
    documentId?: number
    source?: string
    pageNumber?: number
    sourceDate?: string
    score?: number
    category?: string
  }
  summary?: string
  content?: string
  reportId?: number
  projectId?: number
}

export interface AgentReport {
  reportId: number
  projectId: number
  content: string
  refs: unknown[]
  createdAt: string
}

export async function fetchAgentReport(projectId: number) {
  const response = await fetch(`/api/v1/agent/projects/${projectId}/report`)
  const payload = await response.json()
  if (payload.code !== 0) throw new Error(payload.message)
  return payload.data as AgentReport
}

export async function analyzeTopic(
  payload: {
    projectId?: number
    title: string
    description: string
    competitionId?: number
  },
  onEvent: (event: AgentEvent) => void,
) {
  const response = await fetch('/api/v1/agent/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify(payload),
  })
  if (!response.ok || !response.body) {
    throw new Error(`分析请求失败：${response.status}`)
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const frames = buffer.split('\n\n')
    buffer = frames.pop() ?? ''
    for (const frame of frames) {
      const data = frame
        .split('\n')
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).trim())
        .join('')
      if (data) onEvent(JSON.parse(data) as AgentEvent)
    }
  }
}

