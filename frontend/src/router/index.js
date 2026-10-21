import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/components/FrontLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/home/Home.vue') },
      { path: 'projects', name: 'projects', component: () => import('@/views/project/ProjectList.vue') },
      { path: 'projects/:id', name: 'project-detail', component: () => import('@/views/project/ProjectDetail.vue') },
      { path: 'inheritors', name: 'inheritors', component: () => import('@/views/inheritor/InheritorList.vue') },
      { path: 'inheritors/:id', name: 'inheritor-detail', component: () => import('@/views/inheritor/InheritorDetail.vue') },
      { path: 'inheritor-apply', name: 'inheritor-apply', component: () => import('@/views/inheritor/InheritorApply.vue'), meta: { requiresAuth: true } },
      { path: 'articles', name: 'articles', component: () => import('@/views/article/ArticleList.vue') },
      { path: 'articles/:id', name: 'article-detail', component: () => import('@/views/article/ArticleDetail.vue') },
      { path: 'activities', name: 'activities', component: () => import('@/views/activity/ActivityList.vue') },
      { path: 'activities/:id', name: 'activity-detail', component: () => import('@/views/activity/ActivityDetail.vue') },
      { path: 'ai', name: 'ai-chat', component: () => import('@/views/ai/AiChat.vue') },
      { path: 'profile', name: 'profile', component: () => import('@/views/user/Profile.vue'), meta: { requiresAuth: true } },
      { path: 'my-favorites', name: 'my-favorites', component: () => import('@/views/user/MyFavorites.vue'), meta: { requiresAuth: true } },
      { path: 'my-articles', name: 'my-articles', component: () => import('@/views/user/MyArticles.vue'), meta: { requiresAuth: true } },
      { path: 'my-activities', name: 'my-activities', component: () => import('@/views/user/MyActivities.vue'), meta: { requiresAuth: true } },
      { path: 'messages', name: 'messages', component: () => import('@/views/user/Messages.vue'), meta: { requiresAuth: true } }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/auth/Login.vue')
  },
  {
    path: '/register',
    component: () => import('@/views/auth/Register.vue')
  },
  {
    path: '/admin',
    component: () => import('@/components/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', name: 'admin-dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'projects', name: 'admin-projects', component: () => import('@/views/admin/AdminProjects.vue') },
      { path: 'inheritors', name: 'admin-inheritors', component: () => import('@/views/admin/AdminInheritors.vue') },
      { path: 'articles', name: 'admin-articles', component: () => import('@/views/admin/AdminArticles.vue') },
      { path: 'activities', name: 'admin-activities', component: () => import('@/views/admin/AdminActivities.vue') },
      { path: 'users', name: 'admin-users', component: () => import('@/views/admin/AdminUsers.vue') },
      { path: 'comments', name: 'admin-comments', component: () => import('@/views/admin/AdminComments.vue') },
      { path: 'knowledge', name: 'admin-knowledge', component: () => import('@/views/admin/AdminKnowledge.vue') },
      { path: 'ai-settings', name: 'admin-ai-settings', component: () => import('@/views/admin/AdminAiSettings.vue') },
      { path: 'categories', name: 'admin-categories', component: () => import('@/views/admin/AdminCategories.vue') },
      { path: 'banners', name: 'admin-banners', component: () => import('@/views/admin/AdminBanners.vue') },
      { path: 'logs', name: 'admin-logs', component: () => import('@/views/admin/AdminLogs.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：登录与管理员权限
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('ich_token')
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.requiresAdmin) {
    const raw = localStorage.getItem('ich_user')
    let role = ''
    if (raw) {
      try {
        role = JSON.parse(raw).roleCode
      } catch (e) {
        role = ''
      }
    }
    if (role !== 'ROLE_ADMIN') {
      next('/')
      return
    }
  }
  next()
})

export default router
