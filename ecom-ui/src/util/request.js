import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一请求实例：/api 由 devServer 代理到后端 8888（重写去掉 /api 前缀）
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截：附加 token（sessionStorage 的 authorization 字段）
request.interceptors.request.use(config => {
  const token = window.sessionStorage.getItem('authorization')
  if (token) {
    config.headers.authorization = token
  }
  return config
})

// 响应拦截：统一处理 ResponseResult；401 清理登录态并回登录页
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      handleUnauthorized()
      return Promise.reject(new Error(res.msg || '登录已失效'))
    }
    // silentError：调用方自行处理提示（如用对话框），只透传错误不弹 toast
    if (!response.config || !response.config.silentError) {
      ElMessage.error(res.msg || '操作失败')
    }
    return Promise.reject(new Error(res.msg || '操作失败'))
  },
  error => {
    if (error.response && error.response.status === 401) {
      handleUnauthorized()
    } else if (!error.config || !error.config.silentError) {
      // 403/404 等后端会返回 {code,msg}，优先用它，避免只看到 axios 的英文原文
      const body = error.response && error.response.data
      const msg = body && typeof body === 'object' && body.msg
        ? body.msg
        : (error.response && error.response.status === 403 ? '没有访问该功能的权限' : error.message)
      ElMessage.error(msg || '网络异常')
    }
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  window.sessionStorage.clear()
  ElMessage.error('登录已失效，请重新登录')
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

export default request
