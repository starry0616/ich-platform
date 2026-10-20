<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header><b>我的收藏</b></template>
      <el-table :data="items" v-loading="loading" empty-text="暂无收藏">
        <el-table-column label="对象类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ typeText(row.targetType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="名称">
          <template #default="{ row }">
            <router-link :to="targetLink(row)" class="name-link">{{ row.targetTitle || '—' }}</router-link>
          </template>
        </el-table-column>
        <el-table-column label="收藏时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button size="small" type="danger" text @click="remove(row)">取消收藏</el-button>
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
import { ElMessage } from 'element-plus'
import { getMyFavorites, favorite } from '@/api/interaction'
import { formatDate } from '@/utils/format'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

// 与后端 StatusConstants.Interaction 一致：1项目 2资讯 3活动 4传承人
function typeText(t) {
  return { 1: '非遗项目', 2: '资讯', 3: '活动', 4: '传承人' }[t] || '未知对象'
}
function targetLink(row) {
  const map = { 1: '/projects/', 2: '/articles/', 3: '/activities/', 4: '/inheritors/' }
  const prefix = map[row.targetType]
  // 未知类型不拼接、不跳错页面
  return prefix ? prefix + row.targetId : '#'
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await getMyFavorites({ page: query.page, size: query.size })
    const d = res.data || {}
    items.value = d.records || []
    total.value = d.total || 0
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
}

async function remove(row) {
  try {
    await favorite(row.targetType, row.targetId)
    ElMessage.success('已取消收藏')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(() => load(1))
</script>

<style scoped>
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
