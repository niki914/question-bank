import request from './request'

export function autoGeneratePaper(data) {
  return request.post('/papers/auto-generate', data)
}
