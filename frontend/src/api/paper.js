import request from './request'

// 手动组卷 API
export function createPaper(data) {
  return request.post('/papers', data)
}

export function updatePaper(id, data) {
  return request.put(`/papers/${id}`, data)
}

export function deletePaper(id) {
  return request.delete(`/papers/${id}`)
}

export function getPaper(id) {
  return request.get(`/papers/${id}`)
}

export function getPapers(params) {
  return request.get('/papers', { params })
}

export function validatePaper(id) {
  return request.post(`/papers/${id}/validate`)
}

export function submitPaper(id) {
  return request.post(`/papers/${id}/submit`)
}

// 自动组卷 API
export function autoGeneratePaper(data) {
  return request.post('/papers/auto-generate', data)
}
