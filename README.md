# 智能考试系统 (Smart Exam System)

<p align="center">
  <b>一套覆盖用户管理、题库建设、智能组卷、在线考试、AI阅卷及数据分析的全流程智能考试平台</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.5-4fc08d" alt="Vue 3">
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

---

## 目录

- [项目概述](#项目概述)
- [核心功能](#核心功能)
- [技术架构](#技术架构)
- [环境配置](#环境配置)
- [快速开始](#快速开始)
- [API接口文档](#api接口文档)
- [项目结构](#项目结构)
- [开发规范](#开发规范)
- [贡献指南](#贡献指南)
- [常见问题](#常见问题)
- [版权信息](#版权信息)

---

## 项目概述

智能考试系统是一个面向高校的在线考试解决方案，旨在解决传统考试模式中人工出题耗时、考试组织繁琐、阅卷效率低下等问题。系统支持学生、教师、管理员三类角色协同工作，实现考试业务数字化、智能化。

### 系统特色

- **AI辅助出题**：基于大语言模型，根据知识点自动生成高质量题目
- **智能组卷**：支持手动组卷和随机组卷，可按难度、题型等条件智能抽题
- **AI智能阅卷**：主观题支持AI辅助批改，提供评分依据和评语
- **在线监考**：实时监控考生状态，支持切屏检测、异常行为记录
- **数据分析**：多维度成绩分析，知识点掌握度雷达图，错题TOP排行
- **错题本**：自动收录错题，支持知识点归类和复习标记

---

## 核心功能

### 用户角色功能

| 角色 | 功能模块 |
|------|----------|
| **学生** | 仪表盘、我的考试、在线答题、成绩查询、错题本、消息通知 |
| **教师** | 工作台、知识库管理、智能题库、试卷管理、智能组卷、考试发布、AI辅助阅卷、成绩统计、在线监考、课程管理 |
| **管理员** | 用户管理、组织架构、课程管理、系统设置 |

### 功能模块详解

#### 1. 题库管理
- 支持单选题、多选题、判断题、简答题、填空题
- 题目难度分级（简单/中等/困难）
- 知识点标签管理
- Excel批量导入/导出
- AI辅助出题（基于Dify平台）
- 题目查重检测

#### 2. 试卷管理
- **手动组卷**：教师自主选择题目，设置分值
- **智能组卷**：按题型比例、难度分布自动抽题
- 试卷预览与编辑
- 试卷状态管理（草稿/发布）

#### 3. 考试发布
- 设置考试时间窗口
- 指定参考班级/部门
- 考试密码保护
- 防作弊配置（切屏限制）

#### 4. 在线考试
- 实时倒计时提醒
- 答案自动保存
- 全屏模式防作弊
- 切屏次数监控
- 强制交卷保护

#### 5. 阅卷系统
- 客观题自动批改
- 主观题AI辅助评分
- 教师人工复核
- 批改进度实时推送

#### 6. 数据分析
- 班级成绩统计（平均分、及格率、最高/最低分）
- 分数段分布图
- 知识点掌握度雷达图
- 错题TOP10排行榜
- 学生个人成长曲线

---

## 技术架构

### 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         前端层 (Frontend)                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   Vue 3     │  │ Element Plus│  │  ECharts    │              │
│  │ Composition │  │   UI组件库   │  │  图表组件   │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   Pinia     │  │ Vue Router  │  │Tailwind CSS │              │
│  │  状态管理   │  │   路由管理   │  │  样式框架   │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        网关层 (Gateway)                          │
│                    Vite Dev Server / Nginx                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        后端层 (Backend)                          │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    Spring Boot 3.3.4                     │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │    │
│  │  │Controller│ │ Service  │ │  Mapper  │ │  Entity  │    │    │
│  │  │   控制器  │ │  服务层   │ │ 数据访问  │ │  实体类  │    │    │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘    │    │
│  └─────────────────────────────────────────────────────────┘    │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐             │
│  │Spring Security│ │   WebSocket  │ │    Redis     │             │
│  │   安全认证    │ │   实时通信    │ │   缓存服务   │             │
│  └──────────────┘ └──────────────┘ └──────────────┘             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        数据层 (Data)                             │
│  ┌──────────────┐                    ┌──────────────┐           │
│  │   MySQL 8    │                    │    Redis     │           │
│  │  关系数据库   │                    │   缓存数据库  │           │
│  └──────────────┘                    └──────────────┘           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      外部服务 (External)                         │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    Dify AI Platform                       │   │
│  │         (AI出题 / AI阅卷 / RAG知识库检索)                  │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 技术栈详情

#### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.3.4 | 基础框架 |
| Spring Security | - | 安全认证框架 |
| MyBatis-Plus | 3.5.7 | ORM框架 |
| MySQL | 8.0 | 关系数据库 |
| Redis | - | 缓存数据库 |
| JWT | 0.12.6 | Token认证 |
| WebSocket | - | 实时通信 |
| EasyExcel | 4.0.3 | Excel处理 |
| Hutool | 5.8.31 | 工具类库 |
| Lombok | - | 代码简化 |

#### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5 | 前端框架 |
| Vite | 6.0 | 构建工具 |
| TypeScript | 5.6 | 类型支持 |
| Element Plus | 2.12 | UI组件库 |
| Tailwind CSS | 4.0 | CSS框架 |
| Pinia | 3.0 | 状态管理 |
| Vue Router | 4.6 | 路由管理 |
| ECharts | 6.0 | 图表库 |
| Axios | 1.13 | HTTP客户端 |

---

## 环境配置

### 系统要求

- **JDK**: 17 或更高版本
- **Node.js**: 18.x 或更高版本
- **MySQL**: 8.0 或更高版本
- **Redis**: 6.0 或更高版本
- **Maven**: 3.8+

### 开发环境搭建

#### 1. 克隆项目

```bash
git clone <repository-url>
cd Exam
```

#### 2. 数据库配置

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE exam_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

# 导入表结构
mysql -u root -p exam_system < create_tables.sql

# (可选) 导入测试数据
mysql -u root -p exam_system < test_data.sql
```

#### 3. 后端配置

编辑 `Backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/exam_system?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8&allowPublicKeyRetrieval=true
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
      password: # 如有密码请填写

jwt:
  secret: your_jwt_secret_key # 请更换为复杂密钥
```

或使用环境变量：

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=exam_system
export DB_USER=root
export DB_PASSWORD=your_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export JWT_SECRET=your_jwt_secret_key
```

#### 4. 启动后端服务

```bash
cd Backend
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

#### 5. 前端配置与启动

```bash
cd smart-exam-web
npm install
npm run dev
```

前端服务将在 `http://localhost:5173` 启动。

#### 6. Dify AI配置（重要）

本系统的AI功能依赖 [Dify](https://dify.ai/) 平台，需要在Dify中创建相应的应用和知识库。

##### 6.1 Dify平台准备

**方式一：使用Dify云服务**
1. 访问 [Dify Cloud](https://cloud.dify.ai/) 注册账号
2. 创建所需的应用和知识库（见下文）

**方式二：私有化部署**
```bash
# 使用Docker Compose部署
git clone https://github.com/langgenius/dify.git
cd dify/docker
docker compose up -d
```
部署完成后访问 `http://localhost` 进入Dify控制台。

##### 6.2 创建Dify应用

系统需要创建以下Dify资源：

| 资源名称 | 类型 | 用途 | 输入参数 | 输出格式 |
|---------|------|------|---------|---------|
| **出题应用** | Workflow | AI智能出题 | `topic`, `count`, `difficulty`, `types`, `course_name` | JSON数组 |
| **阅卷应用** | Workflow | AI主观题评分 | `question`, `standard_answer`, `student_answer`, `max_score` | `{score, comment}` |
| **课程知识库** | Dataset | RAG检索增强 | - | - |
| **历史题库** | Dataset | 题目查重 | - | - |

##### 6.3 出题应用配置

在Dify中创建一个 **Workflow** 类型应用：

**输入变量**：
```json
{
  "topic": "题目主题/知识点",
  "count": "生成数量",
  "difficulty": "难度（简单/中等/困难）",
  "types": "题型（1单选,2多选,3判断,4简答,5填空）",
  "course_name": "课程名称"
}
```

**Prompt示例**：
```
你是一位专业的考试出题专家。请根据以下要求生成{{count}}道{{difficulty}}难度的题目。

课程：{{course_name}}
知识点：{{topic}}
题型：{{types}}

请严格按照以下JSON格式输出，不要包含其他内容：
[
  {
    "type": "1",
    "content": "题目内容",
    "options": ["A. 选项1", "B. 选项2", "C. 选项3", "D. 选项4"],
    "answer": "B",
    "analysis": "答案解析"
  }
]
```

**输出节点**：确保最终输出变量名为 `text` 或 `result`

##### 6.4 阅卷应用配置

在Dify中创建另一个 **Workflow** 类型应用：

**输入变量**：
```json
{
  "question": "题目内容",
  "standard_answer": "标准答案",
  "student_answer": "学生答案",
  "max_score": "满分"
}
```

**Prompt示例**：
```
你是一位公正的阅卷老师。请根据以下信息对学生答案进行评分。

题目：{{question}}
标准答案：{{standard_answer}}
学生答案：{{student_answer}}
满分：{{max_score}}

请严格按照以下JSON格式输出评分结果：
{
  "score": 评分（0到{{max_score}}之间的数字）,
  "comment": "评语（指出得分点和不足之处）"
}

评分标准：
- 完全正确：满分
- 部分正确：根据得分点给分
- 完全错误：0分
```

##### 6.5 知识库配置

**课程知识库**：用于存储课程资料，支持RAG检索
1. 在Dify中创建Dataset，获取Dataset ID
2. 通过系统"知识库管理"页面上传文档
3. 文档会自动同步到Dify知识库

**历史题库**：用于AI出题时查重
1. 创建空Dataset，获取Dataset ID
2. 系统会在AI生成题目后自动同步到该知识库

##### 6.6 配置系统参数

在数据库 `sys_config` 表中配置以下参数：

```sql
-- Dify API基础地址（云服务或私有部署地址）
UPDATE sys_config SET config_value = 'https://api.dify.ai/v1' 
WHERE config_key = 'dify_base_url';

-- 出题应用的API Key（在Dify应用设置中获取）
UPDATE sys_config SET config_value = 'app-xxxxxxxxxxxxxxxx' 
WHERE config_key = 'dify_key_generation';

-- 阅卷应用的API Key
UPDATE sys_config SET config_value = 'app-xxxxxxxxxxxxxxxx' 
WHERE config_key = 'dify_key_grading';

-- 知识库API Key（用于文档上传）
UPDATE sys_config SET config_value = 'dataset-xxxxxxxxxxxxxxxx' 
WHERE config_key = 'dify_key_knowledge';

-- 课程知识库Dataset ID
UPDATE sys_config SET config_value = 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx' 
WHERE config_key = 'dify_global_dataset_id';

-- 历史题库Dataset ID（用于查重，可选）
UPDATE sys_config SET config_value = 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx' 
WHERE config_key = 'dify_history_dataset_id';
```

##### 6.7 Dify API调用流程

```
┌─────────────────────────────────────────────────────────────────┐
│                     AI出题流程                                   │
│                                                                 │
│  前端请求 ──► QuestionController ──► QuestionGenerationService  │
│                                       │                         │
│                                       ▼                         │
│                              DifyClient.runWorkflow()           │
│                                       │                         │
│                                       ▼                         │
│              POST {dify_base_url}/workflows/run                 │
│              Authorization: Bearer {dify_key_generation}        │
│              Body: { inputs: { topic, count, difficulty... } }  │
│                                       │                         │
│                                       ▼                         │
│                              解析返回的JSON题目数组              │
│                              存入数据库 + 同步到查重库           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     AI阅卷流程                                   │
│                                                                 │
│  学生交卷 ──► RecordService.submitExam() ──► (异步)             │
│                                              │                  │
│                                              ▼                  │
│                                    AutoGradingService           │
│                                              │                  │
│                                              ▼                  │
│                              遍历主观题(简答/填空)               │
│                                              │                  │
│                                              ▼                  │
│                     DifyClient.runWorkflow(dify_key_grading)    │
│                                              │                  │
│                                              ▼                  │
│                     解析 { score, comment } 写入数据库           │
│                     状态保持为"待复核"，需教师确认               │
└─────────────────────────────────────────────────────────────────┘
```

##### 6.8 验证配置

启动后端服务后，可通过以下方式验证Dify配置：

```bash
# 查看当前配置
curl http://localhost:8080/api/admin/config/list

# 测试AI出题（需要登录获取token）
curl -X POST http://localhost:8080/api/question/ai-generate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": 1,
    "topic": "Java基础",
    "totalCount": 5,
    "difficulty": "中等",
    "types": [1, 3]
  }'
```

---

## 快速开始

### 默认账号

系统预置了以下测试账号（密码均为 `123456`）：

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
| 教师 | teacher | 123456 |
| 学生 | student | 123456 |

### 基本使用流程

#### 教师端流程

1. 登录系统，进入教师工作台
2. 创建课程并关联学生
3. 上传知识库资料（用于AI出题）
4. 创建题库（手动录入/Excel导入/AI生成）
5. 创建试卷（手动组卷/智能组卷）
6. 发布考试，设置时间、班级等参数
7. 考试期间进行在线监考
8. 考试结束后批改主观题（AI辅助）
9. 查看成绩统计分析

#### 学生端流程

1. 登录系统，进入学生仪表盘
2. 查看待参加的考试列表
3. 在规定时间内参加考试
4. 提交试卷
5. 查看成绩和解析
6. 在错题本中复习错题

---

## API接口文档

### 通用响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 接口分组

#### 认证接口 `/api/auth`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | /api/auth/login | 用户登录 | 公开 |
| GET | /api/auth/info | 获取当前用户信息 | 登录 |
| POST | /api/auth/logout | 用户登出 | 登录 |

**登录请求示例**：

```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

**登录响应示例**：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userInfo": {
      "id": 1,
      "role": 3,
      "deptId": 1,
      "name": "管理员"
    }
  }
}
```

#### 题库接口 `/api/question`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/question/list | 分页查询题目列表 | 登录 |
| POST | /api/question/create | 创建题目 | 教师/管理员 |
| PUT | /api/question/update | 更新题目 | 教师/管理员 |
| DELETE | /api/question/batch | 批量删除题目 | 教师/管理员 |
| GET | /api/question/template | 下载导入模板 | 登录 |
| POST | /api/question/import | Excel导入题目 | 教师/管理员 |
| POST | /api/question/ai-generate | AI生成题目 | 教师/管理员 |
| GET | /api/question/task/list | 查询AI任务列表 | 登录 |

**AI出题请求示例**：

```bash
POST /api/question/ai-generate
Authorization: Bearer <token>
Content-Type: application/json

{
  "courseId": 1,
  "topic": "Spring Boot自动配置原理",
  "totalCount": 10,
  "difficulty": "中等",
  "types": [1, 2, 3]
}
```

#### 试卷接口 `/api/paper`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/paper/list | 分页查询试卷列表 | 登录 |
| GET | /api/paper/{id} | 获取试卷详情 | 登录 |
| GET | /api/paper/{id}/detail | 获取试卷详情（含题目） | 登录 |
| POST | /api/paper/random-create | 智能组卷 | 教师/管理员 |
| POST | /api/paper/manual-create | 手动组卷 | 教师/管理员 |
| DELETE | /api/paper/{id} | 删除试卷 | 教师/管理员 |
| POST | /api/paper/{id}/add-question | 添加题目到试卷 | 教师/管理员 |
| DELETE | /api/paper/{paperId}/question/{questionId} | 从试卷移除题目 | 教师/管理员 |

**智能组卷请求示例**：

```bash
POST /api/paper/random-create
Authorization: Bearer <token>
Content-Type: application/json

{
  "courseId": 1,
  "title": "Spring Boot单元测试",
  "totalScore": 100,
  "passScore": 60,
  "duration": 90,
  "difficulty": 2,
  "questionTypes": [
    {"type": 1, "count": 20, "scorePerQuestion": 2},
    {"type": 2, "count": 10, "scorePerQuestion": 3},
    {"type": 4, "count": 3, "scorePerQuestion": 10}
  ]
}
```

#### 考试发布接口 `/api/exam/publish`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/exam/publish/list | 获取发布列表 | 教师/管理员 |
| POST | /api/exam/publish | 发布考试 | 教师/管理员 |
| DELETE | /api/exam/publish/{id} | 撤销发布 | 教师/管理员 |

**发布考试请求示例**：

```bash
POST /api/exam/publish
Authorization: Bearer <token>
Content-Type: application/json

{
  "paperId": 1,
  "title": "2025年春季学期期末考试",
  "targetDeptIds": [1, 2, 3],
  "startTime": "2025-06-20 09:00:00",
  "endTime": "2025-06-20 11:00:00",
  "password": "exam2025"
}
```

#### 学生考试接口 `/api/exam`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/exam/my-list | 获取我的考试列表 | 学生 |
| POST | /api/exam/verify-password/{publishId} | 验证考试密码 | 学生 |
| POST | /api/exam/start/{publishId} | 开始考试 | 学生 |
| POST | /api/exam/submit | 提交试卷 | 学生 |
| GET | /api/exam/result/{recordId} | 获取考试结果 | 学生 |
| GET | /api/exam/result/publish/{publishId} | 获取最新考试结果 | 学生 |

#### 分析统计接口 `/api/analysis`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/analysis/exam/list | 获取考试场次列表 | 教师/管理员 |
| GET | /api/analysis/exam/{publishId} | 获取考试详细分析 | 教师/管理员 |

#### 错题本接口 `/api/mistake-book`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/mistake-book/list | 获取错题列表 | 学生 |
| DELETE | /api/mistake-book/{id} | 移除错题 | 学生 |

#### 系统管理接口 `/api/admin`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/admin/user/list | 用户列表 | 管理员 |
| POST | /api/admin/user | 创建用户 | 管理员 |
| PUT | /api/admin/user | 更新用户 | 管理员 |
| DELETE | /api/admin/user/{id} | 删除用户 | 管理员 |
| GET | /api/admin/dept/list | 部门列表 | 管理员 |
| POST | /api/admin/dept | 创建部门 | 管理员 |
| GET | /api/admin/course/list | 课程列表 | 管理员 |

### WebSocket接口

#### 监考WebSocket

```
ws://localhost:8080/ws/proctor/{recordId}
```

用于实时推送考生状态、切屏警告等监考信息。

#### 通知WebSocket

```
ws://localhost:8080/ws/notice
```

用于实时推送系统通知、考试提醒等消息。

---

## 项目结构

```
Exam/
├── Backend/                          # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/university/exam/
│   │   │   │   ├── common/           # 公共模块
│   │   │   │   │   ├── dto/          # 数据传输对象
│   │   │   │   │   ├── exception/    # 异常处理
│   │   │   │   │   ├── interceptor/  # 拦截器
│   │   │   │   │   ├── request/      # 请求对象
│   │   │   │   │   ├── result/       # 响应对象
│   │   │   │   │   ├── security/     # 安全组件
│   │   │   │   │   ├── utils/        # 工具类
│   │   │   │   │   └── vo/           # 视图对象
│   │   │   │   ├── config/           # 配置类
│   │   │   │   ├── controller/       # 控制器
│   │   │   │   ├── dto/              # 数据传输对象
│   │   │   │   ├── entity/           # 实体类
│   │   │   │   ├── mapper/           # MyBatis Mapper
│   │   │   │   ├── service/          # 服务层
│   │   │   │   │   └── impl/         # 服务实现
│   │   │   │   ├── websocket/        # WebSocket处理
│   │   │   │   └── ExamSystemApplication.java
│   │   │   └── resources/
│   │   │       ├── mapper/           # MyBatis XML
│   │   │       └── application.yml   # 配置文件
│   │   └── test/                     # 测试代码
│   └── pom.xml                       # Maven配置
│
├── smart-exam-web/                   # 前端项目
│   ├── src/
│   │   ├── components/               # 公共组件
│   │   ├── hooks/                    # 组合式函数
│   │   ├── layout/                   # 布局组件
│   │   ├── router/                   # 路由配置
│   │   ├── store/                    # 状态管理
│   │   ├── utils/                    # 工具函数
│   │   ├── views/                    # 页面组件
│   │   │   ├── admin/                # 管理员页面
│   │   │   ├── common/               # 公共页面
│   │   │   ├── error/                # 错误页面
│   │   │   ├── login/                # 登录页面
│   │   │   ├── profile/              # 个人中心
│   │   │   ├── student/              # 学生页面
│   │   │   └── teacher/              # 教师页面
│   │   ├── App.vue
│   │   └── main.ts
│   ├── public/                       # 静态资源
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts
│
├── uploads/                          # 上传文件目录
├── create_tables.sql                 # 数据库建表脚本
├── test_data.sql                     # 测试数据脚本
└── README.md                         # 项目说明文档
```

---

## 开发规范

### 代码规范

#### 后端规范

- 遵循阿里巴巴Java开发手册
- 使用Lombok简化POJO
- Controller只负责参数校验和结果封装
- Service层处理业务逻辑
- 使用MyBatis-Plus的LambdaQueryWrapper进行查询
- 统一使用Result类封装响应

**命名规范**：
- 类名：大驼峰（PascalCase）
- 方法名：小驼峰（camelCase）
- 常量：全大写下划线分隔（UPPER_SNAKE_CASE）
- 包名：全小写

#### 前端规范

- 使用Vue 3 Composition API
- 使用TypeScript进行类型约束
- 组件命名：大驼峰（PascalCase）
- 文件命名：小驼峰或短横线分隔
- 使用Pinia进行状态管理

### Git提交规范

```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建/工具链相关
```

**示例**：
```
feat: 添加AI辅助阅卷功能
fix: 修复登录token过期问题
docs: 更新API接口文档
```

### 分支管理

- `master`: 主分支，稳定版本
- `develop`: 开发分支
- `feature/*`: 功能分支
- `hotfix/*`: 紧急修复分支

---

## 贡献指南

我们欢迎所有形式的贡献，包括但不限于：

1. **报告Bug**：请通过Issue描述问题，包含复现步骤和环境信息
2. **建议新功能**：通过Issue描述功能需求和使用场景
3. **提交代码**：Fork项目，创建分支，提交Pull Request

### Pull Request流程

1. Fork本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: 添加某某功能'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建Pull Request

### 代码审查

所有PR都需要至少一位维护者审查通过后才能合并。请确保：

- 代码符合项目规范
- 所有测试通过
- 更新相关文档

---

## 常见问题

### Q1: 启动后端报数据库连接错误？

**A**: 请检查以下配置：
1. MySQL服务是否启动
2. 数据库是否已创建
3. `application.yml`中的数据库连接配置是否正确
4. MySQL用户是否有足够权限

### Q2: 前端启动后无法访问后端接口？

**A**: 请检查：
1. 后端服务是否已启动（默认端口8080）
2. 前端vite.config.ts中的代理配置是否正确
3. 是否存在跨域问题（后端已配置CORS）

### Q3: JWT Token过期如何处理？

**A**: Token默认有效期为2小时。过期后需要重新登录。可以在`application.yml`中调整`jwt.expire`配置。

### Q4: AI出题功能无法使用？

**A**: AI功能依赖Dify平台，请按以下步骤排查：

1. **检查Dify服务状态**
   - 云服务：确认账号正常、配额充足
   - 私有部署：确认Docker容器正常运行

2. **检查配置参数**
   ```sql
   SELECT * FROM sys_config WHERE config_key LIKE 'dify%';
   ```
   确认所有Key值正确，特别是 `dify_key_generation` 不能包含 `******` 占位符

3. **检查API连通性**
   ```bash
   # 测试Dify API是否可访问
   curl -X POST 'https://api.dify.ai/v1/workflows/run' \
     -H 'Authorization: Bearer your-api-key' \
     -H 'Content-Type: application/json' \
     -d '{"inputs":{"topic":"test"},"response_mode":"blocking","user":"test"}'
   ```

4. **常见错误码**
   | 错误码 | 原因 | 解决方案 |
   |--------|------|---------|
   | 401 | API Key无效或过期 | 重新获取Key并更新配置 |
   | 403 | 权限不足 | 检查Dify应用权限设置 |
   | 404 | Dataset ID不存在 | 确认知识库ID正确 |
   | 400 | 参数格式错误 | 检查Workflow输入变量定义 |

5. **查看后端日志**
   ```bash
   # 查看Dify调用日志
   grep "Dify" logs/spring.log
   ```

### Q4.1: AI出题返回空结果或格式错误？

**A**: 检查Dify Workflow配置：
1. 确保输出变量名为 `text` 或 `result`
2. 确保LLM节点输出为纯JSON数组格式
3. 检查Prompt是否要求输出JSON格式
4. 在Dify控制台测试Workflow是否能正常返回

### Q4.2: AI阅卷分数不准确？

**A**: 优化阅卷Prompt：
1. 在Prompt中明确评分标准和得分点
2. 要求输出具体分数而非等级
3. 添加示例答案作为参考
4. AI评分后需要教师人工复核确认

### Q5: 如何修改默认密码？

**A**: 默认密码使用BCrypt加密，可通过以下方式修改：
1. 登录后在个人中心修改
2. 管理员在用户管理中重置
3. 直接修改数据库（需BCrypt加密）

### Q6: WebSocket连接失败？

**A**: 请检查：
1. 后端WebSocket配置是否正确
2. 前端连接地址是否正确（ws://或wss://）
3. 是否存在网络代理问题

---

## 版权信息

本项目采用 MIT 许可证。

```
MIT License

Copyright (c) 2025 Smart Exam System

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<p align="center">
  如有问题或建议，欢迎提交 <a href="https://github.com/your-repo/issues">Issue</a>
</p>
