-- 阶段 3：初始化 admin 用户密码与状态字段
-- 为了演示登录流程，这里给 admin 设置一个已加密的密码。
-- 明文密码：password

ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS status SMALLINT DEFAULT 1;

COMMENT ON COLUMN sys_user.status IS '用户状态：1=正常, 0=禁用';

UPDATE sys_user
SET password_hash = '$2b$12$KIXIDzJ2Rf2zXSpwURZrYe0djyyu3E4yFczVqTx3FWSMsvjrdIhJy'
WHERE username = 'admin';

