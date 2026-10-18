<template>
  <div class="front-layout">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <img src="/favicon.svg" alt="logo" class="logo-img" />
          <span class="logo-text">非遗文化传承与互动平台</span>
        </router-link>
        <nav class="nav">
          <router-link to="/" class="nav-item" exact-active-class="active">首页</router-link>
          <router-link to="/projects" class="nav-item" active-class="active">非遗项目</router-link>
          <router-link to="/inheritors" class="nav-item" active-class="active">传承人</router-link>
          <router-link to="/articles" class="nav-item" active-class="active">非遗资讯</router-link>
          <router-link to="/activities" class="nav-item" active-class="active">活动</router-link>
          <router-link to="/ai" class="nav-item" active-class="active">智能问答</router-link>
        </nav>
        <div class="user-area">
          <template v-if="userStore.isLoggedIn">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="msg-badge">
              <el-icon class="msg-icon" @click="router.push('/messages')"><Bell /></el-icon>
            </el-badge>
            <el-dropdown @command="handleCommand">
              <span class="user-link">
                <el-avatar :size="30" :src="avatar" class="avatar">{{ avatarText }}</el-avatar>
                <span class="uname">{{ userStore.nickname }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="apply">传承人认证</el-dropdown-item>
                  <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                  <el-dropdown-item command="articles">我的发布</el-dropdown-item>
                  <el-dropdown-item command="activities">我的报名</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>管理后台</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link to="/login" class="btn-login">登录</router-link>
            <router-link to="/register" class="btn-register">注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <!-- 内容区 -->
    <main class="content">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-cols">
          <div class="f-col f-brand">
            <div class="f-logo">非遗文化传承与互动平台</div>
            <p class="f-desc">基于 Spring Boot 的非遗文化传承与互动平台设计与实现，汇聚非遗项目、传承人、资讯与活动，推动中华优秀传统文化数字化传承。</p>
          </div>
          <div class="f-col">
            <div class="f-title">快速导航</div>
            <router-link to="/projects">非遗项目</router-link>
            <router-link to="/inheritors">传承人</router-link>
            <router-link to="/articles">非遗资讯</router-link>
            <router-link to="/activities">传承活动</router-link>
          </div>
          <div class="f-col">
            <div class="f-title">我的服务</div>
            <router-link to="/ai">智能问答</router-link>
            <router-link to="/profile">个人中心</router-link>
            <router-link to="/inheritor-apply">传承人认证</router-link>
            <router-link to="/messages">消息通知</router-link>
          </div>
          <div class="f-col">
            <div class="f-title">关于</div>
            <p>本科毕业设计</p>
            <p>© 2026 非遗文化传承与互动平台</p>
            <p class="f-tech">Spring Boot · MyBatis-Plus · Vue 3 · Element Plus</p>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUnreadCount } from '@/api/message'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const unreadCount = ref(0)

const avatar = computed(() => userStore.user && userStore.user.avatar)
const avatarText = computed(() => (userStore.nickname || 'U').charAt(0).toUpperCase())

async function loadUnread() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await getUnreadCount()
    unreadCount.value = (res.data && res.data.unread) || 0
  } catch (e) {
    unreadCount.value = 0
  }
}

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } else if (cmd === 'admin') {
    router.push('/admin')
  } else if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'apply') {
    router.push('/inheritor-apply')
  } else if (cmd === 'favorites') {
    router.push('/my-favorites')
  } else if (cmd === 'articles') {
    router.push('/my-articles')
  } else if (cmd === 'activities') {
    router.push('/my-activities')
  }
}

onMounted(loadUnread)
</script>

<style scoped>
.front-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.header {
  background: linear-gradient(135deg, #6f370f, #8b4513 45%, #a0522d);
  color: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 4px 16px rgba(111, 55, 15, 0.35);
  backdrop-filter: blur(6px);
  animation: header-drop 0.5s ease;
}
@keyframes header-drop {
  from { transform: translateY(-16px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 62px;
  padding: 0 16px;
  gap: 20px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-weight: bold;
  font-size: 18px;
  white-space: nowrap;
}
.logo-img {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  transition: transform 0.4s ease;
}
.logo:hover .logo-img {
  transform: rotate(-8deg) scale(1.1);
}
.nav {
  display: flex;
  gap: 4px;
  flex: 1;
}
.nav-item {
  color: rgba(255, 255, 255, 0.88);
  padding: 9px 14px;
  border-radius: 8px;
  font-size: 14px;
  position: relative;
  transition: color 0.2s, background 0.2s;
}
.nav-item::after {
  content: "";
  position: absolute;
  left: 50%;
  bottom: 2px;
  width: 0;
  height: 3px;
  border-radius: 2px;
  background: #f5deb3;
  transform: translateX(-50%);
  transition: width 0.25s ease;
}
.nav-item:hover::after,
.nav-item.active::after {
  width: 60%;
}
.nav-item:hover,
.nav-item.active {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}
.user-area {
  display: flex;
  align-items: center;
  gap: 14px;
}
.msg-icon {
  color: #fff;
  font-size: 20px;
  cursor: pointer;
}
.user-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}
.avatar {
  background: #f5deb3;
  color: #8b4513;
  font-weight: bold;
}
.uname {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.btn-login,
.btn-register {
  color: #fff;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  transition: all 0.2s;
}
.btn-login:hover,
.btn-register:hover {
  background: rgba(255, 255, 255, 0.2);
}
.btn-register {
  background: #f5deb3;
  color: #8b4513;
  border-color: #f5deb3;
}
.btn-register:hover {
  background: #ffe8bd;
}
.content {
  flex: 1;
}
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.28s ease, transform 0.28s ease;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
.footer {
  background: linear-gradient(160deg, #2e2013, #3a2a1a 60%, #46321f);
  color: rgba(255, 255, 255, 0.72);
  padding: 36px 0 28px;
  margin-top: 40px;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}
.footer-cols {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1.2fr;
  gap: 32px;
}
.f-col {
  font-size: 13px;
  line-height: 1.9;
}
.f-brand .f-logo {
  font-size: 17px;
  font-weight: 700;
  color: #f5deb3;
  margin-bottom: 10px;
}
.f-desc {
  color: rgba(255, 255, 255, 0.6);
}
.f-title {
  font-size: 14px;
  font-weight: 600;
  color: #f5deb3;
  margin-bottom: 10px;
}
.f-col a {
  display: block;
  color: rgba(255, 255, 255, 0.72);
  text-decoration: none;
  transition: color 0.2s, transform 0.2s;
}
.f-col a:hover {
  color: #fff;
  transform: translateX(4px);
}
.f-col p {
  margin: 0;
}
.f-tech {
  color: rgba(255, 255, 255, 0.45);
  font-size: 12px;
}
@media (max-width: 900px) {
  .footer-cols {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
