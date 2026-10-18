# 非遗文化传承与互动平台（ICh-Platform）

基于 **Spring Boot 3 + Vue 3** 的非物质文化遗产文化传承与互动平台。面向公众提供非遗项目、代表性传承人、文化资讯、体验活动的展示与互动（收藏/关注、点赞、评论、活动报名、签到核销），并为已认证传承人提供自助管理接口、为管理员提供全量审核与运营后台，内置基于本地知识库的 AI 智能问答（无需外部付费 API）。

## 仓库信息

- GitHub：https://github.com/starry0616/ich-platform
- 默认分支：`main`（开发与发布均在 `main` 上线性推进，不使用 force 推送）
- 克隆：

```bash
# HTTPS
git clone https://github.com/starry0616/ich-platform.git
# SSH（推荐，需已配置本机 SSH Key）
git clone git@github.com:starry0616/ich-platform.git
```

## 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Java 17 · Spring Boot 3 · Spring Security + JWT · MyBatis-Plus · H2 / MySQL · BCrypt |
| 前端 | Vue 3 · Vite · Element Plus · Pinia · Vue Router · Axios（趋势/分布图为纯 CSS 柱状条，未引入图表库） |
| 数据 | H2（开发演示，内存模式）/ MySQL（生产全量） |

## 功能模块

**前台（公众端）**
- 首页：轮播推荐、非遗十大门类、热门项目、最新资讯
- 非遗项目：分类/级别/地区/批次/关键词检索，详情含官方名录溯源信息、代表性传承人、关联体验活动
- 传承人名录：传承人列表与详情（官方认证标识、证书编号、主理体验活动）
- 文化资讯：资讯列表与详情（含审核状态控制）
- 非遗活动：展演/体验课堂/讲座/展览，报名、取消报名、签到核销（乐观锁防超卖、名额限制、截止时间校验）
- AI 问答：基于本地非遗知识库的智能问答（**关键词检索：标题/内容 LIKE 加权匹配 + 引用来源展示**，非向量检索/RAG；无命中时明确提示"暂未收录"）
- 个人中心：我的收藏/点赞、我的报名、我的评论

**传承人自助管理（后端接口已实现，前端专属工作台页面未单独开发）**
- 已认证传承人可通过后端接口维护本人名下项目与活动、查看本人活动报名名单并执行签到核销（均带数据级归属权限校验，仅能操作本人已认证档案对应的资源）；前端当前提供传承人认证申请（`/inheritor-apply`）与个人中心，未单独提供传承人管理页面。

**管理后台**
- 数据总览（真实数据库统计：趋势、类别分布、热门项目、传承人看板）
- 用户管理、传承人认证审核、项目审核、资讯审核、活动审核
- 类别管理、评论与举报管理、推荐位配置、AI 知识库管理、问答日志、登录日志

## 目录结构

```
backend/                 Spring Boot 后端工程（包根 com.yuxi.ich）
  src/main/java/com/yuxi/ich/
                        业务包：auth/user/project/inheritor/article/activity/
                        interaction/ai/admin/stats/message/media；
                        common/config/security 为鉴权与公共组件
  src/main/resources/   application.yml（h2/mysql 双 profile）、Mapper XML、
                        schema-h2.sql、data-h2-demo.sql（H2 演示数据）
  uploads/              项目/活动封面图（静态映射 /uploads/**，随仓库入库）
frontend/                Vue 3 前端工程（Vite + Element Plus + Pinia）
  src/views/            前台页面 / 管理后台（传承人自助管理走后端接口，无独立工作台页面）
  src/api/              各模块接口封装（axios + JWT 拦截器）
  src/stores/           Pinia 用户与权限状态
  src/router/           路由与角色守卫；src/components/、src/utils/ 公共组件与工具
sql/                     MySQL 脚本：00_schema.sql 建库、10_ich_seed_data.sql 全国名录、
                        11_ich_project_inheritor.sql 项目-传承人关联、README_字段映射.md
start-prod.cmd           生产一键启动脚本（强制 mysql profile 并校验环境变量）
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

- 后端 API 端口：`8080`；H2 调试控制台 `http://localhost:8080/h2-console`（JDBC URL `jdbc:h2:mem:ich;MODE=MySQL;DB_CLOSE_DELAY=-1`，用户名 `sa`，密码留空，仅开发调试用）
- H2 内存库：启动自动导入演示数据（`schema-h2.sql` + `data-h2-demo.sql`），重启即恢复初始状态，不持久化任何数据
- 演示账号（密码均为 `123456`）：

| 账号 | 角色 | 用途 |
|---|---|---|
| `admin` | 平台管理员 | 管理后台（前端路由 `/admin`） |
| `inheritor` | 非遗传承人 | 体验传承人角色（认证申请 `/inheritor-apply`、个人中心；自助管理走后端接口） |
| `user` | 普通用户 | 前台互动（收藏/评论/报名） |

### 2. 前端

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173，vite 代理 /api、/uploads → 8080
```

管理后台通过前端 `http://localhost:5173/admin` 访问（仅 `ROLE_ADMIN`），`8080` 仅为后端 API。

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

## Git 使用约定

- **分支**：仅维护 `main`；个人改动先在本地提交，确认 `mvn test` 与 `npm run build` 通过后再推送。
- **暂存**：按文件路径逐个 `git add <file>`，不使用 `git add .`，避免把临时文件、日志或构建产物带入提交。
- **提交信息**：采用 `类型: 简述` 格式（如 `fix: ...`、`feat: ...`、`docs: ...`），一句话说明改动。
- **推送**：`git push origin main`，使用快进推送；**禁止 `--force` / `--force-with-lease`**。推送前先 `git fetch origin`，若远端已有新提交则先合并/rebase，不得强推覆盖。
- **不入库内容**（已在 `.gitignore` 声明）：
  - 构建产物：`target/`、`dist/`、`node_modules/`、`*.jar`、`*.class`
  - 日志与临时文件：`*.log`、`*.tmp`、`_` 前缀的本地脚本/缓存、`tools/`、`scripts/`
  - 本地文档：根目录 `*.docx / *.html / *.pdf / *.png / *.jpg`、`答辩图表/`、`figures/`、`preview/`
- **凭据红线**：数据库密码、JWT 密钥、第三方 API Key 一律通过环境变量注入，**严禁写入 `application.yml` 或提交进仓库**。

## 测试与验证

- 后端：132 个单元/集成测试（JUnit，覆盖登录、权限、数据级归属校验、项目、活动报名并发、审核链路、项目-传承人关联展示等）`mvn test`
- 前端：`npm run build` 构建通过；核心链路经后端自动化测试与 H2 演示模式接口验证（登录、项目/传承人/活动详情、收藏、点赞、评论、报名/取消/核销、后台审核、数据统计），页面目视效果以本地实际运行为准
- 页面异常处理：加载态、空状态、错误提示、图片缺失占位均已覆盖

## 数据来源说明

**演示数据（H2，`data-h2-demo.sql`）——分三类，请勿混淆：**

1. **官方国家级名录项目（4 条）**：编号、批次、类型、保护单位已对照中国非物质文化遗产网核验（2026-10-18）：
   - 花灯戏（玉溪花灯戏）Ⅳ-78 · 2006 第一批 · 玉溪市文化馆
   - 滇剧 Ⅳ-132 · 2008 第二批（新增项目）
   - 抬阁（通海高台）Ⅹ-87 · 2011 第三批（扩展项目）
   - 傣族服饰（花腰傣服饰）Ⅹ-183 · 2021 第五批（民俗类）
2. **省级示例项目（2 条，`YN-` 自定义编号，页面级别显示"省级"）**：玉溪青花瓷烧制技艺、峨山彝族刺绣。二者在国家级名录中无对应记录（原演示数据曾误用国家级编号 Ⅷ-201 / Ⅶ-96，分属河北"衡水法帖雕版拓印技艺"与山西"建筑彩绘（炕围画）"，属编号冒用，已修正），现明确标注为省级非遗示例数据，不作为国家级名录内容展示。
3. **虚构测试数据（1 条）**："某待审核技艺"（待审核状态、前台不可见），仅用于验证审核链路。

> 除上述重点标注的本地示例外，演示库另收录若干公开知名国家级项目（如景德镇手工制瓷、宜兴紫砂陶、昆曲、侗族大歌、二十四节气、中医诊疗法、太极拳等）作为前台列表展示样例；其编号、批次、保护单位沿用公开名录表述，仅用于演示，不代表本平台已逐条核验。

- 演示传承人（李文 / 李秀英 / 李文亮）为**平台演示档案**：`cert_status=1` 仅表示平台内认证审核通过，不代表官方代表性传承人认定，个人简介已标注"平台演示档案"。
- AI 知识库（5 篇文档 / 6 切片）内容已按中国非物质文化遗产网及各级政府公开资料重写，正文末尾附【来源】URL 与检索日期。本地检索为**关键词 LIKE 加权匹配**（非向量检索/RAG），无命中时返回"暂未收录"提示，不会生成貌似确定的非遗事实；默认离线模板回答，可选接免费大模型接口（智谱 GLM-4-Flash，OpenAI 兼容），通过环境变量 `ICH_AI_ENABLED=true`、`ICH_AI_API_KEY`、`ICH_AI_BASE_URL`、`ICH_AI_MODEL` 或在 `application.yml` 的 `ich.ai` 段配置，未配置 Key 时自动回退离线回答。
- 项目/活动/资讯封面共 **16 张为 AI 生成的国风视觉素材**（`backend/uploads/.cover-brief.md` 记录生成任务），非非遗实物官方实拍照片，仅作界面演示用。
- MySQL 全量种子脚本 `sql/10_ich_seed_data.sql`、`sql/11_ich_project_inheritor.sql` 源自 OpenKG《中国非物质文化遗产知识图谱》（http://data.openkg.cn/dataset/china-intangible-cultural-heritage-kg），提供 1557 项目 / 3057 传承人 / 3054 条项目-传承人关联。需注意：全量数据中项目正文类字段（summary/content/history/cover_url/inheritor_id 等）与传承人 project_id **均为空**，列表页按缺失降级展示；全量数据未逐条核验，论文引用前需抽样比对官方名录。
- 平台内用户生成内容（评论、收藏、报名、举报）与审核状态由系统运行产生。

## 设计文档

系统设计说明书、数据库设计、可行性分析报告、答辩图表（数据流图/用例图/ER 图）等文档材料本地留存，未随源码入库。

## License

仅用于学习与毕业设计，禁止用于商业用途。
