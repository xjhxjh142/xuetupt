<template>
  <div class="page">
    <NavBar />
    <div class="page-content">
      <h2 class="page-title">自习室</h2>

      <!-- 签到 -->
      <el-card shadow="never" class="sign-card">
        <div class="sign-info">
          <div>
            <h3>每日签到</h3>
            <p v-if="signStatus">已连续签到 {{ signStatus.continuousDays }} 天</p>
          </div>
          <el-button type="warning" :disabled="signStatus?.signed" @click="handleSign">
            {{ signStatus?.signed ? '已签到' : '签到' }}
          </el-button>
        </div>
      </el-card>

      <!-- 自习室列表 -->
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="8" v-for="room in rooms" :key="room.id" style="margin-bottom: 20px">
          <el-card shadow="hover" class="room-card">
            <div class="room-header">
              <h3>{{ room.name }}</h3>
              <el-tag size="small">{{ room.currentCount || 0 }} 人在线</el-tag>
            </div>
            <p class="room-desc">{{ room.description || '一起专注学习' }}</p>
            <div class="room-actions">
              <el-button size="small" :type="currentRoomId === room.id ? 'default' : 'primary'"
                         @click="handleEnter(room.id)">
                {{ currentRoomId === room.id ? '切换进入' : '进入' }}
              </el-button>
              <el-button size="small" type="primary" @click="handleStartFocus(room.id)">开始专注</el-button>
              <el-button size="small" type="danger" @click="handleEndFocus(room.id)">结束专注</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import NavBar from '@/components/NavBar.vue'
import { studyRoomApi } from '@/api'

const rooms = ref([])
const signStatus = ref(null)
const currentRoomId = ref(null)
let refreshTimer = null

const loadRooms = async () => {
  const res = await studyRoomApi.getList()
  if (res.success) rooms.value = res.data
}

const handleSign = async () => {
  const res = await studyRoomApi.sign()
  if (res.success) {
    ElMessage.success('签到成功')
    loadSignStatus()
  }
}

const handleEnter = async (roomId) => {
  // 如果已在其他房间，先离开
  if (currentRoomId.value && currentRoomId.value !== roomId) {
    await studyRoomApi.leave(currentRoomId.value)
  }
  const res = await studyRoomApi.enter(roomId)
  if (res.success) {
    currentRoomId.value = roomId
    ElMessage.success('已进入自习室')
    loadRooms() // 刷新在线人数
  }
}

const handleStartFocus = async (roomId) => {
  const res = await studyRoomApi.startFocus(roomId)
  if (res.success) {
    currentRoomId.value = roomId
    ElMessage.success('开始专注')
    loadRooms()
  }
}

const handleEndFocus = async (roomId) => {
  const res = await studyRoomApi.endFocus(roomId)
  if (res.success) {
    ElMessage.success(res.data || '专注结束')
    loadRooms()
  }
}

const loadSignStatus = async () => {
  const res = await studyRoomApi.getSignStatus()
  if (res.success) signStatus.value = res.data
}

// 定时刷新在线人数
const startRefresh = () => {
  refreshTimer = setInterval(() => {
    loadRooms()
  }, 10000) // 每10秒刷新一次
}

onMounted(async () => {
  await loadRooms()
  loadSignStatus()
  startRefresh()
})

onUnmounted(() => {
  // 离开页面时离开自习室
  if (currentRoomId.value) {
    studyRoomApi.leave(currentRoomId.value)
  }
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; }
.page-content { max-width: 1200px; margin: 0 auto; padding: 24px; }
.page-title { font-size: 22px; margin-bottom: 20px; }
.sign-card { margin-bottom: 20px; }
.sign-info { display: flex; justify-content: space-between; align-items: center; }
.sign-info h3 { font-size: 16px; margin-bottom: 4px; }
.sign-info p { color: #e6a23c; font-size: 14px; }
.room-card { }
.room-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.room-header h3 { font-size: 16px; }
.room-desc { color: #999; font-size: 13px; margin-bottom: 12px; }
.room-actions { display: flex; gap: 8px; }
</style>
