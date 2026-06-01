<template>
  <AppLayout>
    <div class="paper-detail" v-loading="loading">
      <div class="header-bar">
        <el-button @click="$router.back()">返回</el-button>
        <h2>{{ paper.paperName }}</h2>
        <el-tag :type="paper.status === 'SUBMITTED' ? 'success' : 'info'" size="large">
          {{ paper.status === 'SUBMITTED' ? '已提交' : '草稿' }}
        </el-tag>
        <div class="spacer" />
        <el-button v-if="paper.status === 'DRAFT'" type="primary" @click="$router.push(`/papers/${paper.id}/edit`)">编辑</el-button>
      </div>

      <el-descriptions :column="3" border class="info-card">
        <el-descriptions-item label="总分">{{ paper.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="实际总分">{{ paper.actualTotalScore || 0 }}</el-descriptions-item>
        <el-descriptions-item label="目标难度">
          <el-tag :type="diffTag(paper.difficulty)" size="small">{{ diffLabel(paper.difficulty) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="知识点覆盖率">
          <el-progress
            :percentage="paper.knowledgePointCoverage || 0"
            :color="paper.knowledgePointCoverage >= 85 ? '#67c23a' : '#f56c6c'"
            :format="() => (paper.knowledgePointCoverage || 0) + '%'"
            :stroke-width="16"
          />
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ paper.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ paper.updateTime }}</el-descriptions-item>
      </el-descriptions>

      <el-card class="questions-card" shadow="never">
        <template #header><span>试卷题目（共 {{ paper.questions?.length || 0 }} 题）</span></template>
        <div v-for="(q, index) in paper.questions" :key="q.id" class="paper-question">
          <div class="q-header">
            <span class="q-order">第{{ index + 1 }}题</span>
            <el-tag size="small">{{ typeLabel(q.questionType) }}</el-tag>
            <el-tag size="small" :type="diffTag(q.questionDifficulty)" effect="plain" style="margin-left:4px">{{ diffLabel(q.questionDifficulty) }}</el-tag>
            <span class="q-score">分值: {{ q.questionScore }}</span>
          </div>
          <div class="q-title">{{ q.questionTitle }}</div>
          <div class="q-kp" v-if="q.knowledgePoints">
            <span class="kp-label">知识点:</span>
            <el-tag v-for="kp in q.knowledgePoints.split(',')" :key="kp" size="small" type="info" style="margin:2px">{{ kp.trim() }}</el-tag>
          </div>
        </div>
      </el-card>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTeacherStore } from '../stores/teacher'
import { getPaper } from '../api/paper'
import AppLayout from '../components/AppLayout.vue'

const route = useRoute()
const router = useRouter()
const store = useTeacherStore()

const paper = ref({})
const loading = ref(false)

onMounted(() => {
  if (!store.isLoggedIn) { router.push('/'); return }
  loadPaper()
})

function loadPaper() {
  loading.value = true
  getPaper(route.params.id).then(res => {
    paper.value = res.data
  }).finally(() => { loading.value = false })
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
.paper-detail { max-width: 1000px; margin: 0 auto; }
.header-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.header-bar h2 { margin: 0; font-size: 20px; }
.spacer { flex: 1; }
.info-card { margin-bottom: 16px; }
.questions-card { margin-bottom: 16px; }
.paper-question { padding: 12px 0; border-bottom: 1px solid #ebeef5; }
.paper-question:last-child { border-bottom: none; }
.q-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.q-order { font-weight: bold; color: #409EFF; }
.q-score { margin-left: auto; font-weight: bold; color: #e6a23c; }
.q-title { font-size: 15px; margin-bottom: 6px; }
.q-kp { display: flex; align-items: center; gap: 4px; }
.kp-label { font-size: 12px; color: #909399; }
</style>
