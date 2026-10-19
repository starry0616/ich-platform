# 毕业设计论文图表（系统分析与设计图）

> 论文题目：《基于 Spring Boot 与 Vue 的非遗文化传承与互动平台设计与实现》
> 本目录所有图表均依据已推送至 GitHub 的 `ich-platform` 真实源码、建表脚本与接口绘制，未虚构未实现功能。

## 一、图表清单

| 文件 | 图名 | 类型 | 主要依据 |
|------|------|------|---------|
| `01-use-case.puml` | 系统总体用例图（公众端 + 传承人） | UML 用例图 | SecurityConfig 公开路径、RoleConstants、各 Controller `@PreAuthorize` |
| `02-use-case-admin.puml` | 平台后台管理用例图（仅 ADMIN） | UML 用例图 | Admin*Controller、`/api/admin/**` 鉴权 |
| `03-context-dfd.puml` | 顶层系统上下文图 | DFD 上下文图 | 外部参与者与系统边界、AiService 可选大模型 |
| `04-dfd-level-0.puml` | 0 层数据流图（系统分解·按业务领域分簇） | DFD 0 层 | 六个处理过程与八个逻辑数据存储，按认证/非遗内容/互动报名/问答/后台五域分簇（完整 24 表见 08） |
| `05-dfd-level-1.puml` | 1 层数据流图：活动报名与核销 | DFD 1 层 | ActivityService 乐观锁扣减名额、act_signup 唯一键 |
| `06-function-modules.puml` | 系统功能模块图 | 模块结构图 | frontend/router/index.js、各页面与 Controller |
| `07-er-core.puml` | 核心业务概念 E-R 图 | 概念 E-R | 项目/传承人/关联表/用户/活动/报名核心实体 |
| `08-er-database.puml` | 完整数据库逻辑关系图（24 表·全景总览） | 逻辑 E-R | sql/00_schema.sql、schema-h2.sql；图幅较宽，供附录全景，正文建议用下列局部图 |
| `08a-er-auth.puml` | 局部 E-R：认证与权限域（5 表） | 局部逻辑 E-R | sys_user / sys_role / sys_menu / sys_role_menu / sys_login_log |
| `08b-er-resource.puml` | 局部 E-R：非遗资源与内容域（7 表，含项目—传承人 M:N） | 局部逻辑 E-R | ich_category/ich_project/ich_inheritor/ich_project_inheritor/ich_project_media/cms_article(_category) |
| `08c-er-activity-interaction.puml` | 局部 E-R：活动与互动域（6 表） | 局部逻辑 E-R | ich_activity/act_signup/interaction/user_favorite/user_browse_history/interaction_report |
| `08d-er-system-ai.puml` | 局部 E-R：系统支撑与 AI 问答域（6 表） | 局部逻辑 E-R | sys_audit_log/sys_message/sys_banner/ai_knowledge/ai_knowledge_chunk/ai_chat |
| `entity-relationship-audit.md` | 实体关系核对表 | 审计文档 | 建表脚本逐表核对主键、唯一键、逻辑外键、基数 |

渲染产物：`svg/`（矢量，适合 Word 插入后无损缩放）、`png/`（位图，便于快速预览）。

> **E-R 图使用建议**：`08-er-database` 为 24 表全景总览，图幅较宽，适合放附录做整体参照；论文正文受 A4 幅面限制，建议改用 08a–08d 四张局部逻辑图，每张仅含一个业务域，文字清晰、连线短。局部图中跨域引用（如业务表的 `user_id`、多态 `target_type+target_id`）以文字标注"跨域见 08x"，不画跨域长线，避免交叉；全部 24 表的完整清单与唯一键见 `entity-relationship-audit.md`。

## 二、符号说明

- **用例图**：小人=参与者（角色），椭圆=用例，矩形框=系统边界；实线=参与者与用例关联。
- **DFD**：矩形=外部实体（蓝），圆角矩形=处理过程（绿），圆柱=数据存储（黄），虚线=可选/条件数据流。
- **E-R 图**：矩形=实体，`*` 与 PK=主键，UK=唯一键，`||--o{` 表示一对多、`}o--o{` 表示多对多；本项目所有关系均为**业务层维护的逻辑外键，数据库未建物理 FOREIGN KEY**。

## 三、审计要点（已核实，防止误画）

1. **系统仅三个数据库角色**：ROLE_USER、ROLE_INHERITOR、ROLE_ADMIN（sys_role 种子数据）；后台 `/api/admin/**` 仅 ADMIN。
2. **项目—传承人多对多唯一依据是 `ich_project_inheritor(project_id,inheritor_id)`**；`ich_project.inheritor_id`（第一传承人）与 `ich_inheritor.project_id`（代表性项目）只是单值冗余展示字段，不是多对多依据，也不授予管理权限。
3. **关联展示 ≠ 管理授权**：归属校验在 Service 层 `checkCanManage`，与关联表无关。
4. **AI 问答为关键词停用词切分 → LIKE 检索 → 标题 2 分/内容 1 分加权排序**；`ai_knowledge_chunk.vector_id` 仅预留，当前未接入向量库 / Embedding / RAG；可选智谱 GLM 仅做答案润色，未配置时用离线模板。
5. **活动报名**靠 `ich_activity.version` 乐观锁 + `(activity_id,user_id)` 唯一键防超卖与重复报名。

## 四、H2 与 MySQL 差异

- 表、字段、唯一键两套脚本一致（共 24 张）；MySQL 多 `ENGINE=InnoDB`、`utf8mb4`、`COMMENT`、`FULLTEXT(ft_name_summary)`，H2 省略这些专有语法。
- 检索均走 LIKE，不依赖 MySQL 全文索引；长文本 MySQL 用 LONGTEXT、H2 用 TEXT，兼容。
- 两库均无物理外键。

## 五、渲染方法（可复现）

```bash
# 需 Java 17 与 plantuml.jar（本机未内置，可从 Maven Central 获取）
java -jar plantuml.jar -charset UTF-8 -tsvg -o svg  *.puml   # 导出 SVG
java -jar plantuml.jar -charset UTF-8 -tpng  -o png  *.puml   # 导出 PNG
```
字体使用 Microsoft YaHei；如在无中文字体环境渲染，需替换 `defaultFontName`。

## 六、验证范围说明

- 已完成：源码/建表脚本静态审计、PlantUML 语法渲染成功、中文显示与布局目视检查。
- 未在本目录涉及：真实 MySQL 全量联调、浏览器页面像素级目视验收（属功能验证阶段遗留项，与图表绘制无关）。
