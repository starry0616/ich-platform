<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <b>传承人管理</b>
          <div class="filters">
            <el-input v-model="query.keyword" placeholder="搜索姓名/项目" clearable style="width: 180px" @keyup.enter="load(1)" />
            <el-select v-model="query.certStatus" placeholder="认证状态" clearable style="width: 130px" @change="load(1)">
              <el-option label="待审核" :value="0" />
              <el-option label="已认证" :value="1" />
              <el-option label="已驳回" :value="2" />
            </el-select>
            <el-button type="primary" @click="load(1)">查询</el-button>
          </div>
        </div>
      </template>
      <el-table :data="items" v-loading="loading" empty-text="暂无数据">
        <el-table-column label="ID" width="60" prop="id" />
        <el-table-column label="姓名" width="100" prop="name" />
        <el-table-column label="级别" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ levelText(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属项目" min-width="160" prop="projectName" />
        <el-table-column label="地区" width="110" prop="region" />
        <el-table-column label="从业年限" width="90">
          <template #default="{ row }">{{ row.craftYears || 0 }} 年</template>
        </el-table-column>
        <el-table-column label="认证状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.certStatus)">{{ statusText(row.certStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.certStatus === 0" size="small" type="success" @click="audit(row, 1)">通过</el-button>
            <el-button v-if="row.certStatus === 0" size="small" type="danger" @click="audit(row, 2)">驳回</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminInheritorList, auditInheritor, deleteInheritor } from '@/api/inheritor'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', certStatus: undefined })

function levelText(l) {
  return { 1: '国家级', 2: '省级', 3: '市级', 4: '县级' }[l] || '—'
}
function statusText(s) {
  return { 0: '待审核', 1: '已认证', 2: '已驳回' }[s] || '未知'
}
function statusTag(s) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info'
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.certStatus !== undefined && query.certStatus !== '') params.certStatus = query.certStatus
    const res = await getAdminInheritorList(params)
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
  if (status === 2) {
    try {
      const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回认证', {
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      })
      remark = value
    } catch (e) {
      return
    }
  }
  try {
    await auditInheritor(row.id, status)
    ElMessage.success('操作成功')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除传承人「${row.name}」？`, '删除确认', { type: 'warning' })
    await deleteInheritor(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e.message)
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
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
