<template>
  <div class="page">
    <NavBar />
    <div class="page-content">
      <h2 class="page-title">课程中心</h2>
      <el-row :gutter="20">
        <el-col :span="8" v-for="course in courses" :key="course.id" style="margin-bottom: 20px">
          <el-card shadow="hover" class="course-card" @click="router.push(`/course/${course.id}`)">
            <div class="course-img" :style="{ background: getColor(course.id) }">
              <el-icon :size="48" color="#fff"><Reading /></el-icon>
            </div>
            <div class="course-info">
              <h3>{{ course.name }}</h3>
              <p class="course-desc">{{ course.description || '暂无描述' }}</p>
              <div class="course-meta">
                <span>¥{{ course.price }}</span>
                <el-tag size="small" :type="course.stock > 0 ? 'danger' : 'info'">
                  {{ course.stock > 0 ? '秒杀中' : '已售罄' }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <div class="pagination-wrap" v-if="total > 10">
        <el-pagination
          v-model:current-page="current"
          :page-size="10"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadCourses"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
import { courseApi } from '@/api'

const router = useRouter()
const courses = ref([])
const current = ref(1)
const total = ref(0)

const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb']
const getColor = (id) => colors[id % colors.length]

const loadCourses = async () => {
  const res = await courseApi.getList(null, current.value)
  if (res.success) {
    courses.value = res.data
    total.value = res.total || 0
  }
}

onMounted(loadCourses)
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; }
.page-content { max-width: 1200px; margin: 0 auto; padding: 24px; }
.page-title { font-size: 22px; margin-bottom: 20px; }
.course-card { cursor: pointer; transition: transform 0.2s; border-radius: 8px; overflow: hidden; }
.course-card:hover { transform: translateY(-4px); }
.course-img { height: 140px; display: flex; align-items: center; justify-content: center; }
.course-info { padding: 12px 0; }
.course-info h3 { font-size: 16px; margin-bottom: 6px; }
.course-desc { color: #999; font-size: 13px; margin-bottom: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.course-meta { display: flex; justify-content: space-between; align-items: center; }
.course-meta span { color: #f56c6c; font-weight: bold; font-size: 18px; }
.pagination-wrap { text-align: center; margin-top: 20px; }
</style>
