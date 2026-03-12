# SQL 练习区（隔离环境）

在**当前项目使用的同一 PostgreSQL** 里，用独立 **schema `practice`** 做 SQL 练习，不动业务表、不触发 Flyway。

## 连接信息（与 application.yml 一致）

| 项     | 值              |
|--------|-----------------|
| 主机   | localhost       |
| 端口   | 5432            |
| 数据库 | project_java    |
| 用户   | app             |
| 密码   | app_secret      |

## 使用步骤

### 1. 初始化（只需做一次）

用 **DBeaver**、**IntelliJ Database**、**psql** 等连接上述库，执行：

```text
scripts/sql-practice/00_setup_practice_schema.sql
```

会创建 schema `practice` 和示例表：`practice.students`、`practice.classes`、`practice.scores`，并插入少量数据。

### 2. 平时练习

- **方式 A**：在 SQL 窗口先执行 `SET search_path = practice;`，之后直接写表名：
  ```sql
  SET search_path = practice;
  SELECT * FROM students WHERE class_id = 1;
  ```
- **方式 B**：不改 search_path，表名带 schema：
  ```sql
  SELECT * FROM practice.students;
  ```

在 `practice` 下可以随意 **CREATE TABLE / INSERT / UPDATE / DELETE**，不会影响 `public` 下的 `sys_*` 等业务表。

### 3. 重置示例数据（可选）

想恢复成脚本里的示例数据时，再执行一次 `00_setup_practice_schema.sql` 即可（脚本里有 TRUNCATE + INSERT）。

## 练习表示例用途

| 表               | 可练内容 |
|------------------|----------|
| practice.students | WHERE, ORDER BY, LIKE, 简单 JOIN |
| practice.classes  | 与 students 做 JOIN、按班级统计 |
| practice.scores   | 聚合 (SUM/AVG/COUNT)、GROUP BY、子查询、多表 JOIN |

## 建议

- 所有练习表、练习数据都放在 **`practice`** 下，和业务库表隔离。
- **不要**把练习脚本放到 `src/main/resources/db/migration/`，否则会被 Flyway 执行。
- 本目录下可以自建 `01_my_exercises.sql` 等文件，保存练习语句，随时在客户端执行。
