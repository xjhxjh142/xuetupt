<template>
  <div class="home-page">
    <NavBar />
    <div class="home-content">
      <!-- 欢迎横幅 -->
      <el-card class="welcome-card" shadow="never">
        <h2>欢迎来到学途</h2>
        <p>在线学习平台 · 专注提升 · 成就未来</p>
      </el-card>

      <!-- 功能入口 -->
      <el-row :gutter="20" class="feature-row">
        <el-col :span="6" v-for="item in features" :key="item.path">
          <el-card shadow="hover" class="feature-card" @click="router.push(item.path)">
            <el-icon :size="40" :color="item.color"><component :is="item.icon" /></el-icon>
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
          </el-card>
        </el-col>
      </el-row>

      <!-- 快捷操作 -->
      <el-row :gutter="20">
        <!-- 热门课程 -->
        <el-col :span="14">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span><el-icon color="#e6a23c"><StarFilled /></el-icon> 热门课程</span>
                <el-button text type="primary" @click="router.push('/course')">查看更多</el-button>
              </div>
            </template>
            <div v-if="courses.length === 0" class="empty-tip">暂无热门课程</div>
            <div v-for="course in courses" :key="course.id" class="hot-course-item" @click="router.push(`/course/${course.id}`)">
              <div class="course-info">
                <span class="course-title">{{ course.title }}</span>
                <span class="course-subject">{{ subjectMap[course.subject] || course.subject }}</span>
              </div>
              <div class="course-meta">
                <span class="course-price">¥{{ course.price }}</span>
                <el-tag v-if="course.stock > 0 && course.stock <= 10" size="small" type="danger" effect="dark">仅剩{{ course.stock }}个</el-tag>
                <el-tag v-else-if="course.stock > 10" size="small" type="warning">秒杀中</el-tag>
                <el-tag v-else size="small" type="info">已售罄</el-tag>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 专注排行榜 -->
        <el-col :span="10">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span><el-icon color="#409eff"><TrendCharts /></el-icon> 专注排行榜</span>
                <el-button text type="primary" @click="router.push('/rank')">查看更多</el-button>
              </div>
            </template>
            <div v-if="rankList.length === 0" class="empty-tip">暂无排行数据</div>
            <div v-for="(item, index) in rankList" :key="index" class="rank-item">
              <div class="rank-left">
                <span class="rank-badge" :class="getRankClass(index)">{{ index + 1 }}</span>
                <span class="rank-user">用户 {{ item.userId }}</span>
              </div>
              <div class="rank-right">
                <span class="rank-score">{{ formatDuration(item.score) }}</span>
                <el-progress
                  :percentage="Math.min(100, (item.score / maxScore) * 100)"
                  :color="getProgressColor(index)"
                  :show-text="false"
                  :stroke-width="6"
                  class="rank-progress"
                />
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 底部快捷入口 -->
      <el-row :gutter="20" class="quick-row">
        <el-col :span="8">
          <el-card shadow="hover" class="quick-card" @click="router.push('/study-room')">
            <el-icon :size="28" color="#409eff"><Clock /></el-icon>
            <div class="quick-text">
              <span class="quick-title">开始专注</span>
              <span class="quick-desc">进入自习室学习</span>
            </div>
            <el-icon color="#c0c4cc"><ArrowRight /></el-icon>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="quick-card" @click="router.push('/ai-tutor')">
            <el-icon :size="28" color="#67c23a"><MagicStick /></el-icon>
            <div class="quick-text">
              <span class="quick-title">AI 辅导</span>
              <span class="quick-desc">苏格拉底式教学</span>
            </div>
            <el-icon color="#c0c4cc"><ArrowRight /></el-icon>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="quick-card" @click="handleSign">
            <el-icon :size="28" :color="signed ? '#909399' : '#e6a23c'"><Select /></el-icon>
            <div class="quick-text">
              <span class="quick-title">{{ signed ? '已签到' : '每日签到' }}</span>
              <span class="quick-desc">{{ signed ? `连续 ${continuousDays} 天` : '签到领积分' }}</span>
            </div>
            <el-icon color="#c0c4cc"><ArrowRight /></el-icon>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
import { courseApi, studyRoomApi } from '@/api'
import { StarFilled, TrendCharts, Clock, MagicStick, Select, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const features = [
  { title: '课程中心', desc: '精选优质课程', icon: 'Reading', color: '#409eff', path: '/course' },
  { title: '学习资料', desc: '海量学习资源', icon: 'FolderOpened', color: '#67c23a', path: '/material' },
  { title: '名师团队', desc: '专业师资力量', icon: 'UserFilled', color: '#e6a23c', path: '/teacher' },
  { title: '自习室', desc: '专注学习空间', icon: 'Clock', color: '#f56c6c', path: '/study-room' }
]

const subjectMap = {
  math: '数学', phys: '物理', chem: '化学',
  bio: '生物', eng: '英语', chn: '语文'
}

const courses = ref([])
const rankList = ref([])
const maxScore = ref(1)
const signed = ref(false)
const continuousDays = ref(0)

const getRankClass = (index) => {
  if (index === 0) return 'rank-gold'
  if (index === 1) return 'rank-silver'
  if (index === 2) return 'rank-bronze'
  return ''
}

const getProgressColor = (index) => {
  if (index === 0) return '#ffd700'
  if (index === 1) return '#c0c0c0'
  if (index === 2) return '#cd7f32'
  return '#409eff'
}

const formatDuration = (minutes) => {
  if (!minutes) return '0 分钟'
  const h = Math.floor(minutes / 60)
  const m = Math.floor(minutes % 60)
  return h > 0 ? `${h}小时${m}分钟` : `${m}分钟`
}

const handleSign = async () => {
  if (signed.value) {
    ElMessage.info('今日已签到')
    return
  }
  const res = await studyRoomApi.sign()
  if (res.success) {
    signed.value = true
    continuousDays.value++
    ElMessage.success('签到成功！')
  } else {
    ElMessage.warning(res.message || '签到失败')
  }
}

onMounted(async () => {
  // 加载热门课程
  const courseRes = await courseApi.getHotList()
  if (courseRes.success) courses.value = courseRes.data || []

  // 加载排行榜
  const rankRes = await studyRoomApi.getTotalRank()
  if (rankRes.success) {
    rankList.value = (rankRes.data || []).slice(0, 10)
    if (rankList.value.length > 0) {
      maxScore.value = Math.max(...rankList.value.map(i => i.score || 0), 1)
    }
  }

  // 加载签到状态
  const signRes = await studyRoomApi.getSignStatus()
  if (signRes.success) {
    signed.value = signRes.data.signed
    continuousDays.value = signRes.data.continuousDays
  }
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.home-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.welcome-card {
  text-align: center;
  padding: 40px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  color: #fff;
  border: none;
  border-radius: 12px;
}

.welcome-card h2 {
  font-size: 28px;
  margin-bottom: 8px;
}

.feature-row {
  margin-bottom: 24px;
}

.feature-card {
  text-align: center;
  padding: 20px;
  cursor: pointer;
  transition: transform 0.2s;
}

.feature-card:hover {
  transform: translateY(-4px);
}

.feature-card h3 {
  margin: 12px 0 8px;
  font-size: 16px;
}

.feature-card p {
  color: #999;
  font-size: 13px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header span {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: bold;
  font-size: 15px;
}

/* 热门课程样式 */
.hot-course-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.hot-course-item:last-child {
  border-bottom: none;
}

.hot-course-item:hover {
  background: #fafafa;
}

.course-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.course-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.course-subject {
  font-size: 12px;
  color: #909399;
}

.course-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.course-price {
  font-size: 16px;
  font-weight: bold;
  color: #f56c6c;
}

/* 排行榜样式 */
.rank-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 100px;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: bold;
  color: #666;
  background: #f0f0f0;
  flex-shrink: 0;
}

.rank-gold {
  background: linear-gradient(135deg, #ffd700, #ffb700);
  color: #fff;
}

.rank-silver {
  background: linear-gradient(135deg, #e8e8e8, #c0c0c0);
  color: #fff;
}

.rank-bronze {
  background: linear-gradient(135deg, #e8a87c, #cd7f32);
  color: #fff;
}

.rank-user {
  font-size: 13px;
  color: #606266;
}

.rank-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  max-width: 180px;
}

.rank-score {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  min-width: 70px;
  text-align: right;
}

.rank-progress {
  flex: 1;
}

/* 底部快捷入口 */
.quick-row {
  margin-top: 24px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  cursor: pointer;
  transition: transform 0.2s;
}

.quick-card:hover {
  transform: translateY(-2px);
}

.quick-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.quick-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.quick-desc {
  font-size: 12px;
  color: #909399;
}

.empty-tip {
  text-align: center;
  padding: 20px;
  color: #c0c4cc;
  font-size: 14px;
}
</style>
