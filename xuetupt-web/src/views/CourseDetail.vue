<template>
  <div class="page">
    <NavBar />
    <div class="page-content">
      <el-card v-if="course" shadow="never">
        <template #header>
          <el-button text @click="router.back()">< 返回</el-button>
          <span style="margin-left: 12px">{{ course.name }}</span>
        </template>

        <div class="detail-body">
          <div class="detail-img" :style="{ background: getColor(course.id) }">
            <el-icon :size="64" color="#fff"><Reading /></el-icon>
          </div>

          <div class="detail-info">
            <h2>{{ course.name }}</h2>
            <p class="desc">{{ course.description || '暂无描述' }}</p>
            <div class="meta">
              <div class="meta-item">
                <span class="label">价格</span>
                <span class="value price">¥{{ course.price }}</span>
              </div>
              <div class="meta-item">
                <span class="label">库存</span>
                <span class="value">{{ course.stock || 0 }}</span>
              </div>
              <div class="meta-item">
                <span class="label">类型</span>
                <span class="value">{{ course.type === 1 ? '录播' : '直播' }}</span>
              </div>
            </div>

            <div class="actions">
              <el-button type="danger" size="large" :disabled="!course.stock || course.stock <= 0" @click="handleSeckill" :loading="seckilling">
                {{ course.stock > 0 ? '立即秒杀 ¥' + course.price : '已售罄' }}
              </el-button>
            </div>
          </div>
        </div>
      </el-card>

      <el-empty v-else description="课程不存在" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '@/components/NavBar.vue'
import { courseApi } from '@/api'

const route = useRoute()
const router = useRouter()
const course = ref(null)
const seckilling = ref(false)

const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb']
const getColor = (id) => colors[id % colors.length]

const handleSeckill = async () => {
  seckilling.value = true
  try {
    const res = await courseApi.seckill(course.value.id)
    if (res.success) {
      ElMessage.success(res.data || '预约成功')
    }
  } finally {
    seckilling.value = false
  }
}

onMounted(async () => {
  const res = await courseApi.getById(route.params.id)
  if (res.success) course.value = res.data
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; }
.page-content { max-width: 900px; margin: 0 auto; padding: 24px; }
.detail-body { display: flex; gap: 32px; padding: 20px 0; }
.detail-img { width: 300px; height: 200px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.detail-info { flex: 1; }
.detail-info h2 { font-size: 24px; margin-bottom: 12px; }
.desc { color: #666; line-height: 1.6; margin-bottom: 20px; }
.meta { display: flex; gap: 40px; margin-bottom: 24px; }
.meta-item .label { display: block; color: #999; font-size: 13px; margin-bottom: 4px; }
.meta-item .value { font-size: 16px; font-weight: bold; }
.price { color: #f56c6c; font-size: 24px; }
.actions { margin-top: 20px; }
</style>
