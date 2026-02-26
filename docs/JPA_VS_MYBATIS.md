# JPA 与 MyBatis 对比与选型建议

> 你问：ORM 用 JPA 还是 MyBatis？不太了解，需要解释后再决定。  
> 下面用「是什么、怎么写、适合谁」说清楚，并给出本项目的推荐。

---

## 一、一句话区分

| 维度 | JPA（含 Spring Data JPA） | MyBatis |
|------|---------------------------|---------|
| **本质** | 用「对象」操作数据库，框架根据对象和注解**自动生成/执行 SQL** | 你**手写 SQL**，框架只负责把结果映射成 Java 对象 |
| **谁主导** | 模型（Entity）主导，表结构建议用 Flyway 先建好，再写实体 | SQL 主导，表和你写的 SQL 一一对应 |
| **适合场景** | CRUD 多、业务模型清晰、想少写 SQL、和 Spring 深度整合 | 复杂查询多、报表、对 SQL 要精细控制、遗留库 |

---

## 二、写法对比（同一条查询）

需求：根据用户名查用户。

### JPA（Spring Data JPA）

```java
// 1. 实体：和表 sys_user 对应
@Entity
@Table(name = "sys_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    // ...
}

// 2. Repository：只写接口，不写实现，不写 SQL
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);  // 方法名即查询
}

// 3. Service 里直接用
User user = userRepository.findByUsername("admin").orElse(null);
```

- 你不用写 `SELECT * FROM sys_user WHERE username = ?`，框架根据方法名 `findByUsername` 自动生成。
- 复杂一点可以用 `@Query("SELECT u FROM User u WHERE u.status = :status")` 写 JPQL（面向对象版的 SQL，用实体名和属性名）。

### MyBatis

```java
// 1. 实体（和 JPA 类似，但无 JPA 注解）
public class User {
    private Long id;
    private String username;
    // getter/setter
}

// 2. Mapper 接口 + XML 里手写 SQL
public interface UserMapper {
    User findByUsername(@Param("username") String username);
}
```

```xml
<!-- UserMapper.xml -->
<select id="findByUsername" resultType="com.xxx.entity.User">
    SELECT id, username, ...
    FROM sys_user
    WHERE username = #{username}
</select>
```

- SQL 完全由你写，可以写很复杂的多表、子查询、统计。
- 结果通过 `resultType` 或 `resultMap` 映射成 User 对象。

---

## 三、优缺点简表

| 点 | JPA（Spring Data JPA） | MyBatis |
|----|------------------------|---------|
| **学习成本** | 要理解实体、Repository、JPQL，但 CRUD 几乎不写 SQL | 会写 SQL 就会用，但要写 XML 或注解 SQL |
| **CRUD 量** | 非常多时很省事（save、findById、delete 自带） | 每个都要写 SQL 或复用通用模板 |
| **复杂查询** | 能写，但复杂多表、动态 SQL 不如 MyBatis 顺手 | 非常擅长，动态 SQL、报表、优化都自己控 |
| **和 Spring 整合** | 原生一家，事务、分页、规范统一 | 整合没问题，但「风格」更偏 SQL 中心 |
| **表结构变更** | 一般配合 Flyway：改表后改实体、改 Repository 方法 | 改表后改 SQL 和 resultMap |
| **多表关联** | 用关联映射（@OneToMany 等）或 JPQL 查，要当心 N+1 | 手写 JOIN，一次查出来，可控 |
| **适合人群** | 想少写 SQL、先搞业务、表结构清晰 | 对 SQL 有要求、复杂查询多、偏 DBA 思维 |

---

## 四、和本项目的匹配度

你的需求简要回顾：

- **数据库**：PostgreSQL，**建表用 Flyway**（表结构你完全掌控）。
- **业务**：审批流、权限管理、订单+商品；要**尽量多用到技术栈**，但**一个人做、不能过于复杂**。
- **学习目标**：第一个 Java 项目，希望有一个**可复用的模板**。

结合这些：

1. **表已经用 Flyway 管好了**：用 JPA 只做「实体 ↔ 表」映射 + 简单/中等查询，不会和「谁管表结构」冲突；复杂查询用 `@Query` 写 JPQL 或原生 SQL 即可。
2. **审批流、订单、商品**：会有多表关联和状态流转，JPA 的实体关联 + 事务用起来直接；真有一两个特别复杂的统计/报表，可以在同一项目里单点用 MyBatis 或原生 SQL。
3. **学习曲线**：你第一次做 Java 全栈，JPA + Spring Data 和 Spring Boot 是一套体系，文档和示例多，更容易形成「一个模板」。
4. **Redis、权限、文件**：和选 JPA 还是 MyBatis 无关，两种都能搭。

**推荐结论**：**本项目以 JPA（Spring Data JPA）为主**做 ORM；表结构继续用 Flyway 管理，实体按表来写。若后面某块（例如订单报表）需要很复杂的 SQL，再单独加 MyBatis 或 `@Query` 原生 SQL 即可。

---

## 五、本项目选型结果

**已确定使用 MyBatis**。架构与实施计划、代码示例、包结构均按 **MyBatis + Flyway** 执行：

- 表结构：Flyway 管理 DDL，先有表再写实体与 Mapper。
- 数据访问：Mapper 接口 + XML（`src/main/resources/mapper/`），手写 SQL。
- 实体类：与表字段一一对应，无 JPA 注解；结果映射用 `resultType` / `resultMap`。
