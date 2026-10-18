<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-head">
        <img src="/favicon.svg" alt="" class="logo" />
        <h2>注册非遗文化传承与互动平台</h2>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="3-50 位字母、数字或下划线" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="可选" :prefix-icon="Avatar" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="8-64 位，需同时包含字母和数字" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="form.confirm" type="password" show-password placeholder="再次输入密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="submit">注册</el-button>
      </el-form>
      <div class="switch">
        已有账号？
        <router-link to="/login" class="link">直接登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Avatar } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', nickname: '', password: '', confirm: '' })

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{3,50}$/, message: '3-50 位字母、数字或下划线', trigger: 'blur' }
  ],
  nickname: [{ max: 50, message: '昵称不能超过 50 字', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/, message: '8-64 位且同时包含字母和数字', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function submit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.register({
        username: form.username,
        nickname: form.nickname || undefined,
        password: form.password
      })
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (e) {
      ElMessage.error(e.message || '注册失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(ellipse at 20% 15%, rgba(245, 222, 179, 0.22), transparent 45%),
    radial-gradient(ellipse at 85% 80%, rgba(201, 162, 39, 0.18), transparent 40%),
    linear-gradient(140deg, #4a2c12 0%, #6d3b14 45%, #a05a2c 100%);
}
.auth-page::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px);
  background-size: 26px 26px;
  pointer-events: none;
}
.auth-page {
  position: relative;
  overflow: hidden;
}
.auth-card {
  width: 440px;
  background: #fff;
  border-radius: 14px;
  padding: 32px 40px;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.35);
  animation: auth-card-in 0.6s ease both;
}
@keyframes auth-card-in {
  from { opacity: 0; transform: translateY(26px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.auth-head {
  text-align: center;
  margin-bottom: 22px;
}
.logo {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  margin-bottom: 10px;
}
.auth-head h2 {
  font-size: 20px;
  color: #5a4632;
}
.submit {
  width: 100%;
  margin-top: 6px;
  background: #8b4513;
  border-color: #8b4513;
}
.switch {
  text-align: center;
  margin-top: 16px;
  color: #999;
  font-size: 14px;
}
.link {
  color: #8b4513;
  font-weight: 600;
}
</style>
