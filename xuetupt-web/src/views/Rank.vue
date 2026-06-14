<template>
  <div class="page">
    <NavBar />
    <div class="page-content">
      <h2 class="page-title">
        <el-icon color="#409eff"><TrendCharts /></el-icon>
        专注排行榜
      </h2>

      <el-tabs v-model="activeTab" @tab-change="loadRank">
        <el-tab-pane label="日榜" name="daily">
          <el-table :data="rankList" stripe style="width: 100%" v-if="rankList.length > 0">
            <el-table-column label="排名" width="80">
              <template #default="{ row }">
                <span class="rank-badge" :class="getRankClass(row.rank)">{{ row.rank }}</span>
              </template>
            </el-table-column>
            <el-table-column label="用户" min-width="120">
              <template #default="{ row }">
                <div class="user-cell">
                  <el-avatar :size="28" icon="UserFilled" />
                  <span>用户 {{ row.userId }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="专注时长" width="160">
              <template #default="{ row }">
                <div class="duration-cell">
                  <span class="duration-value">{{ formatDuration(row.score) }}</span>
                  <el-progress
                    :percentage="getPercentage(row.score)"
                    :color="getProgressColor(row.rank)"
                    :show-text="false"
                    :stroke-width="8"
                    class="duration-progress"
                  />
                </div>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-state">
            <el-empty description="暂无排行数据" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="周榜" name="weekly">
          <el-table :data="rankList" stripe style="width: 100%" v-if="rankList.length > 0">
            <el-table-column label="排名" width="80">
              <template #default="{ row }">
                <span class="rank-badge" :class="getRankClass(row.rank)">{{ row.rank }}</span>
              </template>
            </el-table-column>
            <el-table-column label="用户" min-width="120">
              <template #default="{ row }">
                <div class="user-cell">
                  <el-avatar :size="28" icon="UserFilled" />
                  <span>用户 {{ row.userId }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="专注时长" width="160">
              <template #default="{ row }">
                <div class="duration-cell">
                  <span class="duration-value">{{ formatDuration(row.score) }}</span>
                  <el-progress
                    :percentage="getPercentage(row.score)"
                    :color="getProgressColor(row.rank)"
                    :show-text="false"
                    :stroke-width="8"
                    class="duration-progress"
                  />
                </div>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-state">
            <el-empty description="暂无排行数据" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="总榜" name="total">
          <el-table :data="rankList" stripe style="width: 100%" v-if="rankList.length > 0">
            <el-table-column label="排名" width="80">
              <template #default="{ row }">
                <span class="rank-badge" :class="getRankClass(row.rank)">{{ row.rank }}</span>
              </template>
            </el-table-column>
            <el-table-column label="用户" min-width="120">
              <template #default="{ row }">
                <div class="user-cell">
                  <el-avatar :size="28" icon="UserFilled" />
                  <span>用户 {{ row.userId }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="专注时长" width="160">
              <template #default="{ row }">
                <div class="duration-cell">
                  <span class="duration-value">{{ formatDuration(row.score) }}</span>
                  <el-progress
                    :percentage="getPercentage(row.score)"
                    :color="getProgressColor(row.rank)"
                    :show-text="false"
                    :stroke-width="8"
                    class="duration-progress"
                  />
                </div>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-state">
            <el-empty description="暂无排行数据" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import NavBar from '@/components/NavBar.vue'
import { studyRoomApi } from '@/api'
import { TrendCharts, UserFilled } from '@element-plus/icons-vue'

const activeTab = ref('daily')
const rankList = ref([])
const maxScore = ref(1)

const getRankClass = (rank) => {
  if (rank === 1) return 'rank-gold'
  if (rank === 2) return 'rank-silver'
  if (rank === 3) return 'rank-bronze'
  return ''
}

const getProgressColor = (rank) => {
  if (rank === 1) return '#ffd700'
  if (rank === 2) return '#c0c0c0'
  if (rank === 3) return '#cd7f32'
  return '#409eff'
}

const getPercentage = (score) => {
  return Math.min(100, ((score || 0) / maxScore.value) * 100)
}

const formatDuration = (minutes) => {
  if (!minutes) return '0 分钟'
  const h = Math.floor(minutes / 60)
  const m = Math.floor(minutes % 60)
  return h > 0 ? `${h}小时${m}分钟` : `${m}分钟`
}

const loadRank = async () => {
  let res
  if (activeTab.value === 'daily') res = await studyRoomApi.getDailyRank()
  else if (activeTab.value === 'weekly') res = await studyRoomApi.getWeeklyRank()
  else res = await studyRoomApi.getTotalRank()

  if (res.success) {
    rankList.value = res.data || []
    if (rankList.value.length > 0) {
      maxScore.value = Math.max(...rankList.value.map(i => i.score || 0), 1)
    }
  }
}

onMounted(loadRank)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.page-title {
  font-size: 22px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 13px;
  font-weight: bold;
  color: #666;
  background: #f0f0f0;
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

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.duration-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.duration-value {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
  min-width: 80px;
}

.duration-progress {
  flex: 1;
  max-width: 120px;
}

.empty-state {
  padding: 40px 0;
}
</style>
