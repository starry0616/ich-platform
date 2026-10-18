<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <b>用户管理</b>
          <div class="filters">
            <el-input v-model="query.keyword" placeholder="搜索用户名/昵称" clearable style="width: 200px" @keyup.enter="load(1)" />
            <el-button type="primary" @click="load(1)">查询</el-button>
          </div>
        </div>
      </template>
      <el-table :data="items" v-loading="loading" empty-text="暂无数据">
        <el-table-column label="ID" width="70" prop="id" />
        <el-table-column label="用户名" width="130" prop="username" />
        <el-table-column label="昵称" width="130" prop="nickname" />
        <el-table-column label="角色" width="140">
          <template #default="{ row }">
            <el-tag size="small" :type="roleTag(row.roleId)">{{ roleName(row.roleId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-select
              :model-value="row.roleId"
              size="small"
              style="width: 110px"
              @change="(v) => changeRole(row, v)"
            >
              <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
            </el-select>
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
import { getAdminUserList, setUserStatus, setUserRole, getRoles } from '@/api/admin'
import { formatDate } from '@/utils/format'

const items = ref([])
const roles = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '' })

function roleName(roleId) {
  const r = roles.value.find((x) => x.id === roleId)
  return r ? r.roleName : '用户'
}
function roleTag(roleId) {
  const r = roles.value.find((x) => x.id === roleId)
  const code = r ? r.roleCode : ''
  return { ROLE_ADMIN: 'danger', ROLE_INHERITOR: 'warning', ROLE_USER: 'info' }[code] || 'info'
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    const res = await getAdminUserList(params)
    const d = res.data || {}
    items.value = d.records || []
    total.value = d.total || 0
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(`确认${target === 0 ? '禁用' : '启用'}用户「${row.username}」？`, '状态确认', { type: 'warning' })
    await setUserStatus(row.id, { status: target })
    row.status = target
    ElMessage.success('操作成功')
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e.message)
  }
}

async function changeRole(row, roleId) {
  try {
    await setUserRole(row.id, { roleId })
    row.roleId = roleId
    ElMessage.success('角色已更新')
  } catch (e) {
    ElMessage.error(e.message)
    load()
  }
}

onMounted(async () => {
  try {
    const res = await getRoles()
    roles.value = res.data || []
  } catch (e) {}
  load(1)
})
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
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
