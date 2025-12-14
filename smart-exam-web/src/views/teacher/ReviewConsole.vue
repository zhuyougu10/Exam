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
              <span class="badge" :class="paper.pendingCount > 0 ? 'pending' : 'done'">
                待阅: {{ paper.pendingCount }}
              </span>
            </div>
          </div>
          <div v-if="!paperLoading && paperList.length === 0" class="empty-text">暂无需要阅卷的考试</div>
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
              @input="handleStudentFilter"
              class="filter-input"
          />
          <el-select v-model="currentStatus" size="small" class="filter-select" placeholder="状态" @change="handleStudentFilter">
            <el-option label="全部" :value="null" />
            <el-option label="待批" :value="2" />
            <el-option label="已批" :value="3" />
          </el-select>
        </div>
      </div>

      <el-scrollbar class="col-scroll">
        <div v-if="!currentPaperId" class="empty-placeholder-small">
          请先选择左侧试卷
        </div>
        <div v-else class="student-list" v-loading="studentLoading">
          <div
              v-for="item in studentList"
              :key="item.id"
              class="student-item"
              :class="{ active: currentRecordId === item.id }"
              @click="handleSelectStudent(item)"
          >
            <div class="student-info">
              <span class="s-name">{{ item.studentName }}</span>
              <span class="s-dept">{{ item.deptName }}</span>
            </div>
            <div class="student-status">
              <span class="status-dot" :class="item.status === 2 ? 'pending' : 'success'"></span>
              <span class="status-text">{{ item.status === 2 ? '待阅' : '已阅' }}</span>
            </div>
          </div>

          <div v-if="studentTotal > studentList.length" class="load-more" @click="loadMoreStudents">
            加载更多...
          </div>

          <div v-if="!studentLoading && studentList.length === 0" class="empty-text">无匹配考生</div>
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
            </div>
          </div>
          <div class="dh-right">
            <div class="score-preview">
              当前总分: <strong>{{ currentRecordTotalScore }}</strong>
            </div>
            <el-button type="primary" :loading="submitting" @click="submitAllReviews">
              <el-icon class="el-icon--left"><Check /></el-icon> 保存全部更改
            </el-button>
          </div>
        </header>

        <!-- 试题内容滚动区 -->
        <el-scrollbar class="detail-content" v-loading="detailLoading">
          <div class="review-wrapper">
            <!-- 仅显示主观题 -->
            <template v-if="subjectiveQuestions.length > 0">
              <div
                  v-for="(q, index) in subjectiveQuestions"
                  :key="q.questionId"
                  class="question-card"
              >
                <!-- 题目头 -->
                <div class="qc-header">
                  <span class="qc-index">第 {{ index + 1 }} 题</span>
                  <span class="qc-tag">{{ getQuestionTypeName(q.type) }}</span>
                  <span class="qc-score">满分: {{ q.maxScore }}</span>

                  <span v-if="q.isMarked === 1" class="qc-status marked">
                    <el-icon><CircleCheck /></el-icon> 已复核
                  </span>
                  <span v-else class="qc-status pending">
                    AI评分完成，待复核
                  </span>
                </div>

                <!-- 题目内容 -->
                <div class="qc-body">
                  <div class="q-text" v-html="formatContent(q.content)"></div>

                  <!-- 学生答案 -->
                  <div class="student-answer">
                    <div class="sa-label">考生作答</div>
                    <div class="sa-content">{{ q.studentAnswer || '（考生未作答）' }}</div>
                  </div>

                  <!-- 评分区域：AI 建议 + 人工复核 -->
                  <div class="grading-area">

                    <!-- AI 建议区 -->
                    <div class="ai-box">
                      <div class="ai-head">
                        <span class="ai-title"><el-icon><Cpu /></el-icon> AI 预评</span>
                        <el-button
                            type="success"
                            link
                            size="small"
                            @click="adoptAiScore(q)"
                        >
                          采纳建议
                        </el-button>
                      </div>
                      <div class="ai-score-row">
                        建议得分: <strong class="score-val">{{ q.aiScore }}</strong>
                      </div>
                      <div class="ai-comment">{{ q.aiComment || 'AI 暂无详细评语' }}</div>
                    </div>

                    <!-- 人工打分 -->
                    <div class="manual-box">
                      <div class="manual-head">
                        <el-icon><EditPen /></el-icon> 人工复核
                      </div>
                      <div class="manual-body">
                        <div class="form-row score-row">
                          <span class="label">得分</span>
                          <el-input-number
                              v-model="q.score"
                              :min="0"
                              :max="q.maxScore"
                              :precision="1"
                              size="default"
                              controls-position="right"
                              @change="markAsModified(q)"
                              class="score-input"
                          />
                          <span class="score-suffix">/ {{ q.maxScore }}</span>
                        </div>
                        <div class="form-row comment-row">
                          <span class="label">评语</span>
                          <el-input
                              v-model="q.teacherComment"
                              type="textarea"
                              :rows="2"
                              placeholder="请输入评语..."
                              @input="markAsModified(q)"
                              class="comment-input"
                          />
                        </div>
                      </div>

                      <div class="action-row">
                        <el-button
                            type="primary"
                            plain
                            size="small"
                            @click="submitSingleQuestion(q)"
                            :disabled="!q._modified"
                        >
                          保存此题
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <div v-else class="empty-text">
              本试卷没有需要人工阅卷的主观题。
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
import { ref, onMounted } from 'vue'
import { Files, User, Search, EditPen, Check, CircleCheck, Cpu } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

// --- Interfaces ---

interface ReviewPaperResp {
  id: number
  title: string
  courseId: number
  courseName: string
  status: number
  totalCount: number
  pendingCount: number
  reviewedCount: number
}

interface ReviewListResp {
  id: number // recordId
  userId: number
  studentName: string
  deptName: string
  paperId: number
  paperTitle: string
  status: number // 2-待复核, 3-已完成
  totalScore: number
  objectiveScore: number
  submitTime: string
  reviewStatusDesc: string
}

interface ReviewQuestionDetail {
  detailId: number
  questionId: number
  type: number
  content: string
  maxScore: number
  studentAnswer: string
  score: number // 当前得分
  aiComment: string
  isMarked: number

  // 前端辅助字段
  aiScore: number // 初始从后端拿到的分数，视为AI分
  teacherComment: string
  _modified?: boolean
}

// --- State ---

const paperLoading = ref(false)
const paperList = ref<ReviewPaperResp[]>([])
const currentPaperId = ref<number | null>(null)
const currentPaper = ref<ReviewPaperResp | null>(null)

const studentLoading = ref(false)
const studentList = ref<ReviewListResp[]>([])
const studentTotal = ref(0)
const studentPage = ref(1)
const studentSize = ref(20)

const searchKeyword = ref('')
const currentStatus = ref<number | null>(2)

const currentRecordId = ref<number | null>(null)
const currentStudent = ref<ReviewListResp | null>(null)

const detailLoading = ref(false)
const subjectiveQuestions = ref<ReviewQuestionDetail[]>([])
const currentRecordTotalScore = ref(0)
const submitting = ref(false)

// --- Methods ---

const fetchPapers = async (silent = false) => {
  if (!silent) paperLoading.value = true
  try {
    const res = await request.get<ReviewPaperResp[]>('/review/papers')
    paperList.value = res || []
  } catch (e) {
    console.error(e)
    ElMessage.error('获取试卷列表失败')
  } finally {
    if (!silent) paperLoading.value = false
  }
}

const handleSelectPaper = (paper: ReviewPaperResp) => {
  currentPaperId.value = paper.id
  currentPaper.value = paper
  currentRecordId.value = null
  studentPage.value = 1
  studentList.value = []
  fetchStudents()
}

const fetchStudents = async (append = false) => {
  if (!currentPaperId.value) return
  studentLoading.value = true
  try {
    const params: any = {
      page: studentPage.value,
      size: studentSize.value,
      paperId: currentPaperId.value,
    }
    if (searchKeyword.value) params.studentName = searchKeyword.value
    if (currentStatus.value !== null) params.status = currentStatus.value

    const res = await request.get<any>('/review/list', { params })
    if (res && res.records) {
      if (append) {
        studentList.value = [...studentList.value, ...res.records]
      } else {
        studentList.value = res.records
      }
      studentTotal.value = res.total
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取考生列表失败')
  } finally {
    studentLoading.value = false
  }
}

const handleStudentFilter = () => {
  studentPage.value = 1
  fetchStudents(false)
}

const loadMoreStudents = () => {
  studentPage.value++
  fetchStudents(true)
}

const handleSelectStudent = async (student: ReviewListResp) => {
  currentRecordId.value = student.id
  currentStudent.value = student
  detailLoading.value = true
  subjectiveQuestions.value = []

  try {
    const res = await request.get<any>(`/review/detail/${student.id}`)
    if (res) {
      currentRecordTotalScore.value = res.record.totalScore || 0
      const allQuestions = res.questions as any[]

      subjectiveQuestions.value = allQuestions
          .filter(q => q.type === 4 || q.type === 5)
          .map(q => {
            // 初始加载的分数视为 AI 分数 (或上次保存的分数)
            const initialScore = q.score !== undefined ? q.score : 0;
            return {
              ...q,
              score: initialScore, // 人工评分框默认显示当前分（即AI分）
              aiScore: initialScore, // 缓存一份作为AI基准分，用于“采纳”回滚
              // 修改点：不再默认将 aiComment 赋值给 teacherComment
              // 如果后端有保存过的人工评语，则显示；否则显示空字符串
              // 假设后端复用 aiComment 字段存人工评语，则无法区分。
              // 但如果后端没有专门的 teacherComment 字段返回，
              // 且题目状态是未复核(isMarked=0)，则 teacherComment 应该为空。
              // 题目状态是已复核(isMarked=1)，则 teacherComment 显示 q.aiComment (因为后端目前复用了字段)

              // 逻辑优化：
              // 如果已复核(isMarked=1)，显示 q.aiComment (这是老师之前提交的)
              // 如果未复核(isMarked=0)，显示空字符串 (不显示AI评语)
              teacherComment: q.isMarked === 1 ? (q.aiComment || '') : '',

              // 为了显示在左侧AI建议区，我们需要原始的AI评语。
              // 但后端如果复用了字段，且已复核覆盖了AI评语，那么左侧AI建议区也会变成老师的评语。
              // 这是一个后端数据结构的设计缺陷（建议分离 ai_comment 和 teacher_comment）。
              // 前端暂时只能做到：未复核时，人工框为空；已复核时，显示存储的评语。
              _modified: false
            }
          })
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取试卷详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 采纳 AI 建议：恢复到初始加载的分数和评语
const adoptAiScore = (q: ReviewQuestionDetail) => {
  q.score = q.aiScore
  q.teacherComment = q.aiComment || ''
  q._modified = true // 标记为修改，允许提交
  ElMessage.success('已恢复 AI 预评分数')
}

const getQuestionTypeName = (type: number) => {
  const map: Record<number, string> = { 4: '简答题', 5: '填空题' }
  return map[type] || '主观题'
}

const formatContent = (val: string) => val ? val.replace(/\n/g, '<br/>') : ''

const markAsModified = (q: ReviewQuestionDetail) => {
  q._modified = true
}

const submitSingleQuestion = async (q: ReviewQuestionDetail) => {
  if (!currentRecordId.value) return

  try {
    await request.post('/review/submit', {
      recordId: currentRecordId.value,
      questionId: q.questionId,
      score: q.score,
      comment: q.teacherComment
    })

    q._modified = false
    q.isMarked = 1
    // 更新后，当前分数成为新的基准，但我们保持 aiScore 不变作为参考
    ElMessage.success('保存成功')

    // 关键修复：刷新试卷列表，更新 pendingCount
    await fetchPapers(true)
  } catch (e: any) {
    console.error(e)
    ElMessage.error(e.message || '保存失败')
  }
}

const submitAllReviews = async () => {
  if (!currentRecordId.value) return
  const targets = subjectiveQuestions.value

  if (targets.length === 0) {
    ElMessage.warning('没有主观题需要提交')
    return
  }

  submitting.value = true
  try {
    const promises = targets.map(q => {
      return request.post('/review/submit', {
        recordId: currentRecordId.value,
        questionId: q.questionId,
        score: q.score,
        comment: q.teacherComment
      })
    })

    await Promise.all(promises)

    ElMessage.success('全部提交成功')

    if (currentStudent.value) {
      currentStudent.value.status = 3
    }

    // 关键修复：刷新试卷列表，更新 pendingCount
    await fetchPapers(true) // 使用 await 确保状态最新

    const nextIndex = studentList.value.findIndex(s => s.status === 2 && s.id !== currentRecordId.value)
    if (nextIndex !== -1) {
      handleSelectStudent(studentList.value[nextIndex])
    } else {
      handleStudentFilter()
      currentRecordId.value = null
    }

  } catch (e: any) {
    console.error(e)
    ElMessage.error('部分提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchPapers()
})
</script>

<style scoped>
/* 原生 CSS 样式 - 三栏布局 */

.review-console {
  display: flex;
  height: calc(100vh - 64px);
  background-color: #f7f9fc;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #1a202c;
  overflow: hidden;
}

/* --- 第一栏 & 第二栏 保持不变 --- */
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

.col-scroll { flex: 1; }
.paper-list { padding: 8px; }

.paper-item {
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
  border: 1px solid transparent;
}
.paper-item:hover { background-color: #f7fafc; }
.paper-item.active { background-color: #ebf4ff; border-color: #bee3f8; }

.paper-name {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.paper-item.active .paper-name { color: #3182ce; }

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
.badge.pending { background: #fff7ed; color: #c05621; }
.badge.done { background: #f0fff4; color: #38a169; }

.col-student {
  width: 280px;
  background-color: #fafbfc;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.col-student .col-header { height: auto; padding: 12px 16px; gap: 10px; }

.filter-row { display: flex; gap: 8px; }
.filter-input { flex: 1; }
.filter-select { width: 80px; }

.student-list { padding: 8px; }
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
.student-item:hover { border-color: #cbd5e0; }
.student-item.active { background-color: #fff; border-color: #4f46e5; box-shadow: 0 0 0 1px #4f46e5 inset; }

.student-info { display: flex; flex-direction: column; }
.s-name { font-size: 14px; font-weight: 600; color: #1f2937; }
.s-dept { font-size: 12px; color: #9ca3af; }

.student-status { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot.pending { background-color: #f59e0b; }
.status-dot.success { background-color: #10b981; }
.status-text { color: #6b7280; }

.load-more { text-align: center; font-size: 12px; color: #4f46e5; padding: 10px; cursor: pointer; }
.load-more:hover { text-decoration: underline; }

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

.dh-left { display: flex; flex-direction: column; }
.dh-title { font-size: 16px; font-weight: 700; color: #1f2937; }
.dh-student { font-size: 13px; color: #6b7280; }
.dh-student .highlight { color: #4f46e5; font-weight: 600; }

.dh-right { display: flex; align-items: center; gap: 20px; }
.score-preview { font-size: 13px; color: #4b5563; }

.detail-content { flex: 1; }
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
.qc-score { font-size: 12px; color: #9ca3af; }
.qc-status { margin-left: auto; font-size: 12px; display: flex; align-items: center; gap: 4px; }
.qc-status.marked { color: #10b981; }
.qc-status.pending { color: #f59e0b; }

.qc-body { padding: 24px; }
.q-text { font-size: 15px; color: #1f2937; margin-bottom: 20px; line-height: 1.6; }

.student-answer {
  background-color: #f9fafb;
  border-radius: 6px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  margin-bottom: 20px;
}
.sa-label { font-size: 12px; color: #6b7280; margin-bottom: 8px; font-weight: 600; }
.sa-content { font-size: 15px; color: #111827; white-space: pre-wrap; }

/* 评分区域 (恢复 AI + Manual 双栏) */
.grading-area {
  display: flex;
  gap: 16px;
}

/* AI 建议卡片 */
.ai-box {
  flex: 1;
  background-color: #eff6ff; /* 蓝色系背景 */
  border: 1px solid #dbeafe;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
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

.ai-title {
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

.ai-comment {
  font-size: 13px;
  color: #1f2937;
  opacity: 0.9;
  line-height: 1.5;
  background: rgba(255,255,255,0.6);
  padding: 8px;
  border-radius: 4px;
  flex: 1; /* 撑满剩余高度 */
}

/* 人工评分卡片 */
.manual-box {
  flex: 1;
  background-color: #fff;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  padding: 16px;
}

.manual-head {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.manual-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.form-row .label {
  width: 40px;
  font-size: 13px;
  color: #4b5563;
  font-weight: 500;
  text-align: right;
}

.score-row {
  align-items: center;
}

.score-input {
  width: 160px;
}

.score-suffix {
  font-size: 13px;
  color: #9ca3af;
}

.comment-row {
  align-items: flex-start;
}

.comment-row .label {
  margin-top: 6px;
}

.comment-input {
  flex: 1;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  border-top: 1px solid #f3f4f6;
  padding-top: 12px;
}

.empty-text, .empty-placeholder-small, .empty-placeholder-large {
  color: #9ca3af;
  text-align: center;
}
.empty-text { padding: 20px; font-size: 13px; }
.empty-placeholder-small { font-size: 13px; margin-top: 40px; }
.empty-placeholder-large { flex: 1; display: flex; flex-direction: column; justify-content: center; align-items: center; }
.placeholder-icon { font-size: 64px; color: #e5e7eb; margin-bottom: 24px; }
.footer-msg { text-align: center; color: #cbd5e0; font-size: 12px; padding-bottom: 40px; }
</style>