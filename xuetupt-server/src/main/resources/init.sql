-- 创建数据库
CREATE DATABASE IF NOT EXISTS xuetupt DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xuetupt;

-- ==================== 用户表 ====================
DROP TABLE IF EXISTS tb_user;
CREATE TABLE tb_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    nick_name VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    icon VARCHAR(255) DEFAULT NULL COMMENT '头像',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ==================== 教师表 ====================
DROP TABLE IF EXISTS tb_teacher;
CREATE TABLE tb_teacher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '教师姓名',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
    title VARCHAR(100) DEFAULT NULL COMMENT '职称',
    subject VARCHAR(50) DEFAULT NULL COMMENT '学科',
    description TEXT COMMENT '简介',
    score DECIMAL(3,1) DEFAULT 0.0 COMMENT '评分',
    course_count INT DEFAULT 0 COMMENT '课程数',
    student_count INT DEFAULT 0 COMMENT '学生数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师表';

-- ==================== 课程表 ====================
DROP TABLE IF EXISTS tb_course;
CREATE TABLE tb_course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    title VARCHAR(200) NOT NULL COMMENT '课程标题',
    subtitle VARCHAR(500) DEFAULT NULL COMMENT '副标题',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    stock INT DEFAULT 0 COMMENT '库存（秒杀用）',
    type INT DEFAULT 0 COMMENT '课程类型',
    subject VARCHAR(50) DEFAULT NULL COMMENT '学科',
    grade VARCHAR(50) DEFAULT NULL COMMENT '年级',
    begin_time DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    status INT DEFAULT 0 COMMENT '状态 0-未发布 1-已发布',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- ==================== 课程订单表 ====================
DROP TABLE IF EXISTS tb_course_order;
CREATE TABLE tb_course_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    status INT DEFAULT 0 COMMENT '状态 0-未支付 1-已支付 2-已取消',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程订单表';

-- ==================== 自习室表 ====================
DROP TABLE IF EXISTS tb_study_room;
CREATE TABLE tb_study_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '自习室名称',
    subject VARCHAR(50) DEFAULT NULL COMMENT '学科分类',
    max_capacity INT DEFAULT 0 COMMENT '最大容量',
    current_count INT DEFAULT 0 COMMENT '当前人数',
    status INT DEFAULT 0 COMMENT '状态 0-关闭 1-开放',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自习室表';

-- ==================== 专注记录表 ====================
DROP TABLE IF EXISTS tb_focus_record;
CREATE TABLE tb_focus_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    room_id BIGINT DEFAULT NULL COMMENT '自习室ID',
    focus_date DATE NOT NULL COMMENT '专注日期',
    duration_minutes INT DEFAULT 0 COMMENT '专注时长（分钟）',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专注记录表';

-- ==================== 资料表 ====================
DROP TABLE IF EXISTS tb_material;
CREATE TABLE tb_material (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '资料标题',
    description TEXT COMMENT '描述',
    cover_image VARCHAR(255) DEFAULT NULL COMMENT '封面图',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    stock INT DEFAULT 0 COMMENT '库存',
    type INT DEFAULT 0 COMMENT '资料类型',
    subject VARCHAR(50) DEFAULT NULL COMMENT '学科',
    grade VARCHAR(50) DEFAULT NULL COMMENT '年级',
    file_url VARCHAR(500) DEFAULT NULL COMMENT '文件地址',
    preview_images TEXT COMMENT '预览图（JSON数组）',
    status INT DEFAULT 0 COMMENT '状态 0-下架 1-上架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料表';

-- ==================== 测试数据 ====================

-- 用户
INSERT INTO tb_user (phone, password, nick_name, icon) VALUES
('13800138000', 'e10adc3949ba59abbe56e057f20f883e', '张三', 'https://api.dicebear.com/7.x/avataaars/svg?seed=zhangsan'),
('13800138001', 'e10adc3949ba59abbe56e057f20f883e', '李四', 'https://api.dicebear.com/7.x/avataaars/svg?seed=lisi'),
('13800138002', 'e10adc3949ba59abbe56e057f20f883e', '王五', 'https://api.dicebear.com/7.x/avataaars/svg?seed=wangwu');
-- 密码都是 123456 (MD5)

-- 教师
INSERT INTO tb_teacher (name, avatar, title, subject, description, score, course_count, student_count) VALUES
('张老师', 'https://api.dicebear.com/7.x/avataaars/svg?seed=teacher1', '高级教师', '数学', '拥有10年高中数学教学经验，擅长启发式教学', 4.8, 5, 1200),
('李老师', 'https://api.dicebear.com/7.x/avataaars/svg?seed=teacher2', '特级教师', '英语', '英语专业八级，多年雅思托福教学经验', 4.9, 8, 2500),
('王老师', 'https://api.dicebear.com/7.x/avataaars/svg?seed=teacher3', '骨干教师', '物理', '物理竞赛金牌教练，培养多名竞赛获奖学生', 4.7, 3, 800);

-- 课程
INSERT INTO tb_course (teacher_id, title, subtitle, price, stock, type, subject, grade, begin_time, end_time, status) VALUES
(1, '高中数学基础班', '系统学习高中数学核心知识点', 199.00, 100, 1, '数学', '高一', '2026-06-01 09:00:00', '2026-08-31 18:00:00', 1),
(1, '高考数学冲刺班', '针对高考数学重难点突破', 399.00, 50, 1, '数学', '高三', '2026-05-15 09:00:00', '2026-06-06 18:00:00', 1),
(2, '英语语法精讲', '从零开始掌握英语语法', 299.00, 80, 1, '英语', '初中', '2026-06-01 14:00:00', '2026-07-31 16:00:00', 1),
(2, '雅思口语训练营', '外教一对一模拟训练', 999.00, 30, 1, '英语', '高中', '2026-06-10 10:00:00', '2026-07-10 12:00:00', 1),
(3, '初中物理实验课', '动手实验理解物理原理', 249.00, 60, 1, '物理', '初二', '2026-07-01 09:00:00', '2026-08-15 17:00:00', 1);

-- 自习室
INSERT INTO tb_study_room (name, subject, max_capacity, current_count, status) VALUES
('数学自习室', '数学', 50, 12, 1),
('英语自习室', '英语', 40, 8, 1),
('物理自习室', '物理', 30, 5, 1),
('化学自习室', '化学', 30, 3, 1),
('综合自习室', NULL, 100, 25, 1);

-- 资料
INSERT INTO tb_material (title, description, cover_image, price, stock, type, subject, grade, file_url, status) VALUES
('高中数学公式大全', '涵盖高中数学所有公式和定理', 'https://picsum.photos/seed/material1/400/300', 29.90, 500, 1, '数学', '高中', '/files/math_formulas.pdf', 1),
('高考英语词汇手册', '高考必备3500词详解', 'https://picsum.photos/seed/material2/400/300', 39.90, 300, 1, '英语', '高三', '/files/english_vocab.pdf', 1),
('初中物理习题集', '精选100道经典物理题', 'https://picsum.photos/seed/material3/400/300', 19.90, 200, 1, '物理', '初中', '/files/physics_exercises.pdf', 1);
