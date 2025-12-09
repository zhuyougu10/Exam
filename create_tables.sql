-- 大学智能考试系统数据库表结构设计
-- MySQL 8.0 语法，编码：utf8mb4_general_ci

-- 1. 用户表
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `real_name` varchar(20) NOT NULL COMMENT '真实姓名',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `role` tinyint NOT NULL COMMENT '角色：1-学生，2-教师，3-管理员',
  `dept_id` bigint DEFAULT NULL COMMENT '部门/班级ID',
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  INDEX `idx_dept_id` (`dept_id`),
  INDEX `idx_role` (`role`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- 2. 部门/班级表
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dept_name` varchar(50) NOT NULL COMMENT '部门/班级名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门ID',
  `ancestors` varchar(255) DEFAULT '' COMMENT '祖级列表',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `leader` varchar(20) DEFAULT NULL COMMENT '负责人',
  `category` tinyint DEFAULT NULL COMMENT '分类：1-学院，2-专业，3-班级',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  INDEX `idx_parent_id` (`parent_id`),
  INDEX `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门/班级表';

-- 3. 课程表
CREATE TABLE `sys_course` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_name` varchar(100) NOT NULL COMMENT '课程名称',
  `course_code` varchar(20) NOT NULL COMMENT '课程编码',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  `credits` decimal(3,1) DEFAULT NULL COMMENT '学分',
  `cover_img` varchar(255) DEFAULT NULL COMMENT '课程封面',
  `description` text COMMENT '课程描述',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_code` (`course_code`),
  INDEX `idx_dept_id` (`dept_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程表';

-- 4. 选课表
CREATE TABLE `sys_course_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `semester` varchar(20) NOT NULL COMMENT '学期',
  `role` tinyint NOT NULL COMMENT '角色：1-学生，2-教师',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_user` (`course_id`,`user_id`,`semester`),
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='选课表';

-- 5. 系统配置表
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` varchar(500) DEFAULT NULL COMMENT '配置值',
  `description` varchar(255) DEFAULT NULL COMMENT '配置描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统配置表';

-- 6. RAG资料表
CREATE TABLE `sys_knowledge_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `file_name` varchar(100) NOT NULL COMMENT '文件名',
  `file_url` varchar(255) NOT NULL COMMENT '文件URL',
  `dify_document_id` varchar(100) DEFAULT NULL COMMENT 'Dify返回的文档ID',
  `status` tinyint DEFAULT '0' COMMENT '状态：0-上传中，1-索引中，2-可用，3-失败',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_dify_document_id` (`dify_document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='RAG资料表';

-- 7. 题库表
CREATE TABLE `exam_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `type` tinyint NOT NULL COMMENT '题目类型：1-单选，2-多选，3-判断，4-简答',
  `content` text NOT NULL COMMENT '题目内容',
  `image_url` varchar(255) DEFAULT NULL COMMENT '图片URL',
  `options` json DEFAULT NULL COMMENT '选项：JSON格式',
  `answer` text NOT NULL COMMENT '答案',
  `analysis` text COMMENT '解析',
  `difficulty` tinyint DEFAULT '1' COMMENT '难度：1-简单，2-中等，3-困难',
  `tags` json DEFAULT NULL COMMENT '标签：JSON格式',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_type` (`type`),
  INDEX `idx_difficulty` (`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='题库表';

-- 8. AI任务记录表
CREATE TABLE `sys_ai_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称',
  `type` varchar(50) NOT NULL COMMENT '任务类型',
  `total_count` int DEFAULT '0' COMMENT '总数量',
  `current_count` int DEFAULT '0' COMMENT '当前完成数量',
  `status` tinyint DEFAULT '0' COMMENT '状态：0-待执行，1-执行中，2-完成，3-失败',
  `error_msg` text COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  INDEX `idx_type` (`type`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI任务记录表';

-- 9. 试卷模板表
CREATE TABLE `exam_paper` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `title` varchar(100) NOT NULL COMMENT '试卷标题',
  `total_score` decimal(5,1) NOT NULL COMMENT '总分',
  `pass_score` decimal(5,1) NOT NULL COMMENT '及格分数',
  `duration` int NOT NULL COMMENT '考试时长（分钟）',
  `difficulty` tinyint DEFAULT '2' COMMENT '难度：1-简单，2-中等，3-困难',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_difficulty` (`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='试卷模板表';

-- 10. 试卷题目关联表
CREATE TABLE `exam_paper_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `paper_id` bigint NOT NULL COMMENT '试卷ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `score` decimal(5,1) NOT NULL COMMENT '题目分数',
  `sort_order` int NOT NULL COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paper_question` (`paper_id`,`question_id`),
  INDEX `idx_paper_id` (`paper_id`),
  INDEX `idx_question_id` (`question_id`),
  INDEX `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='试卷题目关联表';

-- 11. 考试发布/场次表
CREATE TABLE `exam_publish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `paper_id` bigint NOT NULL COMMENT '试卷ID',
  `title` varchar(100) NOT NULL COMMENT '考试标题',
  `target_dept_ids` json DEFAULT NULL COMMENT '目标部门/班级ID列表：JSON格式',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `limit_count` int DEFAULT '-1' COMMENT '限制人数：-1表示无限制',
  `password` varchar(20) DEFAULT NULL COMMENT '考试密码',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-未发布，1-已发布，2-已结束',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  INDEX `idx_paper_id` (`paper_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_start_time` (`start_time`),
  INDEX `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试发布/场次表';

-- 12. 考试记录表
CREATE TABLE `exam_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `publish_id` bigint NOT NULL COMMENT '考试发布ID',
  `paper_id` bigint NOT NULL COMMENT '试卷ID',
  `total_score` decimal(5,1) DEFAULT '0.0' COMMENT '总得分',
  `user_ip` varchar(50) DEFAULT NULL COMMENT '用户IP',
  `user_device` varchar(255) DEFAULT NULL COMMENT '用户设备',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `status` tinyint DEFAULT '0' COMMENT '状态：0-未开始，1-进行中，2-已提交，3-已过期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_publish` (`user_id`,`publish_id`),
  INDEX `idx_publish_id` (`publish_id`),
  INDEX `idx_paper_id` (`paper_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_start_time` (`start_time`),
  INDEX `idx_submit_time` (`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试记录表';

-- 13. 答题明细表
CREATE TABLE `exam_record_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_id` bigint NOT NULL COMMENT '考试记录ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `student_answer` text COMMENT '学生答案',
  `score` decimal(5,1) DEFAULT '0.0' COMMENT '得分',
  `max_score` decimal(5,1) NOT NULL COMMENT '满分',
  `ai_comment` text COMMENT 'AI评语',
  `is_correct` tinyint DEFAULT '0' COMMENT '是否正确：0-错误，1-正确',
  `is_marked` tinyint DEFAULT '0' COMMENT '是否已批改：0-未批改，1-已批改',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_question` (`record_id`,`question_id`),
  INDEX `idx_record_id` (`record_id`),
  INDEX `idx_question_id` (`question_id`),
  INDEX `idx_is_marked` (`is_marked`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='答题明细表';

-- 14. 监考日志表
CREATE TABLE `exam_proctor_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `record_id` bigint NOT NULL COMMENT '考试记录ID',
  `action_type` varchar(50) NOT NULL COMMENT '动作类型',
  `content` text COMMENT '日志内容',
  `img_snapshot` varchar(255) DEFAULT NULL COMMENT '图片快照URL',
  `happen_time` datetime NOT NULL COMMENT '发生时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  INDEX `idx_record_id` (`record_id`),
  INDEX `idx_action_type` (`action_type`),
  INDEX `idx_happen_time` (`happen_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='监考日志表';

-- 15. 错题本
CREATE TABLE `exam_mistake_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `wrong_count` int DEFAULT '1' COMMENT '错误次数',
  `last_wrong_answer` text COMMENT '最后一次错误答案',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`,`question_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_course_id` (`course_id`),
  INDEX `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='错题本';

-- 16. 系统通知表
CREATE TABLE `sys_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(100) NOT NULL COMMENT '通知标题',
  `content` text NOT NULL COMMENT '通知内容',
  `type` tinyint NOT NULL COMMENT '通知类型：1-系统通知，2-考试通知，3-成绩通知',
  `target_type` tinyint NOT NULL COMMENT '目标类型：1-全体用户，2-指定角色，3-指定用户',
  `sender_id` bigint DEFAULT NULL COMMENT '发送人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  INDEX `idx_type` (`type`),
  INDEX `idx_target_type` (`target_type`),
  INDEX `idx_sender_id` (`sender_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统通知表';

-- 17. 用户消息状态表
CREATE TABLE `sys_user_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `notice_id` bigint NOT NULL COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读：0-未读，1-已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '删除标识：0-正常，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`,`user_id`),
  INDEX `idx_notice_id` (`notice_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_is_read` (`is_read`),
  INDEX `idx_read_time` (`read_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户消息状态表';

-- 插入系统配置预设数据
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`, `create_by`, `update_by`) VALUES
('dify_base_url', 'https://api.dify.ai/v1', 'Dify API 基础地址', 1, 1),
('dify_key_knowledge', 'sk-******', '知识库专用 Key', 1, 1),
('dify_key_generation', 'sk-******', '出题应用 Key', 1, 1),
('dify_key_grading', 'sk-******', '阅卷应用 Key', 1, 1),
('dify_global_dataset_id', 'ds-******', '全局知识库 Dataset ID', 1, 1);