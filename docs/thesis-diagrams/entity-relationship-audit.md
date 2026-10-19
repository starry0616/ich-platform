# 实体关系核对表（entity-relationship-audit）

> 适用项目：ich-platform《基于 Spring Boot 与 Vue 的非遗文化传承与互动平台设计与实现》
> 依据文件：
> - MySQL 建表：`sql/00_schema.sql`
> - H2 建表：`backend/src/main/resources/db/schema-h2.sql`
> - 实体/服务：`backend/src/main/java/com/yuxi/ich/` 下 Entity / Mapper / Service
>
> **总说明：MySQL 与 H2 两套建表脚本的表、字段、唯一键一致；两库均未建立物理 FOREIGN KEY 约束，下列“逻辑外键”全部由应用业务层维护。** MySQL 专有差异仅为 `ENGINE=InnoDB`、`utf8mb4`、`COMMENT`、`FULLTEXT(ft_name_summary)`；H2 去掉这些语法，检索仍由 `LIKE` 实现，不依赖全文索引。

## 一、表清单与主键、唯一键核对（共 24 张）

| # | 表名 | 中文含义 | 主键 | 唯一键 | 依据 |
|---|------|---------|------|--------|------|
| 1 | sys_user | 用户表 | id | uk_username(username) | 00_schema.sql L17-38；schema-h2.sql L9-28 |
| 2 | sys_role | 角色表 | id | uk_role_code(role_code) | L40-50；L30-41 |
| 3 | sys_menu | 菜单权限表 | id | 无（parent_id 普通索引） | L52-68；L244-261 |
| 4 | sys_role_menu | 角色-菜单关联表 | id | uk_role_menu(role_id,menu_id) | L70-77；L263-270 |
| 5 | sys_login_log | 登录日志表 | id | 无 | L79-91；L273-285 |
| 6 | ich_category | 非遗类别表 | id | 无（parent_id 树形自关联） | L96-111；L43-56 |
| 7 | ich_inheritor | 非遗传承人表 | id | uk_cert_no(cert_no) | L113-140；L58-81 |
| 8 | ich_project | 非遗项目表 | id | uk_project_code(project_code) | L142-177；L83-111 |
| 9 | ich_project_inheritor | 项目-传承人关联表 | id | uk_proj_inh(project_id,inheritor_id) | L179-189；L113-122 |
| 10 | ich_project_media | 项目多媒体表 | id | 无（project_id 普通索引） | L191-204；L288-302 |
| 11 | cms_article_category | 资讯分类表 | id | 无 | L209-217；L125-134 |
| 12 | cms_article | 文化资讯表 | id | 无（category_id/audit_status/publish_time 索引） | L219-242；L136-156 |
| 13 | ich_activity | 非遗活动表 | id | 无（status/start_time/project_id 索引，version 乐观锁） | L247-274；L159-184 |
| 14 | act_signup | 活动报名表 | id | uk_signup_no(signup_no)；uk_act_user(activity_id,user_id) | L276-297；L186-208 |
| 15 | interaction | 互动记录表（收藏/点赞/评论） | id | 无（开关式幂等由业务层先查后写） | L302-324；L211-232 |
| 16 | user_favorite | 收藏表 | id | uk_user_target(user_id,target_type,target_id) | L326-336；L305-315 |
| 17 | user_browse_history | 浏览历史表 | id | uk_user_target(user_id,target_type,target_id) | L465-477；L318-330 |
| 18 | interaction_report | 评论举报表 | id | uk_report(interaction_id,user_id) | L449-463；L364-378 |
| 19 | sys_audit_log | 内容审核记录表 | id | 无（target_type,target_id 索引） | L341-352；L333-344 |
| 20 | sys_message | 消息通知表 | id | 无（user_id,is_read 索引） | L354-367；L347-361 |
| 21 | ai_knowledge | AI 知识库文档表 | id | 无（source_type,source_id 索引） | L372-389；L398-415 |
| 22 | ai_knowledge_chunk | AI 知识切片表 | id | 无（knowledge_id 索引） | L391-401；L417-427 |
| 23 | ai_chat | AI 问答记录表 | id | 无（user_id/session_id/time 索引） | L403-423；L429-450 |
| 24 | sys_banner | 首页推荐位表 | id | 无 | L433-447；L381-395 |

## 二、逻辑关系与基数核对（无物理外键）

| 主表 | 从表 | 外键字段 | 基数 | 业务含义与依据 |
|------|------|---------|------|---------------|
| sys_role | sys_user | sys_user.role_id | 1 : N（用户角色可空） | 一个角色分配给多个账号；注册用户默认 ROLE_USER。依据：SecurityConfig + RoleConstants |
| sys_role | sys_role_menu | role_id | 1 : N | 角色持有菜单权限 |
| sys_menu | sys_role_menu | menu_id | 1 : N | 菜单被多角色引用 |
| sys_menu | sys_menu | parent_id | 1 : N（自关联树） | 目录/菜单/按钮层级 |
| sys_user | sys_login_log | user_id | 1 : N（可空，失败记录可无用户） | 登录留痕 |
| sys_user | ich_inheritor | ich_inheritor.user_id | 1 : 0..1 | 一个认证账号对应 0 或 1 条传承人档案 |
| ich_category | ich_project | ich_project.category_id | 1 : N（可空） | 项目归入一个非遗类别 |
| ich_category | ich_category | parent_id | 1 : N（自关联） | 类别树形 |
| ich_project | ich_inheritor | **ich_project_inheritor**（中间表） | **M : N** | **项目—传承人多对多，唯一依据为关联表 (project_id,inheritor_id)** |
| ich_project | ich_project_media | ich_project_media.project_id | 1 : N | 一个项目多条多媒体 |
| cms_article_category | cms_article | cms_article.category_id | 1 : N（可空） | 资讯分类 |
| sys_user | cms_article | cms_article.author_id | 1 : N（可空） | 作者投稿 |
| ich_project | cms_article | cms_article.project_id | 1 : N（可空，弱关联） | 资讯可挂接相关项目 |
| ich_project | ich_activity | ich_activity.project_id | 1 : N（可空） | 活动可关联非遗项目 |
| ich_inheritor | ich_activity | ich_activity.inheritor_id（单值主办） | 1 : N（可空） | 活动主办传承人，单值展示，非多对多 |
| ich_activity | act_signup | act_signup.activity_id | 1 : N | 一个活动多条报名；uk_act_user 防重复 |
| sys_user | act_signup | act_signup.user_id | 1 : N | 用户提交多条报名 |
| sys_user | interaction | interaction.user_id | 1 : N | 收藏/点赞/评论 |
| sys_user | user_favorite | user_favorite.user_id | 1 : N | 收藏 |
| sys_user | user_browse_history | user_browse_history.user_id | 1 : N | 浏览历史 |
| interaction | interaction_report | interaction_report.interaction_id | 1 : N | 一条评论可被多次举报（唯一 interaction_id+user_id 防重复举报） |
| sys_user | interaction_report | interaction_report.user_id | 1 : N | 举报人 |
| sys_user | sys_audit_log | sys_audit_log.auditor_id | 1 : N（可空） | 审核人留痕 |
| sys_user | sys_message | sys_message.user_id（接收人） | 1 : N | 站内消息 |
| ai_knowledge | ai_knowledge_chunk | ai_knowledge_chunk.knowledge_id | 1 : N | 文档切分 |
| sys_user | ai_chat | ai_chat.user_id（可空） | 1 : N | 问答历史 |

## 三、多态（target_type + target_id）约定，不建物理外键

| 表 | target_type 取值 | 说明 |
|----|-----------------|------|
| interaction | 1 项目 / 2 资讯 / 3 活动 | 评论/点赞/收藏统一表，目标由类型解析 |
| user_favorite | 1 项目 / 2 资讯 / 3 活动 | 高频收藏查询表 |
| user_browse_history | 1 项目 / 2 资讯 / 3 活动 | 浏览足迹 |
| sys_banner | 1 项目 / 2 资讯 / 3 活动 / 4 外链 | 推荐位可跳外链 |
| sys_audit_log | 1 项目 / 2 资讯 / 3 活动 / 4 传承人 / 5 评论 / 6 用户 | 审核记录对象 |
| ai_knowledge | 1 项目 / 2 传承人 / 3 资讯 / 4 政策/术语 | 知识来源元数据 |

## 四、关键语义核验（防止误画）

1. **多对多唯一依据 = ich_project_inheritor**。`ich_project.inheritor_id`（第一代表性传承人）与 `ich_inheritor.project_id`（代表性项目）均为**单值冗余展示字段**，既不是外键约束，也不承载多对多关系，更不授予管理权限。
2. **关联展示 ≠ 管理授权**。项目/活动的归属校验在 Service 层 `checkCanManage`，依据是传承人本人 `user_id`/档案归属，与 `ich_project_inheritor` 行无关。
3. **interaction 不建唯一键**：type 同时承载开关式（收藏/点赞）与列表式（评论），唯一键会挡掉第二条评论，幂等由 `InteractionService#toggle` 先查后写保证（见 00_schema.sql L316-320 注释）。
4. **act_signup 双唯一键**：`signup_no` 全局唯一报名编号；`(activity_id,user_id)` 防止同一用户重复报名。
5. **ich_activity.version 为乐观锁**，配合 `signed/quota` 条件更新防止超卖（@Version，见 schema-h2.sql L176-177）。
6. **AI 检索为关键词 LIKE 加权匹配**，`ai_knowledge_chunk.vector_id` 仅为预留字段，当前未接入向量库/Embedding/RAG。

## 五、H2 与 MySQL 结构差异

| 维度 | MySQL（00_schema.sql） | H2（schema-h2.sql） | 影响 |
|------|----------------------|--------------------|------|
| 表/字段/唯一键 | 24 张，见上表 | 同 24 张，一致 | 无功能差异 |
| 全文索引 | `FULLTEXT KEY ft_name_summary` | 无 | 检索走 LIKE，不依赖全文索引 |
| 长文本 | content/history 用 LONGTEXT | TEXT | 兼容 |
| 注释/引擎/字符集 | ENGINE=InnoDB、utf8mb4、COMMENT | 省略 | 仅建表语法差异 |
| 物理外键 | 无（SET FOREIGN_KEY_CHECKS=0） | 无 | 关系均由业务层维护 |
