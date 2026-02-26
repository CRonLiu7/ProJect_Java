# 全栈后台管理系统 - 架构与实施计划

> 目标：完成一个业务相对复杂的中型全栈项目，重点覆盖 **数据库设计**、**表结构**、**Docker**、**Java 后端**、**Redis**。  
> 前端你已有架构经验，**先做后端、前端往后放**；**Java 后端的详细讲解与核心要点**请看 → **[JAVA_BACKEND_GUIDE.md](./JAVA_BACKEND_GUIDE.md)**。  
> **JPA 与 MyBatis 选型说明** → **[JPA_VS_MYBATIS.md](./JPA_VS_MYBATIS.md)**（本项目已选 **MyBatis**）。

---

## 已确定的选型（汇总）

| 项 | 选择 | 说明 |
|----|------|------|
| 数据库 | **PostgreSQL** | Docker 运行，Flyway 管理建表/改表 |
| 缓存 | **Redis** | **学习重点**，会贯穿登录、权限缓存、限流、业务缓存等 |
| 建表方式 | **Flyway** | 所有 DDL/DML 版本化，先有表再写实体 |
| ORM | **MyBatis** | 手写 SQL，Mapper 接口 + XML；详见 [JPA_VS_MYBATIS.md](./JPA_VS_MYBATIS.md) |
| Spring Boot / Java | **3.2 + Java 17** | 与当前生态兼容好 |
| 前端 | **往后放** | 先铺好后端再考虑 |
| Docker | **分阶段引入** | 见下文「Docker 与学习曲线」 |
| 业务模块 | **审批流 + 权限管理 + 订单与商品** | 复杂度适中，尽量覆盖技术栈 |

---

## 一、整体架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户浏览器                                 │
└────────────────────────────┬────────────────────────────────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  React 前端      │ │  Java 后端       │ │  静态文件/MinIO   │
│  (后期)          │ │  Spring Boot     │ │  上传下载         │
│  Port: 3000      │ │  Port: 8080      │ │  Port: 9000      │
└────────┬────────┘ └────────┬────────┘ └─────────────────┘
         │                   │
         │                   ├──────────────┬──────────────┐
         │                   ▼              ▼              ▼
         │           ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
         │           │ PostgreSQL  │ │   Redis     │ │   MinIO     │
         │           │ Port: 5432  │ │ Port: 6379  │ │ 对象存储    │
         │           └─────────────┘ └─────────────┘ └─────────────┘
         │
         └─────────── 全部由 Docker Compose 统一编排（分阶段加入）
```

---

## 二、技术栈选型

| 层级     | 技术选型              | 说明 |
|----------|-----------------------|------|
| 后端     | **Spring Boot 3.2**   | Web、Security、**MyBatis**、Validation、Java 17 |
| 数据库   | **PostgreSQL**        | Docker 运行，Flyway 管理表结构 |
| 缓存     | **Redis**（必选，学习重点） | 见下文「Redis 在本项目中的用法」 |
| 前端     | **React 18 + Vite**（后期） | 先做后端，前端后铺 |
| 权限     | **Spring Security + JWT** | 接口鉴权；配合 Redis 做 Token 黑名单/限流 |
| 文件     | **MinIO**（Docker）   | 上传下载，与 Redis 一样分阶段加入 |
| 容器化   | **Docker + Docker Compose** | 分阶段：先 PG+后端 → 加 Redis → 加 MinIO |

---

## 三、数据库与表设计思路（重点）

### 3.1 设计原则

- **先画 ER 图**：用户、角色、菜单、业务实体之间的关系。
- **表命名**：小写 + 下划线，如 `sys_user`、`sys_role`、`biz_order`。
- **通用字段**：每张业务表建议保留 `id`、`created_at`、`updated_at`、`deleted`（逻辑删除）。
- **版本管理**：用 **Flyway** 或 **Liquibase** 管理 DDL/DML，便于复现与协作。

### 3.2 核心表设计（权限 + 审批流 + 订单商品）

**系统/权限（sys_*）**

| 表名           | 用途 |
|----------------|------|
| `sys_user`     | 用户：账号、密码(加密)、姓名、部门、状态等 |
| `sys_role`     | 角色：管理员、审批人、运营、普通用户等 |
| `sys_user_role`| 用户-角色 多对多 |
| `sys_menu`     | 菜单/权限：名称、路径、父级、类型(菜单/按钮)、权限标识 |
| `sys_role_menu`| 角色-菜单 多对多（前端路由 + 按钮权限） |
| `sys_file`     | 文件记录：原始名、存储路径、大小、上传人、上传时间（上传下载） |

**审批流（biz_approval_*）**

| 表名                   | 用途 |
|------------------------|------|
| `biz_approval_definition` | 审批流程定义：名称、关联业务类型(如订单/请假)、节点配置(JSON 或子表) |
| `biz_approval_instance`   | 审批实例：关联某条业务数据、当前节点、状态(进行中/通过/驳回) |
| `biz_approval_task`      | 审批任务：谁在哪个节点待办、已办记录、批注 |

**订单与商品（biz_*）**

| 表名             | 用途 |
|------------------|------|
| `biz_product`    | 商品：名称、编码、价格、库存、状态、分类等 |
| `biz_product_category` | 商品分类（树形可选） |
| `biz_order`      | 订单：单号、用户、总金额、状态(待审批/已通过/已发货等)、关联审批实例 |
| `biz_order_item` | 订单明细：订单 ID、商品 ID、数量、单价、小计 |

这样：**权限**管谁能看哪些菜单和接口；**审批流**可复用到「订单需审批」等场景；**订单+商品**提供具体业务数据，并和审批关联，技术栈（JPA 关联、事务、Redis 缓存、Flyway）都能用上。

### 3.3 建表方式（已确定：Flyway）

1. **手写 SQL**：在 `src/main/resources/db/migration` 下写 Flyway 脚本（如 `V1__create_sys_tables.sql`），**PostgreSQL 语法**，第一次跑应用时自动建表。
2. **不加 JPA 自动 DDL**：生产与学习都只用 Flyway 管表结构，实体按表来写。
3. **Docker 中先起 PostgreSQL**：Compose 里只挂载数据卷，表结构完全由应用启动时 Flyway 执行。

---

## 四、Docker 相关规划与学习曲线

你希望「全面学习」又「一个人搞」，建议**按阶段加服务**，每步只多一个概念，更容易消化。

### 4.1 学习曲线建议：分三阶段加 Docker 服务

| 阶段 | 跑起来的服务 | 学习重点 |
|------|--------------|----------|
| **Docker 第一阶段** | 只跑 **PostgreSQL + 后端** | Compose 写法、网络、数据卷、后端连 `postgres:5432`；先把「能连库、Flyway 建表」跑通。 |
| **Docker 第二阶段** | 在上一阶段基础上加 **Redis** | 后端连 `redis:6379`；做登录限流、权限/菜单缓存、Token 黑名单（登出）等，**把 Redis 用熟**。 |
| **Docker 第三阶段** | 再加 **MinIO** | 上传下载走 MinIO；对象存储概念、桶与策略。 |

这样不会一次性面对 PG+Redis+MinIO+后端四个变量；每阶段都能单独验证，再叠下一层。

### 4.2 服务划分（最终形态）

| 服务名    | 镜像/构建方式        | 端口  | 说明 |
|-----------|----------------------|-------|------|
| `postgres`| postgres:16-alpine   | 5432  | 数据库，挂载数据卷，表由 Flyway 建 |
| `redis`   | redis:7-alpine      | 6379  | **学习重点**：缓存、限流、会话/黑名单 |
| `minio`   | minio/minio         | 9000, 9001 | 对象存储，上传下载 |
| `backend` | 当前项目 Dockerfile | 8080  | Spring Boot 应用 |
| `frontend`| 后期                | 80/3000 | 前端往后放 |

### 4.3 目录与文件建议

```
ProJect_Java/
├── docker-compose.yml       # 本地：postgres + redis + minio + backend（可先只启 postgres）
├── docker-compose.prod.yml  # 生产：可加 frontend、nginx
├── Dockerfile               # 后端多阶段构建：Maven 构建 + JRE 运行
└── docs/
    └── ARCHITECTURE_PLAN.md
```

### 4.4 学习要点

- **Compose**：先一个 `postgres` + 一个 `backend`，能访问再加 `redis`、`minio`。
- **网络**：后端连数据库/Redis 用**服务名**（如 `postgres`、`redis`），不要写 `localhost`。
- **数据持久化**：`volumes` 挂载 PostgreSQL 数据目录、Redis 可选持久化、MinIO 数据。
- **多阶段构建**：后端镜像只保留 JRE + jar，减小体积。

---

## 五、后端（Java）分层与模块

> 分层含义、请求链路、Maven/Spring/JPA/安全等**详细讲解与 12 个核心点**见 **[JAVA_BACKEND_GUIDE.md](./JAVA_BACKEND_GUIDE.md)**。

### 5.1 包结构示例（MyBatis）

```
org.example
├── config          # 安全、CORS、Jackson、Swagger、MyBatis 等
├── controller      # 接口层
├── service        # 业务逻辑
├── mapper         # MyBatis Mapper 接口（对应 XML 中的 SQL）
├── entity         # 实体/表映射（无 JPA 注解，与表字段对应）
├── dto            # 请求/响应 DTO
├── security       # JWT、UserDetails、权限校验
├── exception      # 全局异常与统一响应
└── util           # 工具类（如文件上传、下载）
```

Mapper XML 建议放在 `src/main/resources/mapper/` 下，与 Mapper 接口对应（如 `UserMapper.xml`）。

### 5.2 核心功能模块（按阶段实现）

1. **基础框架**：统一响应、全局异常、参数校验、Swagger/OpenAPI。
2. **认证与权限**：登录（JWT）、刷新 Token、角色-菜单权限、接口级权限（如 `@PreAuthorize`）。
3. **用户与角色管理**：CRUD、分配角色。
4. **菜单与权限数据**：树形菜单接口，供前端路由与权限控制。
5. **文件上传下载**：上传接口（存本地或 MinIO）、下载/预览接口、`sys_file` 记录。
6. **业务模块**：审批流、权限管理、订单与商品（见上表）。

### 5.3 Redis 在本项目中的用法（学习重点）

Redis 会**贯穿**多个功能，便于你系统学习：数据结构、过期、与 Spring 整合、典型场景。

| 场景 | 用法 | 学习点 |
|------|------|--------|
| **登录限流** | 按 IP 或用户名 key（如 `login:fail:admin`），自增，设过期时间；超过 N 次则拒绝登录 | `String`、`INCR`、`EXPIRE`、防暴力破解 |
| **JWT 黑名单 / 登出** | 用户登出时把 token 放入 Redis（如 `blacklist:<tokenId>`），过期时间 = token 剩余有效时间；校验时先查黑名单 | `SET` + TTL、无状态 JWT 的「失效」 |
| **用户权限/菜单缓存** | 登录后把「角色 + 菜单/权限列表」写入 Redis（如 `user:perms:<userId>`），TTL 10～30 分钟；接口鉴权或返回菜单时先读缓存，未命中再查库 | 降低 DB 压力、缓存失效策略 |
| **接口限流** | 按用户或 IP 做滑动窗口或固定窗口计数（如 `rate:api:<userId>`），超限返回 429 | 限流算法、`INCR`/`EXPIRE` 或 Lua |
| **审批/订单热点数据** | 高频访问的「审批定义」「商品详情」等可缓存一份到 Redis，更新时删缓存或设短 TTL | 缓存穿透/击穿（可选：空值缓存、单飞加载） |

实现上：Spring Boot 用 **Spring Data Redis**（或 **Lettuce** 客户端），配置 `spring.data.redis.host=redis`、`port=6379`；可封装一个 `RedisService` 统一做上述 key 的读写与过期。详细 API 与代码示例见 **[JAVA_BACKEND_GUIDE.md](./JAVA_BACKEND_GUIDE.md)** 中的「Redis 小节」。

---

## 六、前端（React）规划（从简，你主攻后端）

前端你已熟悉，这里只做最小约定，便于和后端联调。

### 6.1 技术选型建议

- **脚手架**：Vite + React + TypeScript。
- **UI 与布局**：Ant Design + Ant Design Pro 布局，或 React Admin。
- **路由**：React Router v6；根据权限动态生成路由（从后端菜单/权限接口拉取）。
- **状态**：React Query 请求接口；全局状态可用 Zustand/Redux（用户信息、权限列表等）。
- **请求**：Axios 封装（baseURL、请求头带 Token、401 跳转登录）。

### 6.2 路由与权限

- **登录页**：独立路由，不校验权限。
- **后端返回**：当前用户的菜单树 + 权限标识（如 `user:list`、`file:upload`）。
- **前端**：根据菜单树动态生成路由；根据权限标识控制菜单显示与按钮显隐（如无 `file:upload` 则隐藏上传按钮）。
- **路由守卫**：进入任意需登录页面时校验 Token，无效则跳转登录。

### 6.3 上传与下载

- **上传**：调用后端上传接口（multipart/form-data），返回文件 ID 或 URL；列表页用 `sys_file` 或业务关联表展示。
- **下载**：后端提供「按 ID 或 path 下载」接口，前端用 `window.open` 或带 Token 的 a 标签下载。

---

## 七、分阶段实施计划（后端优先，Redis 贯穿）

| 阶段 | 内容 | 产出 |
|------|------|------|
| **1. 基础与 Docker（PG）** | Spring Boot 3.2 + Java 17、Web、**MyBatis**、Flyway；Docker Compose 只起 **PostgreSQL**；Flyway 建 sys_ 基础表并跑通 | 后端能连 PG、表自动创建、Mapper 能查库 |
| **2. 表与实体（权限+文件）** | sys_user、sys_role、sys_menu、sys_user_role、sys_role_menu、sys_file 表与实体、Repository | 权限与文件表齐全，可做 CRUD |
| **3. 认证与权限** | 登录、JWT、角色菜单关联、菜单树接口、@PreAuthorize；先不加 Redis 跑通 | 登录与接口权限可用 |
| **4. 引入 Redis（学习重点）** | Docker 加 Redis；登录限流、JWT 黑名单（登出）、用户权限/菜单缓存、接口限流 | Redis 贯穿认证与权限，形成模板 |
| **5. 审批流** | biz_approval_* 表与实体、流程定义与实例、待办/已办接口、与业务关联（如订单提交审批） | 审批可复用到订单等业务 |
| **6. 订单与商品** | biz_product、biz_order、biz_order_item 表与 CRUD；订单状态与审批实例关联；商品/订单热点可走 Redis 缓存 | 完整订单+商品业务，技术栈都用上 |
| **7. 上传下载 + MinIO** | Docker 加 MinIO；上传/下载接口、sys_file；可选：订单附件、审批附件 | 文件能力闭环 |
| **8. 前端（后期）** | 按你习惯搭 React 后台，对接现有接口 | 全栈闭环 |

---

## 八、接下来可做的事

1. **ORM 已定为 MyBatis**：所有数据访问层用 Mapper 接口 + XML 实现。
2. **从阶段 1 开始**：下一步可做「阶段 1」的具体步骤：`pom.xml`（含 MyBatis）、`application.yml`（PostgreSQL）、`docker-compose.yml`（仅 postgres）、Flyway 脚本、第一个 Mapper + XML，以及一个最简单的 Controller 验证跑通。
