import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'

const routes = [
  { path: '/', name: 'Login', component: LoginView },
  // 题库管理
  { path: '/questions', name: 'QuestionList', component: () => import('../views/QuestionList.vue') },
  { path: '/questions/create', name: 'QuestionCreate', component: () => import('../views/QuestionCreate.vue') },
  { path: '/questions/:id/edit', name: 'QuestionEdit', component: () => import('../views/QuestionEdit.vue') },
  { path: '/questions/:id', name: 'QuestionDetail', component: () => import('../views/QuestionDetail.vue') },
  // 题目性质管理
  { path: '/properties', name: 'PropertiesDashboard', component: () => import('../views/properties/PropertiesDashboard.vue') },
  { path: '/properties/chapters', name: 'ChapterManage', component: () => import('../views/properties/ChapterManage.vue') },
  { path: '/properties/knowledge-points', name: 'KnowledgePointManage', component: () => import('../views/properties/KnowledgePointManage.vue') },
  { path: '/properties/question/:id', name: 'QuestionPropertiesEdit', component: () => import('../views/properties/QuestionPropertiesEdit.vue') },
  // 手动组卷
  { path: '/papers', name: 'PaperList', component: () => import('../views/PaperList.vue') },
  { path: '/papers/create', name: 'PaperCreate', component: () => import('../views/PaperCreate.vue') },
  { path: '/papers/:id', name: 'PaperDetail', component: () => import('../views/PaperDetail.vue') },
  { path: '/papers/:id/edit', name: 'PaperEdit', component: () => import('../views/PaperEdit.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
