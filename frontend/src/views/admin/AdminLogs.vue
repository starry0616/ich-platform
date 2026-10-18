<template>
  <div>
    <el-card shadow="never">
      <template #header><b>日志审计</b></template>
      <el-tabs v-model="tab" @tab-change="load(1)">
        <el-tab-pane label="操作日志" name="op" />
        <el-tab-pane label="登录日志" name="login" />
      </el-tabs>
      <!-- 操作日志：SysAuditLog（auditorName/action/remark/createTime） -->
      <el-table v-if="tab === 'op'" :data="items" v-loading="loading" empty-text="暂无日志">
        <el-table-column label="ID" width="70" prop="id" />
        <el-table-column label="操作人" width="120" prop="auditorName" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">{{ actionText(row.action) }}</template>
        </el-table-column>
        <el-table-column label="对象" width="120">
          <template #default="{ row }">{{ targetText(row.targetType) }} #{{ row.targetId }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="200" prop="remark" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <!-- 登录日志：SysLoginLog（username/ip/message/loginTime） -->
      <el-table v-else :data="items" v-loading="loading" empty-text="暂无日志">
        <el-table-column label="ID" width="70" prop="id" />
        <el-table-column label="用户" width="130" prop="username" />
        <el-table-column label="IP" width="140" prop="ip" />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="信息" min-width="200" prop="message" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.loginTime) }}</template>
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
import { getOperLog, getLoginLog } from '@/api/admin'
import { formatDate } from '@/utils/format'

const tab = ref('op')
const items = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

function targetText(t) {
  return { 1: '非遗项目', 2: '传承人', 3: '资讯', 4: '活动', 5: '用户', 6: '评论' }[t] || '对象'
}
function actionText(a) {
  return { 1: '通过', 2: '下架', 3: '驳回', 4: '删除', 5: '禁用', 6: '其他' }[a] || ('操作' + (a ?? ''))
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    const api = tab.value === 'op' ? getOperLog : getLoginLog
    const res = await api(params)
    const d = res.data || {}
    items.value = d.records || []
    total.value = d.total || 0
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
