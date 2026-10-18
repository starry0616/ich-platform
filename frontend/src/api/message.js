// 消息通知 API
import request from '@/utils/request'

export function getMessageList(params) {
  return request.get('/message/list', { params })
}

export function getUnreadCount() {
  return request.get('/message/unread-count')
}

export function markRead(id) {
  return request.put(`/message/${id}/read`)
}

export function markAllRead() {
  return request.put('/message/read-all')
}
