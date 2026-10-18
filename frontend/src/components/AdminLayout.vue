<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="aside-logo">
        <img src="/favicon.svg" alt="" class="logo-img" />
        <span>非遗平台管理后台</span>
      </div>
      <el-menu :default-active="activeMenu" router background-color="#2b1d10" text-color="#c9b79c" active-text-color="#fff">
        <el-menu-item index="/admin">
          <el-icon><DataBoard /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="/admin/projects">
          <el-icon><Collection /></el-icon>
          <span>非遗项目管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/inheritors">
          <el-icon><UserFilled /></el-icon>
          <span>传承人管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/articles">
          <el-icon><Document /></el-icon>
          <span>资讯管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/activities">
          <el-icon><Calendar /></el-icon>
          <span>活动管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><Avatar /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/comments">
          <el-icon><ChatDotRound /></el-icon>
          <span>评论与举报</span>
        </el-menu-item>
        <el-menu-item index="/admin/knowledge">
          <el-icon><Cpu /></el-icon>
          <span>AI 知识库</span>
        </el-menu-item>
        <el-menu-item index="/admin/banners">
          <el-icon><Picture /></el-icon>
          <span>轮播图管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><Tickets /></el-icon>
          <span>日志审计</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-foot">本科毕业设计 · 2026</div>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-title">非遗文化传承与互动平台 · 管理后台</div>
        <div class="header-right">
          <el-dropdown @command="handleCmd">
            <span class="admin-user">
              <el-avatar :size="28" class="avatar">{{ avatarText }}</el-avatar>
              <span>{{ userStore.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home">返回前台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => {
  const p = route.path
  if (p === '/admin') return '/admin'
  return p
})
const avatarText = computed(() => (userStore.nickname || 'A').charAt(0).toUpperCase())

function handleCmd(cmd) {
  if (cmd === 'home') {
    router.push('/')
  } else if (cmd === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}
.aside {
  background: linear-gradient(180deg, #241708, #2b1d10 40%, #362313);
  display: flex;
  flex-direction: column;
}
.aside-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #f5deb3;
  font-weight: bold;
  padding: 18px 16px;
  font-size: 15px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo-img {
  width: 26px;
  height: 26px;
  border-radius: 6px;
}
.aside :deep(.el-menu) {
  border-right: none;
  flex: 1;
}
.aside :deep(.el-menu-item) {
  margin: 4px 10px;
  border-radius: 10px;
  transition: background 0.25s ease, transform 0.25s ease;
}
.aside :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  transform: translateX(3px);
}
.aside :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(139, 69, 19, 0.85), rgba(160, 82, 45, 0.55));
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
}
.aside-foot {
  padding: 14px 16px;
  color: rgba(255, 255, 255, 0.35);
  font-size: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  text-align: center;
}
.admin-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  z-index: 5;
}
.header-title {
  font-weight: bold;
  color: #5a4632;
}
.header-right {
  display: flex;
  align-items: center;
}
.admin-user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #333;
}
.avatar {
  background: #f5deb3;
  color: #8b4513;
  font-weight: bold;
}
.admin-main {
  background: #f5f2ec;
  padding: 16px;
}
</style>
