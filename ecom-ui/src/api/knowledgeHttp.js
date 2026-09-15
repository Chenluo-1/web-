import request from '@/util/request'

// 知识库分页
function selectKnowledgeByPage(data) {
  return request.get('/knowledge', { params: data })
}

function addKnowledge(data) {
  return request.post('/knowledge', data)
}

function updateKnowledge(data) {
  return request.put('/knowledge', data)
}

function delKnowledge(ids) {
  return request.delete('/knowledge/' + ids.toString())
}

export default { selectKnowledgeByPage, addKnowledge, updateKnowledge, delKnowledge }
