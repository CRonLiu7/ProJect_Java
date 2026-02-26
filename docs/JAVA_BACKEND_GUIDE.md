# Java 后端详解与核心要点（面向前端转全栈）

> 本文档面向：**前端架构背景、第一个 Java 项目、基础薄弱**。重点讲清「是什么」「为什么」「核心点」，方便你边做边学。

---

## 一、先建立一张「心智图」：请求从进入到离开

作为前端，你已经熟悉：**浏览器发请求 → 某台服务器处理 → 返回 JSON**。在 Java 里，这条链路大致是这样走的（先不用记细节，有个印象即可）：

```
HTTP 请求 (例如 GET /api/users)
    ↓
① 先经过 Spring Security 的过滤器（Filter）
   → 若是登录/公开接口则放行，否则校验 JWT
    ↓
② 进入 Spring MVC：根据 URL 匹配到某个 @RestController 的某个方法
    ↓
③ Controller 只做「接收参数、调用 Service、返回结果」
    ↓
④ Service 里写业务逻辑（例如查用户、校验权限），需要数据时调用 Mapper
    ↓
⑤ Mapper（数据访问层）通过 MyBatis 执行手写 SQL 访问数据库
    ↓
⑥ 数据库返回数据 → Mapper → Service → Controller → 序列化成 JSON → HTTP 响应
```

**核心点 1**：  
- **Controller** ≈ 你前端的「路由处理函数」：只负责接请求、调逻辑、返回数据。  
- **Service** ≈ 真正的业务逻辑所在，不要在这里写 SQL，只调用 Mapper。  
- **Mapper** ≈ 和数据库打交道的唯一入口，接口 + XML 里手写 SQL，负责「怎么查、怎么存」。

**为什么要分层？**  
和前端「组件 / 业务逻辑 / 请求封装」分层类似：以后改 SQL 或接口格式，只需动 Mapper 或 Controller，而不是满屏改。

---

## 二、Maven 是什么（和 npm 的对应关系）

| 前端 (npm)        | Java (Maven)     



   |
|-------------------|---------------------|
| `package.json`    | `pom.xml`           |
| `npm install`     | `mvn dependency:resolve` / 或直接运行会先拉依赖 |
| `node_modules`    | 本地仓库（如 `~/.m2/repository`） |
| 脚本 `"start"` 等 | `mvn spring-boot:run` 或打包后 `java -jar xxx.jar` |

**核心点 2**：  
- 依赖都写在 `pom.xml` 的 `<dependencies>` 里，Maven 会自动下载。  
- **Spring Boot** 就是一个「大依赖」，它再帮你引入 Web 服务器、JSON 序列化、数据库连接等，所以加一个 `spring-boot-starter-web` 就能跑 HTTP 服务。

---

## 三、Spring Boot 的两个核心概念

### 3.1 依赖注入（DI）与 Bean

前端里你可能是「需要谁就 import 谁，自己 new」。在 Spring 里，**对象的创建和组装由容器负责**，你只声明「我需要一个 UserService」，容器把已经创建好的实例「注入」给你。

```java
// 你「不」写：UserService userService = new UserService();
// 而是：
@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;  // Spring 自动把 Repository 传进来
    }
}
```

- 被 `@Service`、`@RestController`、`@Repository` 等标注的类，会变成 Spring 管理的 **Bean**。  
- 其他 Bean 需要它时，用**构造器参数**或 `@Autowired` 字段声明即可，**不要自己 new**。

**核心点 3**：  
- **Bean** = 由 Spring 创建、放在容器里的对象。  
- **依赖注入** = 你要用的依赖由 Spring 按类型自动「塞」进来，方便单测和替换实现。

### 3.2 配置文件：application.yml / application.properties

和前端的 `.env` 类似：**环境相关、不宜写进代码**的内容放在配置文件里。

- 数据库 URL、用户名、密码  
- 服务端口、日志级别  
- JWT 密钥、过期时间  

Spring Boot 启动时会读 `src/main/resources/application.yml`（或 `.properties`），代码里用 `@Value("${key}")` 或 `@ConfigurationProperties` 注入使用。

**核心点 4**：  
- 敏感信息和环境差异一律放配置文件，不要硬编码。  
- 本地 / 测试 / 生产可用不同文件：`application-dev.yml`、`application-prod.yml`，通过 `spring.profiles.active=dev` 切换。

---

## 四、请求怎么进到 Controller（必会）

### 4.1 用注解把 URL 和方法绑在一起

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserDTO getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping
    public Long create(@RequestBody CreateUserRequest request) {
        return userService.create(request);
    }
}
```

- `@RestController` = 这个类负责处理 HTTP，返回值会序列化成 JSON（类似前端的 res.json()）。  
- `@RequestMapping("/api/users")` = 类级别路径前缀。  
- `@GetMapping("/{id}")` = GET `/api/users/123` 会进 `getById`，`123` 会赋给 `id`。  
- `@PathVariable` = 路径里的变量；`@RequestBody` = 请求体 JSON 反序列化成对象。

**核心点 5**：  
- **一个 URL + 一个方法**：记住「类上的路径 + 方法上的路径 = 完整路径」，和前端路由表一一对应。  
- 参数绑定：路径用 `@PathVariable`，查询用 `@RequestParam`，Body 用 `@RequestBody`。

### 4.2 统一响应格式（和前端约定好）

建议所有接口返回同一结构，例如：

```json
{ "code": 0, "message": "ok", "data": { ... } }
```

这样前端可以统一判断 `code`、取 `data`。实现方式：用 **全局异常处理** 把异常也变成这个格式（下一节），正常情况在 Controller 里直接返回 `data`，再用一个「响应包装类」统一包一层（可选，看你们约定）。

---

## 五、全局异常处理（重要）

Controller 和 Service 里可以直接抛异常，**不要到处 try-catch 再手写 JSON**。用 `@RestControllerAdvice` 统一兜底：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<?> handle(Exception e) {
        // 记录日志
        log.error("error", e);
        // 统一返回 { code, message, data }
        return Result.fail(500, e.getMessage());
    }
}
```

**核心点 6**：  
- 业务里可以写 `throw new BizException("用户名已存在")`，在 Advice 里单独处理 `BizException`，返回 200 但 `code=400` 和友好 `message`。  
- 这样 Controller 代码干净，错误格式统一，前端好处理。

---

## 六、数据库与 MyBatis：表和 Java 类怎么对应

> **本项目使用 MyBatis**：表由 Flyway 建，实体类与表字段对应，SQL 在 Mapper 的 XML 里手写。

### 6.1 表 ↔ 实体（Entity）

**一张表对应一个实体类**，字段与列一一对应。MyBatis 下实体**不加 JPA 注解**，只保留属性和 getter/setter（或 Lombok `@Data`），列名与属性名一致时自动映射，不一致时在 XML 里用 `resultMap` 或 `resultType` 指定。

```java
// 与表 sys_user 对应，列名与属性名一致（下划线转驼峰可在 MyBatis 中全局配置）
public class User {

    private Long id;
    private String username;
    private String passwordHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // getter / setter 或 Lombok @Data
}
```

**核心点 7**：  
- **先有表再写实体**：用 Flyway 建表，再按表结构写实体，实体只放表字段，展示用字段放 DTO。  
- MyBatis 可通过 `map-underscore-to-camel-case: true` 把列名 `created_at` 自动映射到 `createdAt`。

### 6.2 Mapper 接口 + XML：手写 SQL

数据访问层用 **Mapper 接口 + XML**：接口定义方法，XML 里写 SQL，MyBatis 在运行时代理实现。

```java
// org.example.mapper.UserMapper
@Mapper
public interface UserMapper {

    User findById(Long id);

    User findByUsername(@Param("username") String username);

    int insert(User user);

    int updateById(User user);
}
```

```xml
<!-- src/main/resources/mapper/UserMapper.xml -->
<mapper namespace="org.example.mapper.UserMapper">
    <resultMap id="BaseResultMap" type="org.example.entity.User">
        <id column="id" property="id"/>
        <result column="username" property="username"/>
        <result column="password_hash" property="passwordHash"/>
        <result column="created_at" property="createdAt"/>
        <result column="updated_at" property="updatedAt"/>
    </resultMap>

    <select id="findById" resultMap="BaseResultMap">
        SELECT id, username, password_hash, created_at, updated_at
        FROM sys_user WHERE id = #{id}
    </select>

    <select id="findByUsername" resultMap="BaseResultMap">
        SELECT id, username, password_hash, created_at, updated_at
        FROM sys_user WHERE username = #{username}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO sys_user (username, password_hash, created_at, updated_at)
        VALUES (#{username}, #{passwordHash}, #{createdAt}, #{updatedAt})
    </insert>
</mapper>
```

**核心点 8**：  
- **Mapper 是 interface，实现由 MyBatis 生成**；XML 的 `namespace` 必须等于 Mapper 接口全限定名。  
- 参数用 `@Param` 或单个对象，SQL 里用 `#{}` 占位，防注入；结果用 `resultMap` 或 `resultType` 映射到实体。

### 6.3 事务（Transaction）

「一组操作要么全成功，要么全失败」——例如：创建用户 + 分配角色，两步必须一起提交或一起回滚。

在 Service 方法上加 `@Transactional` 即可，MyBatis 与 Spring 整合后同一事务内多句 Mapper 调用会一起提交或回滚：

```java
@Transactional(rollbackFor = Exception.class)
public void createUserWithRole(CreateUserRequest req) {
    User user = new User();
    // ... 设值
    userMapper.insert(user);
    userRoleMapper.insert(new UserRole(user.getId(), roleId));
}
```

**核心点 9**：  
- `@Transactional` 加在 **Service 层**，不要加在 Controller。  
- `rollbackFor = Exception.class` 表示遇到任何异常都回滚（默认只对 RuntimeException 回滚）。

---

## 七、安全：认证与权限（JWT + Spring Security）

### 7.1 整体流程（记住这张图）

1. **登录**：用户提交用户名密码 → 后端校验 → 校验通过则**生成 JWT** 返回给前端。  
2. **之后请求**：前端在请求头里带 `Authorization: Bearer <token>` → 后端每个请求先经过 **Filter**，解析 JWT，取出用户身份，再放行到 Controller。  
3. **权限**：某些接口需要「管理员」或「有某权限」才可访问，在 Controller 方法上加 `@PreAuthorize("hasRole('ADMIN')")` 等，Spring Security 会在进方法前做校验。

### 7.2 几个关键点

- **密码**：入库必须**加密**，用 BCrypt（Spring 自带），不要明文存密码。  
- **JWT**：无状态，Token 里可放 userId、username、角色等；过期时间设短一点，配合刷新 Token 更安全。  
- **Filter 顺序**：登录接口放行、静态资源放行，其余请求都要过 JWT 过滤器；过滤器里校验失败直接返回 401，不进入 Controller。

**核心点 10**：  
- **认证（Authentication）** = 证明「你是谁」（登录、验 JWT）。  
- **授权（Authorization）** = 证明「你能做什么」（角色、权限注解）。  
- 先认证再授权：没登录一定 401；登录了但没权限则 403。

---

## 八、文件上传与下载（思路）

- **上传**：接口用 `MultipartFile` 接收，例如 `@PostMapping("/upload") public String upload(@RequestParam("file") MultipartFile file)`。  
  - 把文件存到本地目录或 MinIO，把「存储路径、原始文件名、大小、上传人」等写入 `sys_file` 表，返回文件 ID 或访问 URL 给前端。  
- **下载**：根据 ID 或 path 查 `sys_file`，读文件流，设置响应头 `Content-Disposition: attachment; filename="xxx"`，写出到 response。

**核心点 11**：  
- 上传要限制大小（Spring 有 `spring.servlet.multipart.max-file-size`）。  
- 文件名要做安全处理，避免路径穿越；存到磁盘或 MinIO 的路径不要用用户原始文件名，用 UUID 等生成唯一名。

---

## 八.五、Redis 详解（学习重点）

本项目把 **Redis** 作为学习重点，会在登录、登出、权限、限流、业务缓存等多处用到。下面按「是什么、在 Spring 里怎么用、典型场景」说明。

### Redis 是什么（和前端类比）

- **内存键值库**：数据放在内存里，读写很快；支持多种数据结构（String、Hash、List、Set、ZSet 等）。  
- 可设 **过期时间（TTL）**，到点自动删，适合「临时数据」：限流计数、会话、黑名单、缓存。  
- 和「前端 localStorage/sessionStorage」的差别：Redis 在**服务端**、多实例共享、可跨请求；单机或集群部署。

### 在 Spring Boot 里怎么连 Redis

1. **依赖**：`spring-boot-starter-data-redis`（默认用 Lettuce 客户端）。  
2. **配置**：`application.yml` 里写 `spring.data.redis.host=redis`、`port=6379`（Docker 里用服务名）；本机直连写 `localhost`。  
3. **使用方式**：注入 `StringRedisTemplate` 或 `RedisTemplate<String, Object>`，调用 `opsForValue().set/get`、`expire` 等。

```java
@Component
public class RedisService {
    private final StringRedisTemplate redis;

    public RedisService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void setWithTtl(String key, String value, long seconds) {
        redis.opsForValue().set(key, value);
        redis.expire(key, Duration.ofSeconds(seconds));
    }

    public Long increment(String key) {
        return redis.opsForValue().increment(key);
    }
}
```

### 本项目中的五大使用场景（必会）

| 场景 | Key 设计示例 | 操作要点 | 学习点 |
|------|--------------|----------|--------|
| **登录失败限流** | `login:fail:<username>` 或 `login:fail:<ip>` | 每次失败 `INCR`；首次设 `EXPIRE 15 分钟`；超过 5 次则拒绝登录 | String + 过期、防暴力破解 |
| **JWT 黑名单（登出）** | `blacklist:<jti>`（jti 为 token 唯一 id） | 登出时 `SET key 1`，并 `EXPIRE` 为 token 剩余有效秒数；校验 JWT 前先查此 key 是否存在 | 无状态 token 的「失效」 |
| **用户权限/菜单缓存** | `user:perms:<userId>` | 登录成功后查库得到菜单/权限列表，序列化成 JSON 写入 Redis，TTL 10～30 分钟；接口鉴权或返回菜单时先读缓存，未命中再查库并回填缓存 | 降低 DB 压力、缓存更新策略 |
| **接口限流** | `rate:api:<userId>` 或 `rate:ip:<ip>` | 每次请求 `INCR`，首次设 `EXPIRE 1 分钟`；超过 N 次返回 429 | 固定窗口限流、可进阶滑动窗口 |
| **业务缓存（如商品详情）** | `product:<id>` | 查商品时先读 Redis，未命中再查 DB 并写入 Redis，TTL 5～10 分钟；更新/删除商品时删 key | 缓存穿透可加「空值缓存」、击穿可加分布式锁（进阶） |

**核心点（Redis）**：  
- Key 命名要有前缀和层次，如 `user:perms:1`、`login:fail:admin`，避免冲突、便于排查。  
- 凡「用过即废」或「短期有效」的数据，都设 TTL，避免 key 无限增长。  
- 本项目先实现「登录限流 + 黑名单 + 权限缓存 + 接口限流」即可覆盖大部分学习目标；业务缓存在订单/商品模块再加。

---

## 九、Docker 在本项目里的作用（简要）

- **PostgreSQL**：用 `docker-compose` 起一个 Postgres 容器，端口 5432 映射到本机；数据用 volume 持久化。  
- **Redis**：同一 Compose 里起 Redis，端口 6379；应用通过服务名 `redis` 连接。  
- **后端**：本地开发时可直接 `mvn spring-boot:run`，连本机 Docker 里的 PG 和 Redis；生产再打成镜像用 Docker 跑。  
- **连接地址**：在 Docker 网络里，应用连数据库/Redis 要用**服务名**（如 `postgres`、`redis`），不要写 `localhost`。

**核心点 12**：  
- 分阶段加服务：先 PG + 后端跑通，再加 Redis 并用起来，最后加 MinIO。  
- `application.yml` 里：PG 写 `jdbc:postgresql://postgres:5432/your_db`（Docker 内）或 `localhost:5432`（本机）；Redis 写 `host: redis` 或 `localhost`。

---

## 十、建议的学习与实现顺序（对应你第一个项目）

1. **跑通 Spring Boot**：建一个 `@GetMapping("/hello")` 返回字符串，用 Maven 跑起来，浏览器能访问。  
2. **连上数据库**：Docker 起 **PostgreSQL**，配置 `application.yml`，用 Flyway 建一张简单表，用 JPA Entity + Repository 查一条数据并返回 JSON。  
3. **统一响应 + 全局异常**：定好 `Result` 结构，写一个 `GlobalExceptionHandler`。  
4. **用户表 + 简单 CRUD**：`sys_user` 表，增删改查接口，体会 Controller → Service → Repository 分层。  
5. **安全**：加 Spring Security，登录接口（BCrypt 校验密码、发 JWT），其余接口校验 JWT。  
6. **角色与权限**：角色表、用户-角色、菜单/权限表，菜单树接口，`@PreAuthorize` 控制接口权限。  
7. **引入 Redis**：Docker 加 Redis；实现登录限流、JWT 黑名单（登出）、用户权限/菜单缓存、接口限流（见上节）。  
8. **上传下载**：一个上传接口、一个下载接口，`sys_file` 表记录；后期可加 MinIO。  
9. **审批流 + 订单商品**：按架构计划中的表与接口实现，业务缓存用 Redis。

每步都只加「当前这一步」的东西，遇到不懂的再回看本文对应小节；核心点 1～12 与 Redis 要点可单独记在笔记里，写代码时对照着用。

---

## 十一、和架构文档的关系

- **ARCHITECTURE_PLAN.md**：整体技术选型、表设计、Docker 规划、分阶段计划。  
- **本文档（JAVA_BACKEND_GUIDE.md）**：偏「为什么这样设计、Java/Spring 核心概念、怎么写」的详解与重点。

实现时以架构计划为「清单」，以本文为「说明」，遇到概念性问题先查本文，再动手写代码。如果你希望某一块再展开（例如 Flyway 怎么写第一版 SQL、JWT Filter 怎么写），可以指定章节，我可以按「步骤 + 代码片段」再写一版。
