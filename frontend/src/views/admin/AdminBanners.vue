<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <b>轮播图管理</b>
          <el-button type="primary" @click="openEdit()">新增轮播图</el-button>
        </div>
      </template>
      <el-table :data="items" v-loading="loading" empty-text="暂无数据">
        <el-table-column label="ID" width="70" prop="id" />
        <el-table-column label="图片" width="160">
          <template #default="{ row }">
            <el-image :src="resolveUrl(row.imageUrl)" fit="cover" class="thumb" :preview-src-list="[resolveUrl(row.imageUrl)]">
              <template #error><div class="img-ph"><el-icon><Picture /></el-icon></div></template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="150" prop="title" />
        <el-table-column label="链接" min-width="160" prop="linkUrl" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑轮播图' : '新增轮播图'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" placeholder="可选" />
        </el-form-item>
        <el-form-item label="图片地址" required>
          <el-input v-model="form.imageUrl" placeholder="请输入图片 URL 或 /uploads/xxx.jpg" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.linkUrl" placeholder="可选，如 /projects/1" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNo" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
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
import { getAdminBannerList, createBanner, updateBanner, deleteBanner } from '@/api/admin'
import { resolveUrl } from '@/utils/format'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })
const dialogVisible = ref(false)
const form = reactive({ id: null, title: '', subtitle: '', imageUrl: '', linkUrl: '', sortNo: 0, status: 1 })

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await getAdminBannerList({ page: query.page, size: query.size })
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
    Object.assign(form, { id: row.id, title: row.title, subtitle: row.subtitle, imageUrl: row.imageUrl, linkUrl: row.linkUrl, sortNo: row.sortNo ?? 0, status: row.status ?? 1 })
  } else {
    Object.assign(form, { id: null, title: '', subtitle: '', imageUrl: '', linkUrl: '', sortNo: 0, status: 1 })
  }
  dialogVisible.value = true
}

async function save() {
  if (!form.title.trim() || !form.imageUrl.trim()) {
    ElMessage.warning('标题和图片地址不能为空')
    return
  }
  try {
    if (form.id) {
      await updateBanner(form.id, { ...form })
    } else {
      await createBanner({ ...form })
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
    await ElMessageBox.confirm(`确认删除轮播图「${row.title}」？`, '删除确认', { type: 'warning' })
    await deleteBanner(row.id)
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
.thumb {
  width: 140px;
  height: 60px;
  border-radius: 4px;
}
.img-ph {
  width: 140px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0ece4;
  color: #c9b79c;
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
