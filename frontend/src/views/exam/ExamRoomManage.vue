<template>
  <AppLayout>
    <div class="room-manage">
      <div class="toolbar">
        <h2 style="margin:0">考场管理</h2>
        <div class="spacer" />
        <el-button type="primary" @click="openDialog()">新建考场</el-button>
      </div>

      <el-table :data="rooms" stripe v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="roomName" label="考场名称" min-width="160" />
        <el-table-column prop="location" label="地点" min-width="160" show-overflow-tooltip />
        <el-table-column prop="capacity" label="容纳人数" width="100" />
        <el-table-column prop="teacherName" label="负责教师" width="120" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除此考场？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 新建/编辑弹窗 -->
      <el-dialog v-model="dialogVisible" :title="editingId ? '编辑考场' : '新建考场'" width="480px">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
          <el-form-item label="考场名称" prop="roomName">
            <el-input v-model="form.roomName" placeholder="如：A-101考场" />
          </el-form-item>
          <el-form-item label="地点" prop="location">
            <el-input v-model="form.location" placeholder="如：教学楼A栋101" />
          </el-form-item>
          <el-form-item label="容纳人数" prop="capacity">
            <el-input-number v-model="form.capacity" :min="1" :max="500" style="width:100%" />
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
import { getExamRooms, createExamRoom, updateExamRoom, deleteExamRoom } from '../../api/exam'

const router = useRouter()
const store = useTeacherStore()
const rooms = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref()

const form = ref({ roomName: '', location: '', capacity: 30 })
const rules = {
  roomName: [{ required: true, message: '请输入考场名称', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入容纳人数', trigger: 'blur' }]
}

onMounted(() => {
  if (!store.isLoggedIn) { router.push('/'); return }
  loadRooms()
})

function loadRooms() {
  loading.value = true
  getExamRooms().then(res => { rooms.value = res.data }).finally(() => { loading.value = false })
}

function openDialog(row = null) {
  editingId.value = row?.id ?? null
  if (row) {
    form.value = { roomName: row.roomName, location: row.location || '', capacity: row.capacity }
  } else {
    form.value = { roomName: '', location: '', capacity: 30 }
  }
  dialogVisible.value = true
}

function handleSubmit() {
  formRef.value.validate(valid => {
    if (!valid) return
    submitting.value = true
    const payload = { ...form.value, teacherId: store.currentTeacher.id }
    const req = editingId.value
      ? updateExamRoom(editingId.value, payload)
      : createExamRoom(payload)
    req.then(() => {
      ElMessage.success(editingId.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadRooms()
    }).finally(() => { submitting.value = false })
  })
}

function handleDelete(id) {
  deleteExamRoom(id).then(() => {
    ElMessage.success('删除成功')
    loadRooms()
  })
}
</script>

<style scoped>
.room-manage { max-width: 1100px; margin: 0 auto; }
.toolbar { display: flex; align-items: center; margin-bottom: 16px; }
.spacer { flex: 1; }
</style>
