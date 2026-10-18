<template>
  <div class="page-container detail-page" v-loading="loading">
    <template v-if="article">
      <el-breadcrumb separator="/" class="crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/articles' }">非遗资讯</el-breadcrumb-item>
        <el-breadcrumb-item>{{ article.title }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="article-wrap">
        <el-card shadow="never" class="main">
          <h1 class="title">{{ article.title }}</h1>
          <div class="meta">
            <span>{{ article.authorName || '平台' }}</span>
            <span>{{ formatDate(article.publishTime) }}</span>
            <span>{{ article.categoryName }}</span>
            <span class="views"><el-icon><View /></el-icon> {{ article.viewCount || 0 }}</span>
          </div>
          <el-image
            v-if="article.coverUrl"
            :src="resolveUrl(article.coverUrl)"
            fit="cover"
            class="cover-img"
            :preview-src-list="[resolveUrl(article.coverUrl)]"
          >
            <template #error><div class="img-ph"><el-icon><Picture /></el-icon></div></template>
          </el-image>
          <div class="content">{{ article.content }}</div>

          <div class="actions">
            <el-button
              :type="favState ? 'warning' : 'primary'"
              :icon="favState ? StarFilled : Star"
              @click="toggleFavorite"
            >
              {{ favState ? '已收藏' : '收藏' }}
            </el-button>
            <el-button :type="likeState ? 'danger' : 'default'" @click="toggleLike">
              {{ likeState ? '已点赞' : '点赞' }}
            </el-button>
          </div>

          <div class="block">
            <h3>评论 ({{ comments.length }})</h3>
            <div class="comment-input" v-if="userStore.isLoggedIn">
              <el-input v-model="commentText" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="发表评论…" />
              <el-button type="primary" class="submit-btn" @click="submitComment">发表评论</el-button>
            </div>
            <el-alert v-else type="info" :closable="false" class="login-tip">
              <template #title>
                登录后即可参与评论 <router-link to="/login" class="tip-link">去登录 →</router-link>
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
              <el-empty v-if="!comments.length" description="暂无评论" :image-size="70" />
            </div>
          </div>
        </el-card>

        <div class="side">
          <el-card shadow="never" v-if="related.length" class="rel-card">
            <h4>相关阅读</h4>
            <router-link v-for="r in related" :key="r.id" :to="`/articles/${r.id}`" class="rel-item">
              <span>{{ r.title }}</span>
            </router-link>
          </el-card>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled, View } from '@element-plus/icons-vue'
import { getArticleDetail, getRelatedArticles } from '@/api/article'
import { favorite, like, comment, getComments, getInteractionState } from '@/api/interaction'
import { useUserStore } from '@/stores/user'
import { resolveUrl, formatDate } from '@/utils/format'

const route = useRoute()
const userStore = useUserStore()
const id = route.params.id

const article = ref(null)
const related = ref([])
const comments = ref([])
const favState = ref(false)
const likeState = ref(false)
const commentText = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await getArticleDetail(id)
    article.value = res.data
    loadComments()
    loadState()
    try {
      if (article.value && article.value.projectId) {
        const rr = await getRelatedArticles({ projectId: article.value.projectId, excludeId: Number(id) })
        related.value = (rr.data && (rr.data.records || rr.data)) || []
      }
    } catch (e) {}
  } catch (e) {
    ElMessage.error('资讯不存在')
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  try {
    const res = await getComments({ targetType: 3, targetId: id, page: 1, size: 100 })
    comments.value = (res.data && (res.data.records || res.data)) || []
  } catch (e) {}
}

async function loadState() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await getInteractionState(3, id)
    favState.value = !!(res.data && res.data.favorited)
    likeState.value = !!(res.data && res.data.liked)
  } catch (e) {}
}

async function toggleFavorite() {
  if (!userStore.isLoggedIn) return ElMessage.warning('请先登录')
  try {
    const res = await favorite(3, Number(id));
    favState.value = !!(res.data && (res.data.active !== undefined ? res.data.active : res.data.favorited))
    ElMessage.success(favState.value ? '收藏成功' : '已取消收藏')
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function toggleLike() {
  if (!userStore.isLoggedIn) return ElMessage.warning('请先登录')
  try {
    const res2 = await like(3, Number(id));
    likeState.value = !!(res2.data && (res2.data.active !== undefined ? res2.data.active : res2.data.liked))
    
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function submitComment() {
  if (!commentText.value.trim()) return ElMessage.warning('请输入评论内容')
  try {
    await comment({ targetType: 3, targetId: Number(id), content: commentText.value.trim() })
    commentText.value = ''
    ElMessage.success('评论成功')
    loadComments()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.crumb {
  margin-bottom: 14px;
}
.article-wrap {
  display: grid;
  grid-template-columns: 1fr 260px;
  gap: 16px;
}
.title {
  font-size: 28px;
  color: #333;
  margin-bottom: 10px;
}
.meta {
  display: flex;
  gap: 14px;
  color: #999;
  font-size: 13px;
  align-items: center;
  padding-bottom: 14px;
  border-bottom: 1px solid #f0ece4;
  margin-bottom: 16px;
}
.views {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 4px;
}
.cover-img {
  width: 100%;
  max-height: 360px;
  border-radius: 8px;
  margin-bottom: 16px;
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
.content {
  line-height: 2;
  color: #444;
  white-space: pre-wrap;
  margin-bottom: 20px;
}
.actions {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.block {
  padding-top: 16px;
  border-top: 1px solid #f0ece4;
}
.block h3 {
  font-size: 17px;
  color: #5a4632;
  margin-bottom: 12px;
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
}
.rel-card h4 {
  font-size: 15px;
  color: #5a4632;
  margin-bottom: 12px;
  border-left: 3px solid #8b4513;
  padding-left: 8px;
}
.rel-item {
  display: block;
  padding: 8px 0;
  border-bottom: 1px dashed #eee;
  font-size: 13px;
  color: #555;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rel-item:hover {
  color: #8b4513;
}
</style>
