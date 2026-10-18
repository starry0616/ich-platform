<template>
  <div v-loading="loading">
    <!-- 概览卡片 -->
    <el-row :gutter="14">
      <el-col :xs="12" :sm="8" :md="4" v-for="card in cards" :key="card.label">
        <el-card shadow="never" class="stat-card">
          <div class="stat-num" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图（用简单柱状列表呈现，避免引入图表库） -->
    <el-row :gutter="14" style="margin-top: 14px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><b>近 7 日注册趋势</b></template>
          <el-empty v-if="!trends.length" description="暂无数据" :image-size="60" />
          <div v-for="t in trends" :key="t.date" class="trend-row">
            <span class="trend-date">{{ t.date }}</span>
            <div class="trend-bar-wrap">
              <div class="trend-bar" :style="{ width: barWidth(t.value) }"></div>
              <span class="trend-val">{{ t.value }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><b>门类分布</b></template>
          <el-empty v-if="!categoryStats.length" description="暂无数据" :image-size="60" />
          <div v-for="c in categoryStats" :key="c.categoryName || c.name" class="trend-row">
            <span class="trend-date">{{ c.categoryName || c.name }}</span>
            <div class="trend-bar-wrap">
              <div class="trend-bar bar-brown" :style="{ width: barWidth(c.count || c.value) }"></div>
              <span class="trend-val">{{ c.count || c.value }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热门活动 -->
    <el-card shadow="never" style="margin-top: 14px">
      <template #header><b>热门活动 Top</b></template>
      <el-table :data="topActivities" empty-text="暂无数据">
        <el-table-column label="活动" prop="title" min-width="200" />
        <el-table-column label="报名人数" width="120" prop="signupCount" />
        <el-table-column label="名额" width="110" prop="quota" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getStatsOverview, getTrends, getCategoryStats, getTopActivities } from '@/api/admin'

const loading = ref(false)
const overview = ref(null)
const trends = ref([])
const categoryStats = ref([])
const topActivities = ref([])

const cards = computed(() => {
  const o = overview.value || {}
  return [
    { label: '注册用户', value: o.users ?? 0, color: '#409EFF' },
    { label: '非遗项目', value: o.projects ?? 0, color: '#E6A23C' },
    { label: '传承人', value: o.inheritors ?? 0, color: '#F56C6C' },
    { label: '资讯文章', value: o.articles ?? 0, color: '#67C23A' },
    { label: '活动', value: o.activities ?? 0, color: '#909399' },
    { label: '累计评论', value: o.comments ?? 0, color: '#8b4513' }
  ]
})

function barWidth(v) {
  const max = Math.max(1, ...trends.value.map((t) => t.value || 0), ...categoryStats.value.map((c) => c.count || c.value || 0))
  return Math.max(2, Math.round(((v || 0) / max) * 100)) + '%'
}

onMounted(async () => {
  loading.value = true
  try {
    const [ov, tr, cs, ta] = await Promise.all([
      getStatsOverview(),
      getTrends({ days: 7 }),
      getCategoryStats(),
      getTopActivities({ limit: 5 })
    ])
    overview.value = ov.data || {}
    const trendData = (tr.data && tr.data.data) || tr.data || {}
    const days = trendData.days || []
    const registers = trendData.registers || []
    trends.value = days.map((d, i) => ({ date: d, value: registers[i] || 0 }))
    categoryStats.value = (cs.data && (cs.data.records || cs.data)) || []
    topActivities.value = (ta.data && (ta.data.records || ta.data)) || []
  } catch (e) {
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-card {
  text-align: center;
  padding: 6px 0;
}
.stat-num {
  font-size: 28px;
  font-weight: bold;
}
.stat-label {
  color: #999;
  font-size: 13px;
  margin-top: 4px;
}
.trend-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}
.trend-date {
  width: 90px;
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 0;
}
.trend-bar-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}
.trend-bar {
  height: 14px;
  background: #409EFF;
  border-radius: 3px;
  min-width: 2px;
  transition: width 0.3s;
}
.bar-brown {
  background: #8b4513;
}
.trend-val {
  font-size: 12px;
  color: #666;
  width: 30px;
}
</style>
