<template>
  <AppLayout>
    <div class="exam-detail" v-loading="loading">
      <!-- 头部信息 -->
      <div class="header-bar">
        <el-button @click="$router.back()">返回</el-button>
        <h2 style="margin:0">{{ exam.examName }}</h2>
        <el-tag :type="statusTag(exam.status)" size="large">{{ statusLabel(exam.status) }}</el-tag>
        <div class="spacer" />
        <!-- 状态操作按钮 -->
        <el-button v-if="exam.status === 'PENDING'" type="success" @click="handleStart">开始考试</el-button>
        <el-button v-if="exam.status === 'ONGOING'" type="danger" @click="handleEnd">结束考试</el-button>
      </div>

      <!-- 基本信息 -->
      <el-descriptions :column="3" border class="info-card">
        <el-descriptions-item label="试卷">{{ exam.paperName }}</el-descriptions-item>
        <el-descriptions-item label="考场">{{ exam.roomName }}</el-descriptions-item>
        <el-descriptions-item label="负责教师">{{ exam.teacherName }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatTime(exam.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="考试时长">{{ exam.durationMinutes }} 分钟</el-descriptions-item>
      </el-descriptions>

      <!-- 限时提示（仅进行中显示倒计时） -->
      <el-alert v-if="exam.status === 'ONGOING' && countdown > 0"
        :title="`考试剩余时间：${countdownStr}`"
        type="warning" :closable="false" class="countdown-alert"
      />
      <el-alert v-if="exam.status === 'ONGOING' && countdown <= 0"
        title="考试时间已到，请及时结束考试"
        type="error" :closable="false" class="countdown-alert"
      />

      <!-- 试卷题目预览 -->
      <el-card shadow="never" class="paper-card">
        <template #header>试卷题目（共 {{ exam.questions?.length || 0 }} 题）</template>
        <div v-for="(q, idx) in exam.questions" :key="q.id" class="paper-question">
          <div class="q-header">
            <span class="q-order">第{{ idx + 1 }}题</span>
            <el-tag size="small">{{ typeLabel(q.questionType) }}</el-tag>
            <el-tag size="small" :type="diffTag(q.questionDifficulty)" effect="plain">{{ diffLabel(q.questionDifficulty) }}</el-tag>
            <span class="q-score">{{ q.questionScore }} 分</span>
          </div>
          <div class="q-title">{{ q.questionTitle }}</div>
        </div>
      </el-card>

      <!-- 模拟发卷区（进行中时展示考生作答表单） -->
      <el-card v-if="exam.status === 'ONGOING'" shadow="never" class="answer-card">
        <template #header>
          <span>考生作答</span>
          <span style="font-size:12px;color:#909399;margin-left:8px">（本区域模拟考场终端，填写后点击提交答卷）</span>
        </template>
        <el-form :model="answerForm" label-width="80px">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="姓名">
                <el-input v-model="answerForm.studentName" placeholder="考生姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="学号">
                <el-input v-model="answerForm.studentNo" placeholder="考生学号" />
              </el-form-item>
            </el-col>
          </el-row>

          <div v-for="(q, idx) in exam.questions" :key="q.questionId" class="answer-block">
            <div class="q-header">
              <span class="q-order">第{{ idx + 1 }}题</span>
              <el-tag size="small">{{ typeLabel(q.questionType) }}</el-tag>
              <span class="q-score">{{ q.questionScore }} 分</span>
            </div>
            <div class="q-title">{{ q.questionTitle }}</div>

            <!-- 单选 -->
            <el-radio-group v-if="q.questionType === 'SINGLE_CHOICE'" v-model="answerMap[q.questionId]">
              <el-radio v-for="opt in parseOptions(q.questionId)" :key="opt.label" :value="opt.label" style="display:block;margin:4px 0">
                {{ opt.label }}. {{ opt.text }}
              </el-radio>
            </el-radio-group>

            <!-- 多选 -->
            <el-checkbox-group v-else-if="q.questionType === 'MULTIPLE_CHOICE'" v-model="multiAnswerMap[q.questionId]">
              <el-checkbox v-for="opt in parseOptions(q.questionId)" :key="opt.label" :value="opt.label" style="display:block;margin:4px 0">
                {{ opt.label }}. {{ opt.text }}
              </el-checkbox>
            </el-checkbox-group>

            <!-- 填空 -->
            <div v-else-if="q.questionType === 'FILL_BLANK'" class="blanks-wrap">
              <div v-for="(blank, bi) in parseBlanks(q.questionId)" :key="bi" class="blank-row">
                <span>第{{ bi + 1 }}空：</span>
                <el-input v-model="blankAnswerMap[`${q.questionId}_${bi}`]" placeholder="请填写答案" style="width:300px" />
              </div>
            </div>

            <!-- 主观 -->
            <div v-else-if="q.questionType === 'SUBJECTIVE'" class="subjective-wrap">
              <el-input
                v-model="answerMap[q.questionId]"
                type="textarea"
                :rows="3"
                placeholder="请作答（文字）"
              />
              <div v-if="needsImageAnswer(q.questionId)" style="margin-top:8px">
                <span style="font-size:12px;color:#e6a23c">该题要求贴图作答</span>
                <el-upload
                  action="/api/files/upload"
                  :limit="1"
                  accept="image/*"
                  :on-success="(res) => handleImageUpload(q.questionId, res)"
                  :show-file-list="false"
                  style="display:inline-block;margin-left:12px"
                >
                  <el-button size="small" type="primary">上传图片</el-button>
                </el-upload>
                <el-image
                  v-if="imageAnswerMap[q.questionId]"
                  :src="`/api/files/${imageAnswerMap[q.questionId]}`"
                  style="width:120px;height:80px;margin-left:8px;vertical-align:middle"
                  fit="contain"
                />
              </div>
            </div>
          </div>
        </el-form>
        <div style="margin-top:16px;text-align:center">
          <el-button type="primary" size="large" @click="handleSubmitAnswer" :loading="submitting">提交答卷</el-button>
        </div>
      </el-card>

      <!-- 考生成绩列表（评分中/已完成显示） -->
      <el-card v-if="exam.status === 'GRADING' || exam.status === 'FINISHED'" shadow="never" class="result-card">
        <template #header>
          <span>考生成绩</span>
          <el-button v-if="exam.status === 'GRADING'" type="success" size="small" style="float:right;margin-top:-4px" @click="handleFinishGrading">完成评分</el-button>
        </template>
        <el-table :data="results" stripe>
          <el-table-column prop="studentNo" label="学号" width="130" />
          <el-table-column prop="studentName" label="姓名" width="120" />
          <el-table-column prop="totalScore" label="总分" width="90" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'GRADED' ? 'success' : 'warning'" size="small">
                {{ row.status === 'GRADED' ? '已评分' : '待评分' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="$router.push(`/exam/grading/${exam.id}?studentNo=${row.studentNo}`)">
                {{ exam.status === 'FINISHED' ? '查看详情' : '去评分' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTeacherStore } from '../../stores/teacher'
import AppLayout from '../../components/AppLayout.vue'
import { getExam, startExam, endExam, submitAnswer, getExamResults, finishGrading } from '../../api/exam'

const route = useRoute()
const router = useRouter()
const store = useTeacherStore()

const exam = ref({})
const loading = ref(false)
const results = ref([])
const submitting = ref(false)

// 答题相关
const answerForm = ref({ studentName: '', studentNo: '' })
const answerMap = ref({})        // 单选/主观/文字
const multiAnswerMap = ref({})   // 多选 checkbox-group -> string[]
const blankAnswerMap = ref({})   // 填空 {questionId_blankIndex: answer}
const imageAnswerMap = ref({})   // 主观贴图 {questionId: path}

// 题目 contentJson 缓存
const contentCache = ref({})

// 倒计时
const countdown = ref(0)
let timer = null

onMounted(() => {
  if (!store.isLoggedIn) { router.push('/'); return }
  loadExam()
})

onUnmounted(() => { clearInterval(timer) })

function loadExam() {
  loading.value = true
  getExam(route.params.id).then(res => {
    exam.value = res.data
    // 预加载题目内容
    if (exam.value.questions) {
      exam.value.questions.forEach(q => {
        // 通过题目列表中含 contentJson 字段（如后端返回）或单独请求
        // 此处假设后端 ExamVO.questions 包含 contentJson
        if (q.contentJson) contentCache.value[q.questionId] = JSON.parse(q.contentJson)
      })
    }
    initCountdown()
    if (exam.value.status === 'GRADING' || exam.value.status === 'FINISHED') {
      loadResults()
    }
  }).finally(() => { loading.value = false })
}

function initCountdown() {
  if (exam.value.status !== 'ONGOING') return
  const start = new Date(String(exam.value.startTime).replace('T', ' ')).getTime()
  const endMs = start + exam.value.durationMinutes * 60 * 1000
  const updateCountdown = () => {
    countdown.value = Math.max(0, Math.round((endMs - Date.now()) / 1000))
  }
  updateCountdown()
  timer = setInterval(updateCountdown, 1000)
}

const countdownStr = computed(() => {
  const s = countdown.value
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  return `${h ? h + '时' : ''}${m}分${sec}秒`
})

function loadResults() {
  getExamResults(exam.value.id).then(res => { results.value = res.data })
}

function handleStart() {
  ElMessageBox.confirm('确定开始考试？开始后考生可作答。', '确认').then(() => {
    startExam(exam.value.id).then(() => { ElMessage.success('考试已开始'); loadExam() })
  })
}

function handleEnd() {
  ElMessageBox.confirm('确定结束考试？结束后考生无法继续作答。', '确认', { type: 'warning' }).then(() => {
    endExam(exam.value.id).then(() => {
      ElMessage.success('考试已结束，进入评分阶段')
      clearInterval(timer)
      loadExam()
    })
  })
}

function handleFinishGrading() {
  ElMessageBox.confirm('确定完成所有评分并公布成绩？', '确认').then(() => {
    finishGrading(exam.value.id).then(() => { ElMessage.success('评分完成'); loadExam() })
  })
}

function handleSubmitAnswer() {
  if (!answerForm.value.studentName.trim()) { ElMessage.warning('请输入考生姓名'); return }
  if (!answerForm.value.studentNo.trim()) { ElMessage.warning('请输入考生学号'); return }

  const answers = (exam.value.questions || []).map(q => {
    let content = ''
    if (q.questionType === 'SINGLE_CHOICE') {
      content = answerMap.value[q.questionId] || ''
    } else if (q.questionType === 'MULTIPLE_CHOICE') {
      content = (multiAnswerMap.value[q.questionId] || []).join(',')
    } else if (q.questionType === 'FILL_BLANK') {
      const blanks = parseBlanks(q.questionId)
      content = blanks.map((_, bi) => blankAnswerMap.value[`${q.questionId}_${bi}`] || '').join(',')
    } else {
      content = answerMap.value[q.questionId] || ''
    }
    return {
      questionId: q.questionId,
      answerContent: content,
      answerImagePath: imageAnswerMap.value[q.questionId] || null
    }
  })

  submitting.value = true
  submitAnswer({
    examId: exam.value.id,
    studentName: answerForm.value.studentName,
    studentNo: answerForm.value.studentNo,
    answers
  }).then(() => {
    ElMessage.success('答卷提交成功')
    answerForm.value = { studentName: '', studentNo: '' }
    answerMap.value = {}
    multiAnswerMap.value = {}
    blankAnswerMap.value = {}
    imageAnswerMap.value = {}
  }).finally(() => { submitting.value = false })
}

function handleImageUpload(questionId, res) {
  if (res && res.filePath) {
    imageAnswerMap.value[questionId] = res.filePath
    ElMessage.success('图片上传成功')
  }
}

// 解析题目选项（依赖后端在 PaperQuestionDetail 中返回 contentJson）
function parseOptions(questionId) {
  const c = contentCache.value[questionId]
  return c?.options || []
}

function parseBlanks(questionId) {
  const c = contentCache.value[questionId]
  return c?.blanks || [{}]
}

function needsImageAnswer(questionId) {
  const c = contentCache.value[questionId]
  return c?.answerImageRequired === true
}

function typeLabel(t) {
  return { SINGLE_CHOICE: '单选题', MULTIPLE_CHOICE: '多选题', FILL_BLANK: '填空题', SUBJECTIVE: '主观题' }[t] || t
}
function diffLabel(d) { return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[d] || d }
function diffTag(d) { return { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }[d] || '' }
function statusLabel(s) { return { PENDING: '待开始', ONGOING: '进行中', GRADING: '评分中', FINISHED: '已完成' }[s] || s }
function statusTag(s) { return { PENDING: 'info', ONGOING: 'warning', GRADING: '', FINISHED: 'success' }[s] || '' }
function formatTime(t) { if (!t) return ''; return String(t).replace('T', ' ').substring(0, 19) }
</script>

<style scoped>
.exam-detail { max-width: 1100px; margin: 0 auto; }
.header-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.spacer { flex: 1; }
.info-card { margin-bottom: 16px; }
.countdown-alert { margin-bottom: 16px; }
.paper-card, .answer-card, .result-card { margin-bottom: 16px; }
.paper-question, .answer-block { padding: 12px 0; border-bottom: 1px solid #ebeef5; }
.paper-question:last-child, .answer-block:last-child { border-bottom: none; }
.q-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.q-order { font-weight: bold; color: #409EFF; }
.q-score { margin-left: auto; color: #e6a23c; font-weight: bold; }
.q-title { font-size: 15px; margin-bottom: 8px; }
.blanks-wrap { display: flex; flex-direction: column; gap: 8px; }
.blank-row { display: flex; align-items: center; gap: 8px; }
.subjective-wrap { margin-top: 4px; }
</style>
