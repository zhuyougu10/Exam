<template>
  <div class="review-console">

    <!-- 第一栏：试卷列表 (Paper List) -->
    <aside class="col-paper">
      <div class="col-header">
        <h3 class="col-title"><el-icon><Files /></el-icon> 考试场次</h3>
      </div>
      <el-scrollbar class="col-scroll">
        <div class="paper-list" v-loading="paperLoading">
          <div
              v-for="paper in paperList"
              :key="paper.id"
              class="paper-item"
              :class="{ active: currentPaperId === paper.id }"
              @click="handleSelectPaper(paper)"
          >
            <div class="paper-name" :title="paper.title">{{ paper.title }}</div>
            <div class="paper-meta">
              <span>{{ paper.courseName }}</span>
              <span class="badge" :class="paper.status === 2 ? 'ongoing' : 'ended'">
                {{ paper.status === 2 ? '进行中' : '已结束' }}
              </span>
            </div>
          </div>
          <div v-if="paperList.length === 0" class="empty-text">暂无需要阅卷的考试</div>
        </div>
      </el-scrollbar>
    </aside>

    <!-- 第二栏：考生列表 (Student List) -->
    <aside class="col-student">
      <div class="col-header">
        <h3 class="col-title"><el-icon><User /></el-icon> 考生名单</h3>
        <!-- 筛选器 -->
        <div class="filter-row">
          <el-input
              v-model="searchKeyword"
              placeholder="搜索姓名..."
              size="small"
              prefix-icon="Search"
              clearable
              class="filter-input"
          />
          <el-select v-model="currentStatus" size="small" class="filter-select" placeholder="状态">
            <el-option label="全部" value="all" />
            <el-option label="待批" value="pending" />
            <el-option label="已批" value="reviewed" />
          </el-select>
        </div>
      </div>

      <el-scrollbar class="col-scroll">
        <div v-if="!currentPaperId" class="empty-placeholder-small">
          请先选择左侧试卷
        </div>
        <div v-else class="student-list" v-loading="studentLoading">
          <div
              v-for="item in filteredStudentList"
              :key="item.recordId"
              class="student-item"
              :class="{ active: currentRecordId === item.recordId }"
              @click="handleSelectStudent(item)"
          >
            <div class="student-info">
              <span class="s-name">{{ item.studentName }}</span>
              <span class="s-id">{{ item.studentNumber }}</span>
            </div>
            <div class="student-status">
              <span class="status-dot" :class="item.status === 2 ? 'pending' : 'success'"></span>
              <span class="status-text">{{ item.status === 2 ? '待阅' : '已阅' }}</span>
            </div>
          </div>
          <div v-if="filteredStudentList.length === 0" class="empty-text">无匹配考生</div>
        </div>
      </el-scrollbar>
    </aside>

    <!-- 第三栏：阅卷详情 (Review Detail) -->
    <main class="col-detail">
      <div v-if="!currentRecordId" class="empty-placeholder-large">
        <el-icon class="placeholder-icon"><EditPen /></el-icon>
        <p>请选择一位考生开始阅卷</p>
      </div>

      <template v-else>
        <!-- 顶部固定栏 -->
        <header class="detail-header">
          <div class="dh-left">
            <div class="dh-title">{{ currentPaper?.title }}</div>
            <div class="dh-student">
              正在批阅: <span class="highlight">{{ currentStudent?.studentName }}</span>
              ({{ currentStudent?.studentNumber }})
            </div>
          </div>
          <div class="dh-right">
            <div class="score-preview">
              客观分: <strong>{{ currentStudent?.objectiveScore }}</strong>
            </div>
            <el-button type="primary" :loading="submitting" @click="submitReview">
              <el-icon class="el-icon--left"><Check /></el-icon> 提交成绩
            </el-button>
          </div>
        </header>

        <!-- 试题内容滚动区 -->
        <el-scrollbar class="detail-content">
          <div class="review-wrapper">
            <div
                v-for="(q, index) in subjectiveQuestions"
                :key="q.id"
                class="question-card"
            >
              <!-- 题目头 -->
              <div class="qc-header">
                <span class="qc-index">第 {{ q.sortOrder || index + 1 }} 题</span>
                <span class="qc-tag">主观题</span>
                <span class="qc-score">满分: {{ q.score }}</span>
              </div>

              <!-- 题目内容 -->
              <div class="qc-body">
                <div class="q-text" v-html="formatContent(q.content)"></div>

                <!-- 学生答案 -->
                <div class="student-answer">
                  <div class="sa-label">考生作答</div>
                  <div class="sa-content">{{ q.studentAnswer || '（考生未作答）' }}</div>
                </div>

                <!-- 评分区域 -->
                <div class="grading-area">
                  <!-- AI 建议 -->
                  <div class="ai-box">
                    <div class="ai-head">
                      <span><el-icon><Cpu /></el-icon> AI 预评</span>
                      <el-button type="success" link size="small" @click="adoptAiScore(q)">采纳</el-button>
                    </div>
                    <div class="ai-score">建议得分: <strong>{{ q.aiScore || 0 }}</strong></div>
                    <div class="ai-comment">{{ q.aiComment || '暂无评语' }}</div>
                  </div>

                  <!-- 人工打分 -->
                  <div class="manual-box">
                    <div class="manual-head">人工复核</div>
                    <div class="form-row">
                      <span class="label">得分</span>
                      <el-input-number
                          v-model="q.finalScore"
                          :min="0"
                          :max="q.score"
                          size="small"
                          controls-position="right"
                      />
                    </div>
                    <div class="form-row">
                      <span class="label">评语</span>
                      <el-input
                          v-model="q.teacherComment"
                          size="small"
                          placeholder="请输入评语..."
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="footer-msg">
              已显示所有待阅题目
            </div>
          </div>
        </el-scrollbar>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Files, User, Search, EditPen, Check, Cpu } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
// import request from '@/utils/request' // 暂用 Mock

// --- Interfaces ---
interface PaperItem {
  id: number
  title: string
  courseName: string
  status: number // 2:进行中, 3:已结束
}

interface ReviewStudent {
  recordId: number
  studentName: string
  studentNumber: string
  status: number // 2:待批, 3:已批
  objectiveScore: number
  submitTime: string
}

interface SubjectiveQuestion {
  id: number
  sortOrder: number
  content: string
  score: number
  studentAnswer: string
  aiScore: number
  aiComment: string
  finalScore: number
  teacherComment: string
}

// --- State ---
// Column 1
const paperLoading = ref(false)
const paperList = ref<PaperItem[]>([])
const currentPaperId = ref<number | null>(null)
const currentPaper = ref<PaperItem | null>(null)

// Column 2
const studentLoading = ref(false)
const rawStudentList = ref<ReviewStudent[]>([])
const searchKeyword = ref('')
const currentStatus = ref('all') // all, pending, reviewed
const currentRecordId = ref<number | null>(null)
const currentStudent = ref<ReviewStudent | null>(null)

// Column 3
const subjectiveQuestions = ref<SubjectiveQuestion[]>([])
const submitting = ref(false)

// --- Methods ---

// 1. 获取试卷列表
const fetchPapers = async () => {
  paperLoading.value = true
  try {
    // const res = await request.get('/api/teacher/papers/reviewable')
    // Mock
    await new Promise(r => setTimeout(r, 300))
    paperList.value = [
      { id: 1, title: 'Java程序设计期末考试', courseName: 'Java基础', status: 3 },
      { id: 2, title: '数据库原理测验', courseName: '数据库', status: 2 },
      { id: 3, title: '数据结构第一次月考', courseName: '数据结构', status: 3 },
    ]
  } finally {
    paperLoading.value = false
  }
}

// 2. 选择试卷 -> 加载学生
const handleSelectPaper = async (paper: PaperItem) => {
  currentPaperId.value = paper.id
  currentPaper.value = paper
  currentRecordId.value = null // 重置选中学生
  studentLoading.value = true

  try {
    // const res = await request.get(`/api/review/paper/${paper.id}/students`)
    // Mock
    await new Promise(r => setTimeout(r, 300))
    rawStudentList.value = Array.from({ length: 10 }).map((_, i) => ({
      recordId: 100 + i,
      studentName: `学生${i + 1}`,
      studentNumber: `202400${i + 1}`,
      status: i < 5 ? 2 : 3, // 前5个待批，后5个已批
      objectiveScore: 40 + Math.floor(Math.random() * 20),
      submitTime: '2024-01-15 10:00'
    }))
  } finally {
    studentLoading.value = false
  }
}

// 3. 筛选学生
const filteredStudentList = computed(() => {
  return rawStudentList.value.filter(s => {
    let statusMatch = true
    if (currentStatus.value === 'pending') statusMatch = s.status === 2
    if (currentStatus.value === 'reviewed') statusMatch = s.status === 3

    const kw = searchKeyword.value.trim()
    const kwMatch = !kw || s.studentName.includes(kw) || s.studentNumber.includes(kw)

    return statusMatch && kwMatch
  })
})

// 4. 选择学生 -> 加载详情
const handleSelectStudent = async (student: ReviewStudent) => {
  currentRecordId.value = student.recordId
  currentStudent.value = student

  // Mock Detail
  subjectiveQuestions.value = [
    {
      id: 1, sortOrder: 1, score: 10,
      content: '请解释什么是死锁，以及产生的四个必要条件。',
      studentAnswer: '死锁是指两个或两个以上的进程在执行过程中，由于竞争资源或者由于彼此通信而造成的一种阻塞的现象。',
      aiScore: 6,
      aiComment: '回答了死锁定义，但缺失了四个必要条件（互斥、占有且等待、不可抢占、循环等待）。',
      finalScore: student.status === 3 ? 6 : 6, // 默认填入AI分
      teacherComment: ''
    },
    {
      id: 2, sortOrder: 2, score: 15,
      content: '简述 TCP 三次握手过程。',
      studentAnswer: '第一次：客户端发SYN；第二次：服务端回SYN+ACK；第三次：客户端回ACK。',
      aiScore: 15,
      aiComment: '回答准确，逻辑清晰。',
      finalScore: student.status === 3 ? 15 : 15,
      teacherComment: ''
    }
  ]
}

// 5. 采纳 AI
const adoptAiScore = (q: SubjectiveQuestion) => {
  q.finalScore = q.aiScore
  q.teacherComment = q.aiComment
  ElMessage.success('已采纳')
}

// 6. 提交
const submitReview = async () => {
  submitting.value = true
  try {
    await new Promise(r => setTimeout(r, 800)) // Mock
    ElMessage.success('提交成功，自动跳转下一位')

    // 逻辑：将当前学生标为已批，自动选下一个待批学生
    if (currentStudent.value) currentStudent.value.status = 3

    const nextPending = filteredStudentList.value.find(s => s.status === 2 && s.recordId !== currentRecordId.value)
    if (nextPending) {
      handleSelectStudent(nextPending)
    } else {
      currentRecordId.value = null // 无待批
    }
  } finally {
    submitting.value = false
  }
}

const formatContent = (val: string) => val ? val.replace(/\n/g, '<br/>') : ''

onMounted(() => {
  fetchPapers()
})
</script>

<style scoped>
/* 三栏布局 (Three-Pane Layout)
  Style: Native CSS (Notion/Linear-like)
*/

.review-console {
  display: flex;
  height: calc(100vh - 64px); /* 假设 Navbar 高度 64px */
  background-color: #f7f9fc;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #1a202c;
  overflow: hidden;
}

/* --- 第一栏：试卷列表 --- */
.col-paper {
  width: 260px;
  background-color: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.col-header {
  height: 56px;
  border-bottom: 1px solid #f0f2f5;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 16px;
  background: #fdfdfd;
}

.col-title {
  font-size: 14px;
  font-weight: 700;
  color: #4a5568;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.col-scroll {
  flex: 1;
}

.paper-list {
  padding: 8px;
}

.paper-item {
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
  border: 1px solid transparent;
}

.paper-item:hover {
  background-color: #f7fafc;
}

.paper-item.active {
  background-color: #ebf4ff;
  border-color: #bee3f8;
}

.paper-name {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.paper-item.active .paper-name {
  color: #3182ce;
}

.paper-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #718096;
}

.badge {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  transform: scale(0.9);
}

.badge.ongoing { background: #f0fff4; color: #38a169; }
.badge.ended { background: #edf2f7; color: #718096; }

/* --- 第二栏：学生列表 --- */
.col-student {
  width: 280px;
  background-color: #fafbfc;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.col-student .col-header {
  height: auto;
  padding: 12px 16px;
  gap: 10px;
}

.filter-row {
  display: flex;
  gap: 8px;
}

.filter-input {
  flex: 1;
}

.filter-select {
  width: 80px;
}

.student-list {
  padding: 8px;
}

.student-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  margin-bottom: 4px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid transparent;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0,0,0,0.02);
}

.student-item:hover {
  border-color: #cbd5e0;
}

.student-item.active {
  background-color: #fff;
  border-color: #4f46e5;
  box-shadow: 0 0 0 1px #4f46e5 inset;
}

.student-info {
  display: flex;
  flex-direction: column;
}

.s-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.s-id {
  font-size: 12px;
  color: #9ca3af;
}

.student-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.pending { background-color: #f59e0b; }
.status-dot.success { background-color: #10b981; }

.status-text {
  color: #6b7280;
}

/* --- 第三栏：详情 --- */
.col-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f7f9fc;
  min-width: 0;
}

.detail-header {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.dh-left {
  display: flex;
  flex-direction: column;
}

.dh-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.dh-student {
  font-size: 13px;
  color: #6b7280;
}

.dh-student .highlight {
  color: #4f46e5;
  font-weight: 600;
}

.dh-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.score-preview {
  font-size: 13px;
  color: #4b5563;
}

.detail-content {
  flex: 1;
}

.review-wrapper {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.question-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  overflow: hidden;
}

.qc-header {
  padding: 16px 24px;
  border-bottom: 1px solid #f3f4f6;
  background-color: #fff;
  display: flex;
  gap: 12px;
  align-items: center;
}

.qc-index { font-weight: 700; font-size: 14px; }
.qc-tag { font-size: 12px; background: #f3f4f6; padding: 2px 6px; border-radius: 4px; color: #4b5563; }
.qc-score { font-size: 12px; color: #9ca3af; margin-left: auto; }

.qc-body {
  padding: 24px;
}

.q-text {
  font-size: 15px;
  color: #1f2937;
  margin-bottom: 20px;
  line-height: 1.6;
}

.student-answer {
  background-color: #f9fafb;
  border-radius: 6px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  margin-bottom: 20px;
}

.sa-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
  font-weight: 600;
}

.sa-content {
  font-size: 15px;
  color: #111827;
  white-space: pre-wrap;
}

.grading-area {
  display: flex;
  gap: 16px;
}

.ai-box, .manual-box {
  flex: 1;
  border-radius: 6px;
  padding: 16px;
}

.ai-box {
  background-color: #eff6ff;
  border: 1px solid #dbeafe;
}

.ai-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #1e40af;
  margin-bottom: 8px;
}

.ai-score {
  font-size: 14px;
  color: #1e3a8a;
  margin-bottom: 6px;
}

.ai-comment {
  font-size: 13px;
  color: #60a5fa;
  color: #1f2937;
  opacity: 0.8;
  line-height: 1.4;
}

.manual-box {
  background-color: #fff;
  border: 1px dashed #d1d5db;
}

.manual-head {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.form-row .label {
  width: 32px;
  font-size: 13px;
  color: #6b7280;
}

.empty-text {
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  padding: 20px;
}

.empty-placeholder-small {
  text-align: center;
  color: #cbd5e0;
  font-size: 13px;
  margin-top: 40px;
}

.empty-placeholder-large {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #9ca3af;
}

.placeholder-icon {
  font-size: 64px;
  color: #e5e7eb;
  margin-bottom: 24px;
}

.footer-msg {
  text-align: center;
  color: #cbd5e0;
  font-size: 12px;
  padding-bottom: 40px;
}
</style>
