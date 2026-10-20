<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header><b>我的报名</b></template>
      <el-table :data="items" v-loading="loading" empty-text="暂无报名记录">
        <el-table-column label="活动" min-width="200">
          <template #default="{ row }">
            <router-link :to="`/activities/${row.activityId}`" class="act-link">{{ row.activityTitle || `活动#${row.activityId}` }}</router-link>
          </template>
        </el-table-column>
        <el-table-column label="报名编号" width="150">
          <template #default="{ row }">
            <span v-if="row.signupNo" class="signup-no">{{ row.signupNo }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="报名时间" width="160">
          <template #default="{ row }">{{ formatDate(row.signupTime) }}</template>
        </el-table-column>
        <el-table-column label="签到状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.status === 2" size="small" type="info">已取消</el-tag>
            <el-tag v-else size="small" :type="row.checkinStatus === 1 ? 'success' : 'warning'">{{ row.checkinStatus === 1 ? '已签到' : '未签到' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 2"
              size="small"
              type="danger"
              text
              :loading="cancellingId === row.id"
              :disabled="cancellingId === row.id"
              @click="cancel(row)"
            >取消报名</el-button>
            <span v-else class="canceled-text">已取消</span>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMySignups, cancelSignup } from '@/api/activity'
import { formatDate } from '@/utils/format'

const items = ref([])
const total = ref(0)
const loading = ref(false)
// 正在取消的报名记录 id，按行加锁，避免重复提交；请求结束在 finally 清空
const cancellingId = ref(null)
const query = reactive({ page: 1, size: 10 })

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await getMySignups({ page: query.page, size: query.size })
    const d = res.data || {}
    items.value = d.records || []
    total.value = d.total || 0
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
}

async function cancel(row) {
  if (cancellingId.value) return
  cancellingId.value = row.id
  try {
    await ElMessageBox.confirm('确认取消该活动报名？', '取消报名', { type: 'warning' })
    await cancelSignup(row.activityId)
    ElMessage.success('已取消报名')
    load()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e.message || '取消失败')
  } finally {
    cancellingId.value = null
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.act-link {
  color: #5a4632;
  font-weight: 600;
}
.act-link:hover {
  color: #8b4513;
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
.canceled-text {
  color: #bbb;
  font-size: 13px;
}
.signup-no {
  font-family: monospace;
  font-size: 13px;
  color: #5a4632;
}
</style>
