# 验收联调报告 · 第 3 批（H2 隔离环境 HTTP 联调）

- 项目：ich-platform（非遗文化传承与互动平台）
- 仓库：`D:\新建文件夹\毕业设计`
- 环境：默认 H2 内存库（`jdbc:h2:mem:ich;MODE=MySQL;DB_CLOSE_DELAY=-1`，sa/空密码，重启即清空），**未连接 MySQL，未写真实库**
- 后端：Tomcat 8080；前端 Vite dev 5173（代理 `/api`、`/uploads` → 8080）
- 联调时间：2026-10-20
- 本批**未修改任何源码/依赖/配置**，纯接口联调与只读核对

## 0. 工作区终检（批前/批后一致）

| 项 | 结果 |
| --- | --- |
| HEAD | `6e3e9fb82b062789cbb07e747cefb8cab2351aa1`（未变） |
| `git status --short` | Modified 26、Untracked 6，全部为历史已有改动（含 AI 管理功能、传承人互动、收藏映射、D1/D2/D3） |
| `git diff --check` | CLEAN（无空白/冲突标记错误） |
| 禁止项 | 未执行 reset/clean/stash/commit/push，未改数据库结构、依赖、安全设置 |

> Untracked 6 项：AI 连接测试 3 个 Java（dto×2 + service）、`AdminAiSettings.vue`、第 1 批审计报告 md；均为历史已有，原样保留。

## 1. 后端 HTTP 联调（直连 8080，经 5173 代理抽验一致）

状态码说明：本后端未授权/未登录统一返回 **HTTP 200 + body `code:401`**，业务错误用 body `code` 表达（400/403/409），前端 `request.js` 在 `code!==200` 时 reject。

### 1.1 认证

| 用例 | 请求 | 结果 | 判定 |
| --- | --- | --- | --- |
| user/123456 登录 | `POST /api/auth/login` | HTTP200 code200，返回双令牌，`roleCode=ROLE_USER` | 通过 |
| admin/123456 登录 | `POST /api/auth/login` | code200，`roleCode=ROLE_ADMIN`，`isAdmin=true` | 通过 |
| `GET /api/auth/me`（user/admin） | 带令牌 | code200，角色/isAdmin 正确 | 通过 |
| disabled/123456 | 登录 | code403「账号已被禁用」 | 通过 |
| 错误短密码 user/wrong | 登录 | code400「密码长度应为 6-64 位」（校验生效） | 通过 |
| 未登录 `GET /api/user/profile` | 无令牌 | body code401「未登录或登录已过期」 | 通过 |

### 1.2 四类公开列表与详情

| 用例 | 请求 | 结果 | 判定 |
| --- | --- | --- | --- |
| 项目列表 | `GET /api/project/list` | total13，含 projectName/coverUrl/categoryName | 通过 |
| 资讯列表 | `GET /api/article/list` | total3，含 title/authorName/projectName | 通过 |
| 活动列表 | `GET /api/activity/list` | total4，含 title/quota/signed/remaining/inheritorName | 通过 |
| 传承人列表 | `GET /api/inheritor/list` | total3，含 name/certStatus=1/projectName | 通过 |
| 项目详情 11 | `GET /api/project/11` | code200，「二十四节气」 | 通过 |
| 资讯详情 1 | `GET /api/article/detail/1` | code200，「玉溪青花瓷：从钴料配方…」含 content | 通过 |
| 活动详情 1 | `GET /api/activity/1` | code200，「玉溪青花瓷拉坯体验课堂」 | 通过 |
| 传承人详情 1 | `GET /api/inheritor/1` | code200，李文，certStatus=1 有效 | 通过 |

### 1.3 D1 传承人收藏状态（targetType=4）

| 步骤 | 请求 | 响应关键字段 | 判定 |
| --- | --- | --- | --- |
| 首次收藏传承人(4,1) | `POST /api/interaction/favorite?targetType=4&targetId=1` | `data.active=true`「收藏成功」 | 通过 |
| 状态回读 | `GET /api/interaction/state?targetType=4&targetId=1` | `favorited=true, liked=false` | 通过 |
| 取消收藏 | 再次 POST favorite | `active=false`「已取消收藏」 | 通过 |
| 回读 | GET state | `favorited=false` | 通过 |

结论：后端 `active` 与 `state.favorited` 一致，D1「以前端实际返回 `active` 为准更新收藏态」的契约成立。

### 1.4 点赞与收藏独立

| 步骤 | 响应 | 判定 |
| --- | --- | --- |
| 收藏后点赞(4,1) | like `active=true`；state `favorited=true, liked=true` | 通过 |
| 仅取消收藏 | state `favorited=false, liked=true`（点赞保留） | 通过 |
| 取消点赞 | like `active=false`；state 全 false | 通过 |

### 1.5 其余三类收藏 + 评论 + 我的收藏

| 用例 | 请求 | 结果 | 判定 |
| --- | --- | --- | --- |
| 项目收藏(1,11) | favorite | `active=true` | 通过 |
| 资讯收藏(2,1) | favorite | `active=true` | 通过 |
| 活动收藏(3,1) | favorite | `active=true` | 通过 |
| 传承人评论 | `POST /api/interaction/comment`{targetType:4,targetId:1,content} | code200，返回评论 id | 通过 |
| 评论列表 | `GET /api/interaction/comments?targetType=4&targetId=1` | total1，内容/昵称正确 | 通过 |
| 匿名评论 | 无令牌 POST comment | body code401 | 通过 |
| 我的收藏 | `GET /api/interaction/my/favorites` | total3，类型 1/2/3，含 targetTitle/targetCover | 通过 |

### 1.6 D2/D3 活动报名（活动 3，user 未报）

| 步骤 | 请求 | 响应 | 判定 |
| --- | --- | --- | --- |
| 报名成功 | `POST /api/activity/3/signup` | code200，返回真实 `signupNo=YX20261020201648-3-3-6393`，status=1 | 通过（D3） |
| 重复报名 | 再次 POST | code409「您已报名该活动，请勿重复报名」（后端兜底拦截） | 通过（D2 后端侧） |
| 我的报名 | `GET /api/activity/my/signups` | 带出 signupNo、activityTitle/StartTime/Address | 通过（D3） |
| 取消报名 | `DELETE /api/activity/3/signup` | code200「已取消报名」 | 通过 |
| 取消后回读 | GET my/signups | 该记录 status=2，cancelTime 已写 | 通过 |

> D2 的前端 busy 锁/按钮 disabled 属 UI 行为，见第 3 节「未验证」。

### 1.7 个人中心与管理端

| 用例 | 请求 | 结果 | 判定 |
| --- | --- | --- | --- |
| 资料 email 修改→回读→还原 | `PUT /api/user/profile` / GET | 改动落库并成功还原 | 通过 |
| 头像 URL 落库 | `POST /api/user/avatar`{avatar} | code200，GET profile 回读含 avatar | 通过 |
| 改密码·缺原密码 | `PUT /api/user/password` | code400「请输入原密码」 | 通过 |
| 改密码·新密码太短 | 同上 | code400「新密码长度需为 8-64 位」 | 通过 |
| 改密码·原密码错误 | 同上 | code400「原密码不正确」 | 通过 |
| admin 用户列表 | `GET /api/admin/user/list` | code200 total4 | 通过 |
| admin 统计 | `GET /api/admin/stats/overview` | code200，各计数返回 | 通过 |
| admin 评论列表 | `GET /api/admin/comment/list` | code200 | 通过 |
| 普通用户访问管理端 | ROLE_USER `GET /api/admin/user/list` | body code403「没有操作权限」 | 通过 |

### 1.8 Vite dev 与代理

| 用例 | 结果 | 判定 |
| --- | --- | --- |
| `npm run dev`（非生产构建） | Vite v5.4.21 ready，5173 监听 | 通过 |
| `GET http://localhost:5173/` | HTTP200 | 通过 |
| `GET 5173/api/project/list`（代理转发） | HTTP200，中文无乱码，total13 | 通过 |

## 2. 自动化测试（非本批执行，沿用第 2 批结果）

- 第 2 批全量 `mvn test`：**147 项全部通过，BUILD SUCCESS**；本批未改源码，未重跑。
- 第 2 批改动的 3 个 Vue 文件已通过 `@vue/compiler-sfc` 语法检查。

## 3. 真实浏览器交互：未验证（环境限制，如实说明）

本环境**无可用的真实浏览器点击通道**，未做页面级交互，以下 UI 行为未实测，HTTP 结果不冒充浏览器通过：

- 桌面自动化 `cu` 激活失败：`gui_coding_session_activation_failed`。
- 隔离产品浏览器 `bu` 对 `localhost:8080`、`127.0.0.1:5173`、`localhost:5173` 均返回 `chrome-error`（提示检查代理/防火墙），无法访问本地回环地址。

因此以下项标记**未验证**，需在可访问本地服务的普通浏览器中人工点验：

1. 登录页输入/登录/登出、未登录访问受保护页的跳转。
2. D1：传承人详情收藏按钮在收藏/取消后的图标与文案切换（依赖后端 active，已验契约）。
3. D2：报名/取消报名快速连点时的 busy、按钮 loading/disabled、失败后状态恢复。
4. D3：报名成功弹窗/页面展示 signupNo、刷新后回显、我的报名列表编号列、取消后编号展示。
5. 评论提交失败时输入内容保留；收藏/点赞按钮即时视觉反馈。
6. 我的收藏点击跳转到对应详情页路由（1→项目、2→资讯、3→活动、4→传承人），未知类型兜底。
7. 个人资料表单提交、改密码成功路径、头像图片实际上传。

人工点验入口（服务若仍在运行）：浏览器打开 `http://localhost:5173`，演示账号 `user/123456`、`admin/123456`。

## 4. 生产构建：本批未执行

按既定决定不运行 `npm run build`；Windows 下 esbuild 临时文件 ACCESS DENIED 为已知环境问题，计划在 Linux 环境构建。本批不重复尝试。

## 5. 观察项（非本批缺陷，不在 D1/D2/D3 范围，未修改）

- `GET /api/user/profile` 与 admin 用户列表返回体中携带了 `password` 的 BCrypt 哈希字段。建议后续在序列化层对密码字段脱敏（`@JsonIgnore` 或 DTO 过滤），属既有实现，本批未动。
- 改密码成功路径、头像 multipart 实际上传、AI 问答/举报/消息/日志等非核心流程本批未覆盖。

## 6. 结论

- 后端 HTTP 接口层：认证、四类列表/详情、D1 收藏 active、点赞独立、评论、我的收藏、D2 重复报名 409、D3 signupNo、资料/头像、admin 列表与 403 权限隔离——**均通过**。
- 真实浏览器 UI 交互：**未验证**（无可用浏览器通道），需人工点验第 3 节清单。
- 自动化测试：沿用第 2 批 147 项通过；生产构建：未执行（计划 Linux）。
- 工作区：HEAD 与全部未提交改动完整保留，`diff --check` 干净，未提交未推送。
