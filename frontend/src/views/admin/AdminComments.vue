<template>
  <div>
    <el-card shadow="never">
      <template #header><b>评论与举报管理</b></template>
      <el-tabs v-model="tab" @tab-change="load(1)">
        <el-tab-pane label="评论列表" name="comments" />
        <el-tab-pane label="举报处理" name="reports" />
      </el-tabs>

      <!-- 评论列表 -->
      <el-table v-if="tab === 'comments'" :data="items" v-loading="loading" empty-text="暂无数据">
        <el-table-column label="ID" width="70" prop="id" />
        <el-table-column label="用户" width="110" prop="userNickname" />
        <el-table-column label="对象" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ targetText(row.targetType) }}</el-tag>
            <span class="tid">#{{ row.targetId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评论内容" min-width="220" prop="content" show-overflow-tooltip />
        <el-table-column label="点赞数" width="80" prop="likeCount" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" type="danger" @click="setStatus(row, 0)">隐藏</el-button>
            <el-button v-else size="small" type="success" @click="setStatus(row, 1)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 举报列表 -->
      <el-table v-else :data="items" v-loading="loading" empty-text="暂无举报">
        <el-table-column label="ID" width="70" prop="id" />
        <el-table-column label="举报人" width="110">
          <template #default="{ row }">{{ row.reporter_name || row.user_id || '—' }}</template>
        </el-table-column>
        <el-table-column label="对象" min-width="200">
          <template #default="{ row }">{{ targetText(row.target_type) }} #{{ row.target_id }}</template>
        </el-table-column>
        <el-table-column label="举报原因" min-width="180" prop="reason" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 0 ? 'warning' : row.status === 1 ? 'success' : 'info'">
              {{ row.status === 0 ? '待处理' : row.status === 1 ? '已处理' : '已驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button size="small" type="danger" @click="handleReport(row)">处理</el-button>
              <el-button size="small" type="info" @click="rejectReport(row)">驳回</el-button>
            </template>
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
import { ref, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCommentList, setCommentStatus, getReportList, handleReport as apiHandleReport, rejectReport as apiRejectReport } from '@/api/admin'

const tab = ref('comments')
const items = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

function targetText(t) {
  return { 1: '非遗项目', 2: '传承人', 3: '资讯', 4: '活动' }[t] || '对象'
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    const api = tab.value === 'comments' ? getCommentList : getReportList
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

async function setStatus(row, status) {
  try {
    await setCommentStatus(row.id, { status })
    ElMessage.success('操作成功')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function handleReport(row) {
  try {
    await ElMessageBox.confirm('确认处理该举报？', '处理举报', { type: 'warning' })
    await apiHandleReport(row.id, {})
    ElMessage.success('已处理')
    load()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e.message)
  }
}

async function rejectReport(row) {
  try {
    await apiRejectReport(row.id, {})
    ElMessage.success('已驳回')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

load(1)
</script>

<style scoped>
.tid {
  color: #bbb;
  font-size: 12px;
  margin-left: 4px;
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
