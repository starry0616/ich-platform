package com.yuxi.ich.common.constant;

/**
 * 业务状态常量。
 *
 * <p>与《系统设计说明书》表 5-10 数据字典严格对应，前后端共同约定，
 * 禁止在业务代码中出现魔法数字。
 */
public final class StatusConstants {

    private StatusConstants() {
    }

    /** 账号状态 */
    public static final class User {
        private User() {
        }

        public static final int DISABLED = 0;
        public static final int NORMAL = 1;
    }

    /** 内容审核状态（项目、资讯、活动通用） */
    public static final class Audit {
        private Audit() {
        }

        public static final int PENDING = 0;
        public static final int PUBLISHED = 1;
        public static final int OFFLINE = 2;
        public static final int REJECTED = 3;
    }

    /** 非遗项目级别 */
    public static final class Level {
        private Level() {
        }

        public static final int NATIONAL = 1;
        public static final int PROVINCIAL = 2;
        public static final int MUNICIPAL = 3;
        public static final int COUNTY = 4;
    }

    /** 传承人认证状态 */
    public static final class CertStatus {
        private CertStatus() {
        }

        public static final int PENDING = 0;
        public static final int CERTIFIED = 1;
        public static final int REJECTED = 2;
    }

    /** 互动类型 */
    public static final class Interaction {
        private Interaction() {
        }

        public static final int TYPE_FAVORITE = 1;
        public static final int TYPE_LIKE = 2;
        public static final int TYPE_COMMENT = 3;

        public static final int TARGET_PROJECT = 1;
        public static final int TARGET_ARTICLE = 2;
        public static final int TARGET_ACTIVITY = 3;
        public static final int TARGET_INHERITOR = 4;
    }
}
