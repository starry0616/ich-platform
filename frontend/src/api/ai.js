// 智能问答 API
import request from '@/utils/request'

export function chat(data) {
  return request.post('/ai/chat', data)
}

export function getChatHistory(params) {
  return request.get('/ai/chat/history', { params })
}

export function postFeedback(id, data) {
  return request.post(`/ai/chat/${id}/feedback`, data)
}

export function getChatRefs(id) {
  return request.get(`/ai/chat/${id}/refs`)
}
