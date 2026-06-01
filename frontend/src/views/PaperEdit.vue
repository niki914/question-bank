<template>
  <AppLayout>
    <div class="paper-edit" v-loading="loading">
      <div class="header-bar">
        <el-button @click="$router.back()">返回</el-button>
        <h2>编辑试卷</h2>
      </div>

      <!-- 基本信息 -->
      <el-card class="basic-info" shadow="never">
        <el-form :inline="true" :model="form" label-width="90px">
          <el-form-item label="试卷名称">
            <el-input v-model="form.paperName" placeholder="请输入试卷名称" style="width:220px" />
          </el-form-item>
          <el-form-item label="目标难度">
            <el-select v-model="form.difficulty" style="width:120px">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
          </el-form-item>
          <el-form-item label="总分">
            <el-input-number v-model="form.totalScore" :min="1" :precision="2" :step="5" />
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 双栏 -->
      <div class="two-columns">
        <!-- 左栏 -->
        <el-card class="left-panel" shadow="never">
          <template #header><span>题目选择器</span></template>
          <div class="filters">
            <el-select v-model="qFilters.type" placeholder="题型" clearable style="width:120px" size="small" @change="loadQuestions">
              <el-option label="单选题" value="SINGLE_CHOICE" />
              <el-option label="多选题" value="MULTIPLE_CHOICE" />
              <el-option label="填空题" value="FILL_BLANK" />
              <el-option label="主观题" value="SUBJECTIVE" />
            </el-select>
            <el-select v-model="qFilters.difficulty" placeholder="难度" clearable style="width:100px" size="small" @change="loadQuestions">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
            <el-input v-model="qFilters.keyword" placeholder="搜索题目或知识点" clearable size="small" style="width:180px" @keyup.enter="loadQuestions" @clear="loadQuestions">
              <template #append><el-button size="small" @click="loadQuestions">搜索</el-button></template>
            </el-input>
          </div>

          <el-table :data="availableQuestions" style="width:100%" size="small" v-loading="qLoading" max-height="400" stripe>
            <el-table-column prop="id" label="ID" width="55" />
            <el-table-column prop="title" label="题目" min-width="160" show-overflow-tooltip />
            <el-table-column label="题型" width="80">
              <template #default="{ row }"><el-tag size="small">{{ typeLabel(row.type) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="难度" width="65">
              <template #default="{ row }"><el-tag size="small" :type="diffTag(row.difficulty)" effect="plain">{{ diffLabel(row.difficulty) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="60">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="addQuestion(row)" :disabled="isSelected(row.id)">添加</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-small">
            <el-pagination
              v-model:current-page="qFilters.page"
              :page-size="qFilters.size"
              :total="qTotal"
              layout="prev,pager,next"
              small
              @current-change="loadQuestions"
            />
          </div>
        </el-card>

        <!-- 右栏 -->
        <el-card class="right-panel" shadow="never">
          <template #header>
            <div class="panel-header">
              <span>试卷预览</span>
              <span class="total-score">实际总分: <strong>{{ actualTotalScore }}</strong></span>
            </div>
          </template>

          <el-empty v-if="selectedQuestions.length === 0" description="请从左侧选择题目" />

          <div v-for="(q, index) in selectedQuestions" :key="q._uid" class="paper-question">
            <div class="q-header">
              <el-tag size="small">{{ typeLabel(q.type) }}</el-tag>
              <el-tag size="small" :type="diffTag(q.difficulty)" effect="plain" style="margin-left:6px">{{ diffLabel(q.difficulty) }}</el-tag>
              <span class="q-title">{{ q.title }}</span>
              <div class="q-actions">
                <el-input-number v-model="q.score" :min="0" :precision="2" :step="1" size="small" style="width:90px;margin-right:6px" @change="calcTotal" />
                <el-button size="small" type="primary" link @click="moveUp(index)" :disabled="index === 0">↑</el-button>
                <el-button size="small" type="primary" link @click="moveDown(index)" :disabled="index === selectedQuestions.length - 1">↓</el-button>
                <el-button size="small" type="danger" link @click="removeQuestion(index)">✕</el-button>
              </div>
            </div>
          </div>

          <div class="footer-actions">
            <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
            <el-button type="success" @click="validateAndSubmit" :loading="saving">校验并提交</el-button>
          </div>
        </el-card>
      </div>

      <!-- 校验弹窗 -->
      <el-dialog v-model="showValidation" title="试卷校验结果" width="500px">
        <el-alert :type="validationResult.passed ? 'success' : 'error'" :title="validationResult.passed ? '校验通过！' : '校验未通过'" :closable="false" style="margin-bottom:16px" />
        <el-descriptions :column="1" border>
          <el-descriptions-item label="知识点覆盖率">
            {{ validationResult.knowledgePointCoverage }}% / 85%
            <el-tag :type="validationResult.knowledgePointCoverage >= 85 ? 'success' : 'danger'" size="small">
              {{ validationResult.knowledgePointCoverage >= 85 ? '通过' : '未通过' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="未覆盖知识点" v-if="validationResult.uncoveredKnowledgePoints?.length">
            <el-tag v-for="kp in validationResult.uncoveredKnowledgePoints" :key="kp" type="warning" size="small" style="margin:2px">{{ kp }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="难度匹配">
            {{ validationResult.difficultyDetail }}
            <el-tag :type="validationResult.difficultyMatched ? 'success' : 'danger'" size="small">
              {{ validationResult.difficultyMatched ? '通过' : '未通过' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总分匹配">
            实际: {{ validationResult.actualTotalScore }} / 预期: {{ validationResult.expectedTotalScore }}
            <el-tag :type="validationResult.totalScoreMatched ? 'success' : 'danger'" size="small">
              {{ validationResult.totalScoreMatched ? '通过' : '未通过' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <template #footer>
          <el-button @click="showValidation = false">返回修改</el-button>
          <el-button v-if="validationResult.passed" type="primary" @click="confirmSubmit">确认提交</el-button>
        </template>
      </el-dialog>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTeacherStore } from '../stores/teacher'
import { getQuestions } from '../api/question'
import { getPaper, updatePaper, validatePaper, submitPaper } from '../api/paper'
import { ElMessage } from 'element-plus'
import AppLayout from '../components/AppLayout.vue'

const route = useRoute()
const router = useRouter()
const store = useTeacherStore()

const form = reactive({ paperName: '', difficulty: 'MEDIUM', totalScore: 100 })
const availableQuestions = ref([])
const qTotal = ref(0)
const qLoading = ref(false)
const qFilters = reactive({ type: '', difficulty: '', keyword: '', page: 1, size: 20 })
const selectedQuestions = ref([])
const saving = ref(false)
const showValidation = ref(false)
const validationResult = ref({})
const loading = ref(false)

let _uidCounter = 0

onMounted(() => {
  if (!store.isLoggedIn) { router.push('/'); return }
  loadPaper()
  loadQuestions()
})

function loadPaper() {
  loading.value = true
  getPaper(route.params.id).then(res => {
    const data = res.data
    form.paperName = data.paperName
    form.difficulty = data.difficulty
    form.totalScore = data.totalScore
    if (data.questions && data.questions.length > 0) {
      selectedQuestions.value = data.questions.map(q => ({
        id: q.questionId,
        title: q.questionTitle,
        type: q.questionType,
        difficulty: q.questionDifficulty,
        score: q.questionScore,
        _uid: ++_uidCounter
      }))
    }
  }).finally(() => { loading.value = false })
}

function loadQuestions() {
  qLoading.value = true
  const params = { ...qFilters, teacherId: store.currentTeacher?.id }
  Object.keys(params).forEach(k => { if (!params[k]) delete params[k] })
  getQuestions(params).then(res => {
    availableQuestions.value = res.data.records
    qTotal.value = res.data.total
  }).finally(() => { qLoading.value = false })
}

function isSelected(id) {
  return selectedQuestions.value.some(q => q.id === id)
}

function addQuestion(q) {
  if (isSelected(q.id)) return
  selectedQuestions.value.push({ ...q, score: 5, _uid: ++_uidCounter })
  calcTotal()
}

function removeQuestion(index) {
  selectedQuestions.value.splice(index, 1)
  calcTotal()
}

function moveUp(index) {
  if (index === 0) return
  const arr = selectedQuestions.value
  ;[arr[index - 1], arr[index]] = [arr[index], arr[index - 1]]
}

function moveDown(index) {
  const arr = selectedQuestions.value
  if (index >= arr.length - 1) return
  ;[arr[index], arr[index + 1]] = [arr[index + 1], arr[index]]
}

const actualTotalScore = computed(() => {
  return selectedQuestions.value.reduce((sum, q) => sum + (q.score || 0), 0)
})

function calcTotal() {
  selectedQuestions.value = [...selectedQuestions.value]
}

function buildPayload() {
  return {
    teacherId: store.currentTeacher?.id,
    paperName: form.paperName,
    totalScore: form.totalScore,
    difficulty: form.difficulty,
    questions: selectedQuestions.value.map(q => ({
      questionId: q.id,
      questionScore: q.score
    }))
  }
}

function saveDraft() {
  if (!form.paperName) { ElMessage.warning('请输入试卷名称'); return }
  if (selectedQuestions.value.length === 0) { ElMessage.warning('请至少选择一道题目'); return }
  saving.value = true
  updatePaper(route.params.id, buildPayload()).then(() => {
    ElMessage.success('保存成功')
  }).finally(() => { saving.value = false })
}

function validateAndSubmit() {
  if (!form.paperName) { ElMessage.warning('请输入试卷名称'); return }
  if (selectedQuestions.value.length === 0) { ElMessage.warning('请至少选择一道题目'); return }
  saving.value = true
  updatePaper(route.params.id, buildPayload()).then(res => {
    const paperId = res.data.id
    validatePaper(paperId).then(vres => {
      validationResult.value = { ...vres.data, _paperId: paperId }
      showValidation.value = true
    })
  }).finally(() => { saving.value = false })
}

function confirmSubmit() {
  const paperId = validationResult.value._paperId
  if (!paperId) return
  saving.value = true
  submitPaper(paperId).then(() => {
    ElMessage.success('试卷提交成功')
    router.push(`/papers/${paperId}`)
  }).finally(() => { saving.value = false })
}

function typeLabel(type) {
  const map = { SINGLE_CHOICE: '单选题', MULTIPLE_CHOICE: '多选题', FILL_BLANK: '填空题', SUBJECTIVE: '主观题' }
  return map[type] || type
}
function diffLabel(d) {
  const map = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
  return map[d] || d
}
function diffTag(d) {
  const map = { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }
  return map[d] || ''
}
</script>

<style scoped>
.paper-edit { max-width: 1600px; margin: 0 auto; }
.header-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.header-bar h2 { margin: 0; font-size: 20px; }
.basic-info { margin-bottom: 16px; }
.two-columns { display: flex; gap: 16px; }
.left-panel { flex: 3; }
.right-panel { flex: 2; max-height: 70vh; overflow-y: auto; }
.filters { display: flex; gap: 8px; margin-bottom: 12px; }
.pagination-small { margin-top: 12px; display: flex; justify-content: center; }
.panel-header { display: flex; justify-content: space-between; align-items: center; }
.total-score { font-size: 14px; color: #606266; }
.total-score strong { color: #409EFF; font-size: 18px; }
.paper-question { padding: 8px 0; border-bottom: 1px dashed #ebeef5; }
.q-header { display: flex; align-items: center; gap: 6px; }
.q-title { flex: 1; font-size: 14px; }
.q-actions { display: flex; align-items: center; gap: 4px; }
.footer-actions { margin-top: 16px; display: flex; gap: 12px; justify-content: flex-end; }
</style>
