<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="header-left">
        <span class="logo">题目管理系统</span>
      </div>
      <div class="header-right">
        <span class="teacher-name">{{ store.currentTeacher?.name }}</span>
        <el-tag size="small">{{ store.currentTeacher?.department }}</el-tag>
        <el-button type="danger" size="small" @click="handleLogout" plain>退出</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="220px" class="aside">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <el-sub-menu index="qb">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span>题库管理</span>
            </template>
            <el-menu-item index="/questions">
              <el-icon><List /></el-icon>
              <span>题目列表</span>
            </el-menu-item>
            <el-menu-item index="/questions/create">
              <el-icon><Plus /></el-icon>
              <span>新建题目</span>
            </el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="prop">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>性质管理</span>
            </template>
            <el-menu-item index="/properties">
              <el-icon><DataAnalysis /></el-icon>
              <span>属性总览</span>
            </el-menu-item>
            <el-menu-item index="/properties/chapters">
              <el-icon><FolderOpened /></el-icon>
              <span>章节管理</span>
            </el-menu-item>
            <el-menu-item index="/properties/knowledge-points">
              <el-icon><CollectionTag /></el-icon>
              <span>知识点管理</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/papers/auto-generate">
            <el-icon><Document /></el-icon>
            <span>自动组卷</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTeacherStore } from '../stores/teacher'

const route = useRoute()
const router = useRouter()
const store = useTeacherStore()

const activeMenu = computed(() => {
  const p = route.path
  if (p.startsWith('/questions/create')) return '/questions/create'
  if (p.startsWith('/questions')) return '/questions'
  if (p.startsWith('/papers/auto-generate')) return '/papers/auto-generate'
  if (p === '/properties') return '/properties'
  if (p.startsWith('/properties/chapters')) return '/properties/chapters'
  if (p.startsWith('/properties/knowledge-points')) return '/properties/knowledge-points'
  if (p.startsWith('/properties/question')) return '/properties'
  return '/questions'
})

function handleLogout() {
  store.logout()
  router.push('/')
}
</script>

<style scoped>
.layout { height: 100vh; }
.header { background: #1f2d3d; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; }
.logo { color: #fff; font-size: 18px; font-weight: bold; }
.header-right { display: flex; align-items: center; gap: 10px; color: #fff; }
.teacher-name { font-size: 14px; }
.aside { background: #304156; }
.main { background: #f0f2f5; padding: 20px; overflow-y: auto; }
</style>
