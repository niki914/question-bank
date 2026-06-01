<template>
  <AppLayout>
    <div class="exam-manage">
      <div class="toolbar">
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:130px" @change="search">
          <el-option label="待开始" value="PENDING" />
          <el-option label="进行中" value="ONGOING" />
          <el-option label="评分中" value="GRADING" />
          <el-option label="已完成" value="FINISHED" />
        </el-select>
        <div class="spacer" />
        <el-button type="primary" @click="openCreateDialog">新建考试</el-button>
      </div>

      <el-table :data="exams" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="examName" label="考试名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="paperName" label="试卷" min-width="150" show-overflow-tooltip />
        <el-table-column prop="roomName" label="考场" width="130" />
        <el-table-column label="开始时间" width="170">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="时长(分)" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/exam/detail/${row.id}`)">详情</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="warning" size="small" @click="handleStart(row.id)">开始</el-button>
            <el-button v-if="row.status === 'ONGOING'" link type="danger" size="small" @click="handleEnd(row.id)">结束考试</el-button>
            <el-button v-if="row.status === 'GRADING'" link type="success" size="small" @click="$router.push(`/exam/grading/${row.id}`)">去评分</el-button>
            <el-button v-if="row.status === 'FINISHED'" link type="info" size="small" @click="$router.push(`/exam/grading/${row.id}`)">查看成绩</el-button>
            <el-button v-if="row.status === 'PENDING'" link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm v-if="row.status !== 'ONGOING'" title="确定删除此考试？" @confirm="handleDelete(row.id)">
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
          layout="total,sizes,prev,pager,next"
          :page-sizes="[5,10,20]"
          @size-change="search"
          @current-change="search"
        />
      </div>

      <!-- 新建/编辑考试弹窗 -->
      <el-dialog v-model="dialogVisible" :title="editingId ? '编辑考试' : '新建考试'" width="560px">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
          <el-form-item label="考试名称" prop="examName">
            <el-input v-model="form.examName" placeholder="请输入考试名称" />
          </el-form-item>
          <el-form-item label="选择试卷" prop="paperId">
            <el-select v-model="form.paperId" placeholder="仅显示已提交试卷" style="width:100%">
              <el-option v-for="p in submittedPapers" :key="p.id" :label="p.paperName" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="选择考场" prop="roomId">
            <el-select v-model="form.roomId" placeholder="请选择考场" style="width:100%">
              <el-option v-for="r in rooms" :key="r.id" :label="`${r.roomName}（容量${r.capacity}人）`" :value="r.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="开始时间" prop="startTime">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              placeholder="选择开始时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width:100%"
            />
          </el-form-item>
          <el-form-item label="时长（分钟）" prop="durationMinutes">
            <el-input-number v-model="form.durationMinutes" :min="10" :max="600" style="width:100%" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTeacherStore } from '../../stores/teacher'
import AppLayout from '../../components/AppLayout.vue'
import { getExams, createExam, updateExam, deleteExam, startExam, endExam } from '../../api/exam'
import { getExamRooms } from '../../api/exam'
import { getPapers } from '../../api/paper'

const router = useRouter()
const store = useTeacherStore()
const exams = ref([])
const total = ref(0)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref('')
const rooms = ref([])
const submittedPapers = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref()

const form = ref({ examName: '', paperId: null, roomId: null, startTime: '', durationMinutes: 120 })
const rules = {
  examName: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  paperId: [{ required: true, message: '请选择试卷', trigger: 'change' }],
  roomId: [{ required: true, message: '请选择考场', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  durationMinutes: [{ required: true, message: '请填写时长', trigger: 'blur' }]
}

onMounted(() => {
  if (!store.isLoggedIn) { router.push('/'); return }
  search()
  loadOptions()
})

function search() {
  loading.value = true
  const params = { teacherId: store.currentTeacher.id, page: currentPage.value, size: pageSize.value }
  if (statusFilter.value) params.status = statusFilter.value
  getExams(params).then(res => {
    exams.value = res.data.records
    total.value = res.data.total
  }).finally(() => { loading.value = false })
}

function loadOptions() {
  getExamRooms().then(res => { rooms.value = res.data })
  getPapers({ teacherId: store.currentTeacher.id, page: 1, size: 100, status: 'SUBMITTED' }).then(res => {
    submittedPapers.value = res.data.records
  })
}

function openCreateDialog() {
  editingId.value = null
  form.value = { examName: '', paperId: null, roomId: null, startTime: '', durationMinutes: 120 }
  dialogVisible.value = true
}

function openEditDialog(row) {
  editingId.value = row.id
  form.value = {
    examName: row.examName,
    paperId: row.paperId,
    roomId: row.roomId,
    startTime: row.startTime,
    durationMinutes: row.durationMinutes
  }
  dialogVisible.value = true
}

function handleSubmit() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitting.value = true
    const payload = { ...form.value, teacherId: store.currentTeacher.id }
    const req = editingId.value ? updateExam(editingId.value, payload) : createExam(payload)
    req.then(() => {
      ElMessage.success(editingId.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      search()
    }).finally(() => { submitting.value = false })
  })
}

function handleDelete(id) {
  deleteExam(id).then(() => { ElMessage.success('删除成功'); search() })
}

function handleStart(id) {
  startExam(id).then(() => { ElMessage.success('考试已开始'); search() })
}

function handleEnd(id) {
  endExam(id).then(() => { ElMessage.success('考试已结束，进入评分阶段'); search() })
}

function statusLabel(s) {
  return { PENDING: '待开始', ONGOING: '进行中', GRADING: '评分中', FINISHED: '已完成' }[s] || s
}
function statusTag(s) {
  return { PENDING: 'info', ONGOING: 'warning', GRADING: '', FINISHED: 'success' }[s] || ''
}
function formatTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.exam-manage { max-width: 1400px; margin: 0 auto; }
.toolbar { display: flex; align-items: center; margin-bottom: 16px; gap: 12px; }
.spacer { flex: 1; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
