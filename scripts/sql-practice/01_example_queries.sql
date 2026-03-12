-- ============================================================
-- 示例查询（仅供参考，在 practice schema 下执行）
-- 执行前请先: SET search_path = practice;
-- ============================================================

-- 1. 简单查询
-- SELECT * FROM students;
-- SELECT name, gender FROM students WHERE class_id = 1;

-- 2. 排序与限制
-- SELECT * FROM scores ORDER BY score DESC LIMIT 5;

-- 3. 聚合
-- SELECT subject, AVG(score) AS avg_score, COUNT(*) AS cnt FROM scores GROUP BY subject;

-- 4. 多表 JOIN（学生 + 班级名）
-- SELECT s.name, s.gender, c.name AS class_name
-- FROM students s
-- JOIN classes c ON s.class_id = c.id
-- ORDER BY c.id, s.id;

-- 5. 子查询（高于数学平均分的学生成绩）
-- SELECT * FROM scores
-- WHERE subject = '数学' AND score > (SELECT AVG(score) FROM scores WHERE subject = '数学');

-- 6. 按学生统计科目数、平均分
-- SELECT s.name, COUNT(sc.id) AS subject_count, ROUND(AVG(sc.score)::numeric, 2) AS avg_score
-- FROM students s
-- LEFT JOIN scores sc ON s.id = sc.student_id
-- GROUP BY s.id, s.name;
