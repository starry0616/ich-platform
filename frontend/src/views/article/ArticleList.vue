<template>
  <div class="page-container">
    <div class="page-head">
      <h2>非遗资讯</h2>
      <p>非遗保护动态、政策法规与传承故事</p>
    </div>

    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-input v-model="query.keyword" placeholder="搜索资讯标题" clearable class="search-input" @keyup.enter="load(1)" @clear="load(1)">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.categoryId" placeholder="全部栏目" clearable class="filter-select" @change="load(1)">
          <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
        </el-select>
        <el-button type="primary" @click="load(1)">查询</el-button>
      </div>
    </el-card>

    <div v-loading="loading">
      <router-link v-for="a in articles" :key="a.id" :to="`/articles/${a.id}`" class="article-item">
        <el-image :src="resolveUrl(a.coverUrl)" fit="cover" class="cover">
          <template #error><div class="img-ph"><el-icon><Picture /></el-icon></div></template>
        </el-image>
        <div class="body">
          <div class="title-row">
            <h3 class="title">{{ a.title }}</h3>
            <el-tag v-if="a.isTop === 1" type="danger" size="small">置顶</el-tag>
          </div>
          <p class="summary">{{ a.summary }}</p>
          <div class="meta">
            <span>{{ a.authorName || '平台' }}</span>
            <span>{{ a.categoryName }}</span>
            <span>{{ formatDate(a.publishTime) }}</span>
            <span class="views"><el-icon><View /></el-icon> {{ a.viewCount || 0 }}</span>
          </div>
        </div>
      </router-link>
    </div>

    <el-empty v-if="!loading && !articles.length" description="暂无资讯" />

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
import { getArticleList, getArticleCategories } from '@/api/article'
import { resolveUrl, formatDate } from '@/utils/format'

const articles = ref([])
const categories = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', categoryId: undefined })

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.categoryId) params.categoryId = query.categoryId
    const res = await getArticleList(params)
    const d = res.data || {}
    articles.value = d.records || []
    total.value = d.total || 0
  } catch (e) {
    articles.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getArticleCategories()
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
.article-item {
  display: flex;
  gap: 16px;
  background: #fff;
  border-radius: 14px;
  padding: 14px;
  margin-bottom: 14px;
  box-shadow: 0 2px 10px rgba(139, 69, 19, 0.08);
  transition: transform 0.35s ease, box-shadow 0.35s ease;
}
.article-item:hover {
  box-shadow: 0 12px 28px rgba(139, 69, 19, 0.16);
  transform: translateY(-4px);
}
.cover {
  width: 180px;
  height: 120px;
  border-radius: 10px;
  flex-shrink: 0;
  overflow: hidden;
}
.cover :deep(img) {
  transition: transform 0.6s ease;
}
.article-item:hover .cover :deep(img) {
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
}
.body {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.title-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.title {
  font-size: 17px;
  color: #333;
  font-weight: 600;
}
.article-item:hover .title {
  color: #8b4513;
}
.summary {
  color: #888;
  font-size: 13px;
  line-height: 1.6;
  margin: 6px 0;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.meta {
  display: flex;
  gap: 14px;
  color: #bbb;
  font-size: 12px;
  align-items: center;
}
.views {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 3px;
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
