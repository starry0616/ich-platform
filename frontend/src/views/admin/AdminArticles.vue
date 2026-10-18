<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <b>资讯管理</b>
          <div class="filters">
            <el-input v-model="query.keyword" placeholder="搜索标题" clearable style="width: 180px" @keyup.enter="load(1)" />
            <el-select v-model="query.auditStatus" placeholder="审核状态" clearable style="width: 130px" @change="load(1)">
              <el-option label="待审核" :value="0" />
              <el-option label="已发布" :value="1" />
              <el-option label="已下架" :value="2" />
              <el-option label="已驳回" :value="3" />
            </el-select>
            <el-button type="primary" @click="load(1)">查询</el-button>
          </div>
        </div>
      </template>
      <el-table :data="items" v-loading="loading" empty-text="暂无数据">
        <el-table-column label="ID" width="60" prop="id" />
        <el-table-column label="标题" min-width="220">
          <template #default="{ row }">
            <router-link :to="`/articles/${row.id}`" class="name-link">{{ row.title }}</router-link>
          </template>
        </el-table-column>
        <el-table-column label="栏目" width="110" prop="categoryName" />
        <el-table-column label="作者" width="100" prop="authorName" />
        <el-table-column label="置顶" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.isTop === 1 ? 'danger' : 'info'">{{ row.isTop === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="浏览量" width="80" prop="viewCount" />
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.auditStatus)">{{ statusText(row.auditStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.auditStatus === 0" size="small" type="success" @click="audit(row, 1)">通过</el-button>
            <el-button v-if="row.auditStatus === 0" size="small" type="danger" @click="audit(row, 3)">驳回</el-button>
            <el-button v-if="row.auditStatus === 1" size="small" type="warning" @click="audit(row, 2)">下架</el-button>
            <el-button v-if="row.auditStatus !== 1" size="small" type="primary" @click="audit(row, 1)">上架</el-button>
            <el-button size="small" :type="row.isTop === 1 ? 'default' : 'primary'" plain @click="toggleTop(row)">
              {{ row.isTop === 1 ? '取消置顶' : '置顶' }}
            </el-button>
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
import { getAdminArticleList, auditArticle, toggleArticleTop, deleteArticle } from '@/api/article'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', auditStatus: undefined })

function statusText(s) {
  return { 0: '待审核', 1: '已发布', 2: '已下架', 3: '已驳回' }[s] || '未知'
}
function statusTag(s) {
  return { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }[s] || 'info'
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.auditStatus !== undefined && query.auditStatus !== '') params.auditStatus = query.auditStatus
    const res = await getAdminArticleList(params)
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
  if (status === 3) {
    try {
      const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回资讯', {
        confirmButtonText: '确认',
        cancelButtonText: '取消'
      })
      remark = value
    } catch (e) {
      return
    }
  }
  try {
    await auditArticle(row.id, status, remark)
    ElMessage.success('操作成功')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function toggleTop(row) {
  try {
    await toggleArticleTop(row.id, row.isTop === 1 ? 0 : 1)
    ElMessage.success('操作成功')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除资讯「${row.title}」？`, '删除确认', { type: 'warning' })
    await deleteArticle(row.id)
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
