# 非遗文化传承与互动平台 · 后端服务

基于 Spring Boot 3 + MyBatis-Plus + Spring Security(JWT) 的前后端分离后端骨架，是《系统设计说明书》第 3—6 章的代码落地。

**当前状态**：骨架已跑通——编译通过、26 个集成测试全绿、可启动并用真实 HTTP 验证 12 项关键链路。

---

## 一、快速开始

### 方式 A：零依赖启动（推荐先跑通这个）

自带 H2 内存库与演示数据，不需要装 MySQL 和 Redis：

```bash
cd backend
mvn spring-boot:run
```

启动后：

| 用途 | 地址 |
|---|---|
| 探活接口 | http://localhost:8080/api/public/health |
| H2 控制台 | http://localhost:8080/h2-console |

H2 控制台连接信息：
```
JDBC URL : jdbc:h2:mem:ich;MODE=MySQL;DB_CLOSE_DELAY=-1
用户名   : sa
密码     : （留空）
```

**内置账号**（密码均为 `123456`）：

| 用户名 | 角色 | 说明 |
|---|---|---|
| `admin` | 平台管理员 | 可访问 `/api/admin/**` 与全部后台接口 |
| `inheritor` | 非遗传承人 | 可提交项目、发布内容 |
| `user` | 普通用户 | 仅浏览与互动 |
| `disabled` | — | 已禁用账号，用于验证禁用后无法登录 |

> 内存库重启即清空，仅用于开发与演示。

### 方式 B：接入真实 MySQL（数据全量 1557 条）

```bash
# 1. 建库建表 + 基础字典
mysql -u root -p < ../sql/00_schema.sql

# 2. 种子数据：10 类别 + 1557 项目 + 3057 传承人
mysql -u root -p dsh_ich < ../sql/10_ich_seed_data.sql

# 3. 项目-传承人关联（3054 条）
mysql -u root -p dsh_ich < ../sql/11_ich_project_inheritor.sql

# 4. 以 mysql profile 启动
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

数据库账号密码通过环境变量注入，不要写进代码：

```bash
set ICH_DB_USER=root
set ICH_DB_PASSWORD=你的密码
```

生产环境另需覆盖 JWT 密钥（默认值仅供开发）：

```bash
set ICH_JWT_SECRET=至少32字节的随机字符串
```

### 方式 C：打包部署

```bash
mvn clean package -DskipTests
java -jar target/ich-platform.jar --spring.profiles.active=mysql
```

---

## 二、技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 17 | Spring Boot 3 的基线版本 |
| Spring Boot | 3.2.5 | Web + Validation + Security + Data Redis |
| MyBatis-Plus | 3.5.7 | 通用 CRUD、分页、乐观锁、逻辑删除 |
| MySQL | 8.0 | 生产数据库 |
| H2 | 内置 | 开发/演示内存库，免安装 |
| JJWT | 0.12.5 | JWT 生成与解析 |
| Spring Security | 6.1 | 认证授权、BCrypt 密码编码 |

---

## 三、目录结构

```
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/yuxi/ich/
    │   │   ├── IchPlatformApplication.java    启动类
    │   │   ├── common/                        通用层（各模块共用）
    │   │   │   ├── response/                  Result 统一响应、ResultCode 状态码、
    │   │   │   │                              PageResult 分页结构、PageUtil
    │   │   │   ├── exception/                 BizException、GlobalExceptionHandler
    │   │   │   ├── query/                     BaseQuery 分页基类
    │   │   │   ├── entity/                    BaseEntity 实体基类
    │   │   │   ├── constant/                  RoleConstants、StatusConstants
    │   │   │   └── controller/                HealthController 探活
    │   │   ├── config/                        SecurityConfig、MybatisPlusConfig、
    │   │   │                                  JwtProperties
    │   │   ├── security/                      JwtTokenProvider、JwtAuthenticationFilter、
    │   │   │                                  LoginUser、SecurityUtils、TokenBlacklist、
    │   │   │                                  SecurityHandlers
    │   │   ├── auth/                          认证模块（注册/登录/刷新/退出）
    │   │   ├── user/                          用户与角色（实体 + Mapper）
    │   │   └── project/                       ★ 示例模块：非遗项目
    │   │       ├── entity/                    IchProject、IchCategory
    │   │       ├── mapper/                    IchProjectMapper、IchCategoryMapper
    │   │       ├── dto/                       ProjectQueryParam、ProjectSaveRequest
    │   │       ├── service/                   ProjectService、CategoryService
    │   │       └── controller/                ProjectController、CategoryController
    │   └── resources/
    │       ├── application.yml                多 profile 配置（h2 / mysql）
    │       ├── mapper/IchProjectMapper.xml    动态 SQL（含排序白名单）
    │       └── db/                            schema-h2.sql、data-h2-demo.sql
    └── test/
        ├── java/com/yuxi/ich/
        │   └── IchPlatformApplicationTests.java   26 个集成测试
        └── resources/mockito-extensions/          Mockito mock maker 配置
```

---

## 四、分层约定

请求自上而下单向流动，**每层只做一件事**：

```
Controller   接收参数、声明权限、包装响应        ← 不写业务规则
    ↓
Service      业务规则、事务、异常抛出            ← 校验都在这里
    ↓
Mapper       数据访问（BaseMapper + 自定义 SQL）
    ↓
MySQL / H2
```

这样分层的好处：Service 不依赖 Web 容器，可以脱离 HTTP 直接单元测试。

---

## 五、统一约定

### 响应体

所有接口（除文件流）统一返回：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { },
  "timestamp": 1790000000000
}
```

列表接口的 `data` 固定为分页结构：

```json
{ "total": 1557, "records": [], "pageNum": 1, "pageSize": 10, "pages": 156 }
```

### 状态码

| code | 含义 | 触发场景 |
|---|---|---|
| 200 | 成功 | — |
| 400 | 参数错误 | 校验注解不通过、参数格式错误 |
| 401 | 未认证 | 未带令牌、令牌过期、退出后的令牌 |
| 403 | 无权限 | 角色不足、操作他人数据 |
| 404 | 不存在 | 对象已删除或未发布 |
| 409 | 状态冲突 | 用户名/编号重复、名额已满 |
| 429 | 限流 | 触发接口限流（骨架未启用） |
| 500 | 服务端异常 | 未预期异常，不向客户端暴露堆栈 |

### HTTP 状态码说明

本项目**统一返回 HTTP 200**，业务结果放在 `code` 字段里。这样前端只需一套拦截器逻辑，不用同时判断 HTTP 状态和业务状态。若你更倾向用 HTTP 状态码表达语义，可修改 `SecurityHandlers` 与 `GlobalExceptionHandler` 中的 `setStatus`。

### 分页参数

所有列表接口共用，继承 `BaseQuery` 即自动获得：

```
?pageNum=1&pageSize=10&sortField=view_count&sortOrder=desc
```

`pageSize` 上限 100，超出自动截断，防止一次拉取过多数据。

---

## 六、如何照着写一个新模块

以「文化资讯」模块为例，`project` 模块就是模板，五步即可：

**第 1 步：实体** —— 继承 `BaseEntity`，自动获得 id、create_time、update_time、逻辑删除

```java
@TableName("cms_article")
public class CmsArticle extends BaseEntity {
    private String title;
    private String content;
    private Integer auditStatus;
    // getter / setter
}
```

**第 2 步：Mapper** —— 继承 `BaseMapper`，单表增删改查直接可用

```java
@Mapper
public interface CmsArticleMapper extends BaseMapper<CmsArticle> {
    // 只有联表查询才需要自定义方法，写在 XML 里
}
```

**第 3 步：查询 DTO** —— 继承 `BaseQuery` 获得分页与排序参数

```java
public class ArticleQueryParam extends BaseQuery {
    private Long categoryId;
    private String keyword;
    // getter / setter
}
```

**第 4 步：Service** —— 业务规则与异常都在这里

```java
@Service
public class ArticleService {
    private final CmsArticleMapper mapper;

    public PageResult<CmsArticle> page(ArticleQueryParam q) {
        long offset = (q.getPageNum() - 1) * q.getPageSize();
        List<CmsArticle> list = mapper.selectArticlePage(q, offset, q.getPageSize());
        long total = mapper.countArticle(q);
        return new PageResult<>(total, list, q.getPageNum(), q.getPageSize());
    }

    public void delete(Long id) {
        if (mapper.selectById(id) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "资讯不存在");
        }
        mapper.deleteById(id);   // 逻辑删除
    }
}
```

**第 5 步：Controller** —— 声明权限、包装响应

```java
@RestController
@RequestMapping("/api/article")
public class ArticleController {
    private final ArticleService service;

    @GetMapping("/list")
    public Result<PageResult<CmsArticle>> list(ArticleQueryParam q) {
        return Result.success(service.page(q));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('INHERITOR', 'ADMIN')")   // 权限声明
    public Result<Long> create(@Valid @RequestBody ArticleSaveRequest req) {
        return Result.success(service.create(req));
    }
}
```

**几个已经踩过的坑，直接照做可避免**：

1. **分页插件必须注册**，否则 `pageNum`/`pageSize` 不生效，会一次查全表 —— 见 `MybatisPlusConfig`
2. **ORDER BY 不能用占位符**，前端传的 `sortField` 必须走白名单，否则是 SQL 注入 —— 见 `IchProjectMapper.xml` 的 `<sql id="orderBy">`
3. **OGNL 里下划线是属性分隔符**，`sortField == 'view_count'` 要加单引号，也不能用 `q.asc` 这种推导
4. **逻辑删除要配全局配置**，`logic-delete-field: deleted` 加上才能自动追加 `deleted = 0`
5. **越权防护在 Service 层做**，用 `SecurityUtils.checkOwnership(ownerId)`，别只靠前端隐藏按钮

---

## 七、安全设计要点

| 项目 | 实现 |
|---|---|
| 密码存储 | BCrypt 加盐哈希，不可逆 |
| 登录失败提示 | 统一返回「用户名或密码错误」，不区分账号是否存在，防账号枚举 |
| 账号禁用 | 登录时与刷新令牌时都校验 `status`，禁用后立即失效 |
| 令牌方案 | 双令牌：access 2 小时 / refresh 7 天 |
| 退出登录 | 令牌写入 `TokenBlacklist`，自然过期前即失效 |
| 令牌校验 | access 与 refresh 类型区分，用 access 冒充 refresh 会被拒绝 |
| 接口鉴权 | `@PreAuthorize` 注解级 + `SecurityConfig` 路径级双重控制 |
| 数据归属 | 按 id 操作前校验归属，防止篡改 id 越权 |
| 排序注入 | 白名单固定可排序字段，非法值回落到默认排序 |
| CSRF | JWT 放请求头而非 Cookie，天然规避 |
| 跨域 | 仅放行 localhost 与 127.0.0.1 的任意端口 |

### 一处需要你替换为 Redis

`TokenBlacklist` 目前是**进程内 ConcurrentHashMap 实现**，好处是不依赖 Redis 即可运行与测试。但多实例部署时黑名单不共享，退出登录可能不生效。

替换方式：把该类改为 Redis 实现，业务代码无需改动。

```java
// 写入：SETEX blacklist:{token} {剩余秒数} 1
// 查询：EXISTS blacklist:{token}
```

---

## 八、测试

```bash
cd backend
mvn test
```

26 个集成测试，覆盖：

| 分组 | 用例数 | 覆盖内容 |
|---|---|---|
| 启动与公开接口 | 2 | 探活、类别列表 |
| 列表分页与筛选 | 5 | 分页、多条件、地域模糊、关键词、排序白名单、热门 |
| 详情与可见性 | 3 | 详情字段、待审核不可见、不存在返回 404 |
| 认证 | 6 | 登录、错误密码、禁用账号、注册、参数校验、令牌校验 |
| 鉴权与越权 | 5 | 401、403、管理员全量可见、传承人提交、编号冲突 |
| 退出与刷新 | 3 | 退出失效、刷新、类型混用拒绝 |
| 创建校验 | 2 | 参数缺失 400、编号重复 409 |

测试使用 H2 内存库真实启动 Spring 容器（含 Security 过滤器链），通过 MockMvc 打真实 HTTP 请求，不是 mock 掉依赖的假测试。

### 如果测试报 Mockito 自我附加失败

在部分受限环境（沙箱、受限容器）中，Mockito 默认的 inline mock maker 无法「自我附加」到 JVM，报：

```
Could not self-attach to current VM using external process
```

本项目已通过 `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` 切换到 subclass 实现规避。若仍遇到，可改用：

```bash
mvn test -DargLine="-javaagent:$HOME/.m2/repository/net/bytebuddy/byte-buddy-agent/1.14.13/byte-buddy-agent-1.14.13.jar"
```

---

## 九、已完成与待实现

### 已完成

- 统一响应体、全局异常处理、分页封装
- JWT 双令牌认证链路（登录 / 刷新 / 退出 / 当前用户）
- RBAC 三级角色鉴权，路径级 + 注解级双重控制
- MyBatis-Plus 配置：分页、乐观锁、防全表更新、字段自动填充、逻辑删除
- 非遗项目与类别模块完整实现（分页、多条件筛选、详情、热度、审核）
- 26 个集成测试，真实 HTTP 链路验证

### 待实现（照第六节模板套即可）

| 模块 | 参考文档 | 难度 |
|---|---|---|
| 非遗传承人 | 说明书 4.3、表 6-3 | 与项目模块同构 |
| 文化资讯 | 说明书 4.4、表 6-4 | 与项目模块同构 |
| 非遗活动与报名 | 说明书 4.5、表 6-4 | **含并发控制，较复杂** |
| 用户互动（收藏/点赞/评论） | 说明书 4.6 | 多态关联 + 幂等 |
| AI 智能问答 | 说明书 4.7、表 6-5 | 需接入 Spring AI |
| 数据统计 | 说明书 4.8 | 聚合查询 + ECharts |
| 文件上传 | 说明书 6.3 | 接对象存储 |
| 接口限流 | 说明书 6.7 | Redis 令牌桶 |

**建议实现顺序**：传承人 → 资讯 → 互动 → 活动报名 → AI 问答 → 统计。

活动报名是唯一有难度的模块，`ich_activity` 表已预留 `version` 字段，配合 `OptimisticLockerInnerInterceptor`（已注册）即可实现乐观锁防超卖：实体上加 `@Version` 注解即可生效。

---

## 十、常见问题

**Q：启动报 `Unable to create tempDir`？**
Tomcat 需要可写的临时目录。指定一个可写路径：
```bash
java -jar target/ich-platform.jar -Djava.io.tmpdir=你的可写目录
```

**Q：报 `角色未初始化：ROLE_ADMIN`？**
基础字典未导入。执行 `sql/00_schema.sql`，它会写入三个角色与默认账号。

**Q：连不上 MySQL？**
检查 `application.yml` 的 mysql profile 中连接串，或用环境变量 `ICH_DB_USER` / `ICH_DB_PASSWORD` 注入。默认值是 `root/root`。

**Q：前端跨域被拦？**
`SecurityConfig.corsConfigurationSource()` 中放行了 `localhost` 与 `127.0.0.1` 的任意端口。若前端跑在其他主机，需在此处补充来源。

**Q：控制台 SQL 日志太多？**
`application.yml` 中把 `log-impl` 改为 `org.apache.ibatis.logging.nologging.NoLoggingImpl`（h2 profile 已默认关闭）。
