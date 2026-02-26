# 阶段 1 运行说明

## 1. 启动 PostgreSQL（Docker）

在项目根目录执行：

```bash
docker compose up -d
```

等待几秒后可用 `docker compose ps` 查看容器状态。若无 Docker，请先安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)。

## 2. 运行后端

- **IDEA**：右键 `ProJectJavaApplication.java` → Run。
- **命令行（无需单独安装 Maven）**：在项目根目录执行：
  ```bash
  .\mvnw.cmd spring-boot:run
  ```
  首次运行会自动下载 Maven 与 wrapper 所需文件。需已安装 JDK 17 并设置 `JAVA_HOME`。

应用启动后会：

1. 连接 `localhost:5432` 的 PostgreSQL；
2. 执行 Flyway 迁移（创建 `sys_user` 表并插入一条 admin）；
3. 监听 **8080** 端口。

## 3. 验证

- 浏览器或 curl：
  - `http://localhost:8080/api/hello` → 返回 `Hello, ProJect_Java.`
  - `http://localhost:8080/api/db-check` → 返回 JSON，内含 `ok: true` 和 `user`（admin 用户信息），即表示连库与 Mapper 正常。

## 4. 本机未装 Docker 时

可安装本地 PostgreSQL，创建数据库 `project_java`、用户 `app`/密码 `app_secret`，并确保端口 5432 可用；`application.yml` 已按此配置。
