import request from '@/util/request'

// 行为上报（曝光/点击；游客也可上报）
function report(data) {
  return request.post('/behavior/report', data)
}

export default { report }
