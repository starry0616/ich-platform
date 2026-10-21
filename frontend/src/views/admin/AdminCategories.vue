<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <b>非遗类别管理</b>
          <el-button type="primary" @click="openEdit()">新增类别</el-button>
        </div>
      </template>
      <el-table :data="items" v-loading="loading" empty-text="暂无数据">
        <el-table-column label="ID" width="70" prop="id" />
        <el-table-column label="类别名称" min-width="160" prop="categoryName" />
        <el-table-column label="编号" width="90" prop="categoryCode" />
        <el-table-column label="父级" min-width="140">
          <template #default="{ row }">
            {{ parentName(row.parentId) }}
          </template>
        </el-table-column>
        <el-table-column label="排序" width="80" prop="sortNo" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" text @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑类别' : '新增类别'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="类别名称" required>
          <el-input v-model="form.categoryName" placeholder="如：传统技艺" maxlength="30" />
        </el-form-item>
        <el-form-item label="类别编号">
          <el-input v-model="form.categoryCode" placeholder="可选，如 Ⅷ" maxlength="10" />
        </el-form-item>
        <el-form-item label="父级类别">
          <el-select v-model="form.parentId" placeholder="默认顶层" clearable>
            <el-option label="顶层（无父级）" :value="0" />
            <el-option
              v-for="p in topParents"
              :key="p.id"
              :label="p.categoryName"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNo" :min="0" />
        </el-form-item>
        <el-form-item label="图标地址">
          <el-input v-model="form.icon" placeholder="可选，图标 URL" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminCategoryList,
  createCategory,
  updateCategory,
  deleteCategory
} from '@/api/admin'

const items = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const form = reactive({
  id: null,
  categoryName: '',
  categoryCode: '',
  parentId: 0,
  sortNo: 0,
  icon: '',
  status: 1
})

// 父级下拉仅提供顶层类别，避免多层嵌套带来的归属混乱
const topParents = computed(() => items.value.filter((c) => !c.parentId || c.parentId === 0))

function parentName(parentId) {
  if (!parentId || parentId === 0) return '顶层'
  const p = items.value.find((c) => c.id === parentId)
  return p ? p.categoryName : `#${parentId}`
}

async function load() {
  loading.value = true
  try {
    const res = await getAdminCategoryList()
    items.value = res.data || []
  } catch (e) {
    items.value = []
    ElMessage.error(e.message || '类别列表加载失败')
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      categoryName: row.categoryName,
      categoryCode: row.categoryCode || '',
      parentId: row.parentId ?? 0,
      sortNo: row.sortNo ?? 0,
      icon: row.icon || '',
      status: row.status ?? 1
    })
  } else {
    Object.assign(form, {
      id: null,
      categoryName: '',
      categoryCode: '',
      parentId: 0,
      sortNo: 0,
      icon: '',
      status: 1
    })
  }
  dialogVisible.value = true
}

async function save() {
  if (!form.categoryName.trim()) {
    ElMessage.warning('类别名称不能为空')
    return
  }
  saving.value = true
  try {
    const payload = {
      categoryName: form.categoryName.trim(),
      categoryCode: form.categoryCode.trim() || null,
      parentId: form.parentId ?? 0,
      sortNo: form.sortNo ?? 0,
      icon: form.icon.trim() || null,
      status: form.status
    }
    if (form.id) {
      await updateCategory(form.id, payload)
    } else {
      await createCategory(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除类别「${row.categoryName}」？`, '删除确认', { type: 'warning' })
    await deleteCategory(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(() => load())
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
