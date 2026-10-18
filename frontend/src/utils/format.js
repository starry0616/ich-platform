// 通用格式化工具
export function resolveUrl(url) {
  if (!url) return ''
  // 已是完整 URL 或相对 /uploads 路径直接返回
  if (url.startsWith('http') || url.startsWith('/')) return url
  // 纯文件名 -> 上传目录
  return `/uploads/${url}`
}

export function formatDate(t) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return String(t)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function formatDateShort(t) {
  if (!t) return ''
  return formatDate(t).split(' ')[0]
}

export function truncate(str, len) {
  if (!str) return ''
  return str.length > len ? str.slice(0, len) + '…' : str
}
