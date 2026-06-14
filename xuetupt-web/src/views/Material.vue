<template>
  <div class="page">
    <NavBar />
    <div class="page-content">
      <h2 class="page-title">学习资料</h2>
      <el-row :gutter="20">
        <el-col :span="8" v-for="item in materials" :key="item.id" style="margin-bottom: 20px">
          <el-card shadow="hover" class="material-card">
            <div class="material-icon">
              <el-icon :size="36" color="#67c23a"><FolderOpened /></el-icon>
            </div>
            <h3>{{ item.name }}</h3>
            <p class="material-desc">{{ item.description || '暂无描述' }}</p>
            <div class="material-meta">
              <span>¥{{ item.price }}</span>
              <el-button size="small" type="success" :disabled="!item.stock || item.stock <= 0" @click="handleBuy(item)">
                {{ item.stock > 0 ? '立即购买' : '已售罄' }}
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import NavBar from '@/components/NavBar.vue'
import { materialApi } from '@/api'

const materials = ref([])

const handleBuy = async (item) => {
  const res = await materialApi.seckill(item.id)
  if (res.success) {
    ElMessage.success('购买成功，可在订单中查看')
  }
}

onMounted(async () => {
  const res = await materialApi.getList(null, 1)
  if (res.success) materials.value = res.data
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; }
.page-content { max-width: 1200px; margin: 0 auto; padding: 24px; }
.page-title { font-size: 22px; margin-bottom: 20px; }
.material-card { text-align: center; padding: 20px; }
.material-icon { margin-bottom: 12px; }
.material-card h3 { font-size: 16px; margin-bottom: 8px; }
.material-desc { color: #999; font-size: 13px; margin-bottom: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.material-meta { display: flex; justify-content: space-between; align-items: center; }
.material-meta span { color: #f56c6c; font-weight: bold; font-size: 18px; }
</style>
