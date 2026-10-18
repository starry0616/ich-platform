-- ============================================================================
-- H2 演示数据（h2 profile 使用，启动即自动导入）
--
-- 内容：3 个账号 + 10 个类别 + 14 个非遗项目 + 3 位传承人
-- 项目数据取自公开的国家级非遗代表性项目名录，含云南/玉溪本地项目。
-- 注意：真实全量数据（1557 项目 / 3057 传承人）请用 mysql profile +
--      sql/10_ich_seed_data.sql。
--
-- 账号（密码均为 123456，BCrypt 加盐哈希，已回验）：
--   admin     平台管理员
--   inheritor 非遗传承人
--   user      普通用户
-- ============================================================================

-- ---------------- 角色 ----------------
INSERT INTO sys_role (role_name, role_code, remark) VALUES
  ('普通用户',   'ROLE_USER',      '注册公众用户'),
  ('非遗传承人', 'ROLE_INHERITOR', '经认证的非遗代表性传承人'),
  ('平台管理员', 'ROLE_ADMIN',     '平台运营与内容管理人员');

-- ---------------- 用户 ----------------
INSERT INTO sys_user (username, password, nickname, real_name, phone, email, role_id, status) VALUES
  ('admin',     '$2a$10$FRngAL6quWadcddGBqcYPOqDarCO3Lqxwcw3IVrWg9dExq.VFFgI.', '平台管理员', '管理员', '13800000001', 'admin@ich.local',     3, 1),
  ('inheritor', '$2a$10$FRngAL6quWadcddGBqcYPOqDarCO3Lqxwcw3IVrWg9dExq.VFFgI.', '玉溪青花传承人', '李文', '13800000002', 'inh@ich.local',   2, 1),
  ('user',      '$2a$10$FRngAL6quWadcddGBqcYPOqDarCO3Lqxwcw3IVrWg9dExq.VFFgI.', '非遗爱好者', '王小明', '13800000003', 'user@ich.local', 1, 1),
  -- 用于验证「账号禁用后无法登录」的测试账号
  ('disabled',  '$2a$10$FRngAL6quWadcddGBqcYPOqDarCO3Lqxwcw3IVrWg9dExq.VFFgI.', '已禁用账号', '赵六', '13800000004', 'dis@ich.local',  1, 0);

-- ---------------- 非遗类别（十大门类） ----------------
INSERT INTO ich_category (category_name, category_code, parent_id, sort_no, status) VALUES
  ('民间文学',               'Ⅰ', 0, 1,  1),
  ('传统音乐',               'Ⅱ', 0, 2,  1),
  ('传统舞蹈',               'Ⅲ', 0, 3,  1),
  ('传统戏剧',               'Ⅳ', 0, 4,  1),
  ('曲艺',                   'Ⅴ', 0, 5,  1),
  ('传统体育、游艺与杂技',   'Ⅵ', 0, 6,  1),
  ('传统美术',               'Ⅶ', 0, 7,  1),
  ('传统技艺',               'Ⅷ', 0, 8,  1),
  ('传统医药',               'Ⅸ', 0, 9,  1),
  ('民俗',                   'Ⅹ', 0, 10, 1);

-- ---------------- 非遗项目 ----------------
-- audit_status：1 已发布，0 待审核（用于验证前台不可见）
-- 名录字段于 2026-10-18 依据中国非物质文化遗产网 / 国务院公布名录核验：
--   玉溪花灯戏     编号 Ⅳ-78（2006 第一批，保护单位玉溪市文化馆）
--   滇剧           编号 Ⅳ-132（2008 第二批，类型：新增项目，保护单位云南省滇剧院）
--   抬阁（通海高台）编号 Ⅹ-87（2011 第三批，类型：扩展项目，保护单位通海县文化馆）
--   傣族服饰（花腰傣服饰）编号 Ⅹ-183（2021 第五批，类别：民俗，保护单位新平彝族傣族自治县文化馆）
--   玉溪青花瓷、峨山彝族刺绣为省级非遗项目（示例数据），project_code 用 YN- 前缀，
--   不再使用国家级编号（原 Ⅷ-201 属河北衡水法帖雕版拓印技艺、Ⅶ-96 属山西建筑彩绘，均与他项错配）
INSERT INTO ich_project
  (project_name, project_code, category_id, level, region, batch_no, announce_year,
   type, protection_unit, summary, audit_status, view_count, favorite_count, sort_no)
VALUES
  ('玉溪青花瓷烧制技艺',       'YN-QHC', 8,  2, '云南省玉溪市红塔区',       NULL, NULL, NULL, '玉溪市文化馆',
   '（示例数据）玉溪青花瓷烧制技艺为云南省省级非物质文化遗产代表性项目（云南省第三批非遗名录），非国家级名录项目。云南玉溪传统制瓷技艺，以钴料在坯体上绘制纹样后高温一次烧成，釉色青中泛蓝，是云南陶瓷艺术的代表。', 1, 1580, 86, 1),
  ('玉溪花灯戏',               'Ⅳ-78',  4,  1, '云南省玉溪市',             1, 2006, '新增项目', '玉溪市文化馆',
   '流行于云南玉溪一带的地方戏曲剧种，由民间花灯歌舞发展而成，唱腔活泼、表演载歌载舞。', 1, 1320, 74, 2),
  ('滇剧',                     'Ⅳ-132', 4,  1, '云南省玉溪市、昆明市',     2, 2008, '新增项目', '云南省滇剧院',
   '云南主要地方剧种，融合丝弦、胡琴、襄阳三大声腔，以唱做并重、程式严谨著称。', 1, 1180, 63, 3),
  ('抬阁（通海高台）',         'Ⅹ-87',  10, 1, '云南省玉溪市通海县',       3, 2011, '扩展项目', '通海县文化馆（通海县非物质文化遗产保护中心）',
   '通海县传统民俗巡游艺术，以铁杆为骨架将孩童装扮成戏剧人物悬于高台，造型奇巧、惊险壮观。', 1, 960, 51, 4),
  ('峨山彝族刺绣',             'YN-ESXC', 7, 2, '云南省玉溪市峨山彝族自治县', NULL, 2009, NULL, NULL,
   '（示例数据）峨山彝族服饰（含刺绣技艺）2009 年列入云南省非物质文化遗产名录，非国家级名录项目。峨山彝族传统刺绣技艺，纹样多取日、月、火、花鸟，色彩浓烈，用于服饰与背被等生活用品。', 1, 870, 45, 5),
  ('傣族服饰（花腰傣服饰）',   'Ⅹ-183', 10, 1, '云南省玉溪市新平彝族傣族自治县', 5, 2021, '新增项目', '新平彝族傣族自治县文化馆',
   '新平傣族传统服饰制作技艺，含纺织、染色、挑花与银饰加工，纹样体现傣族对自然的观察。', 1, 760, 38, 6),
  ('景德镇手工制瓷技艺',       'Ⅷ-7',   8,  1, '江西省景德镇市',           1, 2006, '新增项目', '景德镇市陶瓷非物质文化遗产保护中心',
   '景德镇千年制瓷技艺体系，涵盖拉坯、利坯、施釉、绘画、烧成等七十二道工序。', 1, 2450, 132, 7),
  ('宜兴紫砂陶制作技艺',       'Ⅷ-8',   8,  1, '江苏省宜兴市',             1, 2006, '新增项目', '宜兴市陶瓷行业协会',
   '以宜兴特有紫砂泥为原料，采用手工成型法制作茶具，泥料透气性佳，素有“世间茶具称为首”之誉。', 1, 2180, 118, 8),
  ('侗族大歌',                 'Ⅱ-28',  2,  1, '贵州省黔东南苗族侗族自治州', 1, 2006, '新增项目', '黎平县文化馆',
   '侗族多声部民间合唱，无指挥、无伴奏，高音低声部自然相和，模拟蝉鸣鸟叫与流水之声。', 1, 1650, 92, 9),
  ('昆曲',                     'Ⅳ-1',   4,  1, '江苏省苏州市',             1, 2006, '新增项目', '江苏省苏州昆剧院',
   '中国最古老的戏曲声腔之一，唱腔婉转、程式典雅，被誉为“百戏之祖”。', 1, 2890, 165, 10),
  ('二十四节气',               'Ⅹ-68',  10, 1, '中国',                     1, 2006, '新增项目', '中国农业博物馆',
   '中国人通过观察太阳周年运动形成的知识体系与实践，用于指导农事与日常生活。', 1, 1980, 104, 11),
  ('中医诊疗法',               'Ⅸ-2',   9,  1, '中国',                     2, 2008, '扩展项目', '中国中医科学院',
   '中医诊断与治疗方法的统称，含望闻问切四诊与针灸、推拿、方药等治疗手段。', 1, 1420, 71, 12),
  ('太极拳',                   'Ⅵ-31',  6,  1, '河北省永年县',             3, 2011, '扩展项目', '永年县文化馆',
   '中国传统武术拳种，以掤捋挤按等十三势为核心，讲究以柔克刚、内外兼修。', 1, 1760, 88, 13),
  -- 待审核项目：前台列表与详情都不可见，用于验证审核链路
  ('某待审核技艺',             'Ⅷ-999', 8,  4, '云南省玉溪市',             5, 2021, '新增项目', '玉溪市文化馆',
   '该项目尚在审核中，前台不应展示。', 0, 0, 0, 99);

-- ---------------- 非遗传承人 ----------------
-- 注意必须带 project_id：列表与详情会 LEFT JOIN ich_project 带出 projectName，
-- 漏掉该列会导致 projectName 恒为空（曾经踩过）。
INSERT INTO ich_inheritor
  (user_id, name, gender, birth_year, level, region, project_id, craft_years,
   cert_no, intro, cert_status)
VALUES
  (2, '李文', 1, 1968, 2, '云南省玉溪市红塔区', 1, 32, 'YN-YX-0001',
   '民族：汉族。（平台演示档案）长期从事玉溪青花瓷烧制技艺实践，主持恢复传统钴料配方。', 1),
  (NULL, '李秀英', 2, 1975, 2, '云南省玉溪市峨山彝族自治县', 5, 26, 'YN-YX-0002',
   '民族：彝族。（平台演示档案）长期开展峨山彝族刺绣实践，擅长日月纹与火纹，并在中小学校开展非遗教学。', 1),
  (NULL, '李文亮', 1, 1972, 2, '云南省玉溪市通海县', 4, 24, 'YN-YX-0003',
   '民族：汉族。（平台演示档案）通海高台民俗活动组织者，主持高台扎制与巡游编排。', 1);

-- ---------------- 项目与传承人关联 ----------------
INSERT INTO ich_project_inheritor (project_id, inheritor_id, relation) VALUES
  (1, 1, '传承'),
  (5, 2, '传承'),
  (4, 3, '传承');

-- ---------------- 资讯分类 ----------------
INSERT INTO cms_article_category (name, sort_no) VALUES
  ('非遗资讯',   1),
  ('政策文件',   2),
  ('传承人故事', 3),
  ('活动报道',   4),
  ('学术研究',   5);

-- ---------------- 文化资讯 ----------------
-- audit_status：1 已发布，0 待审核（用于验证审核链路与前台可见性）
INSERT INTO cms_article
  (title, category_id, summary, content, project_id, author_id, source, is_top,
   audit_status, view_count, publish_time)
VALUES
  ('玉溪青花瓷：从钴料配方到一次烧成的技艺密码', 3,
   '玉溪青花瓷以釉色青中泛蓝著称，本文梳理其钴料配方与烧成工艺的关键环节。',
   '玉溪青花瓷烧制技艺是云南陶瓷艺术的代表。传统工序包括选料、练泥、拉坯、修坯、绘纹、施釉与高温一次烧成……',
   1, 2, '玉溪市文化馆提供', 1, 1, 326, NOW()),

  ('通海高台：铁杆上的民俗奇观', 1,
   '通海高台以铁杆为骨架，将孩童装扮成戏剧人物悬于高台，造型奇巧。',
   '通海高台是流传于通海县的传统民俗巡游艺术，多在春节与重大节庆期间展演……',
   4, 1, '玉溪市文化馆提供', 0, 1, 158, NOW()),

  ('峨山彝族刺绣纹样中的自然观', 5,
   '峨山彝族刺绣的日、月、火、花鸟纹样，折射出彝族对自然的观察方式。',
   '峨山彝族刺绣多用于服饰与背被，纹样取日、月、火、花鸟等元素，色彩浓烈……',
   5, 1, '本站整理', 0, 1, 97, NOW()),

  -- 待审核稿件：前台不可见，用于验证审核链路
  ('待审核稿件：某非遗项目调研记录', 1,
   '这是一篇尚未通过审核的稿件，前台列表与详情均不应展示。',
   '待审核内容……',
   NULL, 2, '本站整理', 0, 0, 0, NULL);

-- ---------------- 非遗活动 ----------------
-- 时间用 DATEADD 相对当前时间生成，保证任何时候执行演示数据都「未过期」。
-- status：1 报名中，0 待审核（前台不可见）
-- quota/signed：用于验证报名与名额控制；其中 id=3 名额仅 1，便于做并发测试
INSERT INTO ich_activity
  (title, activity_type, project_id, inheritor_id, content, start_time, end_time,
   address, quota, signed, signup_deadline, version, status)
VALUES
  ('玉溪青花瓷拉坯体验课堂', 2, 1, 1,
   '由传承人现场示范拉坯与修坯，参与者可上手体验制作一只小碗。',
   DATEADD('DAY', 15, CURRENT_TIMESTAMP), DATEADD('DAY', 15, DATEADD('HOUR', 3, CURRENT_TIMESTAMP)),
   '玉溪市红塔区非遗展示馆', 20, 2,
   DATEADD('DAY', 10, CURRENT_TIMESTAMP), 0, 1),

  ('通海高台巡游展演', 1, 4, 3,
   '传统民俗巡游展演，含高台扎制过程讲解与巡游表演。',
   DATEADD('DAY', 30, CURRENT_TIMESTAMP), DATEADD('DAY', 30, DATEADD('HOUR', 5, CURRENT_TIMESTAMP)),
   '通海县秀山公园广场', 100, 5,
   DATEADD('DAY', 25, CURRENT_TIMESTAMP), 0, 1),

  -- 名额只有 1 的活动，用于验证「名额已满」与并发不超卖
  ('峨山彝族刺绣小班教学', 3, 5, 2,
   '限额小班教学，讲解日月纹与火纹的针法。',
   DATEADD('DAY', 20, CURRENT_TIMESTAMP), DATEADD('DAY', 20, DATEADD('HOUR', 2, CURRENT_TIMESTAMP)),
   '峨山县文化馆', 1, 0,
   DATEADD('DAY', 18, CURRENT_TIMESTAMP), 0, 1),

  -- 已过报名截止的活动，用于验证「报名已截止」
  ('非遗大讲堂：云南陶瓷史', 3, NULL, NULL,
   '讲座已在报名截止后，用于验证报名时间窗口校验。',
   DATEADD('DAY', 2, CURRENT_TIMESTAMP), DATEADD('DAY', 2, DATEADD('HOUR', 2, CURRENT_TIMESTAMP)),
   '玉溪市图书馆报告厅', 50, 0,
   DATEADD('DAY', -1, CURRENT_TIMESTAMP), 0, 1),

  -- 待审核活动：前台不可见
  ('待审核活动：某非遗展演', 1, NULL, NULL,
   '该活动尚在审核中，前台不应展示。',
   DATEADD('DAY', 40, CURRENT_TIMESTAMP), DATEADD('DAY', 40, DATEADD('HOUR', 3, CURRENT_TIMESTAMP)),
   '玉溪市文化馆', 30, 0,
   DATEADD('DAY', 35, CURRENT_TIMESTAMP), 0, 0);

-- ---------------- 报名记录 ----------------
-- 与上面活动的 signed 计数保持一致（活动 1 已报 2 人，活动 2 已报 5 人由代码侧造数不必要，
-- 此处只为活动 1 造两条真实记录，便于验证名单与核销）
INSERT INTO act_signup
  (activity_id, user_id, signup_no, real_name, phone, signup_time, status, checkin_status)
VALUES
  (1, 3, 'YX-DEMO-0001', '王小明', '13800000003', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 1, 0),
  (1, 2, 'YX-DEMO-0002', '李文',   '13800000002', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 1, 0);


-- ============================================================================
-- 第二批种子：菜单 / 角色菜单 / 推荐位 / AI 知识库
-- ============================================================================

-- ---------------- 菜单 ----------------
-- menu_type：1 目录，2 菜单。visible：1 显示，0 隐藏。
-- 后端鉴权仍以 @PreAuthorize 与 SecurityConfig 为准，菜单仅用于前端动态渲染导航。
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_no, visible) VALUES
  (1,  0,  '首页',       2, '/home',       'home/Home.vue',            'HomeFilled',       1, 1),
  (2,  0,  '非遗项目',   2, '/project',    'project/ProjectList.vue',  'Collection',       2, 1),
  (3,  0,  '传承人名录', 2, '/inheritor',  'inheritor/InheritorList.vue', 'UserFilled',   3, 1),
  (4,  0,  '文化资讯',   2, '/article',    'article/ArticleList.vue',  'Document',         4, 1),
  (5,  0,  '非遗活动',   2, '/activity',   'activity/ActivityList.vue','Calendar',         5, 1),
  (6,  0,  'AI 问答',    2, '/ai',         'ai/AiChat.vue',            'ChatDotRound',     6, 1),
  (7,  0,  '个人中心',   2, '/profile',    'user/Profile.vue',         'Avatar',           7, 1),
  (8,  0,  '传承人工作台', 1, '/workbench', '',                        'Tools',            10, 1),
  (9,  8,  '我的项目',   2, '/workbench/project',   'workbench/MyProject.vue',  'Collection',  11, 1),
  (10, 8,  '我的活动',   2, '/workbench/activity',  'workbench/MyActivity.vue', 'Calendar',   12, 1),
  (11, 8,  '报名名单',   2, '/workbench/signups',   'workbench/SignupList.vue',  'List',       13, 1),
  (12, 8,  '数据看板',   2, '/workbench/stats',     'workbench/Stats.vue',       'TrendCharts',14, 1),
  (13, 0,  '管理后台',   1, '/admin',       '',                       'Setting',             20, 1),
  (14, 13, '数据总览',   2, '/admin/stats',     'admin/Stats.vue',      'DataAnalysis',  21, 1),
  (15, 13, '用户管理',   2, '/admin/user',      'admin/UserList.vue',   'User',          22, 1),
  (16, 13, '传承人审核', 2, '/admin/inheritor', 'admin/InheritorAudit.vue', 'Stamp',     23, 1),
  (17, 13, '项目审核',   2, '/admin/project',   'admin/ProjectAudit.vue',  'Checked',     24, 1),
  (18, 13, '资讯审核',   2, '/admin/article',   'admin/ArticleAudit.vue',  'DocumentChecked', 25, 1),
  (19, 13, '活动审核',   2, '/admin/activity',  'admin/ActivityAudit.vue', 'Calendar',    26, 1),
  (20, 13, '类别管理',   2, '/admin/category',  'admin/CategoryManage.vue', 'Menu',       27, 1),
  (21, 13, '评论与举报', 2, '/admin/comment',   'admin/CommentManage.vue', 'ChatLineSquare', 28, 1),
  (22, 13, '推荐位配置', 2, '/admin/banner',    'admin/BannerManage.vue',  'Picture',     29, 1),
  (23, 13, 'AI 知识库',  2, '/admin/ai',        'admin/AiKnowledge.vue',  'Reading',     30, 1),
  (24, 13, '问答日志',   2, '/admin/ai/logs',   'admin/AiLogs.vue',       'Tickets',     31, 1),
  (25, 13, '系统日志',   2, '/admin/log',       'admin/SystemLog.vue',    'Memo',        32, 1);

-- ---------------- 角色菜单分配 ----------------
-- ROLE_USER(1)：前台 1-7；ROLE_INHERITOR(2)：前台 1-7 + 工作台 8-12；ROLE_ADMIN(3)：全部 1-25
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
  (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7),
  (2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7),
  (2, 8), (2, 9), (2, 10), (2, 11), (2, 12),
  (3, 1), (3, 2), (3, 3), (3, 4), (3, 5), (3, 6), (3, 7),
  (3, 8), (3, 9), (3, 10), (3, 11), (3, 12),
  (3, 13), (3, 14), (3, 15), (3, 16), (3, 17), (3, 18), (3, 19),
  (3, 20), (3, 21), (3, 22), (3, 23), (3, 24), (3, 25);

-- ---------------- 首页推荐位 ----------------
INSERT INTO sys_banner (title, image_url, target_type, target_id, sort_no, status) VALUES
  ('玉溪青花瓷烧制技艺', NULL, 1, 1, 1, 1),
  ('通海高台巡游展演',   NULL, 3, 2, 2, 1),
  ('昆曲：百戏之祖',     NULL, 1, 10, 3, 1);

-- ---------------- AI 知识库种子文档 ----------------
-- source_type：1 非遗项目 2 传承人 3 资讯 4 政策/术语
-- 内容于 2026-10-18 依据中国非物质文化遗产网 / 各级政府公开资料重写，
-- 并在正文末尾标注【来源】URL 与检索日期；删除原先基于错误名录信息的表述。
INSERT INTO ai_knowledge
  (id, title, source_type, source_id, category_id, region, content, chunk_count, index_status)
VALUES
  (1, '玉溪青花瓷烧制技艺', 1, 1, 8, '云南省玉溪市红塔区',
   '玉溪青花瓷烧制技艺是流传于云南省玉溪市红塔区的传统制瓷技艺，以含钴矿物为着色原料，在坯体上绘制青花纹样后施以透明釉，经高温一次烧成，釉色青中泛蓝、深浅有致，与江西景德镇、浙江江山并称中国青花瓷器的三大产地之一。传统工序包括选料、练泥、拉坯、修坯、绘制纹样、施釉与入窑烧成，成品以碗、盘、罐等日用器为主。需要说明：本项目为平台演示数据，玉溪青花瓷烧制技艺属于云南省省级非物质文化遗产代表性项目（云南省第三批非遗名录），并非国家级非物质文化遗产代表性项目，请勿与国家级名录混淆。【来源：玉溪市文化馆官网项目页 https://www.yxswhg.org.cn/h-pd-654.html；玉溪市人民政府提案答复 http://www.yuxi.gov.cn/yxszfxxgk/jyyatabljg7037/20241112/1567091.html，检索日期 2026-10-18】',
   2, 1),
  (2, '花灯戏（玉溪花灯戏）', 1, 2, 4, '云南省玉溪市',
   '花灯戏（玉溪花灯戏）是流行于云南省玉溪一带的地方戏曲剧种，由民间花灯歌舞发展而成，唱腔活泼明快，表演载歌载舞，具有鲜明的乡土气息。2006 年列入第一批国家级非物质文化遗产代表性项目名录，项目编号 Ⅳ-78，申报地区为云南省玉溪市，保护单位为玉溪市文化馆。【来源：中国非物质文化遗产网·花灯戏（玉溪花灯戏）https://www.ihchina.cn/project_details/13340/，检索日期 2026-10-18】',
   1, 1),
  (3, '滇剧', 1, 3, 4, '云南省玉溪市、昆明市',
   '滇剧是云南省主要的地方剧种，融合丝弦、胡琴、襄阳三大声腔，以唱做并重、程式严谨著称。2008 年列入第二批国家级非物质文化遗产代表性项目名录，项目编号 Ⅳ-132，类型为新增项目，申报地区或单位为云南省滇剧院、玉溪市滇剧团、昆明市，国家级保护单位为云南省滇剧院。【来源：中国非物质文化遗产网·滇剧（云南省滇剧院）https://www.ihchina.cn/project_details/13545/；国务院关于公布第二批国家级非物质文化遗产名录的通知（国发〔2008〕19号）https://www.gov.cn/zwgk/2008-06/14/content_1016331.htm，检索日期 2026-10-18】',
   1, 1),
  (4, '抬阁（通海高台）', 1, 4, 10, '云南省玉溪市通海县',
   '抬阁（通海高台）是流传于云南省玉溪市通海县的传统民俗巡游艺术，以铁杆为骨架，将装扮成戏剧人物的孩童悬于数米高空，造型奇巧、惊险壮观，多在春节与重大节庆期间巡游展演，制作过程涉及木工、铁艺、彩扎与戏曲装扮等多种技艺。2011 年列入第三批国家级非物质文化遗产代表性项目名录（扩展项目），项目编号 Ⅹ-87，申报地区为云南省通海县，保护单位为通海县文化馆（通海县非物质文化遗产保护中心）。【来源：中国非物质文化遗产网·抬阁（通海高台）https://www.ihchina.cn/project_details/15212/；通海县人民政府 http://www.tonghai.gov.cn/thxzfxxgk/gzjz/20200114/1122303.html，检索日期 2026-10-18】',
   1, 1),
  (5, '峨山彝族刺绣', 1, 5, 7, '云南省玉溪市峨山彝族自治县',
   '峨山彝族刺绣是流传于云南省玉溪市峨山彝族自治县的彝族传统刺绣技艺，纹样多取日、月、火、花鸟等元素，色彩浓烈、构图饱满，广泛用于彝族服饰、背被与头帕等生活用品。需要说明：本项目为平台演示数据，官方名录中对应项目为峨山彝族服饰（2009 年列入云南省非物质文化遗产名录），峨山彝族刺绣为其重要组成部分，不属于国家级非物质文化遗产代表性项目，请勿与国家级名录混淆。【来源：峨山彝族自治县人民政府·县文化和旅游局提案答复 http://www.yxes.gov.cn/esxzfxxgk/esxwhhlyjjyyatbljg9395/20190905/1078081.html，检索日期 2026-10-18】',
   1, 1);

-- ---------------- AI 知识切片 ----------------
INSERT INTO ai_knowledge_chunk (knowledge_id, chunk_index, content) VALUES
  (1, 0, '玉溪青花瓷烧制技艺是流传于云南省玉溪市红塔区的传统制瓷技艺，以含钴矿物为着色原料，在坯体上绘制青花纹样后施以透明釉，经高温一次烧成，釉色青中泛蓝、深浅有致，与江西景德镇、浙江江山并称中国青花瓷器的三大产地之一。传统工序包括选料、练泥、拉坯、修坯、绘制纹样、施釉与入窑烧成。'),
  (1, 1, '需要说明：本项目为平台演示数据，玉溪青花瓷烧制技艺属于云南省省级非物质文化遗产代表性项目（云南省第三批非遗名录），并非国家级非物质文化遗产代表性项目，请勿与国家级名录混淆。【来源：玉溪市文化馆官网项目页 https://www.yxswhg.org.cn/h-pd-654.html；玉溪市人民政府提案答复 http://www.yuxi.gov.cn/yxszfxxgk/jyyatabljg7037/20241112/1567091.html，检索日期 2026-10-18】'),
  (2, 0, '花灯戏（玉溪花灯戏）是流行于云南省玉溪一带的地方戏曲剧种，由民间花灯歌舞发展而成，唱腔活泼明快，表演载歌载舞，具有鲜明的乡土气息。2006 年列入第一批国家级非物质文化遗产代表性项目名录，项目编号 Ⅳ-78，申报地区为云南省玉溪市，保护单位为玉溪市文化馆。【来源：中国非物质文化遗产网 https://www.ihchina.cn/project_details/13340/，检索日期 2026-10-18】'),
  (3, 0, '滇剧是云南省主要的地方剧种，融合丝弦、胡琴、襄阳三大声腔，以唱做并重、程式严谨著称。2008 年列入第二批国家级非物质文化遗产代表性项目名录，项目编号 Ⅳ-132，类型为新增项目，申报地区或单位为云南省滇剧院、玉溪市滇剧团、昆明市，国家级保护单位为云南省滇剧院。【来源：中国非物质文化遗产网 https://www.ihchina.cn/project_details/13545/；国务院关于公布第二批国家级非物质文化遗产名录的通知（国发〔2008〕19号）https://www.gov.cn/zwgk/2008-06/14/content_1016331.htm，检索日期 2026-10-18】'),
  (4, 0, '抬阁（通海高台）是流传于云南省玉溪市通海县的传统民俗巡游艺术，以铁杆为骨架，将装扮成戏剧人物的孩童悬于数米高空，造型奇巧、惊险壮观，多在春节与重大节庆期间巡游展演，制作过程涉及木工、铁艺、彩扎与戏曲装扮等多种技艺。2011 年列入第三批国家级非物质文化遗产代表性项目名录（扩展项目），项目编号 Ⅹ-87，申报地区为云南省通海县，保护单位为通海县文化馆（通海县非物质文化遗产保护中心）。【来源：中国非物质文化遗产网 https://www.ihchina.cn/project_details/15212/；通海县人民政府 http://www.tonghai.gov.cn/thxzfxxgk/gzjz/20200114/1122303.html，检索日期 2026-10-18】'),
  (5, 0, '峨山彝族刺绣是流传于云南省玉溪市峨山彝族自治县的彝族传统刺绣技艺，纹样多取日、月、火、花鸟等元素，色彩浓烈、构图饱满，广泛用于彝族服饰、背被与头帕等生活用品。需要说明：本项目为平台演示数据，官方名录中对应项目为峨山彝族服饰（2009 年列入云南省非物质文化遗产名录），峨山彝族刺绣为其重要组成部分，不属于国家级非物质文化遗产代表性项目，请勿与国家级名录混淆。【来源：峨山彝族自治县人民政府·县文化和旅游局提案答复 http://www.yxes.gov.cn/esxzfxxgk/esxwhhlyjjyyatbljg9395/20190905/1078081.html，检索日期 2026-10-18】');
-- ============================================================================
-- 第三批：封面图（uploads 目录静态映射 /uploads/**，见 WebConfig）
-- 项目 1-13 封面、活动/资讯复用项目封面、banner 独立横幅
-- ============================================================================
UPDATE ich_project SET cover_url = '/uploads/p1.jpg'  WHERE id = 1;
UPDATE ich_project SET cover_url = '/uploads/p2.jpg'  WHERE id = 2;
UPDATE ich_project SET cover_url = '/uploads/p3.jpg'  WHERE id = 3;
UPDATE ich_project SET cover_url = '/uploads/p4.jpg'  WHERE id = 4;
UPDATE ich_project SET cover_url = '/uploads/p5.jpg'  WHERE id = 5;
UPDATE ich_project SET cover_url = '/uploads/p6.jpg'  WHERE id = 6;
UPDATE ich_project SET cover_url = '/uploads/p7.jpg'  WHERE id = 7;
UPDATE ich_project SET cover_url = '/uploads/p8.jpg'  WHERE id = 8;
UPDATE ich_project SET cover_url = '/uploads/p9.jpg'  WHERE id = 9;
UPDATE ich_project SET cover_url = '/uploads/p10.jpg' WHERE id = 10;
UPDATE ich_project SET cover_url = '/uploads/p11.jpg' WHERE id = 11;
UPDATE ich_project SET cover_url = '/uploads/p12.jpg' WHERE id = 12;
UPDATE ich_project SET cover_url = '/uploads/p13.jpg' WHERE id = 13;
UPDATE ich_activity SET cover_url = '/uploads/p1.jpg' WHERE id = 1;
UPDATE ich_activity SET cover_url = '/uploads/p4.jpg' WHERE id = 2;
UPDATE ich_activity SET cover_url = '/uploads/p5.jpg' WHERE id = 3;
UPDATE ich_activity SET cover_url = '/uploads/p1.jpg' WHERE id = 4;
UPDATE cms_article SET cover_url = '/uploads/p1.jpg' WHERE id = 1;
UPDATE cms_article SET cover_url = '/uploads/p4.jpg' WHERE id = 2;
UPDATE cms_article SET cover_url = '/uploads/p5.jpg' WHERE id = 3;
UPDATE sys_banner SET image_url = '/uploads/b1.jpg' WHERE id = 1;
UPDATE sys_banner SET image_url = '/uploads/b2.jpg' WHERE id = 2;
UPDATE sys_banner SET image_url = '/uploads/b3.jpg' WHERE id = 3;
