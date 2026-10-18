<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <b>活动管理</b>
          <div class="filters">
            <el-input v-model="query.keyword" placeholder="搜索活动" clearable style="width: 180px" @keyup.enter="load(1)" />
            <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="load(1)">
              <el-option label="待审核" :value="0" />
              <el-option label="报名中" :value="1" />
              <el-option label="进行中" :value="2" />
              <el-option label="已结束" :value="3" />
            </el-select>
            <el-button type="primary" @click="load(1)">查询</el-button>
          </div>
        </div>
      </template>
      <el-table :data="items" v-loading="loading" empty-text="暂无数据">
        <el-table-column label="ID" width="60" prop="id" />
        <el-table-column label="活动标题" min-width="200">
          <template #default="{ row }">
            <router-link :to="`/activities/${row.id}`" class="name-link">{{ row.title }}</router-link>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ typeText(row.activityType) }}</template>
        </el-table-column>
        <el-table-column label="地点" width="130" prop="address" />
        <el-table-column label="报名" width="90">
          <template #default="{ row }">{{ row.signed || 0 }} / {{ row.quota || '∞' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="success" @click="audit(row, 1)">通过</el-button>
            <el-button v-if="row.status === 0" size="small" type="danger" @click="audit(row, 5)">驳回</el-button>
            <el-button v-if="row.status === 1 || row.status === 2" size="small" type="primary" plain @click="openSignups(row)">报名名单</el-button>
            <el-button size="small" type="danger" text @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="load()"
        />
      </div>
    </el-card>
    
    <!-- 报名名单对话框 -->
    <el-dialog v-model="signupVisible" :title="`报名名单：${currentActivity ? currentActivity.title : ''}`" width="760px">
      <el-table :data="signups" v-loading="signupLoading" empty-text="暂无报名记录">
        <el-table-column label="报名编号" width="110" prop="signupNo" />
        <el-table-column label="姓名" width="110" prop="realName" />
        <el-table-column label="电话" width="130" prop="phone" />
        <el-table-column label="备注" min-width="140" prop="remark" show-overflow-tooltip />
        <el-table-column label="报名时间" width="160">
          <template #default="{ row }">{{ formatDate(row.signupTime) }}</template>
        </el-table-column>
        <el-table-column label="签到" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.checkinStatus === 1 ? 'success' : 'info'">{{ row.checkinStatus === 1 ? '已签到' : '未签到' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.checkinStatus !== 1" size="small" type="primary" @click="doCheckin(row)">核销</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminActivityList, auditActivity, deleteActivity, getActivitySignups, checkin } from '@/api/activity'
import { formatDate } from '@/utils/format'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', status: undefined })

function typeText(t) {
  return { 1: '展演展示', 2: '体验课堂', 3: '技艺传习', 4: '展览讲座' }[t] || '活动'
}
function statusText(s) {
  return { 0: '待审核', 1: '报名中', 2: '进行中', 3: '已结束' }[s] || '未知'
}
function statusTag(s) {
  return { 0: 'warning', 1: 'success', 2: 'primary', 3: 'info' }[s] || 'info'
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== undefined && query.status !== '') params.status = query.status
    const res = await getAdminActivityList(params)
    const d = res.data || {}
    items.value = d.records || []
    total.value = d.total || 0
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
}

async function audit(row, status) {
  let remark = ''
  if (status === 5) {
    try {
      const { value } = await ElMessageBox.prompt('请输入操作原因', '操作确认', {
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      })
      remark = value
    } catch (e) {
      return
    }
  }
  try {
    await auditActivity(row.id, status, remark)
    ElMessage.success('操作成功')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除活动「${row.title}」？`, '删除确认', { type: 'warning' })
    await deleteActivity(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e.message)
  }
}


const signupVisible = ref(false)
const signupLoading = ref(false)
const currentActivity = ref(null)
const signups = ref([])

async function openSignups(row) {
  currentActivity.value = row
  signupVisible.value = true
  signupLoading.value = true
  try {
    const res = await getActivitySignups(row.id, { page: 1, size: 100 })
    const d = res.data || {}
    signups.value = (d.records || d) || []
  } catch (e) {
    signups.value = []
  } finally {
    signupLoading.value = false
  }
}

async function doCheckin(row) {
  try {
    await checkin(currentActivity.value.id, row.signupNo)
    ElMessage.success('签到成功')
    row.checkinStatus = 1
  } catch (e) {
    ElMessage.error(e.message || '核销失败')
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.filters {
  display: flex;
  gap: 8px;
}
.name-link {
  color: #5a4632;
  font-weight: 600;
}
.name-link:hover {
  color: #8b4513;
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
