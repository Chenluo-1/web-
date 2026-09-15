import axios from 'axios'

// 统一请求实例：/api 由 devServer 代理到后端 8888（重写去掉 /api 前缀）
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => response.data,
  error => Promise.reject(error)
)

export default request
