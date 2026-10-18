<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <b>AI 知识库管理</b>
          <div>
            <el-button type="primary" plain @click="reindex">重建索引</el-button>
            <el-button type="primary" @click="openEdit()">新增知识</el-button>
          </div>
        </div>
      </template>
      <el-table :data="items" v-loading="loading" empty-text="暂无数据">
        <el-table-column label="ID" width="70" prop="id" />
        <el-table-column label="标题" min-width="180" prop="title" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ typeText(row.sourceType) }}</template>
        </el-table-column>
        <el-table-column label="关联对象" width="120">
          <template #default="{ row }">{{ row.sourceId ? `关联#${row.sourceId}` : row.categoryId ? `门类#${row.categoryId}` : '通用' }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">{{ formatDate(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
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

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑知识' : '新增知识'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入知识标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.sourceType" style="width: 100%">
            <el-option label="非遗项目" :value="1" />
            <el-option label="传承人" :value="2" />
            <el-option label="活动" :value="3" />
            <el-option label="通用知识" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联项目">
          <el-input-number v-model="form.sourceId" :min="0" placeholder="可选" style="width: 100%" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入知识内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getKnowledgeList, createKnowledge, updateKnowledge, deleteKnowledge, reindexKnowledge } from '@/api/admin'
import { formatDate } from '@/utils/format'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })
const dialogVisible = ref(false)
const form = reactive({ id: null, title: '', sourceType: 0, sourceId: null, content: '' })

function typeText(t) {
  return { 0: '通用知识', 1: '非遗项目', 2: '传承人', 3: '活动' }[t] || '通用'
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await getKnowledgeList({ page: query.page, size: query.size })
    const d = res.data || {}
    items.value = d.records || []
    total.value = d.total || 0
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      title: row.title || '',
      sourceType: row.sourceType ?? 0,
      sourceId: row.sourceId || null,
      content: row.content || ''
    })
  } else {
    Object.assign(form, { id: null, title: '', sourceType: 0, sourceId: null, content: '' })
  }
  dialogVisible.value = true
}

async function save() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  try {
    if (form.id) {
      await updateKnowledge(form.id, { ...form })
    } else {
      await createKnowledge({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除知识「${row.title}」？`, '删除确认', { type: 'warning' })
    await deleteKnowledge(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e.message)
  }
}

async function reindex() {
  try {
    await ElMessageBox.confirm('确认重建知识库索引？', '重建索引', { type: 'warning' })
    await reindexKnowledge()
    ElMessage.success('索引已重建')
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
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
