-- 阶段 1：第一张表 sys_user，用于验证 Flyway + MyBatis 连库
-- PostgreSQL 语法

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(128),
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_username ON sys_user (username);

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.username IS '登录名';
COMMENT ON COLUMN sys_user.password_hash IS '密码哈希';

-- 插入一条测试数据，便于阶段 1 验证查询
INSERT INTO sys_user (username, password_hash)
VALUES ('admin', 'placeholder')
ON CONFLICT (username) DO NOTHING;
