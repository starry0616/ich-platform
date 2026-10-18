<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <b>消息中心</b>
          <el-button size="small" text type="primary" @click="markAll">全部已读</el-button>
        </div>
      </template>
      <el-tabs v-model="tab" @tab-change="load(1)">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="未读" name="unread" />
      </el-tabs>
      <div v-loading="loading">
        <el-empty v-if="!items.length" description="暂无消息" />
        <div v-for="m in items" :key="m.id" class="msg-item" :class="{ unread: !m.isRead }" @click="read(m)">
          <el-badge is-dot :hidden="m.isRead">
            <el-icon class="msg-icon" :size="22"><Bell /></el-icon>
          </el-badge>
          <div class="msg-body">
            <div class="msg-title">{{ m.title }}</div>
            <div class="msg-content">{{ m.content }}</div>
          </div>
          <div class="msg-time">{{ formatDate(m.createTime) }}</div>
        </div>
      </div>
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
import { getMessageList, markRead, markAllRead } from '@/api/message'
import { formatDate } from '@/utils/format'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const tab = ref('all')
const query = reactive({ page: 1, size: 10 })

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (tab.value === 'unread') params.unread = true
    const res = await getMessageList(params)
    const d = res.data || {}
    items.value = d.records || []
    total.value = d.total || 0
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
}

async function read(m) {
  if (m.isRead) return
  try {
    await markRead(m.id)
    m.isRead = true
  } catch (e) {}
}

async function markAll() {
  try {
    await markAllRead()
    items.value.forEach((m) => (m.isRead = true))
  } catch (e) {}
}

onMounted(() => load(1))
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.msg-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 8px;
  border-bottom: 1px solid #f5f2ec;
  cursor: pointer;
}
.msg-item.unread {
  background: #fdf9f0;
}
.msg-icon {
  color: #8b4513;
}
.msg-body {
  flex: 1;
}
.msg-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}
.msg-content {
  color: #888;
  font-size: 13px;
}
.msg-time {
  color: #bbb;
  font-size: 12px;
  white-space: nowrap;
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
