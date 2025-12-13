<template>
  <div class="review-console">

    <!-- 左侧：考生列表 -->
    <aside class="console-sidebar">
      <div class="sidebar-header">
        <h2 class="sidebar-title">
          <el-icon><List /></el-icon> 阅卷列表
        </h2>

        <!-- 搜索与筛选 -->
        <div class="filter-box">
          <el-input
              v-model="searchKeyword"
              placeholder="搜索姓名或学号..."
              prefix-icon="Search"
              clearable
              class="search-input"
          />
          <div class="status-tabs">
            <span
                v-for="tab in statusTabs"
                :key="tab.value"
                class="tab-item"
                :class="{ active: currentStatus === tab.value }"
                @click="currentStatus = tab.value"
            >
              {{ tab.label }}
            </span>
          </div>
        </div>
      </div>

      <!-- 列表内容 -->
      <div class="student-list" v-loading="listLoading">
        <div
            v-for="item in filteredList"
            :key="item.recordId"
            class="student-card"
            :class="{ 'active': currentRecordId === item.recordId }"
            @click="handleSelectStudent(item)"
        >
          <div class="card-top">
            <span class="student-name">{{ item.studentName }}</span>
            <span class="status-tag" :class="getStatusClass(item.status)">
              {{ getStatusText(item.status) }}
            </span>
          </div>
          <div class="card-bottom">
            <span class="student-id">ID: {{ item.studentNumber }}</span>
            <span class="score-info">客观分: {{ item.objectiveScore || 0 }}</span>
          </div>
        </div>

        <div v-if="filteredList.length === 0" class="empty-state">
          暂无数据
        </div>
      </div>
    </aside>

    <!-- 右侧：阅卷详情 -->
    <main class="console-main">
      <div v-if="!currentRecordId" class="empty-placeholder">
        <el-icon class="placeholder-icon"><DocumentChecked /></el-icon>
        <p>请从左侧选择一位考生开始阅卷</p>
      </div>

      <template v-else>
        <!-- 详情头部 -->
        <header class="main-header">
          <div class="header-info">
            <div class="student-meta">
              <span class="meta-name">{{ currentStudent?.studentName }}</span>
              <span class="meta-id">{{ currentStudent?.studentNumber }}</span>
            </div>
            <div class="exam-meta">
              <span>{{ examTitle }}</span>
              <el-divider direction="vertical" />
              <span>提交时间: {{ formatTime(currentStudent?.submitTime) }}</span>
            </div>
          </div>
          <div class="header-actions">
            <el-button type="primary" size="large" @click="submitReview" :loading="submitting">
              <el-icon class="el-icon--left"><Check /></el-icon> 提交复核
            </el-button>
          </div>
        </header>

        <!-- 试题内容区 -->
        <el-scrollbar class="main-content">
          <div class="questions-wrapper">
            <div
                v-for="(q, index) in subjectiveQuestions"
                :key="q.id"
                class="review-card"
            >
              <!-- 题目区域 -->
              <div class="q-section">
                <div class="q-header">
                  <span class="q-index">第 {{ q.sortOrder || index + 1 }} 题</span>
                  <span class="q-type">主观题</span>
                  <span class="q-score">满分 {{ q.score }}</span>
                </div>
                <div class="q-content" v-html="formatContent(q.content)"></div>
              </div>

              <!-- 学生答案 -->
              <div class="answer-section">
                <div class="section-label">学生作答</div>
                <div class="student-answer-box">
                  {{ q.studentAnswer || '（未作答）' }}
                </div>
              </div>

              <!-- 评分区域 (AI + 人工) -->
              <div class="grading-section">
                <!-- AI 建议 -->
                <div class="ai-suggestion">
                  <div class="ai-header">
                    <span class="ai-title"><el-icon><Cpu /></el-icon> AI 智能预评</span>
                    <el-button
                        type="success"
                        link
                        size="small"
                        @click="adoptAiScore(q)"
                    >
                      采纳建议
                    </el-button>
                  </div>
                  <div class="ai-body">
                    <div class="ai-score-row">
                      建议得分：<strong class="score-val">{{ q.aiScore || 0 }}</strong>
                    </div>
                    <div class="ai-reason">
                      {{ q.aiComment || 'AI 暂无详细评语' }}
                    </div>
                  </div>
                </div>

                <!-- 人工打分 -->
                <div class="manual-grading">
                  <div class="section-label">人工复核</div>
                  <div class="grading-form">
                    <div class="form-item">
                      <span class="label">得分：</span>
                      <el-input-number
                          v-model="q.finalScore"
                          :min="0"
                          :max="q.score"
                          :precision="1"
                          controls-position="right"
                          class="score-input"
                      />
                    </div>
                    <div class="form-item block">
                      <span class="label">评语：</span>
                      <el-input
                          v-model="q.teacherComment"
                          type="textarea"
                          rows="2"
                          placeholder="请输入评语（可选）"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="footer-tip">
              已显示所有主观题，确认无误后请点击右上角提交。
            </div>
          </div>
        </el-scrollbar>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router' // 假设从路由参数获取 examId
import { List, Search, DocumentChecked, Check, Cpu } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
// 假设有如下 API，需根据实际后端情况调整
import request from '@/utils/request'

// --- Mock Data Types (按实际 DTO 调整) ---
interface ReviewStudent {
  recordId: number
  studentName: string
  studentNumber: string
  status: number // 2-待批改, 3-已完成
  objectiveScore: number
  submitTime: string
}

interface SubjectiveQuestion {
  id: number // questionId
  sortOrder: number
  content: string
  score: number
  studentAnswer: string
  aiScore: number
  aiComment: string
  finalScore: number // 绑定输入框
  teacherComment: string // 绑定输入框
}

// --- State ---
const route = useRoute()
const examId = route.query.examId || 1 // 临时 Mock
const examTitle = ref('2024年期末考试 - Java程序设计') // 应该从 API 获取

const listLoading = ref(false)
const rawStudentList = ref<ReviewStudent[]>([])
const searchKeyword = ref('')
const currentStatus = ref('pending') // all, pending, reviewed

const statusTabs = [
  { label: '全部', value: 'all' },
  { label: '待批改', value: 'pending' },
  { label: '已批改', value: 'reviewed' }
]

const currentRecordId = ref<number | null>(null)
const currentStudent = ref<ReviewStudent | null>(null)
const subjectiveQuestions = ref<SubjectiveQuestion[]>([])
const submitting = ref(false)

// --- Methods ---

// 1. 获取考生列表
const fetchStudentList = async () => {
  listLoading.value = true
  try {
    // const res = await request.get('/api/review/list', { params: { examId } })
    // Mock Data
    await new Promise(r => setTimeout(r, 500))
    rawStudentList.value = [
      { recordId: 101, studentName: '张三', studentNumber: '2021001', status: 2, objectiveScore: 58, submitTime: '2023-12-20 10:30' },
      { recordId: 102, studentName: '李四', studentNumber: '2021002', status: 2, objectiveScore: 42, submitTime: '2023-12-20 10:32' },
      { recordId: 103, studentName: '王五', studentNumber: '2021003', status: 3, objectiveScore: 60, submitTime: '2023-12-20 10:25' },
    ]
  } catch (e) {
    ElMessage.error('获取列表失败')
  } finally {
    listLoading.value = false
  }
}

// 2. 筛选逻辑
const filteredList = computed(() => {
  return rawStudentList.value.filter(item => {
    // 状态筛选
    let statusMatch = true
    if (currentStatus.value === 'pending') statusMatch = item.status === 2
    if (currentStatus.value === 'reviewed') statusMatch = item.status === 3

    // 关键词筛选
    const keywordMatch = item.studentName.includes(searchKeyword.value) ||
        item.studentNumber.includes(searchKeyword.value)

    return statusMatch && keywordMatch
  })
})

// 3. 选择考生获取详情
const handleSelectStudent = async (student: ReviewStudent) => {
  currentRecordId.value = student.recordId
  currentStudent.value = student

  // Fetch Detail
  try {
    // const res = await request.get(`/api/review/detail/${student.recordId}`)
    // Mock Data
    const mockQuestions: SubjectiveQuestion[] = [
      {
        id: 1, sortOrder: 1, score: 10,
        content: '请简述 Spring IOC 的原理。',
        studentAnswer: 'IOC 就是控制反转，把对象的创建交给容器管理。',
        aiScore: 8,
        aiComment: '回答了基本概念，但未涉及依赖注入细节。',
        finalScore: student.status === 3 ? 8 : 0, // 如果已批改，回显分数
        teacherComment: student.status === 3 ? '同AI' : ''
      },
      {
        id: 2, sortOrder: 2, score: 15,
        content: '什么是线程安全？Java 中如何保证线程安全？',
        studentAnswer: '不知道。',
        aiScore: 0,
        aiComment: '未作答或回答错误。',
        finalScore: 0,
        teacherComment: ''
      }
    ]
    // 默认如果未批改，初始化 finalScore 为 AI 分数建议？或者 0？这里设为 0 让老师确认
    // 为了方便，这里演示：如果是待批改，预填 AI 分数但需老师确认
    if (student.status === 2) {
      mockQuestions.forEach(q => {
        q.finalScore = q.aiScore
      })
    }
    subjectiveQuestions.value = mockQuestions
  } catch (e) {
    ElMessage.error('获取试卷详情失败')
  }
}

// 4. 采纳 AI 建议
const adoptAiScore = (q: SubjectiveQuestion) => {
  q.finalScore = q.aiScore
  q.teacherComment = q.aiComment
  ElMessage.success('已采纳 AI 评分')
}

// 5. 提交复核
const submitReview = async () => {
  if (!currentRecordId.value) return

  submitting.value = true
  try {
    const payload = {
      recordId: currentRecordId.value,
      reviews: subjectiveQuestions.value.map(q => ({
        questionId: q.id,
        score: q.finalScore,
        comment: q.teacherComment
      }))
    }
    // await request.post('/api/review/submit', payload)
    await new Promise(r => setTimeout(r, 1000)) // Mock API delay

    ElMessage.success('提交成功')

    // 更新本地状态
    if (currentStudent.value) {
      currentStudent.value.status = 3
    }
    // 自动跳到下一个？或者留在当前
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

// Tools
const getStatusText = (status: number) => {
  const map: Record<number, string> = { 2: '待批改', 3: '已完成' }
  return map[status] || '未知'
}

const getStatusClass = (status: number) => {
  return status === 2 ? 'tag-pending' : 'tag-success'
}

const formatTime = (timeStr?: string) => timeStr || '--'

const formatContent = (content: string) => {
  if (!content) return ''
  return content.replace(/\n/g, '<br/>')
}

onMounted(() => {
  fetchStudentList()
})
</script>

<style scoped>
/* 原生 CSS 样式重构 */

.review-console {
  display: flex;
  height: calc(100vh - 60px); /* 减去顶部 Navbar 高度，假设是 60px */
  background-color: #f5f7fa;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  overflow: hidden;
}

/* --- 左侧侧边栏 --- */
.console-sidebar {
  width: 320px;
  background-color: #fff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-shadow: 2px 0 8px rgba(0,0,0,0.02);
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #f0f2f5;
  background-color: #fff;
  z-index: 10;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 状态 Tabs */
.status-tabs {
  display: flex;
  background-color: #f3f4f6;
  border-radius: 6px;
  padding: 4px;
}

.tab-item {
  flex: 1;
  text-align: center;
  font-size: 12px;
  color: #6b7280;
  padding: 6px 0;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  font-weight: 500;
}

.tab-item:hover {
  color: #374151;
}

.tab-item.active {
  background-color: #fff;
  color: #4f46e5;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  font-weight: 600;
}

/* 列表区 */
.student-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.student-card {
  background-color: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.student-card:hover {
  border-color: #cbd5e0;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}

.student-card.active {
  border-color: #4f46e5;
  background-color: #eef2ff;
  box-shadow: 0 0 0 1px #4f46e5 inset;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.student-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.status-tag {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.tag-pending {
  background-color: #fff7ed;
  color: #c2410c;
  border: 1px solid #ffedd5;
}

.tag-success {
  background-color: #f0fdf4;
  color: #15803d;
  border: 1px solid #dcfce7;
}

.card-bottom {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #6b7280;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
  font-size: 13px;
}

/* --- 右侧主内容 --- */
.console-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background-color: #f9fafb;
}

.empty-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #e5e7eb;
}

.main-header {
  height: 64px;
  background-color: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 2px rgba(0,0,0,0.02);
  flex-shrink: 0;
}

.header-info {
  display: flex;
  flex-direction: column;
}

.student-meta {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 4px;
}

.meta-name {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.meta-id {
  font-size: 13px;
  color: #6b7280;
  font-family: monospace;
}

.exam-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #9ca3af;
}

.main-content {
  flex: 1;
  padding: 0;
}

.questions-wrapper {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 题目卡片 */
.review-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

/* 1. 题目区 */
.q-section {
  padding: 24px;
  border-bottom: 1px solid #f3f4f6;
  background-color: #fff;
}

.q-header {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.q-index {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.q-type {
  font-size: 12px;
  background-color: #f3f4f6;
  color: #4b5563;
  padding: 2px 8px;
  border-radius: 4px;
}

.q-score {
  font-size: 12px;
  color: #9ca3af;
}

.q-content {
  font-size: 15px;
  color: #374151;
  line-height: 1.6;
}

/* 2. 答案区 */
.answer-section {
  padding: 20px 24px;
  background-color: #fafafa; /* 稍微深一点的灰白 */
}

.section-label {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 8px;
  font-weight: 600;
}

.student-answer-box {
  background-color: #fff;
  border: 1px solid #e5e7eb;
  border-left: 4px solid #4f46e5; /* 强调线 */
  padding: 16px;
  border-radius: 4px;
  font-size: 15px;
  color: #1f2937;
  line-height: 1.6;
  white-space: pre-wrap;
}

/* 3. 评分区 */
.grading-section {
  padding: 24px;
  display: flex;
  gap: 24px;
}

/* AI 建议 */
.ai-suggestion {
  flex: 1;
  background-color: #eff6ff; /* 蓝色底 */
  border: 1px solid #dbeafe;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.ai-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.ai-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e40af;
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-score-row {
  font-size: 14px;
  color: #1e3a8a;
  margin-bottom: 8px;
}

.score-val {
  font-size: 18px;
  color: #2563eb;
}

.ai-reason {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.5;
  background: rgba(255,255,255,0.5);
  padding: 8px;
  border-radius: 4px;
}

/* 人工评分 */
.manual-grading {
  flex: 1;
  background-color: #fff;
  border: 1px dashed #d1d5db; /* 虚线框 */
  border-radius: 8px;
  padding: 16px;
}

.grading-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.form-item.block {
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.form-item .label {
  font-size: 13px;
  color: #374151;
  font-weight: 600;
}

.score-input {
  width: 140px;
}

.footer-tip {
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  margin-bottom: 40px;
}
</style>