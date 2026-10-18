<template>
  <div>
    <!-- 轮播图 -->
    <el-carousel v-if="banners.length" height="420px" class="banner">
      <el-carousel-item v-for="b in banners" :key="b.id">
        <a :href="b.linkUrl || 'javascript:void(0)'" target="_blank" class="banner-item">
          <img :src="resolveUrl(b.imageUrl)" :alt="b.title" class="banner-img" />
          <div class="banner-mask">
            <h2>{{ b.title }}</h2>
            <p v-if="b.subtitle">{{ b.subtitle }}</p>
          </div>
        </a>
      </el-carousel-item>
    </el-carousel>
    <div v-else class="banner banner-fallback">
      <div class="banner-mask">
        <h2>非遗文化传承与互动平台</h2>
        <p>让非遗走进生活，让传承生生不息</p>
      </div>
    </div>

    <div class="page-container">
      <!-- 数据概览 -->
      <el-card class="stats-card" shadow="never">
        <div class="stats-grid" v-if="stats">
          <div class="stat-item" v-for="(label, key) in statLabels" :key="key">
            <div class="stat-num">{{ displayStats[key] }}</div>
            <div class="stat-label">{{ label }}</div>
          </div>
        </div>
      </el-card>

      <!-- 门类导航 -->
      <div class="section reveal">
        <div class="section-head">
          <div class="bar"></div>
          <h3>非遗十大门类</h3>
          <router-link to="/projects" class="more">查看全部 →</router-link>
        </div>
        <div class="category-grid">
          <router-link
            v-for="(c, i) in categories"
            :key="c.id"
            :to="{ path: '/projects', query: { categoryId: c.id } }"
            class="category-item"
            :style="{ '--i': i }"
          >
            <el-icon class="cat-icon"><OfficeBuilding /></el-icon>
            <span>{{ c.categoryName }}</span>
          </router-link>
        </div>
      </div>

      <!-- 热门非遗项目 -->
      <div class="section reveal">
        <div class="section-head">
          <div class="bar"></div>
          <h3>热门非遗项目</h3>
          <router-link to="/projects" class="more">更多项目 →</router-link>
        </div>
        <el-row :gutter="16">
          <el-col :xs="12" :sm="8" :md="6" v-for="p in hotProjects" :key="p.id">
            <router-link :to="`/projects/${p.id}`" class="project-card ich-card">
              <div class="cover-wrap">
                <el-image :src="resolveUrl(p.coverUrl)" fit="cover" class="project-cover" :preview-src-list="[resolveUrl(p.coverUrl)]">
                  <template #error>
                    <div class="img-placeholder"><el-icon><Picture /></el-icon></div>
                  </template>
                </el-image>
              </div>
              <div class="project-body">
                <div class="project-name">{{ p.projectName }}</div>
                <div class="project-meta">
                  <el-tag size="small" type="warning" effect="plain">{{ p.categoryName || '非遗' }}</el-tag>
                  <span class="views"><el-icon class="v-icon"><View /></el-icon>{{ p.viewCount || 0 }} 浏览</span>
                </div>
              </div>
            </router-link>
          </el-col>
        </el-row>
      </div>

      <!-- 最新资讯 -->
      <div class="section reveal">
        <div class="section-head">
          <div class="bar"></div>
          <h3>最新资讯</h3>
          <router-link to="/articles" class="more">更多资讯 →</router-link>
        </div>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8" v-for="a in articles" :key="a.id">
            <router-link :to="`/articles/${a.id}`" class="article-card ich-card">
              <div class="cover-wrap">
                <el-image :src="resolveUrl(a.coverUrl)" fit="cover" class="article-cover">
                  <template #error>
                    <div class="img-placeholder"><el-icon><Picture /></el-icon></div>
                  </template>
                </el-image>
              </div>
              <div class="article-body">
                <div class="article-title">{{ a.title }}</div>
                <div class="article-summary">{{ a.summary }}</div>
                <div class="article-meta">
                  <span class="a-author"><el-icon><User /></el-icon>{{ a.authorName || '平台' }}</span>
                  <span>{{ formatDate(a.publishTime) }}</span>
                </div>
              </div>
            </router-link>
          </el-col>
        </el-row>
      </div>

      <!-- 近期活动 -->
      <div class="section reveal">
        <div class="section-head">
          <div class="bar"></div>
          <h3>近期活动</h3>
          <router-link to="/activities" class="more">更多活动 →</router-link>
        </div>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8" v-for="act in activities" :key="act.id">
            <router-link :to="`/activities/${act.id}`" class="activity-card ich-card">
              <div class="act-date">
                <div class="act-month">{{ monthOf(act.startTime) }}月</div>
                <div class="act-day">{{ dayOf(act.startTime) }}</div>
              </div>
              <div class="act-body">
                <div class="act-title">{{ act.title }}</div>
                <div class="act-info">
                  <el-icon><Location /></el-icon>
                  <span>{{ act.address }}</span>
                </div>
                <el-tag v-if="act.status === 1" type="success" size="small" effect="light">报名中</el-tag>
                <el-tag v-else type="info" size="small">已结束</el-tag>
              </div>
            </router-link>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { getBannerList } from '@/api/admin'
import { getCategoryList, getHotProjects } from '@/api/project'
import { getArticleList } from '@/api/article'
import { getActivityList } from '@/api/activity'
import request from '@/utils/request'

import { resolveUrl, formatDate } from '@/utils/format'

const banners = ref([])
const categories = ref([])
const hotProjects = ref([])
const articles = ref([])
const activities = ref([])
const stats = ref(null)

const statLabels = { projects: '非遗项目', inheritors: '代表性传承人', articles: '资讯文章', activities: '传承活动', signups: '报名人次' }
const displayStats = ref({ projects: 0, inheritors: 0, articles: 0, activities: 0, signups: 0 })

let revealObserver = null
let rafId = 0

// 数字滚动动画（缓动）
function animateCountUp(target) {
  cancelAnimationFrame(rafId)
  const start = performance.now()
  const duration = 1000
  const tick = (now) => {
    const p = Math.min((now - start) / duration, 1)
    const ease = 1 - Math.pow(1 - p, 3)
    Object.keys(target).forEach((k) => {
      displayStats.value[k] = Math.round((target[k] || 0) * ease)
    })
    if (p < 1) rafId = requestAnimationFrame(tick)
  }
  rafId = requestAnimationFrame(tick)
}

// 滚动渐显
function initReveal() {
  const els = document.querySelectorAll('.reveal')
  if (!('IntersectionObserver' in window)) {
    els.forEach((el) => el.classList.add('in'))
    return
  }
  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((en) => {
        if (en.isIntersecting) {
          en.target.classList.add('in')
          revealObserver.unobserve(en.target)
        }
      })
    },
    { threshold: 0.1 }
  )
  els.forEach((el) => revealObserver.observe(el))
}

async function loadAll() {
  try {
    const [b, c, h, a, act, s] = await Promise.all([
      getBannerList(),
      getCategoryList(),
      getHotProjects(),
      getArticleList({ page: 1, size: 3, status: 1 }),
      getActivityList({ page: 1, size: 3 }),
      request.get('/stats/overview')
    ])
    banners.value = b.data || []
    categories.value = c.data || []
    hotProjects.value = (h.data && (h.data.records || h.data)) || []
    articles.value = (a.data && (a.data.records || a.data)) || []
    activities.value = (act.data && (act.data.records || act.data)) || []
    stats.value = s.data || null
    if (stats.value) animateCountUp(stats.value)
  } catch (e) {
    // 单项失败不阻塞页面
  }
}

function monthOf(t) {
  if (!t) return '—'
  return formatDate(t).split('-')[1]
}
function dayOf(t) {
  if (!t) return '—'
  return formatDate(t).split('-')[2]
}

onMounted(async () => {
  await loadAll()
  // 等 DOM 渲染完再挂观察器
  setTimeout(initReveal, 60)
})
onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  if (revealObserver) revealObserver.disconnect()
})
</script>

<style scoped>
.banner {
  margin-bottom: 0;
}
.banner-item {
  display: block;
  position: relative;
  height: 100%;
}
.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.banner-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 34px 44px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.72), transparent);
  color: #fff;
}
.banner-mask h2 {
  font-size: 30px;
  margin-bottom: 8px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
  animation: banner-text 0.7s ease both;
}
.banner-mask p {
  font-size: 15px;
  opacity: 0.92;
  animation: banner-text 0.7s ease 0.15s both;
}
@keyframes banner-text {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}
.banner-fallback {
  height: 420px;
  background: linear-gradient(135deg, #6f370f, #8b4513 50%, #c97a3d);
  position: relative;
}
.stats-card {
  margin-top: -40px;
  position: relative;
  z-index: 2;
  border-radius: 14px;
  border: none;
  box-shadow: 0 8px 24px rgba(139, 69, 19, 0.14);
}
.stats-grid {
  display: flex;
  justify-content: space-around;
  text-align: center;
  padding: 14px 0;
}
.stat-item {
  flex: 1;
  position: relative;
}
.stat-item:not(:last-child)::after {
  content: "";
  position: absolute;
  right: 0;
  top: 20%;
  height: 60%;
  width: 1px;
  background: #f0e2d0;
}
.stat-num {
  font-size: 32px;
  font-weight: bold;
  color: #8b4513;
  font-variant-numeric: tabular-nums;
}
.stat-label {
  color: #999;
  font-size: 13px;
  margin-top: 4px;
}
.section {
  margin-top: 30px;
}
.section-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.section-head h3 {
  font-size: 22px;
  color: #4a3a28;
  margin: 0;
}
.more {
  color: #8b4513;
  font-size: 13px;
  margin-left: auto;
}
.category-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
}
.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  background: #fff;
  border-radius: 12px;
  padding: 22px 10px;
  box-shadow: 0 2px 8px rgba(139, 69, 19, 0.06);
  transition: transform 0.3s ease, box-shadow 0.3s ease, background 0.3s ease;
  font-size: 14px;
  color: #5a4632;
  position: relative;
  overflow: hidden;
}
.category-item::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #8b4513, #d4a05f);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s ease;
}
.category-item:hover::before {
  transform: scaleX(1);
}
.category-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 24px rgba(139, 69, 19, 0.18);
  background: #fdf8f1;
  color: #8b4513;
}
.cat-icon {
  font-size: 32px;
  color: #a0522d;
  transition: transform 0.35s ease, color 0.35s ease;
}
.category-item:hover .cat-icon {
  transform: scale(1.18) rotate(-6deg);
  color: #8b4513;
}
.project-card,
.article-card {
  display: block;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
}
.activity-card {
  display: flex;
  padding: 16px;
  gap: 14px;
}
.project-card .el-image,
.article-card .el-image {
  overflow: hidden;
}
.project-card .el-image :deep(img),
.article-card .el-image :deep(img) {
  transition: transform 0.6s ease;
}
.project-card:hover .el-image :deep(img),
.article-card:hover .el-image :deep(img) {
  transform: scale(1.08);
}
.project-cover {
  width: 100%;
  height: 160px;
  display: block;
}
.project-body {
  padding: 12px;
}
.project-name {
  font-weight: 600;
  font-size: 15px;
  color: #333;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.project-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.views {
  color: #aaa;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.v-icon {
  font-size: 13px;
}
.article-cover {
  width: 100%;
  height: 140px;
  display: block;
}
.article-body {
  padding: 12px;
}
.article-title {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.article-summary {
  color: #888;
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
  min-height: 36px;
}
.article-meta {
  display: flex;
  justify-content: space-between;
  color: #bbb;
  font-size: 12px;
}
.a-author {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.act-date {
  text-align: center;
  background: linear-gradient(160deg, #8b4513, #a0522d);
  color: #fff;
  border-radius: 10px;
  padding: 10px 12px;
  min-width: 54px;
  height: fit-content;
  box-shadow: 0 4px 10px rgba(139, 69, 19, 0.3);
}
.act-month {
  font-size: 12px;
  opacity: 0.85;
}
.act-day {
  font-size: 22px;
  font-weight: bold;
}
.act-body {
  flex: 1;
}
.act-title {
  font-weight: 600;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.act-info {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #888;
  font-size: 12px;
  margin-bottom: 8px;
}
.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0ece4;
  color: #c9b79c;
  font-size: 30px;
}
</style>
