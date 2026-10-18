// 媒体上传 API
import request from '@/utils/request'

// 上传图片/视频/文档，返回访问 URL
export function uploadMedia(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/media/upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getMediaList(params) {
  return request.get('/media/list', { params })
}

export function deleteMedia(id) {
  return request.delete(`/media/${id}`)
}
