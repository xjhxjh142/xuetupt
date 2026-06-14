<template>
  <el-menu
    :default-active="activeMenu"
    mode="horizontal"
    :ellipsis="false"
    class="nav-bar"
    @select="handleSelect"
  >
    <el-menu-item index="/home">
      <el-icon><HomeFilled /></el-icon>
      <span>首页</span>
    </el-menu-item>
    <el-menu-item index="/course">
      <el-icon><Reading /></el-icon>
      <span>课程中心</span>
    </el-menu-item>
    <el-menu-item index="/material">
      <el-icon><FolderOpened /></el-icon>
      <span>学习资料</span>
    </el-menu-item>
    <el-menu-item index="/teacher">
      <el-icon><UserFilled /></el-icon>
      <span>名师团队</span>
    </el-menu-item>
    <el-menu-item index="/study-room">
      <el-icon><Clock /></el-icon>
      <span>自习室</span>
    </el-menu-item>
    <el-menu-item index="/ai-tutor">
      <el-icon><MagicStick /></el-icon>
      <span>AI 辅导</span>
    </el-menu-item>
    <el-menu-item index="/rank">
      <el-icon><TrophyBase /></el-icon>
      <span>排行榜</span>
    </el-menu-item>

    <div class="nav-right">
      <!-- 用户信息 -->
      <div class="user-info" v-if="user">
        <el-icon><User /></el-icon>
        <span class="user-name">{{ user.nickName }}</span>
        <span class="user-id">ID: {{ user.id }}</span>
      </div>
      <el-button type="primary" text @click="handleLogout">
        <el-icon><SwitchButton /></el-icon>
        退出登录
      </el-button>
    </div>
  </el-menu>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { userApi } from '@/api'

const router = useRouter()
const route = useRoute()
const user = ref(null)

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/course')) return '/course'
  if (path.startsWith('/material')) return '/material'
  if (path.startsWith('/teacher')) return '/teacher'
  if (path.startsWith('/study-room')) return '/study-room'
  if (path.startsWith('/ai-tutor')) return '/ai-tutor'
  if (path.startsWith('/rank')) return '/rank'
  return '/home'
})

const handleSelect = (index) => {
  router.push(index)
}

const handleLogout = () => {
  ElMessageBox.confirm('确定退出登录吗？', '提示').then(() => {
    localStorage.removeItem('token')
    user.value = null
    router.push('/login')
  }).catch(() => {})
}

// 获取当前登录用户信息
const loadUserInfo = async () => {
  const token = localStorage.getItem('token')
  if (!token) return
  const res = await userApi.getInfo()
  if (res.success) {
    user.value = res.data
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.nav-bar {
  padding: 0 20px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
}

.nav-bar .el-menu-item {
  font-size: 15px;
  height: 60px;
  line-height: 60px;
}

.nav-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  height: 36px;
  background: #f0f5ff;
  border-radius: 18px;
  font-size: 14px;
  color: #409eff;
}

.user-name {
  font-weight: 500;
}

.user-id {
  color: #999;
  font-size: 12px;
}
</style>
