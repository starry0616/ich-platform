<template>
  <div class="page-container detail-page" v-loading="loading">
    <template v-if="inheritor">
      <el-breadcrumb separator="/" class="crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/inheritors' }">传承人</el-breadcrumb-item>
        <el-breadcrumb-item>{{ inheritor.name }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="detail-grid">
        <div class="main">
          <el-card shadow="never">
            <div class="hero">
              <el-avatar :size="72" class="avatar">{{ (inheritor.name || '?').charAt(0) }}</el-avatar>
              <div class="hero-info">
                <h1 class="name">{{ inheritor.name }}</h1>
                <div class="sub">{{ inheritor.projectName }}</div>
                <div class="tags">
                  <el-tag v-if="inheritor.certStatus === 1" type="success" effect="dark">已认证</el-tag>
                  <el-tag :type="levelTag" effect="plain">{{ levelText }}</el-tag>
                  <el-tag v-if="inheritor.region" effect="plain">{{ inheritor.region }}</el-tag>
                  <el-tag v-if="inheritor.craftYears" effect="plain">从业 {{ inheritor.craftYears }} 年</el-tag>
                </div>
              </div>
            </div>

            <div class="actions">
              <el-button
                :type="favState ? 'warning' : 'primary'"
                :icon="favState ? StarFilled : Star"
                :loading="favBusy"
                :disabled="favBusy"
                @click="toggleFavorite"
              >
                {{ favState ? '已关注' : '关注' }}
              </el-button>
              <el-button
                :type="likeState ? 'danger' : 'default'"
                :loading="likeBusy"
                :disabled="likeBusy"
                @click="toggleLike"
              >
                {{ likeState ? '已点赞' : '点赞' }}
              </el-button>
              <span class="views"><el-icon><View /></el-icon> {{ inheritor.viewCount || 0 }} 次浏览</span>
            </div>

            <div class="block">
              <h3>人物介绍</h3>
              <p class="text">{{ inheritor.intro }}</p>
            </div>

            <div class="block" v-if="inheritor.works">
              <h3>代表作品</h3>
              <p class="text">{{ inheritor.works }}</p>
            </div>

            <!-- 代表项目（来自项目-传承人关联表，可能多个；仅展示，非管理归属） -->
            <div class="block" v-if="relatedProjects.length">
              <h3>代表项目（{{ relatedProjects.length }}）</h3>
              <div class="proj-list">
                <router-link
                  v-for="p in relatedProjects"
                  :key="p.id"
                  :to="`/projects/${p.id}`"
                  class="proj-item"
                >
                  <div class="proj-body">
                    <div class="proj-name">{{ p.projectName }}</div>
                    <div class="proj-meta">
                      <el-tag size="small" effect="plain">{{ p.projectCode || '—' }}</el-tag>
                      <span>{{ p.region || '—' }}</span>
                      <span v-if="p.batchNo">第{{ p.batchNo }}批</span>
                    </div>
                  </div>
                  <el-icon class="arrow"><ArrowRight /></el-icon>
                </router-link>
              </div>
            </div>

            <!-- 主理体验活动（该传承人主办/主讲的展演、体验课堂、讲座） -->
            <div class="block">
              <h3>主理体验活动</h3>
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
                <div v-else-if="!relActLoading && relActFailed" class="rel-act-empty">主理活动加载失败，请稍后重试</div>
                <div v-else-if="!relActLoading" class="rel-act-empty">该传承人暂未发起体验活动</div>
              </div>
            </div>

            <div class="block">
              <h3>互动评论</h3>
              <div class="comment-input" v-if="userStore.isLoggedIn">
                <el-input v-model="commentText" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="留言致敬…" />
                <el-button
                  type="primary"
                  class="submit-btn"
                  :loading="commentBusy"
                  :disabled="commentBusy"
                  @click="submitComment"
                >发表评论</el-button>
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
        </div>

        <div class="side">
          <el-card shadow="never" class="info-card">
            <h4>个人档案</h4>
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="姓名">{{ inheritor.name }}</el-descriptions-item>
              <el-descriptions-item label="性别">{{ inheritor.gender === 1 ? '男' : inheritor.gender === 2 ? '女' : '—' }}</el-descriptions-item>
              <el-descriptions-item label="出生年份">{{ inheritor.birthYear || '—' }}</el-descriptions-item>
              <el-descriptions-item label="级别">{{ levelText }}</el-descriptions-item>
              <el-descriptions-item label="地区">{{ inheritor.region || '—' }}</el-descriptions-item>
              <el-descriptions-item label="所属项目">{{ inheritor.projectName || '—' }}</el-descriptions-item>
              <el-descriptions-item label="从业年限">{{ inheritor.craftYears ? `${inheritor.craftYears} 年` : '—' }}</el-descriptions-item>
              <el-descriptions-item label="证书编号">{{ inheritor.certNo || '—' }}</el-descriptions-item>
            </el-descriptions>
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
import { Star, StarFilled, View, Clock, Location, Picture, ArrowRight } from '@element-plus/icons-vue'
import { getInheritorDetail } from '@/api/inheritor'
import { getActivityList } from '@/api/activity'
import { favorite, like, comment, getComments, getInteractionState } from '@/api/interaction'
import { useUserStore } from '@/stores/user'
import { resolveUrl, formatDate } from '@/utils/format'

const route = useRoute()
const userStore = useUserStore()
const id = route.params.id

const inheritor = ref(null)
const relatedProjects = ref([])
const comments = ref([])
const relActivities = ref([])
const relActLoading = ref(false)
const relActFailed = ref(false)
const favState = ref(false)
const likeState = ref(false)
const commentText = ref('')
const loading = ref(false)
// 三个操作各自独立的提交锁，请求结束在 finally 恢复
const favBusy = ref(false)
const likeBusy = ref(false)
const commentBusy = ref(false)

const levelText = computed(() => ({ 1: '国家级', 2: '省级', 3: '市级', 4: '县级' }[inheritor.value && inheritor.value.level] || ''))
const levelTag = computed(() => ({ 1: 'danger', 2: 'warning', 3: 'primary', 4: 'info' }[inheritor.value && inheritor.value.level] || 'info'))

function actStatusText(s) {
  return { 1: '报名中', 2: '进行中', 3: '已结束', 0: '待审核' }[s] || '未知'
}
function actStatusTag(s) {
  return { 1: 'success', 2: 'primary', 3: 'info', 0: 'warning' }[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await getInheritorDetail(id)
    inheritor.value = res.data
    relatedProjects.value = (inheritor.value && inheritor.value.relatedProjects) || []
    loadComments()
    loadState()
    loadRelActivities()
  } catch (e) {
    ElMessage.error('传承人信息不存在')
  } finally {
    loading.value = false
  }
}

/** 加载该传承人主理的体验活动（最多 3 条，inheritorId 由后端精确过滤） */
async function loadRelActivities() {
  relActLoading.value = true
  relActFailed.value = false
  try {
    const res = await getActivityList({ inheritorId: Number(id), page: 1, size: 3 })
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
    const res = await getComments({ targetType: 4, targetId: id, page: 1, size: 100 })
    comments.value = (res.data && (res.data.records || res.data)) || []
  } catch (e) {}
}

async function loadState() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await getInteractionState(4, id)
    favState.value = !!(res.data && res.data.favorited)
    likeState.value = !!(res.data && res.data.liked)
  } catch (e) {}
}

async function toggleFavorite() {
  if (!userStore.isLoggedIn) return ElMessage.warning('请先登录')
  if (favBusy.value) return
  favBusy.value = true
  try {
    const res = await favorite(4, Number(id));
    // 仅在接口成功后更新状态（非乐观更新）；后端 toggle 返回 { active }，兼容旧字段 favorited
    favState.value = !!(res.data && (res.data.active !== undefined ? res.data.active : res.data.favorited))
    ElMessage.success(favState.value ? '关注成功' : '已取消关注')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    favBusy.value = false
  }
}

async function toggleLike() {
  if (!userStore.isLoggedIn) return ElMessage.warning('请先登录')
  if (likeBusy.value) return
  likeBusy.value = true
  try {
    const res2 = await like(4, Number(id));
    // 仅在接口成功后更新状态（非乐观更新）
    likeState.value = !!(res2.data && (res2.data.active !== undefined ? res2.data.active : res2.data.liked))
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    likeBusy.value = false
  }
}

async function submitComment() {
  if (!commentText.value.trim()) return ElMessage.warning('请输入评论内容')
  if (commentBusy.value) return
  commentBusy.value = true
  try {
    await comment({ targetType: 4, targetId: Number(id), content: commentText.value.trim() })
    // 仅在接口成功后清空输入；失败时保留用户已输入内容
    commentText.value = ''
    ElMessage.success('评论成功')
    loadComments()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    commentBusy.value = false
  }
}

onMounted(load)
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
.hero {
  display: flex;
  gap: 18px;
  align-items: center;
  margin-bottom: 16px;
}
.avatar {
  background: #8b4513;
  color: #fff;
  font-size: 28px;
  font-weight: bold;
}
.name {
  font-size: 26px;
  color: #5a4632;
}
.sub {
  color: #999;
  font-size: 14px;
  margin: 4px 0 8px;
}
.tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.actions {
  display: flex;
  gap: 10px;
  align-items: center;
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
/* 代表项目列表（关联表只读展示） */
.proj-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.proj-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #faf6ee;
  border: 1px solid #f0e6d2;
  border-radius: 8px;
  padding: 12px 14px;
}
.proj-body {
  flex: 1;
  min-width: 0;
}
.proj-name {
  font-weight: 600;
  font-size: 15px;
  color: #5a4632;
  margin-bottom: 4px;
}
.proj-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #999;
  font-size: 12px;
}
.arrow {
  margin-left: auto;
  color: #c9b79c;
}
/* 主理体验活动卡片（与项目详情/活动列表同风格，路由 /activities/{id}） */
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
}
.info-card h4 {
  font-size: 15px;
  color: #5a4632;
  margin-bottom: 12px;
  border-left: 3px solid #8b4513;
  padding-left: 8px;
}
</style>
