-- ============================================================
-- SQL 练习区 - 初始化脚本（仅练习用，与业务表完全隔离）
-- 数据库: project_java（与 application.yml 一致）
-- 用法: 在 DBeaver / psql / IntelliJ 中手动执行本脚本，只需执行一次
-- ============================================================

-- 1. 创建练习专用 schema（与 public 下的 sys_* 表隔离）
CREATE SCHEMA IF NOT EXISTS practice;

-- 切换到 practice 方便后续建表（在客户端里也可以 SET search_path = practice;）
SET search_path = practice;

-- 2. 示例表：学生（供练习 SELECT / WHERE / ORDER / JOIN）
CREATE TABLE IF NOT EXISTS practice.students (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    gender      VARCHAR(8),
    birth_date  DATE,
    class_id    INT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE practice.students IS '练习用-学生表';

-- 3. 示例表：班级
CREATE TABLE IF NOT EXISTS practice.classes (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(32) NOT NULL,
    grade       INT
);
COMMENT ON TABLE practice.classes IS '练习用-班级表';

-- 4. 示例表：成绩（练习聚合、分组、子查询）
CREATE TABLE IF NOT EXISTS practice.scores (
    id          BIGSERIAL PRIMARY KEY,
    student_id  BIGINT NOT NULL,
    subject     VARCHAR(32) NOT NULL,
    score       NUMERIC(5,2),
    exam_date   DATE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE practice.scores IS '练习用-成绩表';

-- 5. 可选：清空后重新塞入示例数据（方便反复练习）
TRUNCATE TABLE practice.scores, practice.students, practice.classes RESTART IDENTITY;

INSERT INTO practice.classes (name, grade) VALUES
    ('高一1班', 1), ('高一2班', 1), ('高二1班', 2);

INSERT INTO practice.students (name, gender, birth_date, class_id) VALUES
    ('张三', '男', '2008-03-15', 1),
    ('李四', '女', '2008-07-22', 1),
    ('王五', '男', '2008-01-10', 2),
    ('赵六', '女', '2007-11-05', 2),
    ('钱七', '男', '2007-09-18', 3);

INSERT INTO practice.scores (student_id, subject, score, exam_date) VALUES
    (1, '数学', 92, '2024-06-01'),
    (1, '语文', 88, '2024-06-01'),
    (2, '数学', 78, '2024-06-01'),
    (2, '语文', 95, '2024-06-01'),
    (3, '数学', 85, '2024-06-01'),
    (4, '数学', 70, '2024-06-01'),
    (4, '英语', 90, '2024-06-01'),
    (5, '语文', 82, '2024-06-01');

-- 恢复默认 search_path（可选）
SET search_path = public;

-- 完成。之后练习时：
-- - 在客户端里 SET search_path = practice; 然后直接写 SELECT * FROM students;
-- - 或者始终带 schema：SELECT * FROM practice.students;
-- - 建自己的练习表也放在 practice 下：CREATE TABLE practice.my_table (...);
