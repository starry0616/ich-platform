<template>
  <div class="page-container">
    <!-- 页头 -->
    <div class="page-head">
      <h2>非遗项目</h2>
      <p>国家级、省级非物质文化遗产代表性项目名录</p>
    </div>

    <!-- 筛选栏 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-input
          v-model="query.keyword"
          placeholder="搜索项目名称 / 地区 / 保护单位"
          clearable
          class="search-input"
          @keyup.enter="load(1)"
          @clear="load(1)"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.categoryId" placeholder="全部门类" clearable class="filter-select" @change="load(1)">
          <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
        </el-select>
        <el-select v-model="query.level" placeholder="全部级别" clearable class="filter-select" @change="load(1)">
          <el-option label="国家级" :value="1" />
          <el-option label="省级" :value="2" />
          <el-option label="市级" :value="3" />
          <el-option label="县级" :value="4" />
        </el-select>
        <el-select v-model="query.sort" placeholder="排序" clearable class="filter-select" @change="load(1)">
          <el-option label="最新公布" value="year" />
          <el-option label="最多浏览" value="views" />
          <el-option label="最多收藏" value="favorites" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
      </div>
    </el-card>

    <!-- 项目列表 -->
    <el-row :gutter="16" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="8" v-for="p in projects" :key="p.id">
        <router-link :to="`/projects/${p.id}`" class="project-card">
          <el-image :src="resolveUrl(p.coverUrl)" fit="cover" class="cover">
            <template #error>
              <div class="img-ph"><el-icon><Picture /></el-icon></div>
            </template>
          </el-image>
          <div class="card-body">
            <div class="title-row">
              <span class="name">{{ p.projectName }}</span>
              <el-tag size="small" :type="levelTag(p.level)" effect="plain">{{ levelText(p.level) }}</el-tag>
            </div>
            <div class="summary">{{ p.summary }}</div>
            <div class="meta">
              <el-tag size="small" type="warning" effect="plain">{{ p.categoryName }}</el-tag>
              <span class="meta-text"><el-icon><View /></el-icon> {{ p.viewCount || 0 }}</span>
              <span class="meta-text"><el-icon><Star /></el-icon> {{ p.favoriteCount || 0 }}</span>
            </div>
          </div>
        </router-link>
      </el-col>
    </el-row>

    <!-- 请求失败：明确提示并提供重试，避免误显示为“暂无数据” -->
    <el-result
      v-if="!loading && loadFailed"
      icon="warning"
      :title="loadError || '加载失败'"
      sub-title="网络异常或服务暂时不可用，请检查连接后重试"
    >
      <template #extra>
        <el-button type="primary" @click="load()">重新加载</el-button>
      </template>
    </el-result>
    <!-- 请求成功但结果为空 -->
    <el-empty
      v-else-if="!loading && !projects.length"
      description="暂无符合条件的非遗项目"
    />

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
import { useRoute } from 'vue-router'
import { getProjectList, getCategoryList } from '@/api/project'
import { resolveUrl } from '@/utils/format'

const route = useRoute()
const categories = ref([])
const projects = ref([])
const total = ref(0)
const loading = ref(false)
// 区分“请求失败”与“成功但结果为空”
const loadFailed = ref(false)
const loadError = ref('')

const query = reactive({
  page: 1,
  size: 12,
  keyword: '',
  categoryId: route.query.categoryId || undefined,
  level: undefined,
  sort: undefined
})

function levelText(l) {
  return { 1: '国家级', 2: '省级', 3: '市级', 4: '县级' }[l] || ''
}
function levelTag(l) {
  return { 1: 'danger', 2: 'warning', 3: 'primary', 4: 'info' }[l] || 'info'
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  loadFailed.value = false
  loadError.value = ''
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.categoryId) params.categoryId = query.categoryId
    if (query.level) params.level = query.level
    if (query.sort) params.sort = query.sort
    const res = await getProjectList(params)
    const d = res.data || {}
    projects.value = d.records || []
    total.value = d.total || 0
  } catch (e) {
    projects.value = []
    total.value = 0
    loadFailed.value = true
    loadError.value = (e && e.message) || '网络异常，加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch (e) {}
  load(1)
})
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
.project-card {
  display: block;
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(139, 69, 19, 0.08);
  margin-bottom: 16px;
  transition: transform 0.35s ease, box-shadow 0.35s ease;
}
.project-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 28px rgba(139, 69, 19, 0.16);
}
.cover {
  width: 100%;
  height: 170px;
  display: block;
  overflow: hidden;
}
.cover :deep(img) {
  transition: transform 0.6s ease;
}
.project-card:hover .cover :deep(img) {
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
  font-size: 30px;
}
.card-body {
  padding: 12px;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.name {
  font-weight: 600;
  font-size: 15px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.summary {
  color: #888;
  font-size: 13px;
  min-height: 38px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
}
.meta {
  display: flex;
  align-items: center;
  gap: 10px;
}
.meta-text {
  display: flex;
  align-items: center;
  gap: 3px;
  color: #aaa;
  font-size: 12px;
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
