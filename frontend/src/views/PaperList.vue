<template>
  <AppLayout>
    <div class="paper-list">
      <div class="search-bar">
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width:120px" @change="search">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已提交" value="SUBMITTED" />
        </el-select>
        <div class="spacer" />
        <el-button type="primary" @click="$router.push('/papers/create')">新建试卷</el-button>
      </div>

      <el-table :data="papers" style="width:100%" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="paperName" label="试卷名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="总分" width="80">
          <template #default="{ row }">{{ row.totalScore }}</template>
        </el-table-column>
        <el-table-column label="目标难度" width="100">
          <template #default="{ row }">
            <el-tag :type="diffTag(row.difficulty)" size="small">{{ diffLabel(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="知识点覆盖率" width="140">
          <template #default="{ row }">
            <el-progress
              :percentage="row.knowledgePointCoverage || 0"
              :color="row.knowledgePointCoverage >= 85 ? '#67c23a' : '#f56c6c'"
              :format="() => (row.knowledgePointCoverage || 0) + '%'"
              :stroke-width="14"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUBMITTED' ? 'success' : 'info'" size="small">
              {{ row.status === 'SUBMITTED' ? '已提交' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ row.createTime }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/papers/${row.id}`)">查看</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="primary" size="small" @click="$router.push(`/papers/${row.id}/edit`)">编辑</el-button>
            <el-popconfirm title="确定删除此试卷？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[5,10,20,50]"
          layout="total,sizes,prev,pager,next"
          @size-change="search"
          @current-change="search"
        />
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTeacherStore } from '../stores/teacher'
import { getPapers, deletePaper } from '../api/paper'
import { ElMessage } from 'element-plus'
import AppLayout from '../components/AppLayout.vue'

const router = useRouter()
const store = useTeacherStore()

const papers = ref([])
const total = ref(0)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref('')

onMounted(() => {
  if (!store.isLoggedIn) { router.push('/'); return }
  search()
})

function search() {
  loading.value = true
  const params = { teacherId: store.currentTeacher?.id, page: currentPage.value, size: pageSize.value }
  if (statusFilter.value) params.status = statusFilter.value
  getPapers(params).then(res => {
    papers.value = res.data.records
    total.value = res.data.total
  }).finally(() => { loading.value = false })
}

function handleDelete(id) {
  deletePaper(id).then(() => {
    ElMessage.success('删除成功')
    search()
  })
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
.paper-list { max-width: 1400px; margin: 0 auto; }
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
.spacer { flex: 1; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
