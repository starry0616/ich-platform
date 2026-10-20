<template>
  <div class="page-container detail-page" v-loading="loading">
    <template v-if="activity">
      <el-breadcrumb separator="/" class="crumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/activities' }">活动</el-breadcrumb-item>
        <el-breadcrumb-item>{{ activity.title }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="detail-grid">
        <div class="main">
          <el-card shadow="never">
            <h1 class="title">{{ activity.title }}</h1>
            <div class="tags">
              <el-tag :type="statusTag" effect="plain">{{ statusText }}</el-tag>
              <el-tag effect="plain">{{ typeText }}</el-tag>
            </div>

            <el-image v-if="activity.coverUrl" :src="resolveUrl(activity.coverUrl)" fit="cover" class="cover-img" :preview-src-list="[resolveUrl(activity.coverUrl)]">
              <template #error><div class="img-ph"><el-icon><Picture /></el-icon></div></template>
            </el-image>

            <div class="info-row">
              <div class="info-item">
                <el-icon><Clock /></el-icon>
                <div>
                  <div class="k">活动时间</div>
                  <div class="v">{{ formatDate(activity.startTime) }} ~ {{ formatDate(activity.endTime) }}</div>
                </div>
              </div>
              <div class="info-item">
                <el-icon><Location /></el-icon>
                <div>
                  <div class="k">活动地点</div>
                  <div class="v">{{ activity.address }}</div>
                </div>
              </div>
              <div class="info-item">
                <el-icon><User /></el-icon>
                <div>
                  <div class="k">报名人数</div>
                  <div class="v">{{ activity.signed || 0 }} / {{ activity.quota || '不限' }} 人</div>
                </div>
              </div>
              <div class="info-item" v-if="activity.inheritorName">
                <el-icon><UserFilled /></el-icon>
                <div>
                  <div class="k">传承人</div>
                  <div class="v">{{ activity.inheritorName }}</div>
                </div>
              </div>
            </div>

            <div class="signup-bar">
              <el-button
                v-if="canSignup && !signedUp"
                type="primary"
                size="large"
                :loading="signupBusy"
                :disabled="signupBusy || signedUp"
                @click="doSignup"
              >
                {{ signupBusy ? '报名中…' : signedUp ? '已报名' : '立即报名' }}
              </el-button>
              <el-button
                v-else-if="canSignup && signedUp"
                type="warning"
                size="large"
                :loading="cancelBusy"
                :disabled="cancelBusy"
                @click="doCancel"
              >
                {{ cancelBusy ? '取消中…' : '取消报名' }}
              </el-button>
              <el-tag v-else size="large" type="info">报名已结束 / 未开放</el-tag>
            </div>
            <el-alert
              v-if="mySignupNo"
              type="success"
              :closable="false"
              class="signup-no"
              title="您已报名，现场核销请出示报名编号"
            >
              <div>报名编号：<b>{{ mySignupNo }}</b>（请妥善保存）</div>
            </el-alert>

            <div class="block">
              <h3>活动详情</h3>
              <div class="rich-text">{{ activity.content }}</div>
            </div>
          </el-card>
        </div>

        <div class="side">
          <el-card shadow="never" class="info-card">
            <h4>报名信息</h4>
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="状态">{{ statusText }}</el-descriptions-item>
              <el-descriptions-item label="类型">{{ typeText }}</el-descriptions-item>
              <el-descriptions-item label="报名截止">{{ formatDate(activity.signupDeadline) }}</el-descriptions-item>
              <el-descriptions-item label="名额">{{ activity.signed || 0 }} / {{ activity.quota || '不限' }}</el-descriptions-item>
              <el-descriptions-item v-if="mySignupNo" label="报名编号">{{ mySignupNo }}</el-descriptions-item>
              <el-descriptions-item label="所属项目">{{ activity.projectName || '—' }}</el-descriptions-item>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Location, User, UserFilled } from '@element-plus/icons-vue'
import { getActivityDetail, signupActivity, cancelSignup, getMySignups } from '@/api/activity'
import { useUserStore } from '@/stores/user'
import { resolveUrl, formatDate } from '@/utils/format'

const route = useRoute()
const userStore = useUserStore()
const id = route.params.id

const activity = ref(null)
const signedUp = ref(false)
const loading = ref(false)
// 报名 / 取消报名各自独立的提交锁，请求结束在 finally 恢复
const signupBusy = ref(false)
const cancelBusy = ref(false)
// 当前用户在本活动的报名编号（后端返回，现场核销出示）
const mySignupNo = ref('')

const statusText = computed(() => ({ 1: '报名中', 2: '进行中', 3: '已结束', 0: '待审核' }[activity.value && activity.value.status] || '未知'))
const statusTag = computed(() => ({ 1: 'success', 2: 'primary', 3: 'info', 0: 'warning' }[activity.value && activity.value.status] || 'info'))
const typeText = computed(() => ({ 1: '展演展示', 2: '体验课堂', 3: '技艺传习', 4: '展览讲座' }[activity.value && activity.value.activityType] || '活动'))
const canSignup = computed(() => activity.value && activity.value.status === 1)

async function load() {
  loading.value = true
  try {
    const res = await getActivityDetail(id)
    activity.value = res.data
    if (userStore.isLoggedIn) {
      checkSigned()
    }
  } catch (e) {
    ElMessage.error('活动不存在')
  } finally {
    loading.value = false
  }
}

async function checkSigned() {
  try {
    const res = await getMySignups({ page: 1, size: 200 })
    const records = (res.data && (res.data.records || res.data)) || []
    const matched = records.find((r) => String(r.activityId) === String(id) && r.status !== 2)
    signedUp.value = !!matched
    mySignupNo.value = matched ? (matched.signupNo || '') : ''
  } catch (e) {}
}

async function doSignup() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再报名')
    return
  }
  if (signupBusy.value) return
  signupBusy.value = true
  try {
    await ElMessageBox.confirm('确认报名参加该活动？', '报名确认', { type: 'warning' })
    const res = await signupActivity(id)
    signedUp.value = true
    mySignupNo.value = (res.data && res.data.signupNo) || ''
    ElMessage.success(mySignupNo.value
      ? `报名成功，报名编号：${mySignupNo.value}（请保存，现场核销出示）`
      : '报名成功')
    load()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e.message || '报名失败')
  } finally {
    signupBusy.value = false
  }
}

async function doCancel() {
  if (cancelBusy.value) return
  cancelBusy.value = true
  try {
    await ElMessageBox.confirm('确认取消报名？', '取消确认', { type: 'warning' })
    await cancelSignup(id)
    signedUp.value = false
    mySignupNo.value = ''
    ElMessage.success('已取消报名')
    load()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e.message || '取消失败')
  } finally {
    cancelBusy.value = false
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
  grid-template-columns: 1fr 280px;
  gap: 16px;
}
.title {
  font-size: 26px;
  color: #5a4632;
  margin-bottom: 10px;
}
.tags {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
.cover-img {
  width: 100%;
  max-height: 360px;
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
.info-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  background: #faf6ee;
  border: 1px solid #f0e6d2;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 14px;
}
.info-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  font-size: 14px;
}
.info-item > .el-icon {
  margin-top: 2px;
  color: #8b4513;
  font-size: 18px;
}
.k {
  color: #999;
  font-size: 12px;
}
.v {
  color: #444;
}
.signup-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.signup-no {
  margin-bottom: 16px;
}
.block {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0ece4;
}
.block h3 {
  font-size: 17px;
  color: #5a4632;
  margin-bottom: 10px;
}
.rich-text {
  line-height: 1.9;
  color: #555;
  white-space: pre-wrap;
}
.info-card h4 {
  font-size: 15px;
  color: #5a4632;
  margin-bottom: 12px;
  border-left: 3px solid #8b4513;
  padding-left: 8px;
}
</style>
