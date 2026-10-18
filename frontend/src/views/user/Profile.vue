<template>
  <div class="page-container">
    <div class="profile-wrap">
      <el-card shadow="never" class="profile-card" v-loading="loading">
        <div class="head">
          <el-avatar :size="64" :src="resolveUrl(profile.avatar)" class="avatar">{{ avatarText }}</el-avatar>
          <div class="info">
            <div class="name">{{ profile.nickname || profile.username }}</div>
            <div class="sub">@{{ profile.username }} · {{ roleText }}</div>
          </div>
        </div>
        <el-divider />
        <el-form :model="form" label-width="90px" class="form">
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="头像">
            <div class="avatar-upload">
              <el-upload :show-file-list="false" :http-request="doUploadAvatar" accept="image/*">
                <el-button type="primary" plain>上传头像</el-button>
              </el-upload>
              <el-input v-model="form.avatar" placeholder="或粘贴头像 URL" class="avatar-input" />
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>

        <el-divider>修改密码</el-divider>
        <el-form :model="pwd" label-width="90px" class="form">
          <el-form-item label="原密码">
            <el-input v-model="pwd.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="pwd.newPassword" type="password" show-password placeholder="8-64 位，含字母和数字" />
          </el-form-item>
          <el-form-item>
            <el-button type="warning" plain @click="savePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile, updatePassword, updateAvatar } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { resolveUrl } from '@/utils/format'
import { uploadMedia } from '@/api/media'

const userStore = useUserStore()
const loading = ref(false)
const profile = ref({})
const form = reactive({ nickname: '', phone: '', email: '', avatar: '' })
const pwd = reactive({ oldPassword: '', newPassword: '' })

const avatarText = computed(() => (profile.value.nickname || profile.value.username || 'U').charAt(0).toUpperCase())
const roleText = computed(() => {
  const r = userStore.user && userStore.user.roleCode
  return { ROLE_ADMIN: '管理员', ROLE_INHERITOR: '非遗传承人', ROLE_USER: '普通用户' }[r] || '普通用户'
})

async function load() {
  loading.value = true
  try {
    const res = await getProfile()
    profile.value = res.data || {}
    form.nickname = profile.value.nickname || ''
    form.phone = profile.value.phone || ''
    form.email = profile.value.email || ''
    form.avatar = profile.value.avatar || ''
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  try {
    await updateProfile({ nickname: form.nickname, phone: form.phone, email: form.email, avatar: form.avatar })
    // 同步 store
    userStore.user.nickname = form.nickname
    localStorage.setItem('ich_user', JSON.stringify(userStore.user))
    ElMessage.success('保存成功')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function doUploadAvatar(opt) {
  try {
    // 先经媒体接口上传拿 URL，再调用头像接口
    const res = await uploadMedia(opt.file)
    const url = res.data || ''
    const avatarUrl = typeof url === 'string' ? url : (url.url || url)
    await updateAvatar(avatarUrl)
    form.avatar = avatarUrl
    userStore.user.avatar = avatarUrl
    localStorage.setItem('ich_user', JSON.stringify(userStore.user))
    ElMessage.success('头像已更新')
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function savePassword() {
  if (pwd.newPassword.length < 8) return ElMessage.warning('新密码至少 8 位')
  try {
    await updatePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码修改成功')
    pwd.oldPassword = ''
    pwd.newPassword = ''
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.profile-wrap {
  max-width: 720px;
  margin: 0 auto;
}
.head {
  display: flex;
  align-items: center;
  gap: 16px;
}
.avatar {
  background: #8b4513;
  color: #fff;
  font-size: 24px;
  font-weight: bold;
}
.name {
  font-size: 20px;
  font-weight: 700;
  color: #333;
}
.sub {
  color: #999;
  font-size: 13px;
  margin-top: 4px;
}
.form {
  max-width: 460px;
}
.avatar-upload {
  display: flex;
  gap: 10px;
  width: 100%;
}
.avatar-input {
  flex: 1;
}
</style>
