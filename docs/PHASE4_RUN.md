# 阶段 4 运行说明（Redis）

## 1. 启动 Redis

在项目根目录执行：

```bash
docker compose up -d
```

会同时启动 **PostgreSQL** 和 **Redis**。若之前只启了 postgres，再执行一次即可把 redis 也拉起来。

可用 `docker compose ps` 确认两个容器都在运行。

## 2. 运行后端

```powershell
.\run.ps1
```

或 IDEA 运行 `ProJectJavaApplication`。应用会连 `localhost:6379` 的 Redis。

## 3. 阶段 4 功能验证

- **登录限流**：用错误密码对同一用户连续登录 5 次，第 6 次应返回「登录失败次数过多，请稍后再试」；等 15 分钟或重启 Redis 后恢复。
- **登出黑名单**：登录后拿到 token，调用 `POST /api/auth/logout`（Header 带 `Authorization: Bearer <token>`），再用该 token 调 `/api/auth/me` 或 `/api/auth/menus`，应返回 401。
- **菜单缓存**：第一次访问 `GET /api/auth/menus` 会查库并写入 Redis；同一用户再次访问在 TTL 内会直接读缓存（可看日志或 Redis 键 `user:menus:<userId>`）。

## 4. 配置说明

- `application.yml` 中：
  - `spring.data.redis.host/port`：Redis 地址（本机 Docker 为 localhost:6379）。
  - `security.login-lock.max-attempts`：连续失败几次后锁定（默认 5）。
  - `security.login-lock.lock-minutes`：锁定时长（分钟，默认 15）。
  - `security.menu-cache-minutes`：菜单缓存过期时间（分钟，默认 30）。
