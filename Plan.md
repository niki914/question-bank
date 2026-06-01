# 手动组卷子系统 -- 设计与实现计划

## 一、需求分析

### 1.1 原始需求（TASK.md 第 4 项）
> 手动组卷，出题时设置总分、难易度和题目类型，在题库中手动选择每个题型的题目并设置分值，
> 要求提交时自动检验所有的题目基本覆盖了大部分的知识点(85%以上)以及题目难度符合要求，
> 成卷后自动计算总分。

### 1.2 设计原则
- **完全解耦**：子系统只拥有自己的两张表（`exam_paper`、`exam_paper_question`），只读已有表，不改已有表
- **遵循现有模式**：Controller/Service/Mapper/Entity 分层，路径统一 `/api/xxx`
- **可答辩**：架构清晰、算法可解释、有设计亮点

---

## 二、环境配置（使用 Homebrew）

当前设备缺少 **Maven** 和 **MySQL**，通过 Homebrew 安装：

```bash
# 安装 Maven
brew install maven

# 安装 MySQL 8.0
brew install mysql

# 启动 MySQL
brew services start mysql

# 初始化 MySQL（如需要）
mysql_secure_installation

# 导入数据库
mysql -u root -p < /Users/bytedance/IdeaProjects/question-bank/schema.sql

# 如需后续清理
brew services stop mysql
brew uninstall mysql
```

> 注：JDK 17 和 Node.js 已就绪，无需安装。

---

## 三、数据库设计

### 3.1 新增表 DDL

```sql
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
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_paper (paper_id),
    INDEX idx_question (question_id),
    FOREIGN KEY (paper_id) REFERENCES exam_paper(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES question(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目关联表';
```

### 3.2 设计决策

| 决策 | 理由 |
|------|------|
| `total_score` vs `actual_total_score` | 预期总分 vs 实际总分，便于对比校验 |
| `question_type_distribution` 存 JSON | 灵活存储各题型目标数量，不新增关联表 |
| `validation_result` 存 JSON | 存储校验详情，便于前端展示 |
| `ON DELETE CASCADE` | 删除试卷自动清理关联题目 |
| `DRAFT/SUBMITTED` 两阶段 | 支持先保存草稿再校验提交 |

---

## 四、后端设计

### 4.1 文件结构

```
backend/src/main/java/com/example/questionbank/
├── entity/
│   ├── ExamPaper.java              (新建)
│   └── ExamPaperQuestion.java      (新建)
├── mapper/
│   ├── ExamPaperMapper.java        (新建)
│   └── ExamPaperQuestionMapper.java(新建)
├── dto/
│   ├── PaperCreateDTO.java         (新建)
│   ├── PaperQuestionItem.java      (新建)
│   ├── PaperVO.java                (新建)
│   ├── PaperQuestionDetail.java    (新建)
│   └── PaperValidationResult.java  (新建)
├── service/
│   ├── ExamPaperService.java       (新建)
│   └── impl/
│       └── ExamPaperServiceImpl.java (新建) ← 核心校验逻辑
└── controller/
    └── ExamPaperController.java    (新建)
```

### 4.2 REST API 端点

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/papers` | 创建试卷（草稿） |
| PUT | `/api/papers/{id}` | 更新试卷 |
| DELETE | `/api/papers/{id}` | 删除试卷 |
| GET | `/api/papers/{id}` | 获取试卷详情（含题目列表） |
| GET | `/api/papers` | 分页查询（teacherId, page, size, status） |
| POST | `/api/papers/{id}/validate` | 校验试卷 |
| POST | `/api/papers/{id}/submit` | 提交试卷（校验通过后改状态） |

### 4.3 核心校验逻辑（ExamPaperServiceImpl）

#### 校验 1：知识点覆盖率 ≥ 85%
```
1. totalKPs = 所有知识点数量（SELECT * FROM knowledge_point）
2. coveredKPIds = 所有选中题目关联的知识点并集
   - 优先用 question_knowledge_point 关联表
   - 降级用 question.knowledge_points 逗号字符串
3. coverage = coveredKPIds.size / totalKPs.size * 100
4. 判定: coverage >= 85 ? 通过 : 不通过
```

#### 校验 2：难度匹配
```
1. 难度映射: EASY=1, MEDIUM=2, HARD=3
2. targetValue = 目标难度对应值
3. avgDiff = sum(所有题目难度值) / 题目数
4. 判定: |avgDiff - targetValue| <= 0.5 ? 通过 : 不通过
```

#### 校验 3：总分匹配
```
actualTotal = sum(每道题设置的 questionScore)
判定: actualTotal == totalScore ? 通过 : 不通过
```

### 4.4 依赖关系

```
ExamPaperController
    └── ExamPaperService
         ├── ExamPaperMapper         (读写 exam_paper)
         ├── ExamPaperQuestionMapper (读写 exam_paper_question)
         ├── QuestionMapper          (只读 question)
         ├── KnowledgePointMapper    (只读 knowledge_point)
         ├── QuestionKnowledgePointMapper (只读关联表)
         └── TeacherMapper           (只读 teacher)
```

---

## 五、前端设计

### 5.1 文件结构

```
frontend/src/
├── api/
│   └── paper.js                    (新建)
├── views/
│   ├── PaperList.vue               (新建)  -- 试卷列表
│   ├── PaperCreate.vue             (新建)  -- 新建试卷（核心页面）
│   ├── PaperDetail.vue             (新建)  -- 试卷详情
│   └── PaperEdit.vue               (新建)  -- 编辑试卷
├── router/
│   └── index.js                    (修改)  -- 新增4条路由
└── components/
    └── AppLayout.vue               (修改)  -- 新增"手动组卷"菜单
```

### 5.2 PaperCreate.vue 交互流程（核心页面）

```
+--------------------------------------------------------------+
| [返回]  新建试卷                                              |
+--------------------------------------------------------------+
|  基本信息                                                     |
|  试卷名称: [________]  目标难度: [下拉]  总分: [____]          |
+--------------------------------------------------------------+
|  左栏: 题目选择器              |  右栏: 试卷预览（已选题目）     |
|  [单选][多选][填空][主观][全部]|                                |
|  [简单][中等][困难][全部]      |  #1 单选题  分值:[5] [删除]    |
|  章节: [下拉]                  |  #2 填空题  分值:[10] [删除]   |
|  搜索: [____] [搜索]           |  #3 主观题  分值:[15] [删除]   |
|                                |                                |
|  ID | 标题 | 题型 | 难度 | [+]  |  实际总分: 30                 |
|                                |                                |
|                                |  [保存草稿]  [校验并提交]       |
+--------------------------------------------------------------+
```

### 5.3 校验结果弹窗

- 知识点覆盖率：`XX% / 85%` + 通过/不通过标签
- 未覆盖知识点：以 tag 列表展示（如有）
- 难度匹配：详情说明 + 通过/不通过标签
- 总分匹配：`实际: XX / 预期: XX` + 通过/不通过标签

---

## 六、实施步骤

### Phase 1: 环境配置
1. `brew install maven`
2. `brew install mysql && brew services start mysql`
3. `mysql -u root -p < schema.sql`（执行建表脚本）

### Phase 2: 数据库
4. 在 `schema.sql` 末尾追加 exam_paper 和 exam_paper_question 的 DDL
5. 执行追加的 SQL

### Phase 3: 后端 Entity + Mapper
6. 创建 `ExamPaper.java` 和 `ExamPaperQuestion.java`
7. 创建 `ExamPaperMapper.java` 和 `ExamPaperQuestionMapper.java`

### Phase 4: 后端 DTO/VO
8. 创建 `PaperCreateDTO.java`、`PaperQuestionItem.java`
9. 创建 `PaperVO.java`、`PaperQuestionDetail.java`
10. 创建 `PaperValidationResult.java`

### Phase 5: 后端 Service（核心）
11. 创建 `ExamPaperService.java` 接口
12. 创建 `ExamPaperServiceImpl.java` -- 实现 CRUD + 校验逻辑

### Phase 6: 后端 Controller
13. 创建 `ExamPaperController.java`

### Phase 7: 前端 API + 路由
14. 创建 `paper.js`
15. 修改 `router/index.js` 新增4条路由
16. 修改 `AppLayout.vue` 新增"手动组卷"菜单

### Phase 8: 前端页面
17. 创建 `PaperList.vue`
18. 创建 `PaperCreate.vue`（核心，最复杂）
19. 创建 `PaperDetail.vue`
20. 创建 `PaperEdit.vue`

### Phase 9: 联调测试
21. 端到端测试：创建 → 添加题目 → 校验 → 提交
22. 边界测试：覆盖率不足、难度不匹配、总分不一致

---

## 七、验证方式

### 7.1 后端验证
```bash
cd backend && mvn spring-boot:run
# 访问 http://localhost:8080
# 用 curl 或 Postman 测试 /api/papers 各端点
```

### 7.2 前端验证
```bash
cd frontend && npm run dev
# 访问 http://localhost:5173
# 登录教师 -> 手动组卷菜单 -> 新建试卷 -> 完整流程
```

### 7.3 测试用例
| 场景 | 预期结果 |
|------|---------|
| 知识点覆盖率 >= 85% | 校验通过 |
| 知识点覆盖率 < 85% | 校验失败，显示未覆盖知识点 |
| 难度匹配（偏差 <= 0.5） | 校验通过 |
| 难度不匹配（偏差 > 0.5） | 校验失败 |
| 实际总分 == 预期总分 | 校验通过 |
| 实际总分 != 预期总分 | 校验失败 |
| 所有校验通过 + 提交 | 状态变更为 SUBMITTED |

---

## 八、答辩亮点

| 亮点 | 说明 |
|------|------|
| **完全解耦** | 子系统独立表结构，不修改已有表，通过只读方式复用题库数据 |
| **双重知识点数据源** | 优先关联表、降级逗号字符串，健壮性好 |
| **草稿/提交两阶段** | 支持先保存草稿再校验提交，符合真实场景 |
| **校验算法可解释** | 覆盖率用集合运算、难度用数值化加权平均、总分精确匹配 |
| **双栏交互设计** | 左选题库右组卷，直观高效 |
| **校验结果可视化** | 弹窗展示各项结果，标红未通过项 |
