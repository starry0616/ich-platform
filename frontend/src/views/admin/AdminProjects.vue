<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <b>非遗项目管理</b>
          <div class="filters">
            <el-input v-model="query.keyword" placeholder="搜索名称" clearable style="width: 180px" @keyup.enter="load(1)" />
            <el-select v-model="query.auditStatus" placeholder="审核状态" clearable style="width: 130px" @change="load(1)">
              <el-option label="待审核" :value="0" />
              <el-option label="已发布" :value="1" />
              <el-option label="已下架" :value="2" />
              <el-option label="已驳回" :value="3" />
            </el-select>
            <el-button type="primary" @click="load(1)">查询</el-button>
          </div>
        </div>
        <el-button type="success" @click="openCreate">新增项目</el-button>
      </template>
      <el-table :data="items" v-loading="loading" empty-text="暂无数据">
        <el-table-column label="ID" width="60" prop="id" />
        <el-table-column label="项目名称" min-width="200">
          <template #default="{ row }">
            <router-link :to="`/projects/${row.id}`" class="name-link">{{ row.projectName }}</router-link>
          </template>
        </el-table-column>
        <el-table-column label="门类" width="110" prop="categoryName" />
        <el-table-column label="级别" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ levelText(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="浏览量" width="80" prop="viewCount" />
        <el-table-column label="收藏量" width="80" prop="favoriteCount" />
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.auditStatus)">{{ statusText(row.auditStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.auditStatus === 0" size="small" type="success" @click="audit(row, 1)">通过</el-button>
            <el-button v-if="row.auditStatus === 0" size="small" type="danger" @click="audit(row, 3)">驳回</el-button>
            <el-button v-if="row.auditStatus === 1" size="small" type="warning" @click="audit(row, 2)">下架</el-button>
            <el-button v-if="row.auditStatus !== 1" size="small" type="primary" @click="audit(row, 1)">上架</el-button>
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
    
    <!-- 新增项目对话框 -->
    <el-dialog v-model="dialogVisible" title="新增非遗项目" width="720px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="项目名称" prop="projectName">
              <el-input v-model="form.projectName" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目编号" prop="projectCode">
              <el-input v-model="form.projectCode" maxlength="50" placeholder="如 YX-001" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="所属门类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="选择门类" style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="级别">
              <el-select v-model="form.level" style="width: 100%">
                <el-option label="国家级" :value="1" />
                <el-option label="省级" :value="2" />
                <el-option label="市级" :value="3" />
                <el-option label="县级" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="申报地区">
              <el-input v-model="form.region" maxlength="120" placeholder="如 云南省玉溪市" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="批次">
              <el-input-number v-model="form.batchNo" :min="1" :max="10" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="公布年份">
              <el-input-number v-model="form.announceYear" :min="2000" :max="2100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目类型">
              <el-select v-model="form.type" clearable style="width: 100%">
                <el-option label="新增项目" value="新增项目" />
                <el-option label="扩展项目" value="扩展项目" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="保护单位">
          <el-input v-model="form.protectionUnit" maxlength="150" />
        </el-form-item>
        <el-form-item label="封面图">
          <div class="upload-row">
            <el-upload :show-file-list="false" :http-request="doUploadCover" accept="image/*">
              <el-button type="primary" plain>上传封面</el-button>
            </el-upload>
            <el-input v-model="form.coverUrl" placeholder="或粘贴图片 URL" class="url-input" />
          </div>
        </el-form-item>
        <el-form-item label="项目简介">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="项目详情">
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="10000" show-word-limit />
        </el-form-item>
        <el-form-item label="保护现状">
          <el-input v-model="form.statusDesc" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="传承历史">
          <el-input v-model="form.history" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="关联传承人">
          <el-select v-model="form.inheritorId" placeholder="选填" clearable filterable style="width: 100%">
            <el-option v-for="i in inheritors" :key="i.id" :label="i.name" :value="i.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortNo" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">保存（直接发布）</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminProjectList, auditProject, deleteProject, createProject } from '@/api/project'
import { getCategoryList } from '@/api/project'
import { getAdminInheritorList } from '@/api/inheritor'
import { uploadMedia } from '@/api/media'

const items = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', auditStatus: undefined })

function levelText(l) {
  return { 1: '国家级', 2: '省级', 3: '市级', 4: '县级' }[l] || '—'
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
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.auditStatus !== undefined && query.auditStatus !== '') params.auditStatus = query.auditStatus
    const res = await getAdminProjectList(params)
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
      const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回项目', {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消'
      })
      remark = value
    } catch (e) {
      return
    }
  }
  try {
    await auditProject(row.id, status, remark)
    ElMessage.success('操作成功')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除项目「${row.projectName}」？`, '删除确认', { type: 'warning' })
    await deleteProject(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e.message)
  }
}


const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const categories = ref([])
const inheritors = ref([])
const form = reactive({
  projectName: '', projectCode: '', categoryId: undefined, level: 1, region: '',
  batchNo: undefined, announceYear: undefined, type: undefined, protectionUnit: '',
  coverUrl: '', summary: '', content: '', history: '', statusDesc: '',
  inheritorId: undefined, sortNo: 0
})
const formRules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择所属门类', trigger: 'change' }]
}

function openCreate() {
  Object.assign(form, {
    projectName: '', projectCode: '', categoryId: undefined, level: 1, region: '',
    batchNo: undefined, announceYear: undefined, type: undefined, protectionUnit: '',
    coverUrl: '', summary: '', content: '', history: '', statusDesc: '',
    inheritorId: undefined, sortNo: 0
  })
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

async function submitCreate() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await createProject({ ...form })
      ElMessage.success('已保存并发布')
      dialogVisible.value = false
      load(1)
    } catch (e) {
      ElMessage.error(e.message || '保存失败')
    } finally {
      submitting.value = false
    }
  })
}

onMounted(async () => {
  load(1)
  try {
    const [cs, its] = await Promise.all([
      getCategoryList(),
      getAdminInheritorList({ page: 1, size: 100 })
    ])
    categories.value = cs.data || []
    inheritors.value = (its.data && (its.data.records || its.data)) || []
  } catch (e) {}
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
.upload-row {
  display: flex;
  gap: 10px;
  width: 100%;
}
.url-input {
  flex: 1;
}
</style>
