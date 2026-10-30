import http from './http'

export interface HealthData {
  status: string
  timestamp: string
}

export async function getHealth() {
  const response = await http.get('/health')
  return response.data.data as HealthData
}

