<template>
  <AppLayout>
    <div class="auto-paper-page">
      <el-card shadow="never" class="form-card">
        <template #header>
          <div class="card-header">
            <span>自动组卷</span>
            <el-button type="primary" :loading="loading" @click="handleGenerate">自动组卷</el-button>
          </div>
        </template>

        <el-form :model="form" label-width="110px">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="总分">
                <el-input-number v-model="form.totalScore" :min="1" :step="10" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="目标难度">
                <el-select v-model="form.difficulty" placeholder="请选择难度" style="width: 100%">
                  <el-option label="简单" value="EASY" />
                  <el-option label="中等" value="MEDIUM" />
                  <el-option label="困难" value="HARD" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="章节">
                <el-select v-model="form.chapterId" placeholder="全部章节" clearable style="width: 100%">
                  <el-option
                    v-for="item in chapterOptions"
                    :key="item.id"
                    :label="item.label"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="left">题型数量</el-divider>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="单选题">
                <el-input-number v-model="form.singleChoiceCount" :min="0" :max="99" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="多选题">
                <el-input-number v-model="form.multipleChoiceCount" :min="0" :max="99" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="填空题">
                <el-input-number v-model="form.fillBlankCount" :min="0" :max="99" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="主观题">
                <el-input-number v-model="form.subjectiveCount" :min="0" :max="99" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-card>

      <el-card v-if="result" shadow="never" class="result-card">
        <template #header>
          <div class="card-header">
            <span>组卷结果</span>
            <el-tag :type="result.success ? 'success' : 'danger'">
              {{ result.success ? '成功' : '失败' }}
            </el-tag>
          </div>
        </template>

        <el-alert
          :title="result.message || (result.success ? '组卷成功' : '组卷失败')"
          :type="result.success ? 'success' : 'error'"
          :closable="false"
          show-icon
        />

        <template v-if="result.success">
          <el-descriptions :column="3" border class="summary">
            <el-descriptions-item label="总分">{{ result.totalScore }}</el-descriptions-item>
            <el-descriptions-item label="总题数">{{ result.totalQuestionCount }}</el-descriptions-item>
            <el-descriptions-item label="每题分值">{{ result.questionScore }}</el-descriptions-item>
            <el-descriptions-item label="实际总分">{{ result.actualTotalScore }}</el-descriptions-item>
            <el-descriptions-item label="目标难度">{{ diffLabel(result.difficulty) }}</el-descriptions-item>
            <el-descriptions-item label="覆盖率">{{ result.coverageRate }}%</el-descriptions-item>
            <el-descriptions-item label="知识点总数">{{ result.totalKnowledgePointCount }}</el-descriptions-item>
            <el-descriptions-item label="已覆盖知识点">{{ result.coveredKnowledgePointCount }}</el-descriptions-item>
            <el-descriptions-item label="未覆盖知识点">
              {{ Math.max((result.totalKnowledgePointCount || 0) - (result.coveredKnowledgePointCount || 0), 0) }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="uncovered-section">
            <div class="section-title">未覆盖知识点</div>
            <div v-if="result.uncoveredKnowledgePoints?.length" class="tag-list">
              <el-tag
                v-for="item in result.uncoveredKnowledgePoints"
                :key="item"
                type="warning"
                effect="plain"
              >
                {{ item }}
              </el-tag>
            </div>
            <el-empty v-else description="当前筛选范围内知识点已全部覆盖" :image-size="80" />
          </div>

          <el-table :data="result.questions || []" stripe class="result-table">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="题型" width="110">
              <template #default="{ row }">
                <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="题目标题" min-width="260" show-overflow-tooltip />
            <el-table-column label="难度" width="100">
              <template #default="{ row }">
                <el-tag :type="diffTag(row.difficulty)" size="small" effect="plain">
                  {{ diffLabel(row.difficulty) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="chapter" label="章节" min-width="140" show-overflow-tooltip />
            <el-table-column prop="knowledgePoints" label="知识点" min-width="180" show-overflow-tooltip />
            <el-table-column prop="score" label="分值" width="90" />
          </el-table>
        </template>
      </el-card>
    </div>
  </AppLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppLayout from '../components/AppLayout.vue'
import { autoGeneratePaper } from '../api/paper'
import { getChapters } from '../api/chapter'
import { useTeacherStore } from '../stores/teacher'

const router = useRouter()
const store = useTeacherStore()

const loading = ref(false)
const result = ref(null)
const chapterOptions = ref([])
const form = ref({
  totalScore: 100,
  difficulty: 'MEDIUM',
  chapterId: null,
  singleChoiceCount: 5,
  multipleChoiceCount: 0,
  fillBlankCount: 3,
  subjectiveCount: 2
})

onMounted(() => {
  if (!store.isLoggedIn) {
    router.push('/')
    return
  }
  loadBaseData()
})

async function loadBaseData() {
  const res = await getChapters()
  chapterOptions.value = flattenChapters(res.data || [])
}

function flattenChapters(nodes, prefix = '') {
  const resultList = []
  ;(nodes || []).forEach(node => {
    const label = prefix ? `${prefix} / ${node.name}` : node.name
    resultList.push({ id: node.id, name: node.name, label })
    if (node.children?.length) {
      resultList.push(...flattenChapters(node.children, label))
    }
  })
  return resultList
}

function buildRequestPayload() {
  const selectedChapter = chapterOptions.value.find(item => item.id === form.value.chapterId)
  const typeRules = [
    { type: 'SINGLE_CHOICE', count: form.value.singleChoiceCount },
    { type: 'MULTIPLE_CHOICE', count: form.value.multipleChoiceCount },
    { type: 'FILL_BLANK', count: form.value.fillBlankCount },
    { type: 'SUBJECTIVE', count: form.value.subjectiveCount }
  ].filter(item => item.count > 0)

  return {
    teacherId: store.currentTeacher?.id,
    totalScore: form.value.totalScore,
    difficulty: form.value.difficulty,
    chapterId: selectedChapter?.id ?? null,
    chapter: selectedChapter?.name || '',
    typeRules
  }
}

async function handleGenerate() {
  const payload = buildRequestPayload()
  if (!payload.teacherId) {
    router.push('/')
    return
  }
  if (!payload.totalScore || payload.totalScore <= 0) {
    ElMessage.warning('总分必须大于0')
    return
  }
  if (!payload.typeRules.length) {
    ElMessage.warning('请至少选择一种题型')
    return
  }

  loading.value = true
  try {
    const res = await autoGeneratePaper(payload)
    result.value = res.data
  } finally {
    loading.value = false
  }
}

function typeLabel(type) {
  const map = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    FILL_BLANK: '填空题',
    SUBJECTIVE: '主观题'
  }
  return map[type] || type
}

function typeTag(type) {
  const map = {
    SINGLE_CHOICE: '',
    MULTIPLE_CHOICE: 'warning',
    FILL_BLANK: 'success',
    SUBJECTIVE: 'danger'
  }
  return map[type] || ''
}

function diffLabel(difficulty) {
  const map = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
  return map[difficulty] || difficulty
}

function diffTag(difficulty) {
  const map = { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }
  return map[difficulty] || ''
}
</script>

<style scoped>
.auto-paper-page {
  max-width: 1400px;
  margin: 0 auto;
}

.form-card,
.result-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary {
  margin-top: 16px;
}

.uncovered-section {
  margin-top: 20px;
}

.section-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.result-table {
  margin-top: 20px;
}
</style>
