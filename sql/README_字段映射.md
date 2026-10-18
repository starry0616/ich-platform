# OpenKG 非遗数据 -> 平台数据库 字段映射说明

数据来源：[OpenKG 中国非物质文化遗产知识图谱](http://data.openkg.cn/dataset/china-intangible-cultural-heritage-kg)

授权：Other (Open)，满足开放知识定义。原始数据版权归属 CnOpenData 及其合作发布机构，经清洗、对齐与建模后发布。

## 一、非遗项目 `ich_project`

| OpenKG 字段 | 目标字段 | 转换规则 |
|---|---|---|
| name | project_name | 直接写入 |
| id | project_code | 直接写入（如 Ⅰ-1、Ⅷ-12，罗马数字为类别号） |
| category | category_id | 按类别名匹配 `ich_category` 取主键 |
| region | region | 直接写入（申报地区，粒度到县/市） |
| batch | batch_no + announce_year | `2021(第五批)` 拆为批号 5、年份 2021 |
| type | type | 直接写入（新增项目 / 扩展项目） |
| protection_unit | protection_unit | 新增字段，直接写入 |
| — | level | 固定为 1（国家级） |
| — | summary / content / history / status_desc / cover_url | 留空，待补充 |

## 二、非遗传承人 `ich_inheritor`

| OpenKG 字段 | 目标字段 | 转换规则 |
|---|---|---|
| name | name | 直接写入 |
| id | cert_no | 作为认定编号写入（如 01-0005）；**经校验全局唯一** |
| gender | gender | 男→1，女→2，空→NULL |
| work_unit | region | 作为所属地区写入 |
| ethnicity | intro | 以「民族：X。」写入简介前缀 |
| — | level | 固定为 1（国家级） |
| — | cert_status | 固定为 1（已认证） |
| — | user_id | NULL（历史传承人无平台账号） |

## 三、图谱关系 -> 数据库

| OpenKG 关系 | 目标 | 说明 |
|---|---|---|
| 传承（项目—传承人） | `ich_project_inheritor` | 建立多对多关联，共 3054 条 |
| 属于（项目—类别） | `category_id` 外键 | 无需额外表 |
| 公布于（项目—批次） | `batch_no` / `announce_year` | 无需额外表 |
| 位于（项目—地区） | `region` 字段 | 无需额外表 |

## 四、数据质量校验结果

| 校验项 | 结果 |
|---|---|
| 项目 project_code 唯一性 | 1557 个，全部唯一 |
| 传承人 cert_no 唯一性 | 3057 个，全部唯一 |
| 项目重名 | 0 个 |
| 传承人重名 | 21 人重名（但编号唯一，不影响关联） |
| 「传承」关系命中率 | 3054 / 3054 = 100% |

## 五、生成数据统计

| 内容 | 数量 |
|---|---|
| 非遗类别 | 10 |
| 非遗项目 | 1557 |
| 非遗传承人 | 3057 |
| 项目-传承人关联 | 3054 |

### 类别分布

| 类别 | 项目数 |
|---|---|
| 传统技艺 | 287 |
| 传统音乐 | 189 |
| 民俗 | 183 |
| 传统戏剧 | 171 |
| 民间文学 | 167 |
| 曲艺 | 145 |
| 传统舞蹈 | 144 |
| 传统美术 | 139 |
| 传统体育、游艺与杂技 | 109 |
| 传统医药 | 23 |

### 批次分布

| 批次 | 项目数 |
|---|---|
| 2008(第二批) | 437 |
| 2021(第五批) | 325 |
| 2006(第一批) | 315 |
| 2011(第三批) | 247 |
| 2014(第四批) | 233 |

### 项目类型

| 类型 | 项目数 |
|---|---|
| 新增项目 | 1193 |
| 扩展项目 | 364 |

## 六、数据缺口（需后续补充）

- 无项目简介、历史渊源、技艺特征、传承现状等富文本内容
- 无图片与音视频资源
- 无省级、市级、县级项目（全部为国家级）
- 传承人无出生年份、从艺年限、代表性作品

## 七、执行顺序

```bash
# 1. 建库建表 + 基础字典
mysql -u root -p < 00_schema.sql

# 2. 种子数据：类别 + 项目 + 传承人
mysql -u root -p dsh_ich < 10_ich_seed_data.sql

# 3. 项目-传承人关联
mysql -u root -p dsh_ich < 11_ich_project_inheritor.sql
```

执行完成后，`10_ich_seed_data.sql` 末尾会自动输出三张表的记录数用于校验。