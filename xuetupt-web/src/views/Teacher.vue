<template>
  <div class="page">
    <NavBar />
    <div class="page-content">
      <h2 class="page-title">名师团队</h2>
      <el-row :gutter="20">
        <el-col :span="6" v-for="teacher in teachers" :key="teacher.id" style="margin-bottom: 20px">
          <el-card shadow="hover" class="teacher-card">
            <div class="teacher-avatar" :style="{ background: getColor(teacher.id) }">
              <span class="avatar-text">{{ teacher.name?.charAt(0) }}</span>
            </div>
            <h3>{{ teacher.name }}</h3>
            <p class="teacher-title">{{ teacher.title || '讲师' }}</p>
            <div class="teacher-score">
              <el-rate :model-value="teacher.score || 5" disabled show-score text-color="#ff9900" />
            </div>
            <p class="teacher-desc">{{ teacher.description || '暂无介绍' }}</p>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import NavBar from '@/components/NavBar.vue'
import { teacherApi } from '@/api'

const teachers = ref([])
const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb']
const getColor = (id) => colors[id % colors.length]

onMounted(async () => {
  const res = await teacherApi.getList(1)
  if (res.success) teachers.value = res.data
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; }
.page-content { max-width: 1200px; margin: 0 auto; padding: 24px; }
.page-title { font-size: 22px; margin-bottom: 20px; }
.teacher-card { text-align: center; padding: 20px; }
.teacher-avatar { width: 80px; height: 80px; border-radius: 50%; margin: 0 auto 12px; display: flex; align-items: center; justify-content: center; }
.avatar-text { color: #fff; font-size: 32px; font-weight: bold; }
.teacher-card h3 { font-size: 16px; margin-bottom: 4px; }
.teacher-title { color: #999; font-size: 13px; margin-bottom: 8px; }
.teacher-score { margin-bottom: 8px; display: flex; justify-content: center; }
.teacher-desc { color: #666; font-size: 13px; line-height: 1.5; }
</style>
