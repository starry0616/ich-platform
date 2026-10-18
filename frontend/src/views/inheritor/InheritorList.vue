<template>
  <div class="page-container">
    <div class="page-head">
      <h2>非遗代表性传承人</h2>
      <p>匠心守护，薪火相传 —— 走近非遗传承人</p>
    </div>

    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-input v-model="query.keyword" placeholder="搜索姓名 / 地区 / 项目" clearable class="search-input" @keyup.enter="load(1)" @clear="load(1)">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.level" placeholder="全部级别" clearable class="filter-select" @change="load(1)">
          <el-option label="国家级" :value="1" />
          <el-option label="省级" :value="2" />
          <el-option label="市级" :value="3" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="8" v-for="i in inheritors" :key="i.id">
        <router-link :to="`/inheritors/${i.id}`" class="inh-card">
          <div class="card-head">
            <el-avatar :size="56" class="avatar">{{ (i.name || '?').charAt(0) }}</el-avatar>
            <div class="head-info">
              <div class="name">{{ i.name }}</div>
              <div class="sub">{{ i.projectName }}</div>
            </div>
            <el-tag size="small" :type="levelTag(i.level)" effect="plain">{{ levelText(i.level) }}</el-tag>
          </div>
          <div class="card-body">
            <p class="intro">{{ i.intro }}</p>
            <div class="meta">
              <span>从业 {{ i.craftYears || 0 }} 年</span>
              <span>{{ i.viewCount || 0 }} 次浏览</span>
            </div>
          </div>
        </router-link>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && !inheritors.length" description="暂无传承人数据" />

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
import { getInheritorList } from '@/api/inheritor'

const inheritors = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 12, keyword: '', level: undefined })

function levelText(l) {
  return { 1: '国家级', 2: '省级', 3: '市级', 4: '县级' }[l] || ''
}
function levelTag(l) {
  return { 1: 'danger', 2: 'warning', 3: 'primary', 4: 'info' }[l] || 'info'
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.level) params.level = query.level
    const res = await getInheritorList(params)
    const d = res.data || {}
    inheritors.value = d.records || []
    total.value = d.total || 0
  } catch (e) {
    inheritors.value = []
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
.search-input {
  width: 320px;
}
.filter-select {
  width: 150px;
}
.inh-card {
  display: block;
  background: #fff;
  border-radius: 14px;
  padding: 18px;
  box-shadow: 0 2px 10px rgba(139, 69, 19, 0.08);
  margin-bottom: 16px;
  transition: transform 0.35s ease, box-shadow 0.35s ease;
}
.inh-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 28px rgba(139, 69, 19, 0.16);
}
.card-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.avatar {
  background: #8b4513;
  color: #fff;
  font-size: 22px;
  font-weight: bold;
  flex-shrink: 0;
}
.head-info {
  flex: 1;
  min-width: 0;
}
.name {
  font-weight: 700;
  font-size: 17px;
  color: #333;
}
.sub {
  color: #999;
  font-size: 13px;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-body {
  margin-top: 12px;
  border-top: 1px solid #f5f2ec;
  padding-top: 10px;
}
.intro {
  color: #777;
  font-size: 13px;
  line-height: 1.7;
  min-height: 44px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.meta {
  display: flex;
  justify-content: space-between;
  color: #bbb;
  font-size: 12px;
  margin-top: 8px;
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
