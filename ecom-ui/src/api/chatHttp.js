import request from '@/util/request'

// 提问（置信度 ≥ 阈值自动回复，否则转人工）
function ask(question) {
  return request.post('/chat/ask', { question })
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
