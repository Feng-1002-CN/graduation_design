import axios from 'axios'
import { ElMessage } from 'element-plus'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 30_000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>
    if (payload.code !== 0) {
      ElMessage.error(payload.message || '请求处理失败')
      return Promise.reject(new Error(payload.message))
    }
    return response
  },
  (error) => {
    const message = error.response?.data?.message ?? error.message ?? '网络连接失败'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default http

