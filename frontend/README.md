# 非遗文化传承与互动平台 · 前端工程

基于 **Vue 3 + Vite + Element Plus + Pinia + Vue Router + Axios** 的单页应用，对接本地 Spring Boot 后端（`../backend`）。

## 快速开始

```bash
# 1. 安装依赖
npm install

# 2. 启动开发服务器（默认 http://localhost:5173）
npm run dev

# 3. 生产构建（输出到 dist/）
npm run build
```

开发环境已配置 Vite 代理：`/api` 与 `/uploads` 转发到 `http://localhost:8080`（后端默认端口）。

## 后端启动（配套）

```bash
cd ../backend
mvn spring-boot:run          # 默认 h2 内存库，克隆即可启动，无需 MySQL
# 或使用 MySQL：先执行 sql/00_schema.sql + 种子数据，再 --spring.profiles.active=mysql
```

## 目录结构

```
src/
├── api/           # 按模块封装的后端接口调用
│   ├── auth.js    # 注册/登录/刷新/登出/当前用户
│   ├── user.js    # 个人资料/密码/头像/收藏/历史/菜单
│   ├── project.js # 非遗项目 + 门类
│   ├── inheritor.js
│   ├── article.js
│   ├── activity.js
│   ├── interaction.js  # 收藏/点赞/评论
│   ├── message.js
│   ├── ai.js           # 智能问答
│   ├── media.js        # 文件上传
│   └── admin.js        # 管理端全部接口
├── components/    # FrontLayout（前台布局）/ AdminLayout（管理端布局）
├── router/        # 路由 + 登录/管理员守卫
├── stores/        # Pinia 用户状态
├── utils/         # axios 封装（含 token 刷新重放）、auth、格式化
└── views/         # 页面
    ├── home/      首页（轮播/统计/门类/热门项目/资讯/活动）
    ├── project/   非遗项目列表/详情
    ├── inheritor/ 传承人列表/详情
    ├── article/   资讯列表/详情
    ├── activity/  活动列表/详情（报名）
    ├── ai/        智能问答
    ├── user/      个人中心（资料/收藏/发布/报名/消息）
    ├── auth/      登录/注册
    └── admin/     管理后台（概览/项目/传承人/资讯/活动/用户/评论举报/知识库/轮播/日志）
```

## 路由一览

| 路径 | 说明 |
|---|---|
| `/` | 首页 |
| `/projects`、`/projects/:id` | 非遗项目列表 / 详情 |
| `/inheritors`、`/inheritors/:id` | 传承人列表 / 详情 |
| `/articles`、`/articles/:id` | 资讯列表 / 详情 |
| `/activities`、`/activities/:id` | 活动列表 / 详情 |
| `/ai` | 智能问答（需登录） |
| `/login`、`/register` | 登录 / 注册 |
| `/profile`、`/my-favorites`、`/my-articles`、`/my-activities`、`/messages` | 个人中心（需登录） |
| `/admin` 及子路由 | 管理后台（需 ADMIN 角色） |

## 登录说明

- 后端种子数据包含管理员账号（见 `backend/src/main/resources/db/data-h2-demo.sql` 或 `sql/10_ich_seed_data.sql`），登录后前端自动识别 `roleCode` 为 `ADMIN` 的用户，并在右上角/下拉菜单出现「管理后台」入口。
- Token 过期后 axios 拦截器会自动使用 refreshToken 刷新并重放原请求。
