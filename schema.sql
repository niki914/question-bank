-- 创建数据库
CREATE DATABASE IF NOT EXISTS question_bank DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE question_bank;

-- 教师表
CREATE TABLE IF NOT EXISTS teacher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '教师姓名',
    department VARCHAR(100) COMMENT '院系',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师表';

-- 题目表
CREATE TABLE IF NOT EXISTS question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL COMMENT '创建教师ID',
    type VARCHAR(30) NOT NULL COMMENT '题型: SINGLE_CHOICE/MULTIPLE_CHOICE/FILL_BLANK/SUBJECTIVE',
    title TEXT NOT NULL COMMENT '题目标题',
    content_json JSON NOT NULL COMMENT '题型专用数据',
    difficulty VARCHAR(10) NOT NULL COMMENT '难度: EASY/MEDIUM/HARD',
    chapter VARCHAR(100) COMMENT '所属章节',
    knowledge_points VARCHAR(500) COMMENT '知识点(逗号分隔)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_teacher (teacher_id),
    INDEX idx_type (type),
    INDEX idx_difficulty (difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 附件表
CREATE TABLE IF NOT EXISTS question_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT COMMENT '关联题目ID',
    file_name VARCHAR(255) COMMENT '原始文件名',
    file_path VARCHAR(500) COMMENT '服务器存储路径',
    file_type VARCHAR(20) DEFAULT 'QUESTION_IMAGE' COMMENT 'QUESTION_IMAGE/ANSWER_IMAGE',
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    INDEX idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件表';

-- 章节表（树形结构）
CREATE TABLE IF NOT EXISTS course_chapter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0 COMMENT '父章节ID, 0表示根节点',
    name VARCHAR(100) NOT NULL COMMENT '章节名称',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程章节表';

-- 知识点表
CREATE TABLE IF NOT EXISTS knowledge_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_id BIGINT NOT NULL COMMENT '所属章节ID',
    name VARCHAR(100) NOT NULL COMMENT '知识点名称',
    description VARCHAR(500) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_chapter (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点表';

-- 题目-知识点关联表
CREATE TABLE IF NOT EXISTS question_knowledge_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL COMMENT '题目ID',
    knowledge_point_id BIGINT NOT NULL COMMENT '知识点ID',
    INDEX idx_question (question_id),
    INDEX idx_knowledge_point (knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目-知识点关联表';

-- 试卷表
CREATE TABLE IF NOT EXISTS exam_paper (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL COMMENT '创建教师ID',
    paper_name VARCHAR(200) NOT NULL COMMENT '试卷名称',
    total_score DECIMAL(5,2) NOT NULL DEFAULT 100.00 COMMENT '预期总分',
    difficulty VARCHAR(10) NOT NULL COMMENT '目标难度: EASY/MEDIUM/HARD',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED',
    question_type_distribution JSON COMMENT '题型分布配置',
    knowledge_point_coverage DECIMAL(5,2) COMMENT '知识点覆盖率(%)',
    actual_total_score DECIMAL(5,2) DEFAULT 0.00 COMMENT '实际计算总分',
    validation_result JSON COMMENT '校验结果详情',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_teacher (teacher_id),
    INDEX idx_status (status),
    FOREIGN KEY (teacher_id) REFERENCES teacher(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 试卷题目关联表
CREATE TABLE IF NOT EXISTS exam_paper_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paper_id BIGINT NOT NULL COMMENT '试卷ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    question_score DECIMAL(5,2) NOT NULL COMMENT '该题分值',
    question_order INT NOT NULL COMMENT '题目顺序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_paper (paper_id),
    INDEX idx_question (question_id),
    FOREIGN KEY (paper_id) REFERENCES exam_paper(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES question(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目关联表';

-- 种子数据：教师
INSERT INTO teacher (name, department) VALUES
('欧毓毅', '计算机学院'),
('张老师', '计算机学院'),
('李老师', '计算机学院');
