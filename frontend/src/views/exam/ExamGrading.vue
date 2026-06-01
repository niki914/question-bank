<template>
  <AppLayout>
    <div class="grading-page" v-loading="loading">
      <div class="header-bar">
        <el-button @click="$router.back()">返回</el-button>
        <h2 style="margin:0">{{ exam.examName }} — 教师评分</h2>
        <el-tag :type="exam.status === 'FINISHED' ? 'success' : ''" size="large">
          {{ exam.status === 'FINISHED' ? '已完成' : '评分中' }}
        </el-tag>
      </div>

      <!-- 考生选择 -->
      <el-card shadow="never" class="student-selector">
        <template #header>考生列表</template>
        <el-table
          :data="results"
          stripe
          highlight-current-row
          @current-change="selectStudent"
          style="width:100%"
        >
          <el-table-column prop="studentNo" label="学号" width="140" />
          <el-table-column prop="studentName" label="姓名" width="120" />
          <el-table-column prop="totalScore" label="总分" width="90" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'GRADED' ? 'success' : 'warning'" size="small">
                {{ row.status === 'GRADED' ? '已评分' : '待评分' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button
                v-if="exam.status === 'GRADING'"
                link type="success" size="small"
                @click="calcAndRefresh(row.studentNo)"
              >汇总总分</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 选中考生的答题详情 -->
      <el-card v-if="currentResult" shadow="never" class="answer-detail">
        <template #header>
          {{ currentResult.studentName }}（{{ currentResult.studentNo }}）的答题详情
        </template>

        <div v-for="(ans, idx) in currentResult.answers" :key="ans.id" class="answer-row">
          <div class="ans-header">
            <span class="q-order">第{{ idx + 1 }}题</span>
            <el-tag size="small">{{ typeLabel(ans.questionType) }}</el-tag>
            <span class="q-title-text">{{ ans.questionTitle }}</span>
            <span class="q-score">满分：{{ ans.questionScore }}</span>
          </div>

          <!-- 考生作答 -->
          <div class="student-answer">
            <span class="label">考生作答：</span>
            <span v-if="ans.questionType !== 'SUBJECTIVE'" class="answer-text">{{ ans.answerContent || '（未作答）' }}</span>
            <div v-else>
              <p class="answer-text">{{ ans.answerContent || '（未填写文字作答）' }}</p>
              <el-image
                v-if="ans.answerImagePath"
                :src="`/api/files/${ans.answerImagePath}`"
                :preview-src-list="[`/api/files/${ans.answerImagePath}`]"
                style="max-width:300px;max-height:200px;border-radius:4px"
                fit="contain"
              />
            </div>
          </div>

          <!-- 得分显示 -->
          <div class="score-row">
            <template v-if="ans.questionType !== 'SUBJECTIVE'">
              <span class="label">自动得分：</span>
              <el-tag :type="scoreTag(ans.autoScore, ans.questionScore)" size="small">
                {{ ans.autoScore ?? '—' }} 分
              </el-tag>
            </template>
            <template v-else>
              <span class="label">教师评分：</span>
              <template v-if="exam.status === 'GRADING'">
                <el-input-number
                  v-model="gradeInputs[ans.id]"
                  :min="0"
                  :max="Number(ans.questionScore)"
                  :step="0.5"
                  style="width:140px"
                />
                <el-button
                  type="primary" size="small" style="margin-left:8px"
                  @click="handleGrade(ans.id)"
                  :loading="gradingId === ans.id"
                >提交评分</el-button>
              </template>
              <el-tag v-else :type="scoreTag(ans.teacherScore, ans.questionScore)" size="small">
                {{ ans.teacherScore ?? '—' }} 分
              </el-tag>
            </template>
            <span v-if="ans.finalScore !== null && ans.finalScore !== undefined" class="final-score">
              最终得分：<strong>{{ ans.finalScore }}</strong>
            </span>
          </div>
        </div>
      </el-card>

      <el-empty v-else description="请从左侧选择一位考生" />
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTeacherStore } from '../../stores/teacher'
import AppLayout from '../../components/AppLayout.vue'
import { getExam, getExamResults, gradeSubjective, calcTotalScore } from '../../api/exam'

const route = useRoute()
const router = useRouter()
const store = useTeacherStore()

const exam = ref({})
const results = ref([])
const currentResult = ref(null)
const loading = ref(false)
const gradeInputs = ref({})   // answerId -> score
const gradingId = ref(null)

onMounted(() => {
  if (!store.isLoggedIn) { router.push('/'); return }
  loadData()
  // 若 URL 带 studentNo 参数，自动定位到该考生
})

function loadData() {
  loading.value = true
  Promise.all([
    getExam(route.params.examId),
    getExamResults(route.params.examId)
  ]).then(([examRes, resRes]) => {
    exam.value = examRes.data
    results.value = resRes.data
    // 自动选中 URL 中的 studentNo
    const sNo = route.query.studentNo
    if (sNo) {
      const found = results.value.find(r => r.studentNo === sNo)
      if (found) selectStudent(found)
    }
  }).finally(() => { loading.value = false })
}

function selectStudent(row) {
  if (!row) return
  currentResult.value = row
  // 初始化评分输入框
  row.answers?.forEach(ans => {
    if (gradeInputs.value[ans.id] === undefined) {
      gradeInputs.value[ans.id] = ans.teacherScore ?? 0
    }
  })
}

function handleGrade(answerId) {
  const score = gradeInputs.value[answerId]
  if (score === undefined || score === null) { ElMessage.warning('请输入分值'); return }
  gradingId.value = answerId
  gradeSubjective({
    answerId,
    score,
    gradedBy: store.currentTeacher.id
  }).then(res => {
    ElMessage.success('评分成功')
    // 更新当前答案的评分显示
    const ans = currentResult.value?.answers?.find(a => a.id === answerId)
    if (ans) {
      ans.teacherScore = res.data.teacherScore
      ans.finalScore = res.data.finalScore
    }
  }).finally(() => { gradingId.value = null })
}

function calcAndRefresh(studentNo) {
  calcTotalScore(exam.value.id, studentNo).then(res => {
    ElMessage.success(`总分已计算：${res.data.totalScore} 分`)
    // 刷新列表
    const idx = results.value.findIndex(r => r.studentNo === studentNo)
    if (idx >= 0) results.value[idx] = res.data
    if (currentResult.value?.studentNo === studentNo) {
      currentResult.value = res.data
    }
  })
}

function typeLabel(t) {
  return { SINGLE_CHOICE: '单选题', MULTIPLE_CHOICE: '多选题', FILL_BLANK: '填空题', SUBJECTIVE: '主观题' }[t] || t
}
function scoreTag(score, full) {
  if (score === null || score === undefined) return 'info'
  if (Number(score) >= Number(full)) return 'success'
  if (Number(score) > 0) return 'warning'
  return 'danger'
}
</script>

<style scoped>
.grading-page { max-width: 1200px; margin: 0 auto; }
.header-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.student-selector { margin-bottom: 16px; }
.answer-detail { margin-bottom: 16px; }
.answer-row { padding: 14px 0; border-bottom: 1px solid #ebeef5; }
.answer-row:last-child { border-bottom: none; }
.ans-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.q-order { font-weight: bold; color: #409EFF; }
.q-title-text { flex: 1; font-size: 14px; color: #303133; }
.q-score { color: #909399; font-size: 13px; }
.student-answer { margin-bottom: 8px; }
.label { font-size: 13px; color: #606266; margin-right: 8px; }
.answer-text { color: #303133; font-size: 14px; }
.score-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.final-score { margin-left: 12px; color: #e6a23c; font-size: 14px; }
</style>
