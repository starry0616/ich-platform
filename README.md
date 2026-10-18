# 非遗文化传承与互动平台（ICh-Platform）

基于 **Spring Boot 3 + Vue 3** 的非物质文化遗产文化传承与互动平台。面向公众提供非遗项目、代表性传承人、文化资讯、体验活动的展示与互动（收藏/关注、点赞、评论、活动报名、签到核销），并为传承人提供工作台、为管理员提供全量审核与运营后台，内置基于本地知识库的 AI 智能问答（无需外部付费 API）。

## 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Java 17 · Spring Boot 3 · Spring Security + JWT · MyBatis · H2 / MySQL · BCrypt |
| 前端 | Vue 3 · Vite · Element Plus · Pinia · Vue Router · ECharts |
| 数据 | H2（开发演示，内存模式）/ MySQL（生产全量） |

## 功能模块

**前台（公众端）**
- 首页：轮播推荐、非遗十大门类、热门项目、最新资讯
- 非遗项目：分类/级别/地区/批次/关键词检索，详情含官方名录溯源信息、代表性传承人、关联体验活动
- 传承人名录：传承人列表与详情（官方认证标识、证书编号、主理体验活动）
- 文化资讯：资讯列表与详情（含审核状态控制）
- 非遗活动：展演/体验课堂/讲座/展览，报名、取消报名、签到核销（乐观锁防超卖、名额限制、截止时间校验）
- AI 问答：基于本地非遗知识库的智能问答（RAG 检索 + 引用来源展示）
- 个人中心：我的收藏/点赞、我的报名、我的评论

**传承人工作台**
- 我的项目 / 我的活动 / 报名名单（核销）/ 数据看板

**管理后台**
- 数据总览（真实数据库统计：趋势、类别分布、热门项目、传承人看板）
- 用户管理、传承人认证审核、项目审核、资讯审核、活动审核
- 类别管理、评论与举报管理、推荐位配置、AI 知识库管理、问答日志、系统日志

## 目录结构

```
backend/                 Spring Boot 后端工程（包根 com.yuxi.ich）
  src/main/java/         15 个业务模块：common/auth/user/project/inheritor/
                        article/activity/interaction/ai/admin/stats/...
  src/main/resources/    Mapper XML、schema-h2.sql、data-h2-demo.sql（演示数据）
  uploads/               项目/活动封面图（静态映射 /uploads/**）
frontend/                Vue 3 前端工程（Vite + Element Plus + Pinia）
  src/views/             前台页面 / 工作台 / 管理后台
  src/api/               接口封装（axios + JWT 拦截器）
  src/stores/            Pinia 状态（用户、权限）
sql/                     MySQL 全量种子脚本（schema + 全国名录数据，10_ich_seed_data.sql）
README.md                本文件
```

## 快速启动

### 1. 后端（H2 演示模式，零配置）

```bash
cd backend
# 方式 A：IDE 直接运行 IchPlatformApplication（默认 h2 profile）
# 方式 B：打包运行
mvn package -DskipTests
java -jar target/ich-platform.jar
```

- 端口：`8080`，控制台地址 `http://localhost:8080`
- H2 内存库：启动自动导入演示数据（`schema-h2.sql` + `data-h2-demo.sql`），重启即恢复初始状态
- 演示账号（密码均为 `123456`）：

| 账号 | 角色 | 用途 |
|---|---|---|
| `admin` | 平台管理员 | 管理后台 `http://localhost:8080`（前端路由 /admin） |
| `inheritor` | 非遗传承人 | 传承人工作台（/workbench） |
| `user` | 普通用户 | 前台互动（收藏/评论/报名） |

### 2. 前端

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173，vite 代理 /api、/uploads → 8080
```

生产构建：`npm run build`（产物在 `dist/`）。

### 3. 生产（MySQL）模式

> **生产部署必须显式使用 `mysql` profile**，禁止以默认 H2 演示配置承载生产流量（H2 为内存库、重启即清空，且使用开发密钥）。
> 生产配置已做防呆：mysql profile 下**缺失任一必要环境变量时应用启动失败**，不会静默使用公开默认凭据/密钥。

1. 创建数据库并导入建库脚本：`sql/00_schema.sql` → `sql/10_ich_seed_data.sql`、`sql/11_ich_project_inheritor.sql`（含国家级非遗名录全量数据）
2. 设置环境变量（缺失即启动失败）：

| 变量 | 必填 | 说明 |
|---|---|---|
| `ICH_DB_USER` | 是 | MySQL 用户名 |
| `ICH_DB_PASSWORD` | 是 | MySQL 密码 |
| `ICH_JWT_SECRET` | 是 | JWT 签名密钥，至少 32 字节随机串（`openssl rand -base64 48` 生成，勿提交仓库） |
| `ICH_REDIS_HOST` / `ICH_REDIS_PORT` | 否 | Redis 地址（默认 `localhost:6379`） |

3. 启动后端（两种方式任选）：

```bash
# 方式 A：一键脚本（推荐）——自动校验环境变量并强制 mysql profile
start-prod.cmd

# 方式 B：直接启动
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
# 或打包后
mvn package -DskipTests
java -jar target/ich-platform.jar --spring.profiles.active=mysql
```

4. 前端构建产物（`frontend/dist/`）部署至 Nginx 等 Web 服务器，并将 `/api`、`/uploads` 反向代理至后端 `8080` 端口。

## 测试与验证

- 后端：124 个单元/集成测试（JUnit，覆盖登录、权限、数据级归属校验、项目、活动报名并发、审核链路等）`mvn test`
- 前端：`npm run build` 构建通过；已在浏览器实测核心链路（登录、项目/传承人/活动详情、收藏、点赞、评论、报名/取消/核销、后台 CRUD 与审核、数据统计）
- 页面异常处理：加载态、空状态、错误提示、图片缺失占位均已覆盖

## 数据来源说明

- 演示数据取自**公开的国家级非遗代表性项目名录**（`data-h2-demo.sql` 注释声明），含云南/玉溪本地项目（玉溪青花瓷烧制技艺、玉溪花灯戏、滇剧、通海高台、峨山彝族刺绣等）
- 项目编号（如 `Ⅷ-201`）、公布年份、批次、保护单位等名录溯源字段为官方公开信息
- MySQL 全量种子脚本 `sql/10_ich_seed_data.sql` 提供全国名录数据（1557 项目 / 3057 传承人）
- 平台内用户生成内容（评论、报名、举报）与审核状态由系统运行产生

## 设计文档

系统设计说明书、数据库设计、可行性分析报告、答辩图表（数据流图/用例图/ER 图）等文档材料未随源码入库，保存在本地 `D:\新建文件夹\毕业设计` 根目录。

## License

仅用于学习与毕业设计，禁止用于商业用途。
