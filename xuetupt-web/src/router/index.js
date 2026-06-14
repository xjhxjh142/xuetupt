import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/course',
    name: 'Course',
    component: () => import('@/views/Course.vue')
  },
  {
    path: '/course/:id',
    name: 'CourseDetail',
    component: () => import('@/views/CourseDetail.vue')
  },
  {
    path: '/material',
    name: 'Material',
    component: () => import('@/views/Material.vue')
  },
  {
    path: '/teacher',
    name: 'Teacher',
    component: () => import('@/views/Teacher.vue')
  },
  {
    path: '/study-room',
    name: 'StudyRoom',
    component: () => import('@/views/StudyRoom.vue')
  },
  {
    path: '/rank',
    name: 'Rank',
    component: () => import('@/views/Rank.vue')
  },
  {
    path: '/ai-tutor',
    name: 'AITutor',
    component: () => import('@/views/AITutor.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.name !== 'Login' && !token) {
    next({ name: 'Login' })
  } else {
    next()
  }
})

export default router
