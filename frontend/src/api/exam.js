import request from './request'

// -------- 考场 --------
export function getExamRooms() {
  return request.get('/exam-rooms')
}
export function createExamRoom(data) {
  return request.post('/exam-rooms', data)
}
export function updateExamRoom(id, data) {
  return request.put(`/exam-rooms/${id}`, data)
}
export function deleteExamRoom(id) {
  return request.delete(`/exam-rooms/${id}`)
}

// -------- 考试 --------
export function getExams(params) {
  return request.get('/exams', { params })
}
export function getExam(id) {
  return request.get(`/exams/${id}`)
}
export function createExam(data) {
  return request.post('/exams', data)
}
export function updateExam(id, data) {
  return request.put(`/exams/${id}`, data)
}
export function deleteExam(id) {
  return request.delete(`/exams/${id}`)
}

// 状态流转
export function startExam(id) {
  return request.post(`/exams/${id}/start`)
}
export function endExam(id) {
  return request.post(`/exams/${id}/end`)
}
export function finishGrading(id) {
  return request.post(`/exams/${id}/finish-grading`)
}

// -------- 考生作答 --------
export function submitAnswer(data) {
  return request.post('/exams/submit', data)
}
export function getExamResults(examId) {
  return request.get(`/exams/${examId}/results`)
}
export function getStudentResult(examId, studentNo) {
  return request.get(`/exams/${examId}/results/${studentNo}`)
}

// -------- 教师评分 --------
export function gradeSubjective(data) {
  return request.post('/exams/grade', data)
}
export function calcTotalScore(examId, studentNo) {
  return request.post(`/exams/${examId}/results/${studentNo}/calc-total`)
}
