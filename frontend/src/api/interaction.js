// 互动（收藏/点赞/评论）API
import request from '@/utils/request'

// targetType: 1-非遗项目 2-资讯 3-活动 4-传承人
// type: 1-收藏 2-点赞 3-评论
// 后端 favorite/like/state 均为 query 参数
export function favorite(targetType, targetId) {
  return request.post('/interaction/favorite', null, { params: { targetType, targetId } })
}

export function like(targetType, targetId) {
  return request.post('/interaction/like', null, { params: { targetType, targetId } })
}

export function comment(data) {
  return request.post('/interaction/comment', data)
}

export function getInteractionState(targetType, targetId) {
  return request.get('/interaction/state', { params: { targetType, targetId } })
}

export function getComments(params) {
  return request.get('/interaction/comments', { params })
}

export function deleteInteraction(id) {
  return request.delete(`/interaction/${id}`)
}

export function getMyFavorites(params) {
  return request.get('/interaction/my/favorites', { params })
}

// 管理端：评论状态（query 参数 status）
export function setInteractionStatus(id, status) {
  return request.put(`/interaction/${id}/status`, null, { params: { status } })
}
