-- 大学智能考试系统数据库测试数据
-- MySQL 8.0 语法，编码：utf8mb4_general_ci

-- 注意：执行前请确保已创建所有表结构

-- 1. 部门/班级表 (sys_dept)
-- 先插入部门数据，因为用户表依赖部门ID
INSERT INTO `sys_dept` (`dept_name`, `parent_id`, `ancestors`, `sort_order`, `leader`, `category`, `create_by`, `update_by`) VALUES
('计算机学院', 0, '0', 1, '张院长', 1, 1, 1),
('电子工程学院', 0, '0', 2, '李院长', 1, 1, 1),
('计算机科学与技术', 1, '0,1', 1, '王教授', 2, 1, 1),
('软件工程', 1, '0,1', 2, '赵教授', 2, 1, 1),
('通信工程', 2, '0,2', 1, '刘教授', 2, 1, 1),
('计科2021级', 3, '0,1,3', 1, '陈老师', 3, 1, 1),
('计科2022级', 3, '0,1,3', 2, '杨老师', 3, 1, 1),
('软工2021级', 4, '0,1,4', 1, '黄老师', 3, 1, 1),
('软工2022级', 4, '0,1,4', 2, '吴老师', 3, 1, 1),
('通信2021级', 5, '0,2,5', 1, '郑老师', 3, 1, 1);

-- 2. 用户表 (sys_user)
-- 插入不同角色的用户数据
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `avatar`, `role`, `dept_id`, `email`, `phone`, `status`, `create_by`, `update_by`) VALUES
-- 管理员
('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '管理员', 'https://example.com/avatar/admin.jpg', 3, NULL, 'admin@example.com', '13800138000', 1, 1, 1),
-- 教师
('teacher1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '王教授', 'https://example.com/avatar/teacher1.jpg', 2, 3, 'teacher1@example.com', '13800138001', 1, 1, 1),
('teacher2', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '赵教授', 'https://example.com/avatar/teacher2.jpg', 2, 4, 'teacher2@example.com', '13800138002', 1, 1, 1),
('teacher3', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '刘教授', 'https://example.com/avatar/teacher3.jpg', 2, 5, 'teacher3@example.com', '13800138003', 1, 1, 1),
-- 学生
('student1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三', 'https://example.com/avatar/student1.jpg', 1, 6, 'student1@example.com', '13800138010', 1, 1, 1),
('student2', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李四', 'https://example.com/avatar/student2.jpg', 1, 6, 'student2@example.com', '13800138011', 1, 1, 1),
('student3', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '王五', 'https://example.com/avatar/student3.jpg', 1, 7, 'student3@example.com', '13800138012', 1, 1, 1),
('student4', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '赵六', 'https://example.com/avatar/student4.jpg', 1, 8, 'student4@example.com', '13800138013', 1, 1, 1),
('student5', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '孙七', 'https://example.com/avatar/student5.jpg', 1, 9, 'student5@example.com', '13800138014', 1, 1, 1),
('student6', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '周八', 'https://example.com/avatar/student6.jpg', 1, 10, 'student6@example.com', '13800138015', 1, 1, 1);

-- 3. 课程表 (sys_course)
-- 插入课程数据，依赖部门ID
INSERT INTO `sys_course` (`course_name`, `course_code`, `dept_id`, `credits`, `cover_img`, `description`, `status`, `create_by`, `update_by`) VALUES
('数据库原理', 'CS301', 3, 3.0, 'https://example.com/course/db.jpg', '数据库系统的基本原理和应用', 1, 2, 2),
('软件工程', 'CS302', 4, 3.0, 'https://example.com/course/se.jpg', '软件工程的基本原理和方法', 1, 3, 3),
('通信原理', 'EE301', 5, 4.0, 'https://example.com/course/comm.jpg', '通信系统的基本原理和技术', 1, 4, 4),
('人工智能', 'CS401', 3, 3.0, 'https://example.com/course/ai.jpg', '人工智能的基本概念和应用', 1, 2, 2),
('计算机网络', 'CS303', 3, 3.0, 'https://example.com/course/network.jpg', '计算机网络的基本原理和协议', 1, 2, 2);

-- 4. 选课表 (sys_course_user)
-- 插入选课数据，依赖课程ID和用户ID
INSERT INTO `sys_course_user` (`course_id`, `user_id`, `semester`, `role`, `create_by`, `update_by`) VALUES
-- 教师授课
(1, 2, '2023-2024-2', 2, 1, 1),
(2, 3, '2023-2024-2', 2, 1, 1),
(3, 4, '2023-2024-2', 2, 1, 1),
(4, 2, '2023-2024-2', 2, 1, 1),
(5, 2, '2023-2024-2', 2, 1, 1),
-- 学生选课
(1, 5, '2023-2024-2', 1, 1, 1),
(1, 6, '2023-2024-2', 1, 1, 1),
(1, 7, '2023-2024-2', 1, 1, 1),
(2, 8, '2023-2024-2', 1, 1, 1),
(3, 9, '2023-2024-2', 1, 1, 1),
(4, 5, '2023-2024-2', 1, 1, 1),
(4, 6, '2023-2024-2', 1, 1, 1),
(5, 5, '2023-2024-2', 1, 1, 1),
(5, 6, '2023-2024-2', 1, 1, 1),
(5, 7, '2023-2024-2', 1, 1, 1);

-- 5. RAG资料表 (sys_knowledge_file)
-- 插入RAG资料数据，依赖课程ID
INSERT INTO `sys_knowledge_file` (`course_id`, `file_name`, `file_url`, `dify_document_id`, `status`, `create_by`, `update_by`) VALUES
(1, '数据库原理课件.pdf', 'https://example.com/files/db_ppt.pdf', 'ds-doc-001', 2, 2, 2),
(1, '数据库原理教材.pdf', 'https://example.com/files/db_textbook.pdf', 'ds-doc-002', 2, 2, 2),
(2, '软件工程课件.pdf', 'https://example.com/files/se_ppt.pdf', 'ds-doc-003', 2, 3, 3),
(3, '通信原理课件.pdf', 'https://example.com/files/comm_ppt.pdf', 'ds-doc-004', 2, 4, 4),
(4, '人工智能课件.pdf', 'https://example.com/files/ai_ppt.pdf', 'ds-doc-005', 2, 2, 2);

-- 6. 题库表 (exam_question)
-- 插入题目数据，依赖课程ID
INSERT INTO `exam_question` (`course_id`, `type`, `content`, `image_url`, `options`, `answer`, `analysis`, `difficulty`, `tags`, `create_by`, `update_by`) VALUES
-- 数据库原理 - 单选题
(1, 1, 'SQL中用于插入数据的命令是？', NULL, '["SELECT", "INSERT", "UPDATE", "DELETE"]', '1', 'INSERT用于插入数据，SELECT用于查询，UPDATE用于更新，DELETE用于删除', 1, '["SQL基础", "插入操作"]', 2, 2),
(1, 1, '下列哪个不是关系型数据库？', NULL, '["MySQL", "Oracle", "MongoDB", "PostgreSQL"]', '2', 'MongoDB是NoSQL数据库，其他都是关系型数据库', 1, '["数据库类型", "NoSQL"]', 2, 2),
-- 数据库原理 - 多选题
(1, 2, '下列哪些是SQL的聚合函数？', NULL, '["COUNT", "SUM", "AVG", "MAX"]', '[0,1,2,3]', 'COUNT、SUM、AVG、MAX都是SQL的聚合函数', 2, '["SQL函数", "聚合函数"]', 2, 2),
-- 数据库原理 - 判断题
(1, 3, '主键必须是唯一的且不能为空。', NULL, NULL, '1', '主键约束要求字段值唯一且非空', 1, '["主键约束", "基础概念"]', 2, 2),
-- 数据库原理 - 简答题
(1, 4, '简述事务的ACID特性。', NULL, NULL, '原子性、一致性、隔离性、持久性', '原子性：事务是一个不可分割的工作单位；一致性：事务执行前后数据保持一致；隔离性：多个事务并发执行时互不干扰；持久性：事务一旦提交，其结果永久保存在数据库中', 3, '["事务", "ACID"]', 2, 2),
-- 软件工程 - 单选题
(2, 1, '瀑布模型的正确顺序是？', NULL, '["需求分析→设计→编码→测试→维护", "设计→需求分析→编码→测试→维护", "编码→需求分析→设计→测试→维护", "测试→编码→设计→需求分析→维护"]', '0', '瀑布模型按照需求分析、设计、编码、测试、维护的顺序进行', 1, '["瀑布模型", "软件开发模型"]', 3, 3),
-- 软件工程 - 多选题
(2, 2, '下列哪些是敏捷开发方法？', NULL, '["Scrum", "XP", "Kanban", "瀑布模型"]', '[0,1,2]', 'Scrum、XP、Kanban都是敏捷开发方法，瀑布模型是传统开发方法', 2, '["敏捷开发", "开发方法"]', 3, 3),
-- 人工智能 - 单选题
(4, 1, '下列哪个不是机器学习算法？', NULL, '["决策树", "神经网络", "K-means", "冒泡排序"]', '3', '冒泡排序是排序算法，不是机器学习算法', 1, '["机器学习", "算法分类"]', 2, 2),
-- 人工智能 - 判断题
(4, 3, '深度学习是机器学习的一个分支。', NULL, NULL, '1', '深度学习是机器学习的一个重要分支，基于人工神经网络', 1, '["深度学习", "机器学习"]', 2, 2),
-- 计算机网络 - 单选题
(5, 1, 'OSI参考模型分为几层？', NULL, '["5层", "6层", "7层", "8层"]', '2', 'OSI参考模型分为物理层、数据链路层、网络层、传输层、会话层、表示层、应用层共7层', 1, '["OSI模型", "网络基础"]', 2, 2);

-- 7. AI任务记录表 (sys_ai_task)
-- 插入AI任务数据
INSERT INTO `sys_ai_task` (`task_name`, `type`, `total_count`, `current_count`, `status`, `error_msg`, `create_by`, `update_by`) VALUES
('自动生成数据库原理题库', 'question_generation', 100, 100, 2, NULL, 2, 2),
('自动生成软件工程题库', 'question_generation', 80, 80, 2, NULL, 3, 3),
('自动批改数据库原理试卷', 'grading', 50, 50, 2, NULL, 2, 2),
('自动生成人工智能题库', 'question_generation', 60, 45, 1, NULL, 2, 2),
('自动分析学生答题情况', 'analysis', 100, 0, 0, NULL, 2, 2);

-- 8. 试卷模板表 (exam_paper)
-- 插入试卷模板数据，依赖课程ID
INSERT INTO `exam_paper` (`course_id`, `title`, `total_score`, `pass_score`, `duration`, `difficulty`, `status`, `create_by`, `update_by`) VALUES
(1, '数据库原理期末考试', 100.0, 60.0, 120, 2, 1, 2, 2),
(2, '软件工程期中考试', 100.0, 60.0, 90, 2, 1, 3, 3),
(4, '人工智能期中考试', 100.0, 60.0, 90, 2, 1, 2, 2),
(5, '计算机网络期末考试', 100.0, 60.0, 120, 3, 1, 2, 2);

-- 9. 试卷题目关联表 (exam_paper_question)
-- 插入试卷题目关联数据，依赖试卷ID和题目ID
INSERT INTO `exam_paper_question` (`paper_id`, `question_id`, `score`, `sort_order`, `create_by`, `update_by`) VALUES
-- 数据库原理期末考试
(1, 1, 10.0, 1, 2, 2),
(1, 2, 10.0, 2, 2, 2),
(1, 3, 20.0, 3, 2, 2),
(1, 4, 10.0, 4, 2, 2),
(1, 5, 50.0, 5, 2, 2),
-- 软件工程期中考试
(2, 6, 15.0, 1, 3, 3),
(2, 7, 25.0, 2, 3, 3),
-- 人工智能期中考试
(3, 8, 10.0, 1, 2, 2),
(3, 9, 10.0, 2, 2, 2),
-- 计算机网络期末考试
(4, 10, 10.0, 1, 2, 2);

-- 10. 考试发布/场次表 (exam_publish)
-- 插入考试发布数据，依赖试卷ID
INSERT INTO `exam_publish` (`paper_id`, `title`, `target_dept_ids`, `start_time`, `end_time`, `limit_count`, `password`, `status`, `create_by`, `update_by`) VALUES
(1, '2023-2024下学期数据库原理期末考试', '[6,7,8]', '2024-06-20 09:00:00', '2024-06-20 11:00:00', -1, '123456', 2, 2, 2),
(2, '2023-2024下学期软件工程期中考试', '[8,9]', '2024-04-15 14:30:00', '2024-04-15 16:00:00', -1, NULL, 2, 3, 3),
(3, '2023-2024下学期人工智能期中考试', '[6,7]', '2024-04-20 10:00:00', '2024-04-20 11:30:00', -1, '654321', 2, 2, 2),
(4, '2023-2024下学期计算机网络期末考试', '[6,7,8]', '2024-06-21 09:00:00', '2024-06-21 11:00:00', -1, NULL, 1, 2, 2);

-- 11. 考试记录表 (exam_record)
-- 插入考试记录数据，依赖用户ID和发布ID
INSERT INTO `exam_record` (`user_id`, `publish_id`, `paper_id`, `total_score`, `user_ip`, `user_device`, `start_time`, `submit_time`, `status`, `create_by`, `update_by`) VALUES
-- 学生1参加数据库原理期末考试
(5, 1, 1, 85.0, '192.168.1.101', 'Chrome/120.0.0.0', '2024-06-20 09:00:00', '2024-06-20 10:45:00', 2, 2, 2),
-- 学生2参加数据库原理期末考试
(6, 1, 1, 90.0, '192.168.1.102', 'Firefox/119.0.0.0', '2024-06-20 09:05:00', '2024-06-20 10:50:00', 2, 2, 2),
-- 学生3参加数据库原理期末考试
(7, 1, 1, 75.0, '192.168.1.103', 'Safari/17.0.0', '2024-06-20 09:10:00', '2024-06-20 10:30:00', 2, 2, 2),
-- 学生4参加软件工程期中考试
(8, 2, 2, 88.0, '192.168.1.104', 'Chrome/120.0.0.0', '2024-04-15 14:30:00', '2024-04-15 15:45:00', 2, 3, 3),
-- 学生1参加人工智能期中考试
(5, 3, 3, 82.0, '192.168.1.101', 'Chrome/120.0.0.0', '2024-04-20 10:00:00', '2024-04-20 11:15:00', 2, 2, 2),
-- 学生2参加人工智能期中考试
(6, 3, 3, 78.0, '192.168.1.102', 'Firefox/119.0.0.0', '2024-04-20 10:05:00', '2024-04-20 11:20:00', 2, 2, 2),
-- 学生1未参加计算机网络期末考试
(5, 4, 4, 0.0, NULL, NULL, NULL, NULL, 0, 2, 2),
-- 学生2正在参加计算机网络期末考试
(6, 4, 4, 0.0, '192.168.1.102', 'Chrome/120.0.0.0', '2024-06-21 09:00:00', NULL, 1, 2, 2);

-- 12. 答题明细表 (exam_record_detail)
-- 插入答题明细数据，依赖记录ID和题目ID
INSERT INTO `exam_record_detail` (`record_id`, `question_id`, `student_answer`, `score`, `max_score`, `ai_comment`, `is_correct`, `is_marked`, `create_by`, `update_by`) VALUES
-- 学生1的数据库原理期末考试答题情况
(1, 1, '1', 10.0, 10.0, '回答正确', 1, 1, 2, 2),
(1, 2, '2', 10.0, 10.0, '回答正确', 1, 1, 2, 2),
(1, 3, '[0,1,2,3]', 20.0, 20.0, '回答正确', 1, 1, 2, 2),
(1, 4, '1', 10.0, 10.0, '回答正确', 1, 1, 2, 2),
(1, 5, '原子性、一致性、隔离性、持久性', 35.0, 50.0, '回答基本正确，但缺少详细解释', 1, 1, 2, 2),
-- 学生2的数据库原理期末考试答题情况
(2, 1, '1', 10.0, 10.0, '回答正确', 1, 1, 2, 2),
(2, 2, '2', 10.0, 10.0, '回答正确', 1, 1, 2, 2),
(2, 3, '[0,1,2,3]', 20.0, 20.0, '回答正确', 1, 1, 2, 2),
(2, 4, '1', 10.0, 10.0, '回答正确', 1, 1, 2, 2),
(2, 5, '原子性：事务是一个不可分割的工作单位；一致性：事务执行前后数据保持一致；隔离性：多个事务并发执行时互不干扰；持久性：事务一旦提交，其结果永久保存在数据库中', 40.0, 50.0, '回答完整正确', 1, 1, 2, 2),
-- 学生4的软件工程期中考试答题情况
(4, 6, '0', 15.0, 15.0, '回答正确', 1, 1, 3, 3),
(4, 7, '[0,1,2]', 25.0, 25.0, '回答正确', 1, 1, 3, 3);

-- 13. 监考日志表 (exam_proctor_log)
-- 插入监考日志数据，依赖记录ID
INSERT INTO `exam_proctor_log` (`record_id`, `action_type`, `content`, `img_snapshot`, `happen_time`, `create_by`, `update_by`) VALUES
(1, 'login', '学生张三登录考试系统', NULL, '2024-06-20 08:59:30', 2, 2),
(1, 'start_exam', '学生张三开始考试', NULL, '2024-06-20 09:00:00', 2, 2),
(1, 'submit_exam', '学生张三提交考试', NULL, '2024-06-20 10:45:00', 2, 2),
(2, 'login', '学生李四登录考试系统', NULL, '2024-06-20 09:04:20', 2, 2),
(2, 'start_exam', '学生李四开始考试', NULL, '2024-06-20 09:05:00', 2, 2),
(2, 'submit_exam', '学生李四提交考试', NULL, '2024-06-20 10:50:00', 2, 2),
(6, 'login', '学生李四登录考试系统', NULL, '2024-04-20 10:04:15', 2, 2),
(6, 'start_exam', '学生李四开始考试', NULL, '2024-04-20 10:05:00', 2, 2),
(6, 'submit_exam', '学生李四提交考试', NULL, '2024-04-20 11:20:00', 2, 2);

-- 14. 错题本 (exam_mistake_book)
-- 插入错题本数据，依赖用户ID和题目ID
INSERT INTO `exam_mistake_book` (`user_id`, `course_id`, `question_id`, `wrong_count`, `last_wrong_answer`, `create_by`, `update_by`) VALUES
(5, 1, 5, 1, '原子性、一致性、隔离性', 5, 5),
(7, 1, 3, 2, '[0,1,2]', 7, 7),
(9, 3, 6, 1, '1', 9, 9),
(6, 4, 8, 1, '2', 6, 6);

-- 15. 系统通知表 (sys_notice)
-- 插入系统通知数据
INSERT INTO `sys_notice` (`title`, `content`, `type`, `target_type`, `sender_id`, `create_by`, `update_by`) VALUES
('系统维护通知', '系统将于2024年7月1日进行维护，请提前做好准备', 1, 1, 1, 1, 1),
('数据库原理期末考试安排', '数据库原理期末考试将于2024年6月20日上午9:00-11:00进行，请各位同学准时参加', 2, 3, 2, 2, 2),
('软件工程期中考试成绩已发布', '软件工程期中考试成绩已发布，请各位同学登录系统查询', 3, 3, 3, 3, 3),
('人工智能期中考试安排', '人工智能期中考试将于2024年4月20日上午10:00-11:30进行，请各位同学准时参加', 2, 3, 2, 2, 2),
('计算机网络期末考试安排', '计算机网络期末考试将于2024年6月21日上午9:00-11:00进行，请各位同学准时参加', 2, 3, 2, 2, 2);

-- 16. 用户消息状态表 (sys_user_notice)
-- 插入用户消息状态数据，依赖通知ID和用户ID
INSERT INTO `sys_user_notice` (`notice_id`, `user_id`, `is_read`, `read_time`, `create_by`, `update_by`) VALUES
-- 系统通知发给所有用户
(1, 1, 1, '2024-06-15 09:00:00', 1, 1),
(1, 2, 1, '2024-06-15 09:30:00', 1, 1),
(1, 3, 1, '2024-06-15 10:00:00', 1, 1),
(1, 4, 1, '2024-06-15 10:30:00', 1, 1),
(1, 5, 1, '2024-06-15 11:00:00', 1, 1),
(1, 6, 1, '2024-06-15 11:30:00', 1, 1),
(1, 7, 1, '2024-06-15 12:00:00', 1, 1),
(1, 8, 1, '2024-06-15 12:30:00', 1, 1),
(1, 9, 1, '2024-06-15 13:00:00', 1, 1),
(1, 10, 1, '2024-06-15 13:30:00', 1, 1),
-- 数据库原理期末考试通知发给相关学生
(2, 5, 1, '2024-06-10 09:00:00', 2, 2),
(2, 6, 1, '2024-06-10 09:30:00', 2, 2),
(2, 7, 1, '2024-06-10 10:00:00', 2, 2),
(2, 8, 1, '2024-06-10 10:30:00', 2, 2),
-- 软件工程期中考试成绩通知发给相关学生
(3, 8, 1, '2024-04-20 14:00:00', 3, 3),
(3, 9, 1, '2024-04-20 14:30:00', 3, 3),
-- 人工智能期中考试通知发给相关学生
(4, 5, 1, '2024-04-15 09:00:00', 2, 2),
(4, 6, 1, '2024-04-15 09:30:00', 2, 2),
-- 计算机网络期末考试通知发给相关学生
(5, 5, 0, NULL, 2, 2),
(5, 6, 0, NULL, 2, 2),
(5, 7, 0, NULL, 2, 2),
(5, 8, 0, NULL, 2, 2);

-- 插入系统配置预设数据（如果不存在）
INSERT IGNORE INTO `sys_config` (`config_key`, `config_value`, `description`, `create_by`, `update_by`) VALUES
('dify_base_url', 'https://api.dify.ai/v1', 'Dify API 基础地址', 1, 1),
('dify_key_knowledge', 'sk-******', '知识库专用 Key', 1, 1),
('dify_key_generation', 'sk-******', '出题应用 Key', 1, 1),
('dify_key_grading', 'sk-******', '阅卷应用 Key', 1, 1),
('dify_global_dataset_id', 'ds-******', '全局知识库 Dataset ID', 1, 1);
