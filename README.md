# 在线学习系统 — 题目管理平台

## 进度

见 `TASK.md`

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 后端 | Java + Spring Boot | 17 / 3.2.5 |
| ORM | MyBatis-Plus | 3.5.6 |
| 数据库 | MySQL | 8.0 |
| 构建 | Maven | 3.6+ |
| 前端 | Vue 3 + Element Plus | 3.4 / 2.7 |
| 构建 | Vite | 5.2 |

## 环境准备

### 1. 安装依赖软件
- **JDK 17** — https://adoptium.net/
- **Maven 3.6+** — https://maven.apache.org/
- **MySQL 8.0** — https://dev.mysql.com/downloads/
- **Node.js 18+** — https://nodejs.org/

### 2. 创建数据库
在 MySQL 中执行 `schema.sql`：
```bash
mysql -u root -p < schema.sql
```

### 3. 修改数据库密码（如需要）
编辑 `backend/src/main/resources/application.yml`，将 `password: 123456` 改为你的 MySQL 密码。

## 启动后端

```bash
cd backend
mvn spring-boot:run
```
后端运行在 http://localhost:8080

## 启动前端

```bash
cd frontend
npm install
npm run dev
```
前端运行在 http://localhost:5173，代理 /api 到后端 8080。

## 已实现的子系统

### 子系统1：题目管理
- 教师登录（选择教师身份）
- 题目 CRUD（增删改查）
- 支持 4 种题型：单选题、多选题、填空题、主观题（贴图）
- 贴图上传

### 子系统2：题目性质管理
- 章节管理（树形结构）
- 知识点管理
- 题目属性编辑（难度、章节、知识点、出错率、易错点）

## 新增子系统指南

### 后端新增步骤
1. 在 `entity/` 下新建实体类
2. 在 `mapper/` 下新建 Mapper 接口
3. 在 `service/` 和 `service/impl/` 下新建 Service 接口与实现
4. 在 `controller/` 下新建 Controller（路径统一用 `/api/xxx`）
5. 在 `schema.sql` 末尾追加建表语句

### 前端新增步骤
1. 在 `api/` 下新建 API 模块文件（参照已有的 `question.js` 等）
2. 在 `views/` 下新建页面组件
3. 在 `router/index.js` 中追加路由
4. 在 `components/AppLayout.vue` 中追加菜单项

### 注意
- 公共文件（router、AppLayout、schema.sql）多人修改后合并需手动处理冲突
- 建议各自子系统建表语句写在独立 SQL 文件中，最后统一合并到 schema.sql
- 后端已有策略模式处理多题型，扩展新题型只需新增 Strategy 实现类
