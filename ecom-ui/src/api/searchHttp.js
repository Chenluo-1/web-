import request from '@/util/request'

// 搜索（同义词扩展 + 容错模糊 + 无结果推荐）
function search(data) {
  return request.get('/search', { params: data })
}

// 热词榜
function hotWords(limit) {
  return request.get('/search/hotWords', { params: { limit: limit || 10 } })
}

// 联想词
function suggest(prefix) {
  return request.get('/search/suggest', { params: { prefix } })
}

export default { search, hotWords, suggest }
