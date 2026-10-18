<template>
  <div class="page-container detail-page" v-loading="loading">
    <template v-if="project">
      <el-breadcrumb separator="/" class="crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/projects' }">非遗项目</el-breadcrumb-item>
        <el-breadcrumb-item>{{ project.projectName }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="detail-grid">
        <div class="main">
          <el-card shadow="never">
            <h1 class="title">{{ project.projectName }}</h1>
            <div class="tags">
              <el-tag type="warning" effect="plain">{{ project.categoryName }}</el-tag>
              <el-tag :type="levelTag" effect="plain">{{ levelText }}</el-tag>
              <el-tag v-if="project.region" effect="plain">{{ project.region }}</el-tag>
              <el-tag v-if="project.announceYear" effect="plain">{{ project.announceYear }} 年公布</el-tag>
            </div>

            <el-image
              v-if="project.coverUrl"
              :src="resolveUrl(project.coverUrl)"
              fit="cover"
              class="cover-img"
              :preview-src-list="[resolveUrl(project.coverUrl)]"
            >
              <template #error><div class="img-ph"><el-icon><Picture /></el-icon></div></template>
            </el-image>

            <div class="actions">
              <el-button
                :type="favState ? 'warning' : 'primary'"
                :icon="favState ? StarFilled : Star"
                @click="toggleFavorite"
              >
                {{ favState ? '已收藏' : '收藏' }} {{ project.favoriteCount || 0 }}
              </el-button>
              <el-button
                :type="likeState ? 'danger' : 'default'"
                :icon="likeState ? GoodsFilled : Pointer"
                @click="toggleLike"
              >
                {{ likeState ? '已点赞' : '点赞' }}
              </el-button>
              <span class="views"><el-icon><View /></el-icon> {{ project.viewCount || 0 }} 次浏览</span>
            </div>

            <!-- 简介 -->
            <div class="block">
              <h3>项目简介</h3>
              <p class="text">{{ project.summary }}</p>
            </div>

            <!-- 详细内容 -->
            <div class="block">
              <h3>详细内容</h3>
              <div class="rich-text">{{ project.content }}</div>
            </div>

            <!-- 历史渊源 -->
            <div class="block" v-if="project.history">
              <h3>历史渊源</h3>
              <p class="text">{{ project.history }}</p>
            </div>

            <!-- 传承现状 -->
            <div class="block" v-if="project.statusDesc">
              <h3>传承现状</h3>
              <p class="text">{{ project.statusDesc }}</p>
            </div>

            <!-- 代表性传承人 -->
            <div class="block" v-if="inheritor">
              <h3>代表性传承人</h3>
              <router-link :to="`/inheritors/${inheritor.id}`" class="inheritor-link">
                <el-avatar :size="48" class="inh-avatar">{{ (inheritor.name || '?').charAt(0) }}</el-avatar>
                <div>
                  <div class="inh-name">{{ inheritor.name }}</div>
                  <div class="inh-desc">{{ inheritor.projectName }} · {{ levelTextFor(inheritor.level) }}</div>
                </div>
                <el-icon class="arrow"><ArrowRight /></el-icon>
              </router-link>
            </div>

            <!-- 关联体验活动（同一非遗项目的展演/体验课堂/讲座） -->
            <div class="block">
              <h3>关联体验活动</h3>
              <div v-loading="relActLoading" class="rel-act-wrap">
                <template v-if="!relActLoading && relActivities.length">
                  <router-link
                    v-for="a in relActivities"
                    :key="a.id"
                    :to="`/activities/${a.id}`"
                    class="rel-act-item"
                  >
                    <el-image :src="resolveUrl(a.coverUrl)" fit="cover" class="rel-act-cover">
                      <template #error><div class="img-ph-mini"><el-icon><Picture /></el-icon></div></template>
                    </el-image>
                    <div class="rel-act-body">
                      <div class="rel-act-title">{{ a.title }}</div>
                      <div class="rel-act-meta">
                        <el-icon><Clock /></el-icon>
                        <span>{{ formatDate(a.startTime) }}</span>
                      </div>
                      <div class="rel-act-meta">
                        <el-icon><Location /></el-icon>
                        <span>{{ a.address }}</span>
                      </div>
                    </div>
                    <el-tag size="small" :type="actStatusTag(a.status)">{{ actStatusText(a.status) }}</el-tag>
                  </router-link>
                </template>
                <div v-else-if="!relActLoading && relActFailed" class="rel-act-empty">关联活动加载失败，请稍后重试</div>
                <div v-else-if="!relActLoading" class="rel-act-empty">该项目暂未关联体验活动，敬请期待</div>
              </div>
            </div>

            <!-- 评论区 -->
            <div class="block">
              <h3>评论 ({{ comments.length }})</h3>
              <div class="comment-input" v-if="userStore.isLoggedIn">
                <el-input
                  v-model="commentText"
                  type="textarea"
                  :rows="3"
                  maxlength="500"
                  show-word-limit
                  placeholder="分享你的看法…"
                />
                <el-button type="primary" class="submit-btn" @click="submitComment">发表评论</el-button>
              </div>
              <el-alert v-else type="info" :closable="false" class="login-tip">
                <template #title>
                  登录后即可参与评论
                  <router-link to="/login" class="tip-link">去登录 →</router-link>
                </template>
              </el-alert>
              <div class="comment-list">
                <div v-for="c in comments" :key="c.id" class="comment-item">
                  <el-avatar :size="34" class="c-avatar">{{ (c.userNickname || 'U').charAt(0) }}</el-avatar>
                  <div class="c-body">
                    <div class="c-head">
                      <span class="c-name">{{ c.userNickname }}</span>
                      <span class="c-time">{{ formatDate(c.createTime) }}</span>
                    </div>
                    <div class="c-content">{{ c.content }}</div>
                  </div>
                </div>
                <el-empty v-if="!comments.length" description="还没有评论，来抢沙发吧" :image-size="70" />
              </div>
            </div>
          </el-card>
        </div>

        <!-- 侧边栏 -->
        <div class="side">
          <el-card shadow="never" class="info-card">
            <h4>项目信息</h4>
            <!-- 名录收录标识：仅国家级名录项目（level===1）且项目编号来自官方名录时显示，
                 省级/示例数据（level=2）不展示，避免把省级项目标为国家级名录收录 -->
            <div class="list-note" v-if="project.level === 1 && project.projectCode">
              <el-tag type="success" effect="dark" size="small">名录收录</el-tag>
              <span>编号来自国家级非遗代表性项目名录</span>
            </div>
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="项目编号">{{ project.projectCode || '—' }}</el-descriptions-item>
              <el-descriptions-item label="门类">{{ project.categoryName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="级别">{{ levelText || '—' }}</el-descriptions-item>
              <el-descriptions-item label="批次">{{ project.batchNo ? `第${project.batchNo}批` : '—' }}</el-descriptions-item>
              <el-descriptions-item label="申报地区">{{ project.region || '—' }}</el-descriptions-item>
              <el-descriptions-item label="保护单位">{{ project.protectionUnit || '—' }}</el-descriptions-item>
              <el-descriptions-item label="类型">{{ project.type || '—' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card shadow="never" class="info-card" v-if="relatedProjects.length">
            <h4>相关项目</h4>
            <router-link v-for="r in relatedProjects" :key="r.id" :to="`/projects/${r.id}`" class="related-item">
              <span class="r-name">{{ r.projectName }}</span>
              <span class="r-views">{{ r.viewCount || 0 }} 浏览</span>
            </router-link>
          </el-card>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled, Pointer, GoodsFilled, View, ArrowRight, Clock, Location, Picture } from '@element-plus/icons-vue'
import { getProjectDetail } from '@/api/project'
import { getInheritorDetail } from '@/api/inheritor'
import { getActivityList } from '@/api/activity'
import { favorite, like, comment, getComments, getInteractionState } from '@/api/interaction'
import { useUserStore } from '@/stores/user'
import { resolveUrl, formatDate } from '@/utils/format'

const route = useRoute()
const userStore = useUserStore()
const id = route.params.id

const project = ref(null)
const inheritor = ref(null)
const comments = ref([])
const relatedProjects = ref([])
const relActivities = ref([])
const relActLoading = ref(false)
const relActFailed = ref(false)
const favState = ref(false)
const likeState = ref(false)
const commentText = ref('')
const loading = ref(false)

const levelText = computed(() => ({ 1: '国家级', 2: '省级', 3: '市级', 4: '县级' }[project.value && project.value.level] || ''))
const levelTag = computed(() => ({ 1: 'danger', 2: 'warning', 3: 'primary', 4: 'info' }[project.value && project.value.level] || 'info'))
function levelTextFor(l) {
  return { 1: '国家级', 2: '省级', 3: '市级', 4: '县级' }[l] || ''
}

function actStatusText(s) {
  return { 1: '报名中', 2: '进行中', 3: '已结束', 0: '待审核' }[s] || '未知'
}
function actStatusTag(s) {
  return { 1: 'success', 2: 'primary', 3: 'info', 0: 'warning' }[s] || 'info'
}

async function loadProject() {
  loading.value = true
  try {
    const res = await getProjectDetail(id)
    project.value = res.data
    // 加载代表性传承人
    if (project.value && project.value.inheritorId) {
      try {
        const ir = await getInheritorDetail(project.value.inheritorId)
        inheritor.value = ir.data
      } catch (e) {}
    }
    await Promise.all([loadComments(), loadState(), loadRelActivities()])
  } catch (e) {
    ElMessage.error('项目不存在或已下架')
  } finally {
    loading.value = false
  }
}

/** 加载该非遗项目关联的体验活动（最多 3 条，projectId 由后端精确过滤） */
async function loadRelActivities() {
  relActLoading.value = true
  relActFailed.value = false
  try {
    const res = await getActivityList({ projectId: Number(id), page: 1, size: 3 })
    relActivities.value = (res.data && res.data.records) || []
  } catch (e) {
    relActFailed.value = true
    relActivities.value = []
  } finally {
    relActLoading.value = false
  }
}

async function loadComments() {
  try {
    const res = await getComments({ targetType: 1, targetId: id, page: 1, size: 100 })
    comments.value = (res.data && (res.data.records || res.data)) || []
  } catch (e) {}
}

async function loadState() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await getInteractionState(1, id)
    const d = res.data || {}
    favState.value = !!d.favorited
    likeState.value = !!d.liked
  } catch (e) {}
}

async function toggleFavorite() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await favorite(1, Number(id))
    const d = res.data || {}
    favState.value = !!d.active
    project.value.favoriteCount = Math.max((project.value.favoriteCount || 0) + (favState.value ? 1 : -1), 0)
    ElMessage.success(favState.value ? '收藏成功' : '已取消收藏')
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function toggleLike() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await like(1, Number(id))
    const d = res.data || {}
    likeState.value = !!d.active
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function submitComment() {
  if (!commentText.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  try {
    await comment({ targetType: 1, targetId: Number(id), content: commentText.value.trim() })
    commentText.value = ''
    ElMessage.success('评论成功')
    loadComments()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(loadProject)
</script>

<style scoped>
.crumb {
  margin-bottom: 14px;
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 16px;
}
.main .title {
  font-size: 26px;
  color: #5a4632;
  margin-bottom: 10px;
}
.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.cover-img {
  width: 100%;
  max-height: 400px;
  border-radius: 8px;
  margin-bottom: 14px;
}
.img-ph {
  width: 100%;
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0ece4;
  color: #c9b79c;
}
.actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.views {
  margin-left: auto;
  color: #aaa;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.block {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0ece4;
}
.block h3 {
  font-size: 17px;
  color: #5a4632;
  margin-bottom: 10px;
}
.text {
  line-height: 1.9;
  color: #555;
  white-space: pre-wrap;
}
.rich-text {
  line-height: 1.9;
  color: #555;
  white-space: pre-wrap;
}
.inheritor-link {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #faf6ee;
  border: 1px solid #f0e6d2;
  border-radius: 8px;
  padding: 12px 14px;
}
.inh-avatar {
  background: #8b4513;
  color: #fff;
  font-weight: bold;
}
.inh-name {
  font-weight: 600;
  font-size: 15px;
}
.inh-desc {
  color: #999;
  font-size: 12px;
}
.arrow {
  margin-left: auto;
  color: #c9b79c;
}
/* 关联体验活动卡片（与活动列表页同风格，保持路由 /activities/{id}） */
.rel-act-wrap {
  min-height: 40px;
}
.rel-act-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #faf6ee;
  border: 1px solid #f0e6d2;
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 10px;
  transition: box-shadow 0.3s ease, transform 0.3s ease;
}
.rel-act-item:hover {
  box-shadow: 0 6px 16px rgba(139, 69, 19, 0.12);
  transform: translateY(-2px);
}
.rel-act-cover {
  width: 84px;
  height: 60px;
  border-radius: 6px;
  flex-shrink: 0;
  overflow: hidden;
}
.img-ph-mini {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0ece4;
  color: #c9b79c;
}
.rel-act-body {
  flex: 1;
  min-width: 0;
}
.rel-act-title {
  font-weight: 600;
  font-size: 14px;
  color: #5a4632;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rel-act-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #888;
  font-size: 12px;
  margin-bottom: 2px;
}
.rel-act-empty {
  color: #b9a98f;
  font-size: 13px;
  text-align: center;
  padding: 18px 0;
}
.comment-input {
  margin-bottom: 14px;
}
.submit-btn {
  margin-top: 8px;
  float: right;
}
.login-tip {
  margin-bottom: 14px;
}
.tip-link {
  color: #8b4513;
  margin-left: 6px;
}
.comment-item {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f2ec;
}
.c-avatar {
  background: #d2b48c;
  color: #fff;
  flex-shrink: 0;
}
.c-body {
  flex: 1;
}
.c-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}
.c-name {
  font-weight: 600;
  font-size: 13px;
  color: #5a4632;
}
.c-time {
  color: #bbb;
  font-size: 12px;
}
.c-content {
  color: #555;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.info-card {
  margin-bottom: 16px;
}
.info-card h4 {
  font-size: 15px;
  color: #5a4632;
  margin-bottom: 12px;
  border-left: 3px solid #8b4513;
  padding-left: 8px;
}
.list-note {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b8f52;
  background: #f4f8f0;
  border: 1px solid #dfead6;
  border-radius: 6px;
  padding: 6px 8px;
  margin-bottom: 10px;
}
.related-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #eee;
  font-size: 13px;
}
.r-name {
  color: #555;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}
.related-item:hover .r-name {
  color: #8b4513;
}
.r-views {
  color: #bbb;
  font-size: 12px;
}
</style>
