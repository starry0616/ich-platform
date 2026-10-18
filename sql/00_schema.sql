-- ============================================================================
-- 非遗文化传承与互动平台 —— 数据库建表脚本
-- 数据库：MySQL 8.0 / InnoDB / utf8mb4
-- 说明：共 20 张业务表，字段设计见《系统设计说明书》第 5 章
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `dsh_ich`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `dsh_ich`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 一、用户与权限（5 张）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户主键',
  `username`        VARCHAR(50)  NOT NULL                COMMENT '登录用户名',
  `password`        VARCHAR(100) NOT NULL                COMMENT 'BCrypt 加盐哈希密码',
  `nickname`        VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
  `avatar`          VARCHAR(255) DEFAULT NULL            COMMENT '头像地址',
  `phone`           VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  `email`           VARCHAR(100) DEFAULT NULL            COMMENT '电子邮箱',
  `real_name`       VARCHAR(50)  DEFAULT NULL            COMMENT '真实姓名',
  `id_card`         VARCHAR(255) DEFAULT NULL            COMMENT '证件号码（加密存储）',
  `role_id`         BIGINT       DEFAULT NULL            COMMENT '角色标识',
  `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '账号状态 0禁用 1正常',
  `last_login_time` DATETIME     DEFAULT NULL            COMMENT '最近登录时间',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除 0正常 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `role_name`   VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code`   VARCHAR(50) NOT NULL COMMENT '角色编码',
  `remark`      VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父级菜单',
  `menu_name`   VARCHAR(50)  NOT NULL,
  `menu_type`   TINYINT      NOT NULL DEFAULT 1 COMMENT '1目录 2菜单 3按钮',
  `path`        VARCHAR(200) DEFAULT NULL,
  `component`   VARCHAR(200) DEFAULT NULL,
  `perms`       VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
  `icon`        VARCHAR(50)  DEFAULT NULL,
  `sort_no`     INT          NOT NULL DEFAULT 0,
  `visible`     TINYINT      NOT NULL DEFAULT 1,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id`      BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       DEFAULT NULL,
  `username`    VARCHAR(50)  DEFAULT NULL,
  `ip`          VARCHAR(64)  DEFAULT NULL,
  `user_agent`  VARCHAR(500) DEFAULT NULL,
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
  `message`     VARCHAR(255) DEFAULT NULL,
  `login_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- ---------------------------------------------------------------------------
-- 二、非遗资源（5 张）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `ich_category`;
CREATE TABLE `ich_category` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `category_name` VARCHAR(50)  NOT NULL COMMENT '类别名称',
  `category_code` VARCHAR(10)  DEFAULT NULL COMMENT '罗马数字类别编号',
  `parent_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '父级类别',
  `sort_no`       INT          NOT NULL DEFAULT 0,
  `icon`          VARCHAR(255) DEFAULT NULL,
  `status`        TINYINT      NOT NULL DEFAULT 1,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='非遗类别表';

DROP TABLE IF EXISTS `ich_inheritor`;
CREATE TABLE `ich_inheritor` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT       DEFAULT NULL COMMENT '关联平台账号',
  `name`         VARCHAR(50)  NOT NULL COMMENT '姓名',
  `gender`       TINYINT      DEFAULT NULL COMMENT '1男 2女',
  `birth_year`   INT          DEFAULT NULL,
  `level`        TINYINT      DEFAULT NULL COMMENT '1国家级 2省级 3市级 4县级',
  `region`       VARCHAR(100) DEFAULT NULL COMMENT '所属地区',
  `project_id`   BIGINT       DEFAULT NULL COMMENT '代表性项目',
  `craft_years`  INT          DEFAULT NULL COMMENT '从艺年限',
  `cert_no`      VARCHAR(50)  DEFAULT NULL COMMENT '认定编号',
  `cert_img`     VARCHAR(255) DEFAULT NULL COMMENT '证书图片',
  `intro`        TEXT         DEFAULT NULL COMMENT '人物简介',
  `works`        TEXT         DEFAULT NULL COMMENT '代表性作品',
  `contact`      VARCHAR(50)  DEFAULT NULL COMMENT '公开联系方式',
  `cert_status`  TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审核 1已认证 2已驳回',
  `view_count`   INT          NOT NULL DEFAULT 0,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cert_no` (`cert_no`),
  KEY `idx_name` (`name`),
  KEY `idx_region` (`region`),
  KEY `idx_level` (`level`),
  KEY `idx_cert_status` (`cert_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='非遗传承人表';

DROP TABLE IF EXISTS `ich_project`;
CREATE TABLE `ich_project` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `project_name`    VARCHAR(100) NOT NULL COMMENT '项目名称',
  `project_code`    VARCHAR(50)  DEFAULT NULL COMMENT '项目编号（如 Ⅷ-12）',
  `category_id`     BIGINT       DEFAULT NULL COMMENT '所属类别',
  `level`           TINYINT      NOT NULL DEFAULT 1 COMMENT '1国家级 2省级 3市级 4县级',
  `region`          VARCHAR(120) DEFAULT NULL COMMENT '申报地区/流布地域',
  `batch_no`        TINYINT      DEFAULT NULL COMMENT '批次序号',
  `announce_year`   INT          DEFAULT NULL COMMENT '公布年份',
  `type`            VARCHAR(20)  DEFAULT NULL COMMENT '新增项目/扩展项目',
  `protection_unit` VARCHAR(150) DEFAULT NULL COMMENT '保护单位',
  `cover_url`       VARCHAR(255) DEFAULT NULL COMMENT '封面图',
  `summary`         VARCHAR(500) DEFAULT NULL COMMENT '项目简介',
  `content`         LONGTEXT     DEFAULT NULL COMMENT '详细内容',
  `history`         LONGTEXT     DEFAULT NULL COMMENT '历史渊源',
  `status_desc`     VARCHAR(500) DEFAULT NULL COMMENT '传承现状',
  `inheritor_id`    BIGINT       DEFAULT NULL COMMENT '第一代表性传承人',
  `audit_status`    TINYINT      NOT NULL DEFAULT 1 COMMENT '0待审核 1已发布 2已下架 3已驳回',
  `audit_remark`    VARCHAR(255) DEFAULT NULL,
  `view_count`      INT          NOT NULL DEFAULT 0,
  `favorite_count`  INT          NOT NULL DEFAULT 0,
  `sort_no`         INT          NOT NULL DEFAULT 0,
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_name` (`project_name`),
  KEY `idx_category` (`category_id`),
  KEY `idx_region` (`region`),
  KEY `idx_level` (`level`),
  KEY `idx_audit` (`audit_status`),
  KEY `idx_batch` (`batch_no`),
  FULLTEXT KEY `ft_name_summary` (`project_name`, `summary`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='非遗项目表';

DROP TABLE IF EXISTS `ich_project_inheritor`;
CREATE TABLE `ich_project_inheritor` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT,
  `project_id`   BIGINT   NOT NULL,
  `inheritor_id` BIGINT   NOT NULL,
  `relation`     VARCHAR(20) DEFAULT NULL COMMENT '关系类型（传承）',
  `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_proj_inh` (`project_id`, `inheritor_id`),
  KEY `idx_inheritor` (`inheritor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目与传承人关联表';

DROP TABLE IF EXISTS `ich_project_media`;
CREATE TABLE `ich_project_media` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `project_id`  BIGINT       DEFAULT NULL,
  `media_type`  TINYINT      NOT NULL DEFAULT 1 COMMENT '1图片 2音频 3视频',
  `url`         VARCHAR(500) NOT NULL,
  `title`       VARCHAR(200) DEFAULT NULL,
  `sort_no`     INT          NOT NULL DEFAULT 0,
  `source`      VARCHAR(200) DEFAULT NULL COMMENT '来源与授权说明',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目多媒体资源表';

-- ---------------------------------------------------------------------------
-- 三、内容传播（2 张）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `cms_article_category`;
CREATE TABLE `cms_article_category` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(50) NOT NULL,
  `sort_no`     INT         NOT NULL DEFAULT 0,
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资讯分类表';

DROP TABLE IF EXISTS `cms_article`;
CREATE TABLE `cms_article` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `title`         VARCHAR(200) NOT NULL,
  `category_id`   BIGINT       DEFAULT NULL,
  `cover_url`     VARCHAR(255) DEFAULT NULL,
  `summary`       VARCHAR(500) DEFAULT NULL,
  `content`       LONGTEXT     DEFAULT NULL,
  `project_id`    BIGINT       DEFAULT NULL COMMENT '关联非遗项目',
  `author_id`     BIGINT       DEFAULT NULL COMMENT '作者',
  `source`        VARCHAR(200) DEFAULT NULL COMMENT '稿件来源',
  `is_top`        TINYINT      NOT NULL DEFAULT 0,
  `audit_status`  TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审核 1已发布 2已下架 3已驳回',
  `audit_remark`  VARCHAR(255) DEFAULT NULL,
  `view_count`    INT          NOT NULL DEFAULT 0,
  `publish_time`  DATETIME     DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_audit` (`audit_status`),
  KEY `idx_publish` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文化资讯表';

-- ---------------------------------------------------------------------------
-- 四、活动组织（2 张）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `ich_activity`;
CREATE TABLE `ich_activity` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `title`            VARCHAR(150) NOT NULL,
  `activity_type`    TINYINT      DEFAULT NULL COMMENT '1展演 2体验课堂 3讲座 4展览',
  `cover_url`        VARCHAR(255) DEFAULT NULL,
  `project_id`       BIGINT       DEFAULT NULL,
  `inheritor_id`     BIGINT       DEFAULT NULL COMMENT '主办传承人',
  `content`          LONGTEXT     DEFAULT NULL,
  `start_time`       DATETIME     NOT NULL,
  `end_time`         DATETIME     NOT NULL,
  `address`          VARCHAR(200) DEFAULT NULL,
  `longitude`        DECIMAL(10,6) DEFAULT NULL,
  `latitude`         DECIMAL(10,6) DEFAULT NULL,
  `quota`            INT          NOT NULL DEFAULT 0 COMMENT '名额上限',
  `signed`           INT          NOT NULL DEFAULT 0 COMMENT '已报名数',
  `signup_deadline`  DATETIME     DEFAULT NULL,
  `version`          INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `status`           TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审核 1报名中 2报名结束 3进行中 4已结束 5已取消',
  `audit_remark`     VARCHAR(255) DEFAULT NULL,
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_start` (`start_time`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='非遗活动表';

DROP TABLE IF EXISTS `act_signup`;
CREATE TABLE `act_signup` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `activity_id`    BIGINT       NOT NULL,
  `user_id`        BIGINT       NOT NULL,
  `signup_no`      VARCHAR(32)  NOT NULL COMMENT '报名编号',
  `real_name`      VARCHAR(50)  DEFAULT NULL,
  `phone`          VARCHAR(20)  DEFAULT NULL,
  `remark`         VARCHAR(255) DEFAULT NULL,
  `signup_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `cancel_time`    DATETIME     DEFAULT NULL,
  `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '1已报名 2已取消 3已核销 4已释放',
  `checkin_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '0未核销 1已核销',
  `checkin_time`   DATETIME     DEFAULT NULL,
  `checkin_by`     BIGINT       DEFAULT NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_signup_no` (`signup_no`),
  UNIQUE KEY `uk_act_user` (`activity_id`, `user_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动报名表';

-- ---------------------------------------------------------------------------
-- 五、用户互动（2 张）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `interaction`;
CREATE TABLE `interaction` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL,
  `target_type` TINYINT      NOT NULL COMMENT '1非遗项目 2文化资讯 3非遗活动',
  `target_id`   BIGINT       NOT NULL,
  `type`        TINYINT      NOT NULL COMMENT '1收藏 2点赞 3评论',
  `content`     VARCHAR(500) DEFAULT NULL COMMENT '评论内容',
  `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父评论',
  `like_count`  INT          NOT NULL DEFAULT 0,
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '0已屏蔽 1正常',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  -- 注意：这里只建普通索引，不建唯一索引。
  -- 原因：type 同时承载「收藏/点赞」（开关式，一个用户对一个目标只应有一条有效记录）
  -- 与「评论」（列表式，同一用户可以对同一目标发表多条）。
  -- 若对四列建唯一索引，第二条评论会撞唯一键而失败。
  -- 因此开关式互动的幂等改由业务层「先查后写」保证（见 InteractionService#toggle）。
  KEY `idx_user_target_type` (`user_id`, `target_type`, `target_id`, `type`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互动记录表';

DROP TABLE IF EXISTS `user_favorite`;
CREATE TABLE `user_favorite` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT   NOT NULL,
  `target_type` TINYINT  NOT NULL COMMENT '1非遗项目 2文化资讯 3非遗活动',
  `target_id`   BIGINT   NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- ---------------------------------------------------------------------------
-- 六、审计与消息（2 张）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_audit_log`;
CREATE TABLE `sys_audit_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `target_type`   TINYINT      NOT NULL COMMENT '1项目 2资讯 3活动 4传承人 5评论',
  `target_id`     BIGINT       NOT NULL,
  `auditor_id`    BIGINT       DEFAULT NULL,
  `action`        TINYINT      NOT NULL COMMENT '1通过 2驳回 3下架',
  `remark`        VARCHAR(255) DEFAULT NULL,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容审核记录表';

DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL COMMENT '接收人',
  `title`       VARCHAR(150) DEFAULT NULL,
  `content`     VARCHAR(500) DEFAULT NULL,
  `msg_type`    TINYINT      NOT NULL DEFAULT 1 COMMENT '1系统 2报名 3审核 4互动',
  `biz_id`      BIGINT       DEFAULT NULL COMMENT '关联业务主键',
  `is_read`     TINYINT      NOT NULL DEFAULT 0,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- ---------------------------------------------------------------------------
-- 七、智能问答（3 张）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `ai_knowledge`;
CREATE TABLE `ai_knowledge` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `title`        VARCHAR(200) NOT NULL,
  `source_type`  TINYINT      NOT NULL COMMENT '1非遗项目 2传承人 3资讯 4政策/术语',
  `source_id`    BIGINT       DEFAULT NULL,
  `category_id`  BIGINT       DEFAULT NULL COMMENT '检索过滤用元数据',
  `region`       VARCHAR(120) DEFAULT NULL COMMENT '检索过滤用元数据',
  `content`      LONGTEXT     NOT NULL,
  `chunk_count`  INT          NOT NULL DEFAULT 0,
  `index_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '0待索引 1已索引 2索引失败',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_type`, `source_id`),
  KEY `idx_index` (`index_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 知识库文档表';

DROP TABLE IF EXISTS `ai_knowledge_chunk`;
CREATE TABLE `ai_knowledge_chunk` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `knowledge_id`  BIGINT       NOT NULL,
  `chunk_index`   INT          NOT NULL DEFAULT 0,
  `content`       TEXT         NOT NULL,
  `vector_id`     VARCHAR(100) DEFAULT NULL COMMENT '向量库中的标识',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_knowledge` (`knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 知识切片表';

DROP TABLE IF EXISTS `ai_chat`;
CREATE TABLE `ai_chat` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT        DEFAULT NULL,
  `session_id`    VARCHAR(64)   DEFAULT NULL,
  `question`      VARCHAR(1000) NOT NULL,
  `answer`        LONGTEXT      DEFAULT NULL,
  `refs`          TEXT          DEFAULT NULL COMMENT '引用来源 JSON',
  `top_score`     DECIMAL(5,4)  DEFAULT NULL COMMENT '最高相似度',
  `hit_count`     INT           NOT NULL DEFAULT 0,
  `is_useful`     TINYINT       DEFAULT NULL,
  `review_status` TINYINT       NOT NULL DEFAULT 0 COMMENT '0未复核 1通过 2标记问题',
  `token_used`    INT           NOT NULL DEFAULT 0,
  `cost_ms`       INT           NOT NULL DEFAULT 0,
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`       TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_session` (`session_id`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 问答记录表';

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- 七之二、接口落地补充表（H2 schema 同步）
--   sys_banner（首页推荐位，对应接口 66—67）
--   interaction_report（评论举报，对应接口 41、65 的举报处理）
--   user_browse_history（浏览历史，对应接口 10）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_banner`;
CREATE TABLE `sys_banner` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `title`       VARCHAR(100) DEFAULT NULL,
  `image_url`   VARCHAR(255) DEFAULT NULL,
  `target_type` TINYINT      DEFAULT NULL COMMENT '1项目 2资讯 3活动 4外链',
  `target_id`   BIGINT       DEFAULT NULL,
  `link_url`    VARCHAR(255) DEFAULT NULL,
  `sort_no`     INT          NOT NULL DEFAULT 0,
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页推荐位表';

DROP TABLE IF EXISTS `interaction_report`;
CREATE TABLE `interaction_report` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `interaction_id` BIGINT       NOT NULL COMMENT '被举报的互动记录（评论）',
  `user_id`        BIGINT       NOT NULL COMMENT '举报人',
  `reason`         VARCHAR(255) DEFAULT NULL,
  `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理 2已驳回',
  `handle_by`      BIGINT       DEFAULT NULL,
  `handle_time`    DATETIME     DEFAULT NULL,
  `remark`         VARCHAR(255) DEFAULT NULL,
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report` (`interaction_id`, `user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论举报表';

DROP TABLE IF EXISTS `user_browse_history`;
CREATE TABLE `user_browse_history` (
  `id`             BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT   NOT NULL,
  `target_type`    TINYINT  NOT NULL COMMENT '1非遗项目 2文化资讯 3非遗活动',
  `target_id`      BIGINT   NOT NULL,
  `last_view_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `view_count`     INT      NOT NULL DEFAULT 1,
  `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
  KEY `idx_user_time` (`user_id`, `last_view_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户浏览历史表';


-- ---------------------------------------------------------------------------
-- 九、系统扩展表：菜单/日志/媒体/收藏/审计/消息/AI 知识库（与 schema-h2 对齐）
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- 八、基础字典数据：角色与管理员账号
-- ---------------------------------------------------------------------------
INSERT INTO `sys_role` (`role_name`, `role_code`, `remark`) VALUES
  ('普通用户',   'ROLE_USER',      '注册公众用户'),
  ('非遗传承人', 'ROLE_INHERITOR', '经认证的非遗代表性传承人'),
  ('平台管理员', 'ROLE_ADMIN',     '平台运营与内容管理人员');

-- 默认账号密码均为 123456（BCrypt 加密后的值），首次登录后请立即修改
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `role_id`, `status`, `real_name`) VALUES
  ('admin',     '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '平台管理员', 3, 1, '管理员'),
  ('inheritor', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '示例传承人', 2, 1, '示例'),
  ('user',      '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户',   1, 1, '测试');

INSERT INTO `cms_article_category` (`name`, `sort_no`) VALUES
  ('非遗资讯', 1), ('政策文件', 2), ('传承人故事', 3), ('活动报道', 4), ('学术研究', 5);

-- ============================================================================
-- 建表完成。接下来执行：
--   10_ich_seed_data.sql            （类别 + 项目 + 传承人）
--   11_ich_project_inheritor.sql    （项目-传承人关联）
-- ============================================================================
