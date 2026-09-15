import request from '@/util/request'

// 提问（规则命中自动回复 → 大模型兜底 → 转人工）
// 大模型推理耗时较长，单独放宽超时到 60 秒
function ask(question) {
  return request.post('/chat/ask', { question }, { timeout: 60000 })
}

// 会话记录
function selectLogsByPage(data) {
  return request.get('/chat/logs', { params: data })
}

// 待补充问题
function selectPending() {
  return request.get('/chat/pending')
}

export default { ask, selectLogsByPage, selectPending }
