<template>
  <div class="page-container">
    <div class="page-head">
      <h2>非遗活动</h2>
      <p>非遗展演、体验课堂、技艺传习与展览讲座</p>
    </div>

    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-select v-model="query.activityType" placeholder="全部类型" clearable class="filter-select" @change="load(1)">
          <el-option label="展演展示" :value="1" />
          <el-option label="体验课堂" :value="2" />
          <el-option label="技艺传习" :value="3" />
          <el-option label="展览讲座" :value="4" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable class="filter-select" @change="load(1)">
          <el-option label="报名中" :value="1" />
          <el-option label="进行中" :value="2" />
          <el-option label="已结束" :value="3" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="8" v-for="a in activities" :key="a.id">
        <router-link :to="`/activities/${a.id}`" class="act-card">
          <el-image :src="resolveUrl(a.coverUrl)" fit="cover" class="cover">
            <template #error><div class="img-ph"><el-icon><Picture /></el-icon></div></template>
          </el-image>
          <div class="body">
            <div class="title">{{ a.title }}</div>
            <div class="time">
              <el-icon><Clock /></el-icon>
              <span>{{ formatDate(a.startTime) }}</span>
            </div>
            <div class="addr">
              <el-icon><Location /></el-icon>
              <span>{{ a.address }}</span>
            </div>
            <div class="foot">
              <el-tag size="small" :type="statusTag(a.status)">{{ statusText(a.status) }}</el-tag>
              <span class="quota">{{ a.signed || 0 }} / {{ a.quota || '不限' }} 已报名</span>
            </div>
          </div>
        </router-link>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && !activities.length" description="暂无活动" />

    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="load()"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getActivityList } from '@/api/activity'
import { resolveUrl, formatDate } from '@/utils/format'

const activities = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 9, activityType: undefined, status: undefined })

function statusText(s) {
  return { 1: '报名中', 2: '进行中', 3: '已结束', 0: '待审核' }[s] || '未知'
}
function statusTag(s) {
  return { 1: 'success', 2: 'primary', 3: 'info', 0: 'warning' }[s] || 'info'
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.activityType) params.activityType = query.activityType
    if (query.status) params.status = query.status
    const res = await getActivityList(params)
    const d = res.data || {}
    activities.value = d.records || []
    total.value = d.total || 0
  } catch (e) {
    activities.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.page-head {
  margin-bottom: 18px;
}
.page-head h2 {
  font-size: 26px;
  color: #5a4632;
}
.page-head p {
  color: #999;
  font-size: 14px;
  margin-top: 4px;
}
.filter-card {
  margin-bottom: 18px;
}
.filter-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.filter-select {
  width: 160px;
}
.act-card {
  display: block;
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(139, 69, 19, 0.08);
  margin-bottom: 16px;
  transition: transform 0.35s ease, box-shadow 0.35s ease;
}
.act-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 28px rgba(139, 69, 19, 0.16);
}
.cover {
  width: 100%;
  height: 150px;
  display: block;
  overflow: hidden;
}
.cover :deep(img) {
  transition: transform 0.6s ease;
}
.act-card:hover .cover :deep(img) {
  transform: scale(1.08);
}
.img-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0ece4;
  color: #c9b79c;
  font-size: 28px;
}
.body {
  padding: 12px;
}
.title {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.time,
.addr {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #888;
  font-size: 12px;
  margin-bottom: 6px;
}
.foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.quota {
  color: #999;
  font-size: 12px;
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
