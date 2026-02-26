-- 阶段 2：权限与文件相关表（sys_role, sys_menu, sys_user_role, sys_role_menu, sys_file）
-- PostgreSQL 语法

-- 1. 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    code        VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_role_code ON sys_role (code);
COMMENT ON TABLE sys_role IS '角色表';

-- 2. 菜单/权限表
CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT       DEFAULT 0,
    name        VARCHAR(64)  NOT NULL,
    path        VARCHAR(256),
    permission  VARCHAR(64),
    type        SMALLINT     DEFAULT 1,
    sort_order  INT          DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE sys_menu IS '菜单/权限表，type 1=菜单 2=按钮';
COMMENT ON COLUMN sys_menu.parent_id IS '父菜单 id，0 表示顶级';

-- 3. 用户-角色 多对多
CREATE TABLE IF NOT EXISTS sys_user_role (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    role_id    BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);
COMMENT ON TABLE sys_user_role IS '用户-角色关联';

-- 4. 角色-菜单 多对多
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id         BIGSERIAL PRIMARY KEY,
    role_id    BIGINT NOT NULL,
    menu_id    BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu (id) ON DELETE CASCADE,
    CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id)
);
COMMENT ON TABLE sys_role_menu IS '角色-菜单/权限关联';

-- 5. 文件记录表（上传下载用）
CREATE TABLE IF NOT EXISTS sys_file (
    id            BIGSERIAL PRIMARY KEY,
    original_name VARCHAR(256),
    storage_path  VARCHAR(512) NOT NULL,
    file_size     BIGINT       DEFAULT 0,
    uploader_id   BIGINT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user (id) ON DELETE SET NULL
);
COMMENT ON TABLE sys_file IS '文件记录，用于上传下载';

-- 初始化：管理员角色 + 若干菜单 + admin 用户关联管理员角色
INSERT INTO sys_role (name, code) VALUES ('管理员', 'ADMIN'), ('普通用户', 'USER')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sys_menu (parent_id, name, path, permission, type, sort_order)
VALUES
  (0, '首页', '/dashboard', 'dashboard:view', 1, 1),
  (0, '系统管理', '/system', NULL, 1, 2),
  (0, '用户管理', '/system/user', 'user:list', 1, 3),
  (0, '角色管理', '/system/role', 'role:list', 1, 4),
  (0, '文件管理', '/system/file', 'file:list', 1, 5);

-- 将 admin 用户关联到 ADMIN 角色（依赖 V1 的 admin 用户与上面插入的角色）
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.code = 'ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;
