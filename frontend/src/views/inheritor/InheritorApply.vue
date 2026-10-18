<template>
  <div class="page-container apply-page">
    <el-card shadow="never" class="apply-card">
      <template #header><b>传承人认证申请</b></template>

      <!-- 已申请状态展示 -->
      <el-alert v-if="status !== null && status !== undefined" :type="status === 1 ? 'success' : status === 2 ? 'danger' : 'warning'" :closable="false" class="status-tip">
        <template #title>
          {{ status === 1 ? '恭喜，您已成为认证传承人！' : status === 2 ? '认证申请已被驳回，可修改后重新提交。' : '认证申请审核中，请耐心等待。' }}
        </template>
      </el-alert>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="form">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" maxlength="30" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="传承级别" prop="level">
          <el-select v-model="form.level" placeholder="选择级别" style="width: 100%">
            <el-option label="国家级" :value="1" />
            <el-option label="省级" :value="2" />
            <el-option label="市级" :value="3" />
            <el-option label="县级" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属地区" prop="region">
          <el-input v-model="form.region" maxlength="50" placeholder="如：云南省玉溪市" />
        </el-form-item>
        <el-form-item label="关联非遗项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="选择您传承的非遗项目" filterable style="width: 100%">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="从业年限" prop="craftYears">
          <el-input-number v-model="form.craftYears" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="证书编号">
          <el-input v-model="form.certNo" maxlength="50" placeholder="传承人证书编号（选填）" />
        </el-form-item>
        <el-form-item label="证书图片">
          <div class="upload-row">
            <el-upload :show-file-list="false" :http-request="doUploadCert" accept="image/*">
              <el-button type="primary" plain>上传证书</el-button>
            </el-upload>
            <el-input v-model="form.certImg" placeholder="或粘贴证书图片 URL" class="url-input" />
          </div>
        </el-form-item>
        <el-form-item label="个人介绍" prop="intro">
          <el-input v-model="form.intro" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="请介绍您的传承经历、师承关系与技艺特点" />
        </el-form-item>
        <el-form-item label="代表作品">
          <el-input v-model="form.works" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="代表作品或主要成果（选填）" />
        </el-form-item>
        <el-form-item label="联系方式" prop="contact">
          <el-input v-model="form.contact" maxlength="50" placeholder="联系电话或微信" />
        </el-form-item>
        <el-form-item>
          <el-button v-if="status !== 1" type="primary" :loading="loading" @click="submit">提交申请</el-button>
          <el-button v-if="status === 2" @click="loadStatus">刷新状态</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { applyInheritor, getApplyStatus } from '@/api/inheritor'
import { getProjectList } from '@/api/project'
import { uploadMedia } from '@/api/media'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const status = ref(null)
const projects = ref([])

const form = reactive({
  name: '',
  gender: 1,
  level: undefined,
  region: '',
  projectId: undefined,
  craftYears: undefined,
  certNo: '',
  certImg: '',
  intro: '',
  works: '',
  contact: ''
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  level: [{ required: true, message: '请选择传承级别', trigger: 'change' }],
  region: [{ required: true, message: '请输入所属地区', trigger: 'blur' }],
  projectId: [{ required: true, message: '请选择关联非遗项目', trigger: 'change' }],
  craftYears: [{ required: true, message: '请输入从业年限', trigger: 'blur' }],
  intro: [{ required: true, message: '请输入个人介绍', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系方式', trigger: 'blur' }]
}

async function loadStatus() {
  try {
    const res = await getApplyStatus()
    const it = res.data || {}
    // 返回 IchInheritor 实体：certStatus 0待审 1认证 2驳回
    if (it && it.certStatus !== undefined && it.certStatus !== null) {
      status.value = it.certStatus
      form.name = it.name || form.name
      form.gender = it.gender || form.gender
      form.level = it.level ?? form.level
      form.region = it.region || form.region
      form.projectId = it.projectId ?? form.projectId
      form.craftYears = it.craftYears ?? form.craftYears
      form.certNo = it.certNo || ''
      form.certImg = it.certImg || ''
      form.intro = it.intro || ''
      form.works = it.works || ''
      form.contact = it.contact || ''
    } else {
      status.value = null
    }
  } catch (e) {
    // 未提交过申请：后端返回 NOT_FOUND，按未申请处理
    status.value = null
  }
}

async function doUploadCert(opt) {
  try {
    const res = await uploadMedia(opt.file)
    const url = res.data || ''
    form.certImg = typeof url === 'string' ? url : (url.url || url)
    ElMessage.success('证书图片已上传')
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function submit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await applyInheritor({ ...form })
      ElMessage.success('申请已提交，等待管理员审核')
      status.value = 0
    } catch (e) {
      ElMessage.error(e.message || '提交失败')
    } finally {
      loading.value = false
    }
  })
}

onMounted(async () => {
  loadStatus()
  try {
    const res = await getProjectList({ page: 1, size: 100 })
    projects.value = (res.data && (res.data.records || res.data)) || []
  } catch (e) {}
})
</script>

<style scoped>
.apply-page {
  max-width: 760px;
  margin: 0 auto;
}
.form {
  margin-top: 16px;
}
.status-tip {
  margin-bottom: 8px;
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
