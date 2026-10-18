-- ============================================================================
-- H2 建表脚本（h2 profile 使用）
--
-- 说明：本文件与 sql/00_schema.sql 结构一致，仅去掉 MySQL 专有语法
--      （ENGINE / CHARSET / COMMENT / FULLTEXT），以便在 H2 内存库中运行。
--      真实数据库请使用 sql/00_schema.sql。
-- ============================================================================

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  username        VARCHAR(50)  NOT NULL,
  password        VARCHAR(100) NOT NULL,
  nickname        VARCHAR(50)  DEFAULT NULL,
  avatar          VARCHAR(255) DEFAULT NULL,
  phone           VARCHAR(20)  DEFAULT NULL,
  email           VARCHAR(100) DEFAULT NULL,
  real_name       VARCHAR(50)  DEFAULT NULL,
  id_card         VARCHAR(255) DEFAULT NULL,
  role_id         BIGINT       DEFAULT NULL,
  status          TINYINT      NOT NULL DEFAULT 1,
  last_login_time DATETIME     DEFAULT NULL,
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME     DEFAULT NULL,
  deleted         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE (username)
);

DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  role_name   VARCHAR(50) NOT NULL,
  role_code   VARCHAR(50) NOT NULL,
  remark      VARCHAR(255) DEFAULT NULL,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME    DEFAULT NULL,
  deleted     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE (role_code)
);

DROP TABLE IF EXISTS ich_category;
CREATE TABLE ich_category (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  category_name VARCHAR(50)  NOT NULL,
  category_code VARCHAR(10)  DEFAULT NULL,
  parent_id     BIGINT       NOT NULL DEFAULT 0,
  sort_no       INT          NOT NULL DEFAULT 0,
  icon          VARCHAR(255) DEFAULT NULL,
  status        TINYINT      NOT NULL DEFAULT 1,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     DEFAULT NULL,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS ich_inheritor;
CREATE TABLE ich_inheritor (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       DEFAULT NULL,
  name        VARCHAR(50)  NOT NULL,
  gender      TINYINT      DEFAULT NULL,
  birth_year  INT          DEFAULT NULL,
  level       TINYINT      DEFAULT NULL,
  region      VARCHAR(100) DEFAULT NULL,
  project_id  BIGINT       DEFAULT NULL,
  craft_years INT          DEFAULT NULL,
  cert_no     VARCHAR(50)  DEFAULT NULL,
  cert_img    VARCHAR(255) DEFAULT NULL,
  intro       TEXT         DEFAULT NULL,
  works       TEXT         DEFAULT NULL,
  contact     VARCHAR(50)  DEFAULT NULL,
  cert_status TINYINT      NOT NULL DEFAULT 0,
  view_count  INT          NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT NULL,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE (cert_no)
);

DROP TABLE IF EXISTS ich_project;
CREATE TABLE ich_project (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  project_name    VARCHAR(100) NOT NULL,
  project_code    VARCHAR(50)  DEFAULT NULL,
  category_id     BIGINT       DEFAULT NULL,
  level           TINYINT      NOT NULL DEFAULT 1,
  region          VARCHAR(120) DEFAULT NULL,
  batch_no        TINYINT      DEFAULT NULL,
  announce_year   INT          DEFAULT NULL,
  type            VARCHAR(20)  DEFAULT NULL,
  protection_unit VARCHAR(150) DEFAULT NULL,
  cover_url       VARCHAR(255) DEFAULT NULL,
  summary         VARCHAR(500) DEFAULT NULL,
  content         TEXT         DEFAULT NULL,
  history         TEXT         DEFAULT NULL,
  status_desc     VARCHAR(500) DEFAULT NULL,
  inheritor_id    BIGINT       DEFAULT NULL,
  audit_status    TINYINT      NOT NULL DEFAULT 1,
  audit_remark    VARCHAR(255) DEFAULT NULL,
  view_count      INT          NOT NULL DEFAULT 0,
  favorite_count  INT          NOT NULL DEFAULT 0,
  sort_no         INT          NOT NULL DEFAULT 0,
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME     DEFAULT NULL,
  deleted         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE (project_code)
);

DROP TABLE IF EXISTS ich_project_inheritor;
CREATE TABLE ich_project_inheritor (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  project_id   BIGINT      NOT NULL,
  inheritor_id BIGINT      NOT NULL,
  relation     VARCHAR(20) DEFAULT NULL,
  create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (project_id, inheritor_id)
);

-- ---------------- 文化资讯 ----------------
DROP TABLE IF EXISTS cms_article_category;
CREATE TABLE cms_article_category (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  name        VARCHAR(50) NOT NULL,
  sort_no     INT         NOT NULL DEFAULT 0,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME    DEFAULT NULL,
  deleted     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS cms_article;
CREATE TABLE cms_article (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  title        VARCHAR(200) NOT NULL,
  category_id  BIGINT       DEFAULT NULL,
  cover_url    VARCHAR(255) DEFAULT NULL,
  summary      VARCHAR(500) DEFAULT NULL,
  content      TEXT         DEFAULT NULL,
  project_id   BIGINT       DEFAULT NULL,
  author_id    BIGINT       DEFAULT NULL,
  source       VARCHAR(200) DEFAULT NULL,
  is_top       TINYINT      NOT NULL DEFAULT 0,
  audit_status TINYINT      NOT NULL DEFAULT 0,
  audit_remark VARCHAR(255) DEFAULT NULL,
  view_count   INT          NOT NULL DEFAULT 0,
  publish_time DATETIME     DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     DEFAULT NULL,
  deleted      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

-- ---------------- 活动组织 ----------------
DROP TABLE IF EXISTS ich_activity;
CREATE TABLE ich_activity (
  id              BIGINT         NOT NULL AUTO_INCREMENT,
  title           VARCHAR(150)   NOT NULL,
  activity_type   TINYINT        DEFAULT NULL,
  cover_url       VARCHAR(255)   DEFAULT NULL,
  project_id      BIGINT         DEFAULT NULL,
  inheritor_id    BIGINT         DEFAULT NULL,
  content         TEXT           DEFAULT NULL,
  start_time      DATETIME       NOT NULL,
  end_time        DATETIME       NOT NULL,
  address         VARCHAR(200)   DEFAULT NULL,
  longitude       DECIMAL(10, 6) DEFAULT NULL,
  latitude        DECIMAL(10, 6) DEFAULT NULL,
  quota           INT            NOT NULL DEFAULT 0,
  signed          INT            NOT NULL DEFAULT 0,
  signup_deadline DATETIME       DEFAULT NULL,
  -- 乐观锁版本号：MyBatis-Plus 的 @Version 依赖该列防止名额超卖
  version         INT            NOT NULL DEFAULT 0,
  status          TINYINT        NOT NULL DEFAULT 0,
  audit_remark    VARCHAR(255)   DEFAULT NULL,
  create_time     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME       DEFAULT NULL,
  deleted         TINYINT        NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

DROP TABLE IF EXISTS act_signup;
CREATE TABLE act_signup (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  activity_id    BIGINT       NOT NULL,
  user_id        BIGINT       NOT NULL,
  signup_no      VARCHAR(32)  NOT NULL,
  real_name      VARCHAR(50)  DEFAULT NULL,
  phone          VARCHAR(20)  DEFAULT NULL,
  remark         VARCHAR(255) DEFAULT NULL,
  signup_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  cancel_time    DATETIME     DEFAULT NULL,
  status         TINYINT      NOT NULL DEFAULT 1,
  checkin_status TINYINT      NOT NULL DEFAULT 0,
  checkin_time   DATETIME     DEFAULT NULL,
  checkin_by     BIGINT       DEFAULT NULL,
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME     DEFAULT NULL,
  deleted        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE (signup_no),
  -- 同一用户对同一活动只能有一条有效报名记录，从数据层杜绝重复报名
  UNIQUE (activity_id, user_id)
);

-- ---------------- 用户互动 ----------------
DROP TABLE IF EXISTS interaction;
CREATE TABLE interaction (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  target_type TINYINT      NOT NULL,
  target_id   BIGINT       NOT NULL,
  type        TINYINT      NOT NULL,
  content     VARCHAR(500) DEFAULT NULL,
  parent_id   BIGINT       NOT NULL DEFAULT 0,
  like_count  INT          NOT NULL DEFAULT 0,
  status      TINYINT      NOT NULL DEFAULT 1,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT NULL,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  -- 只建普通索引，不建唯一索引。
  -- type 同时承载「收藏/点赞」（开关式，一个用户对一个目标只应有一条有效记录）
  -- 与「评论」（列表式，同一用户可对同一目标发多条）；
  -- 建唯一索引会导致第二条评论撞唯一键失败。
  -- 开关式互动的幂等由业务层「先查后写」保证（InteractionService#toggle）。
  INDEX idx_user_target_type (user_id, target_type, target_id, type)
);

-- ============================================================================
-- 以下为第二批补齐的表（与 sql/00_schema.sql 同步，另加接口落地表）
-- 第二批补齐：sys_menu / sys_role_menu / sys_login_log / ich_project_media /
--             user_favorite / sys_audit_log / sys_message /
--             ai_knowledge / ai_knowledge_chunk / ai_chat（迁移自 00_schema.sql）
-- 接口落地表：sys_banner（推荐位）/ interaction_report（评论举报）/
--             user_browse_history（浏览历史）
-- ============================================================================

-- ---------------- 菜单与权限 ----------------
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  parent_id   BIGINT       NOT NULL DEFAULT 0,
  menu_name   VARCHAR(50)  NOT NULL,
  menu_type   TINYINT      NOT NULL DEFAULT 1 COMMENT '1目录 2菜单 3按钮',
  path        VARCHAR(200) DEFAULT NULL,
  component   VARCHAR(200) DEFAULT NULL,
  perms       VARCHAR(100) DEFAULT NULL,
  icon        VARCHAR(50)  DEFAULT NULL,
  sort_no     INT          NOT NULL DEFAULT 0,
  visible     TINYINT      NOT NULL DEFAULT 1,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT NULL,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_parent (parent_id)
);

DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
  id      BIGINT NOT NULL AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (role_id, menu_id)
);

-- ---------------- 登录日志 ----------------
DROP TABLE IF EXISTS sys_login_log;
CREATE TABLE sys_login_log (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  user_id    BIGINT       DEFAULT NULL,
  username   VARCHAR(50)  DEFAULT NULL,
  ip         VARCHAR(64)  DEFAULT NULL,
  user_agent VARCHAR(500) DEFAULT NULL,
  status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
  message    VARCHAR(255) DEFAULT NULL,
  login_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_login_user_time (user_id, login_time)
);

-- ---------------- 项目多媒体 ----------------
DROP TABLE IF EXISTS ich_project_media;
CREATE TABLE ich_project_media (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  project_id  BIGINT       DEFAULT NULL,
  media_type  TINYINT      NOT NULL DEFAULT 1 COMMENT '1图片 2音频 3视频',
  url         VARCHAR(500) NOT NULL,
  title       VARCHAR(200) DEFAULT NULL,
  sort_no     INT          NOT NULL DEFAULT 0,
  source      VARCHAR(200) DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT NULL,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_project (project_id)
);

-- ---------------- 收藏表（与 interaction 互补，提供高频查询） ----------------
DROP TABLE IF EXISTS user_favorite;
CREATE TABLE user_favorite (
  id          BIGINT   NOT NULL AUTO_INCREMENT,
  user_id     BIGINT   NOT NULL,
  target_type TINYINT  NOT NULL COMMENT '1非遗项目 2文化资讯 3非遗活动',
  target_id   BIGINT   NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (user_id, target_type, target_id),
  INDEX idx_fav_target (target_type, target_id)
);

-- ---------------- 浏览历史 ----------------
DROP TABLE IF EXISTS user_browse_history;
CREATE TABLE user_browse_history (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  user_id        BIGINT       NOT NULL,
  target_type    TINYINT      NOT NULL COMMENT '1非遗项目 2文化资讯 3非遗活动',
  target_id      BIGINT       NOT NULL,
  last_view_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  view_count     INT          NOT NULL DEFAULT 1,
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (user_id, target_type, target_id),
  INDEX idx_history_user_time (user_id, last_view_time)
);

-- ---------------- 审计日志 ----------------
DROP TABLE IF EXISTS sys_audit_log;
CREATE TABLE sys_audit_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  target_type TINYINT      NOT NULL COMMENT '1项目 2资讯 3活动 4传承人 5评论 6用户',
  target_id   BIGINT       NOT NULL,
  auditor_id  BIGINT       DEFAULT NULL,
  action      TINYINT      NOT NULL COMMENT '1通过 2驳回 3下架 4禁用 5其他',
  remark      VARCHAR(255) DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_audit_target (target_type, target_id)
);

-- ---------------- 消息通知 ----------------
DROP TABLE IF EXISTS sys_message;
CREATE TABLE sys_message (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL COMMENT '接收人',
  title       VARCHAR(150) DEFAULT NULL,
  content     VARCHAR(500) DEFAULT NULL,
  msg_type    TINYINT      NOT NULL DEFAULT 1 COMMENT '1系统 2报名 3审核 4互动',
  biz_id      BIGINT       DEFAULT NULL,
  is_read     TINYINT      NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT NULL,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_user_read (user_id, is_read)
);

-- ---------------- 评论举报 ----------------
DROP TABLE IF EXISTS interaction_report;
CREATE TABLE interaction_report (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  interaction_id  BIGINT       NOT NULL,
  user_id         BIGINT       NOT NULL COMMENT '举报人',
  reason          VARCHAR(255) DEFAULT NULL,
  status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理 2已驳回',
  handle_by       BIGINT       DEFAULT NULL,
  handle_time     DATETIME     DEFAULT NULL,
  remark          VARCHAR(255) DEFAULT NULL,
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (interaction_id, user_id),
  INDEX idx_status (status)
);

-- ---------------- 首页推荐位 ----------------
DROP TABLE IF EXISTS sys_banner;
CREATE TABLE sys_banner (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  title       VARCHAR(100) DEFAULT NULL,
  image_url   VARCHAR(255) DEFAULT NULL,
  target_type TINYINT      DEFAULT NULL COMMENT '1项目 2资讯 3活动 4外链',
  target_id   BIGINT       DEFAULT NULL,
  link_url    VARCHAR(255) DEFAULT NULL,
  sort_no     INT          NOT NULL DEFAULT 0,
  status      TINYINT      NOT NULL DEFAULT 1,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT NULL,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

-- ---------------- AI 智能问答（3 张） ----------------
DROP TABLE IF EXISTS ai_knowledge;
CREATE TABLE ai_knowledge (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  title        VARCHAR(200)  NOT NULL,
  source_type  TINYINT       NOT NULL COMMENT '1非遗项目 2传承人 3资讯 4政策/术语',
  source_id    BIGINT        DEFAULT NULL,
  category_id  BIGINT        DEFAULT NULL,
  region       VARCHAR(120)  DEFAULT NULL,
  content      TEXT          NOT NULL,
  chunk_count  INT           NOT NULL DEFAULT 0,
  index_status TINYINT       NOT NULL DEFAULT 0 COMMENT '0待索引 1已索引 2索引失败',
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME      DEFAULT NULL,
  deleted      TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_source (source_type, source_id),
  INDEX idx_index (index_status)
);

DROP TABLE IF EXISTS ai_knowledge_chunk;
CREATE TABLE ai_knowledge_chunk (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  knowledge_id BIGINT        NOT NULL,
  chunk_index  INT           NOT NULL DEFAULT 0,
  content      TEXT          NOT NULL,
  vector_id    VARCHAR(100)  DEFAULT NULL,
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_knowledge (knowledge_id)
);

DROP TABLE IF EXISTS ai_chat;
CREATE TABLE ai_chat (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  user_id       BIGINT        DEFAULT NULL,
  session_id    VARCHAR(64)   DEFAULT NULL,
  question      VARCHAR(1000) NOT NULL,
  answer        TEXT          DEFAULT NULL,
  refs          TEXT          DEFAULT NULL,
  top_score     DECIMAL(5,4)  DEFAULT NULL,
  hit_count     INT           NOT NULL DEFAULT 0,
  is_useful     TINYINT       DEFAULT NULL,
  review_status TINYINT       NOT NULL DEFAULT 0 COMMENT '0未复核 1通过 2标记问题',
  token_used    INT           NOT NULL DEFAULT 0,
  cost_ms       INT           NOT NULL DEFAULT 0,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME      DEFAULT NULL,
  deleted       TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_user (user_id),
  INDEX idx_session (session_id),
  INDEX idx_time (create_time)
);
