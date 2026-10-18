<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <b>我的发布</b>
          <el-button type="primary" @click="openPublish">发布资讯</el-button>
        </div>
      </template>
      <el-table :data="items" v-loading="loading" empty-text="暂无发布记录">
        <el-table-column label="标题" min-width="240">
          <template #default="{ row }">
            <router-link :to="`/articles/${row.id}`" class="title-link">{{ row.title }}</router-link>
          </template>
        </el-table-column>
        <el-table-column label="栏目" width="120">
          <template #default="{ row }">{{ row.categoryName || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.auditStatus)">{{ statusText(row.auditStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="浏览量" width="90" prop="viewCount" />
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ formatDate(row.publishTime) }}</template>
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

    <!-- 发布资讯对话框 -->
    <el-dialog v-model="dialogVisible" title="发布资讯" width="640px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="栏目" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择栏目" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图">
          <div class="upload-row">
            <el-upload :show-file-list="false" :http-request="doUploadCover" accept="image/*">
              <el-button type="primary" plain>上传封面</el-button>
            </el-upload>
            <el-input v-model="form.coverUrl" placeholder="或粘贴图片 URL" class="url-input" />
          </div>
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" maxlength="10000" show-word-limit />
        </el-form-item>
        <el-form-item label="关联项目">
          <el-select v-model="form.projectId" placeholder="选填：关联的非遗项目" clearable filterable style="width: 100%">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.source" maxlength="50" placeholder="选填：如「市非遗保护中心」" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">提交（等待审核）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyArticles, getArticleCategories, createArticle } from '@/api/article'
import { getProjectList } from '@/api/project'
import { uploadMedia } from '@/api/media'
import { formatDate } from '@/utils/format'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const categories = ref([])
const projects = ref([])
const form = reactive({ title: '', categoryId: undefined, coverUrl: '', summary: '', content: '', projectId: undefined, source: '' })

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择栏目', trigger: 'change' }],
  content: [{ required: true, message: '请输入正文内容', trigger: 'blur' }]
}

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
    const res = await getMyArticles({ page: query.page, size: query.size })
    const d = res.data || {}
    items.value = d.records || []
    total.value = d.total || 0
  } catch (e) {
    items.value = []
  } finally {
    loading.value = false
  }
}

function openPublish() {
  Object.assign(form, { title: '', categoryId: undefined, coverUrl: '', summary: '', content: '', projectId: undefined, source: '' })
  dialogVisible.value = true
}

async function doUploadCover(opt) {
  try {
    const res = await uploadMedia(opt.file)
    const url = res.data || ''
    form.coverUrl = typeof url === 'string' ? url : (url.url || url)
    ElMessage.success('封面已上传')
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function submit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await createArticle({ ...form })
      ElMessage.success('发布成功，等待管理员审核')
      dialogVisible.value = false
      load(1)
    } catch (e) {
      ElMessage.error(e.message || '发布失败')
    } finally {
      submitting.value = false
    }
  })
}

onMounted(async () => {
  load(1)
  try {
    const [c, p] = await Promise.all([
      getArticleCategories(),
      getProjectList({ page: 1, size: 100 })
    ])
    categories.value = c.data || []
    projects.value = (p.data && (p.data.records || p.data)) || []
  } catch (e) {}
})
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title-link {
  color: #5a4632;
  font-weight: 600;
}
.title-link:hover {
  color: #8b4513;
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
.upload-row {
  display: flex;
  gap: 10px;
  width: 100%;
}
.url-input {
  flex: 1;
}
</style>
