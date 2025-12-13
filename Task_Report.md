智能考试系统开发进度报告

阶段零：数据库架构设计

完成状态

✅ 已完成

设计说明

根据业务需求，设计了基于MySQL 8.0的数据库表结构，采用utf8mb4_general_ci字符集，所有表均包含系统字段（id、create_time、update_time、create_by、update_by、is_deleted），使用下划线命名风格，未使用物理外键约束，关联关系由应用层维护。

生成的表结构

共设计17张表，包括：

基础用户与组织模块：

sys_user (用户表)

sys_dept (部门/班级表)

sys_course (课程表)

sys_course_user (选课表)

sys_config (系统配置表)

题库与知识库模块：

sys_knowledge_file (RAG资料表)

exam_question (题库表)

sys_ai_task (AI任务记录表)

试卷与组卷模块：

exam_paper (试卷模板表)

exam_paper_question (试卷题目关联表)

exam_publish (考试发布/场次表)

考试过程与记录模块：

exam_record (考试记录表)

exam_record_detail (答题明细表)

exam_proctor_log (监考日志表)

学习反馈与通知模块：

exam_mistake_book (错题本)

sys_notice (系统通知表)

sys_user_notice (用户消息状态表)

预设数据

系统配置表（sys_config）中包含了Dify相关的预设配置项，敏感数据已使用脱敏占位符处理。

文件位置

表结构SQL文件：create_tables.sql

本报告文件：Task_Report.md

阶段 1.1：后端项目初始化与安全架构

完成状态

✅ 已完成

实现说明

后端项目初始化：

创建了符合Maven标准目录结构的Backend文件夹

配置了完整的pom.xml文件，包含所有必要的依赖

实现了Spring Boot 3.3.4项目的基础架构

统一响应与异常处理：

实现了泛型类Result<T>作为统一响应结果

创建了全局异常处理器GlobalExceptionHandler

定义了自定义业务异常BizException

代码生成：

使用MyBatis-Plus和Lombok生成了17张表的完整后端代码

包含Entity、Mapper、Service、ServiceImpl、Controller等层级

认证模块实现：

实现了JWT Token生成和验证机制

创建了登录接口POST /api/auth/login

使用BCrypt进行密码校验

安全配置与拦截器：

实现了JwtAuthenticationFilter过滤器

配置了SecurityFilterChain，定义了URL鉴权规则

实现了操作日志拦截器LogInterceptor

配置了CORS跨域支持

生成的关键配置文件

Backend/pom.xml：项目依赖配置

Backend/src/main/resources/application.yml：应用配置

Backend/src/main/java/com/university/exam/config/SecurityConfig.java：安全配置

Backend/src/main/java/com/university/exam/config/WebMvcConfig.java：Web MVC配置

核心Security相关类

JwtUtils.java：JWT Token生成和验证工具类

JwtAuthenticationFilter.java：JWT认证过滤器

SecurityConfig.java：安全配置类

LogInterceptor.java：操作日志拦截器

WebMvcConfig.java：Web MVC配置类

阶段 1.2：前端脚手架搭建

完成状态

✅ 已完成

实现说明

前端项目初始化：

使用 Vite 创建了名为 smart-exam-web 的 Vue 3 + TypeScript 项目

采用了现代化的前端工程化架构

核心配置文件：

配置了 Vite 开发服务器，包含路径别名和 API 代理

设置了 TypeScript 路径别名支持

集成了 Element Plus 按需加载

配置了 TailwindCSS 样式系统

应用初始化：

实现了完整的 Vue 3 应用入口文件

集成了 Pinia 状态管理（带持久化插件）

配置了 Vue Router 路由系统

全局注册了 Element Plus 图标

安装的依赖库

生产环境依赖

element-plus：UI 组件库

@element-plus/icons-vue：Element Plus 图标

vue-router：路由管理

pinia：状态管理

pinia-plugin-persistedstate：Pinia 持久化插件

axios：HTTP 客户端

sass：CSS 预处理器

@vueuse/core：Vue 组合式 API 工具集

开发环境依赖

unplugin-auto-import：自动导入工具

unplugin-vue-components：组件自动导入

tailwindcss：CSS 框架

postcss：CSS 处理工具

autoprefixer：CSS 浏览器前缀处理

生成的关键配置文件

smart-exam-web/vite.config.ts：Vite 项目配置

smart-exam-web/tsconfig.json：TypeScript 配置

smart-exam-web/src/main.ts：应用入口文件

阶段 1.3：前端核心架构封装

完成状态

✅ 已完成

实现说明

网络层封装：

创建了符合项目需求的 Axios 实例

实现了请求拦截器，添加 JWT Token 认证

实现了响应拦截器，统一处理错误和成功响应

配置了 API 代理，处理跨域请求

状态管理：

使用 Pinia 实现了用户状态管理

实现了登录、登出、获取用户信息等核心功能

集成了 Pinia 持久化插件，保存关键状态

实现了角色权限管理

登录页面：

设计了视觉吸引力强的登录页面

实现了表单验证和提交逻辑

处理了登录成功和失败的状态反馈

支持记住我功能

登录页面可正常访问

生成的关键文件

smart-exam-web/src/utils/request.ts：Axios 网络请求封装

smart-exam-web/src/store/user.ts：用户状态管理

smart-exam-web/src/views/login/index.vue：登录页面

阶段 1.4：前端路由与骨架屏

完成状态

✅ 已完成

实现说明

路由配置：

定义了常量路由（无需权限验证）

定义了动态路由（需要权限验证）

实现了基于角色的路由过滤

路由守卫：

实现了登录验证逻辑

实现了动态路由加载

实现了权限校验

设置了页面标题

核心布局：

使用 Element Plus Container 组件实现整体布局

实现了左侧菜单栏，根据角色动态渲染

实现了顶部导航栏，包含消息通知和用户信息

支持菜单展开/折叠功能

响应式设计，适配不同屏幕尺寸

生成的页面文件列表

公共页面

smart-exam-web/src/views/login/index.vue - 登录页面

smart-exam-web/src/views/profile/index.vue - 个人中心页面

smart-exam-web/src/views/message/index.vue - 消息通知页面

smart-exam-web/src/views/error/404.vue - 404错误页面

学生端页面

smart-exam-web/src/views/student/Dashboard.vue - 仪表盘

smart-exam-web/src/views/student/ExamList.vue - 我的考试列表

smart-exam-web/src/views/student/ExamPaper.vue - 在线答题页面

smart-exam-web/src/views/student/ExamResult.vue - 成绩解析页面

smart-exam-web/src/views/student/MistakeBook.vue - 智能错题本

教师端页面

smart-exam-web/src/views/teacher/Dashboard.vue - 工作台

smart-exam-web/src/views/teacher/KnowledgeBase.vue - 知识库管理

smart-exam-web/src/views/teacher/QuestionBank.vue - 智能题库

smart-exam-web/src/views/teacher/PaperList.vue - 试卷管理

smart-exam-web/src/views/teacher/PaperCreate.vue - 智能组卷向导

smart-exam-web/src/views/teacher/ExamPublish.vue - 考试发布

smart-exam-web/src/views/teacher/ReviewConsole.vue - AI辅助阅卷台

smart-exam-web/src/views/teacher/Analysis.vue - 成绩统计

管理员页面

smart-exam-web/src/views/admin/UserManage.vue - 用户管理

smart-exam-web/src/views/admin/DeptManage.vue - 组织架构

smart-exam-web/src/views/admin/CourseManage.vue - 课程管理

smart-exam-web/src/views/admin/SysConfig.vue - 系统设置

生成的关键文件

smart-exam-web/src/router/index.ts：路由配置

smart-exam-web/src/router/permission.ts：路由守卫

smart-exam-web/src/layout/index.vue：核心布局组件

阶段 1.5：基础管理模块

完成状态

✅ 已完成

实现说明

1. 后端接口实现

SysAdminController

用户管理：

POST /api/admin/user：实现用户创建功能，包含参数验证、权限检查和数据持久化

PUT /api/admin/user/{id}：实现用户信息更新功能，支持部分字段更新

DELETE /api/admin/user/{id}：实现用户删除功能，需处理关联数据

GET /api/admin/user/list：实现用户列表查询功能，支持分页、排序和条件筛选

组织架构：

GET /api/admin/dept/tree：实现部门树形结构查询功能，支持层级展示

POST /api/admin/dept：实现部门创建功能

PUT /api/admin/dept/{id}：实现部门信息更新功能

DELETE /api/admin/dept/{id}：实现部门删除功能，需验证是否存在子部门或关联用户

课程管理：

GET /api/admin/course/list：实现课程列表查询功能，支持分页和筛选

POST /api/admin/course：实现课程创建功能，包含课程基本信息和资源关联

系统设置：

GET /api/admin/config：获取系统配置信息，确保敏感配置项已脱敏处理

PUT /api/admin/config：更新系统配置，包含配置项验证和权限控制

ProfileController

个人中心：

GET /api/profile：获取当前登录用户的详细信息

PUT /api/profile：更新当前用户的基本信息

PUT /api/profile/password：修改当前用户密码，需验证原密码并符合密码复杂度要求

2. 前端页面实现

用户管理页面：完善了用户列表展示、添加、编辑、删除功能

组织架构页面：以树形结构展示部门信息，支持部门的增删改操作

课程管理页面：实现课程列表展示和添加功能

系统设置页面：展示可配置项并支持修改

个人中心页面：包含用户信息展示、基本信息编辑和密码修改功能

3. 通用要求

后端接口实现了统一的响应格式、错误处理和权限验证

前端页面保证了响应式设计，适配不同屏幕尺寸

所有操作都提供了适当的用户反馈和确认机制

涉及敏感操作实现了日志记录功能

生成的关键文件

后端文件

Backend/src/main/java/com/university/exam/controller/SysAdminController.java：系统管理控制器

Backend/src/main/java/com/university/exam/controller/ProfileController.java：个人中心控制器

前端文件

smart-exam-web/src/views/admin/UserManage.vue：用户管理页面

smart-exam-web/src/views/admin/DeptManage.vue：组织架构页面

smart-exam-web/src/views/admin/CourseManage.vue：课程管理页面

smart-exam-web/src/views/admin/SysConfig.vue：系统设置页面

smart-exam-web/src/views/profile/index.vue：个人中心页面

阶段 1.6：Dify 客户端封装

完成状态

✅ 已完成

实现说明

客户端封装：

创建了 DifyClient 工具类 (Backend/src/main/java/com/university/exam/common/utils/DifyClient.java)。

采用 Spring 6 RestClient 作为底层 HTTP 客户端，具备轻量、流式 API 特性。

实现了 runWorkflow（执行工作流）、uploadDocument（RAG 知识库上传）、deleteDocument（删除文档）三个核心方法。

动态配置：

Base URL 不再硬编码，而是通过 ConfigService 从数据库 sys_config 表中读取 dify_base_url 键值，实现了环境隔离和动态切换。

所有 API 方法均设计为接收 apiKey 参数，支持不同应用（如出题应用、阅卷应用）使用不同的 Key。

安全脱敏：

实现了内部拦截器 DifyRequestLogger。

在记录请求日志时，自动拦截 Authorization Header，将敏感的 API Key 替换为 Bearer sk-******。

针对 Multipart 文件上传请求，自动隐藏二进制 Body 内容，防止日志刷屏。

生成的关键文件

Backend/src/main/java/com/university/exam/common/utils/DifyClient.java：Dify API 通信核心类

阶段 2.1：知识库后端接口

完成状态

✅ 已完成

实现说明

控制器 (KnowledgeBaseController)：

实现了 /api/knowledge/upload、/list、/{id} 三个核心接口。

添加了 @PreAuthorize 权限控制，仅允许教师和管理员访问。

业务逻辑 (KnowledgeFileServiceImpl)：

上传流程：实现了"文件本地存储 -> 调用 DifyClient 上传 -> 数据库记录"的完整链路。

权限隔离：在查询列表和删除文件时，严格校验了教师与课程的绑定关系（通过 sys_course_user 表），确保教师只能操作自己课程的资料。管理员拥有所有权限。

数据一致性：在删除文件时，同步删除了 Dify 上的文档、数据库记录以及本地文件。

集成情况：

成功集成了 DifyClient，动态读取 sys_config 中的 Key 进行操作。

文件临时存储路径设置为项目根目录下的 uploads/ 目录。

生成的关键文件

Backend/src/main/java/com/university/exam/controller/KnowledgeBaseController.java

Backend/src/main/java/com/university/exam/service/impl/KnowledgeFileServiceImpl.java

阶段 2.2：知识库前端页面

完成状态

✅ 已完成

实现说明

页面布局：

使用了左右/上下分栏布局：上方为全局筛选（课程选择、搜索），下方左侧为上传区域，右侧为文件列表。

UI 风格采用了 Element Plus 结合 Tailwind CSS，使用了卡片、图标和柔和的配色（Indigo/Gray），视觉效果现代化。

交互逻辑：

课程联动：进入页面自动加载课程列表并选中第一个，文件列表随课程切换而更新。

文件上传：使用 el-upload 的拖拽模式，自定义 http-request 调用后端 /api/knowledge/upload 接口，上传时带有加载状态和课程校验。

状态轮询：实现了智能轮询机制。当文件列表中存在"索引中 (status=1)"的文件时，前端会自动每 5 秒刷新一次列表，直到所有文件索引完成，提升用户体验。

删除保护：删除操作增加了 Popconfirm 二次确认，防止误删。

文件展示：

列表展示了文件名、上传时间、Dify 文档 ID。

根据文件后缀名（PDF/Word/MD）动态显示不同颜色的图标。

状态标签（Tag）直观显示 Dify 索引状态。

生成的关键文件

smart-exam-web/src/views/teacher/KnowledgeBase.vue

阶段 2.3：AI 出题服务逻辑

完成状态

✅ 已完成

实现说明

异步架构：

引入了 AsyncConfig 配置类，配置了名为 aiTaskExecutor 的 Spring 线程池。

实现了 QuestionGenerationServiceImpl，其中的 processGenerationAsync 方法使用 @Async("aiTaskExecutor") 标记，确保出题任务在后台异步执行，不阻塞主线程。

核心流程：

任务初始化：接收出题请求，在 sys_ai_task 表创建一条记录，状态标记为“进行中”。

批处理调用：采用循环分批策略（BATCH_SIZE=10），多次调用 Dify Workflow API，防止单次请求超时或生成量过大。

流控保护：每批次调用后休眠 2 秒，避免触发 Dify 频率限制。

结果解析：解析 Dify 返回的 JSON 数据，提取题目内容、选项、答案等信息。

数据一致性与通知：

MD5 查重：更新了 exam_question 表结构，增加 content_hash 字段。在入库前计算题目内容的 MD5 哈希值，并在数据库中检查是否存在相同内容的题目，有效防止重复出题。

进度更新：每批次完成后实时更新 sys_ai_task 的 current_count，便于前端展示进度条。

完成通知：任务全部完成后，自动调用 NoticeService 发送站内信通知用户，并更新任务状态为“完成”或“失败”。

生成的关键文件

Backend/src/main/java/com/university/exam/config/AsyncConfig.java: 线程池配置

Backend/src/main/java/com/university/exam/service/QuestionGenerationService.java: 接口定义

Backend/src/main/java/com/university/exam/service/impl/QuestionGenerationServiceImpl.java: 核心业务实现

Backend/src/main/java/com/university/exam/entity/Question.java: 实体更新

阶段 2.4：题库后端接口

完成状态

✅ 已完成

实现说明

核心控制器 (QuestionController)：

AI 出题：/api/question/ai-generate (POST) - 异步启动 AI 出题任务，关联 QuestionGenerationService。支持 指定题目类型 (单选/多选/判断/简答/填空) 生成。

任务查询：/api/question/task/list (GET) - 分页查询当前用户的出题任务进度。

题目管理：

/api/question/create (POST) - 手动录入题目，包含 MD5 查重。

/api/question/update (PUT) - 更新题目，支持内容变更时的自动重哈希和查重。

/api/question/batch (DELETE) - 批量删除题目，包含权限校验（教师只能删自己创建的）。

/api/question/list (GET) - 分页查询，实现了基于角色的数据权限隔离（管理员看所有，教师看关联课程或自己创建的）。

Excel 导入：/api/question/import (POST) - 集成 EasyExcel，实现题目批量导入，包含流式读取和自动入库。

查重回调：/api/internal/question/check-duplicate (POST) - 提供给 Dify 或内部系统的查重检查接口。

辅助组件：

EasyExcel 集成：引入了 easyexcel 依赖，编写了 QuestionExcelDto 和 QuestionExcelListener，实现高效的 Excel 解析。

查重逻辑：在 Service 层实现了统一的 checkDuplicate 方法，基于 MD5 Hash 快速判断题目重复。

生成的关键文件

Backend/src/main/java/com/university/exam/controller/QuestionController.java

Backend/src/main/java/com/university/exam/common/utils/QuestionExcelListener.java

Backend/src/main/java/com/university/exam/common/dto/QuestionExcelDto.java

Backend/pom.xml (更新依赖)

阶段 2.5：题库管理前端

完成状态

✅ 已完成

实现说明

页面功能 (QuestionBank.vue)：

多维度筛选：支持按课程、题目内容、题型进行筛选，管理员可查看所有课程题目，教师仅能操作自己权限范围内的题目。

AI 智能出题向导：

实现了弹窗式向导，支持选择课程、输入知识点、选择难度和题型。

集成了后台异步任务，提交后自动唤起任务抽屉。

任务监控抽屉：

实现了右侧抽屉展示 AI 出题任务进度。

采用智能轮询机制，仅在抽屉打开时轮询任务状态，实时更新进度条。

手动录入与编辑：

实现了动态表单，根据选择的题型（单选/多选/判断/简答/填空）自动切换选项录入和答案设置 UI。

支持动态增删选项，并在提交前自动格式化数据以匹配后端存储结构。

数据展示：使用 Element Plus 表格的 expand 功能展示题目的完整信息（含解析、选项），提升信息密度。

生成的关键文件

smart-exam-web/src/views/teacher/QuestionBank.vue

阶段 3.1：组卷与发布后端接口

完成状态

✅ 已完成

实现说明

组卷引擎 (PaperService):

实现了 randomCreate 接口：支持按题目类型、数量配置规则，结合 ORDER BY RAND() 进行随机抽题，自动计算总分并生成 Paper 及 PaperQuestion 关联数据。

实现了 manualCreate 接口：支持接收前端选定的一组题目ID及分值，直接生成试卷。

发布管理 (PublishService):

实现了 publishExam 接口：创建考试场次记录，自动根据当前时间判断状态（未开始/进行中）。

集成了通知系统：考试发布成功后，异步调用 NoticeService 生成通知，并向目标班级（部门）下的所有学生发送站内信。

控制器 (Controller):

PaperController: 提供了组卷、列表查询（带权限控制）、删除（带发布状态检查）接口。

ExamPublishController: 提供了考试发布、撤销发布、列表查询接口。

扩展功能:

QuestionService: 增加了 getRandomQuestions 方法，支撑智能组卷逻辑。

生成的关键文件

Backend/src/main/java/com/university/exam/controller/PaperController.java

Backend/src/main/java/com/university/exam/controller/ExamPublishController.java

Backend/src/main/java/com/university/exam/service/impl/PaperServiceImpl.java

Backend/src/main/java/com/university/exam/service/impl/PublishServiceImpl.java

阶段 4.1：考生后端接口

完成状态

✅ 已完成

实现说明

核心控制器 (StudentExamController)：

GET /api/exam/my-list：查询学生可用考试列表，实现了基于班级ID (target_dept_ids) 的权限过滤和时间有效性筛选。
POST /api/exam/start/{publishId}：开始考试接口。实现了断点续考逻辑，创建考试记录，并返回经过脱敏处理（去除答案和解析）的试卷数据。
POST /api/exam/submit：交卷接口。接收用户答题数据，保存至 exam_record_detail，并触发后台异步阅卷流程。

仪表盘控制器 (StudentDashboardController)：

GET /api/student/dashboard/stats：聚合查询学生的考试记录，计算总考试次数、平均分、及格率及错题数量。

数据传输对象 (DTO)：

定义了 StudentExamDto、ExamPaperVo（脱敏）、SubmitExamRequest 等专用交互对象，规范了前后端数据契约。

生成的关键文件

Backend/src/main/java/com/university/exam/controller/StudentExamController.java
Backend/src/main/java/com/university/exam/controller/StudentDashboardController.java
Backend/src/main/java/com/university/exam/common/dto/student/*.java
Backend/src/main/java/com/university/exam/service/impl/RecordServiceImpl.java (更新)
Backend/src/main/java/com/university/exam/service/impl/PublishServiceImpl.java (更新)

更新日期：2025-12-13
更新人员：MySQL数据库架构师