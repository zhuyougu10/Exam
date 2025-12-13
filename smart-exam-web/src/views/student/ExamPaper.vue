<template>
  <div class="exam-app-container">

    <!-- 全屏检测遮罩 -->
    <!-- 修改点 1: 增加 !isSubmitting 判断，防止交卷退出全屏时闪烁遮罩 -->
    <div v-if="!isFullscreen && !isSubmitting" class="fullscreen-mask">
      <div class="mask-content">
        <el-icon class="mask-icon"><Warning /></el-icon>
        <h2>考试已暂停</h2>
        <p>检测到您退出了全屏模式，内容已遮蔽。<br>请点击下方按钮恢复答题环境。</p>
        <button class="resume-btn" @click="handleResume">恢复全屏并继续</button>
      </div>
    </div>

    <!-- 顶部 Header -->
    <header class="exam-header">
      <div class="header-left">
        <div class="logo-box">
          <el-icon><Document /></el-icon>
        </div>
        <div class="title-box">
          <h1 :title="paperData.title">{{ paperData.title || '试卷加载中...' }}</h1>
          <span class="subtitle">智能考试系统 v2.0</span>
        </div>
      </div>

      <div class="header-center">
        <div class="timer-capsule" :class="{ 'urgent': remainingTime < 300 }">
          <el-icon><Timer /></el-icon>
          <div class="time-info">
            <span class="label">剩余时间</span>
            <span class="value">{{ formattedTime }}</span>
          </div>
        </div>
      </div>

      <div class="header-right">
        <div class="user-info">
          <div class="text-info">
            <span class="name">{{ userStore.userInfo?.realName || '考生' }}</span>
            <span class="id">ID: {{ userStore.userInfo?.username }}</span>
          </div>
          <el-avatar :size="40" :src="userStore.userInfo?.avatar" class="user-avatar">
            {{ userStore.userInfo?.realName?.charAt(0) }}
          </el-avatar>
        </div>
        <button class="submit-btn" @click="confirmSubmit">交 卷</button>
      </div>
    </header>

    <!-- 主体布局 -->
    <div class="exam-body">
      <!-- 左侧答题卡 -->
      <aside class="exam-sidebar">
        <div class="sidebar-header">
          <h3><el-icon><Grid /></el-icon> 答题概览</h3>
          <div class="legend">
            <span class="dot uncheck"></span> 未答
            <span class="dot checked"></span> 已答
            <span class="dot current"></span> 当前
          </div>
        </div>

        <el-scrollbar class="sidebar-scroll">
          <div class="card-grid">
            <button
                v-for="(q, index) in paperData.questions"
                :key="q.id"
                @click="scrollToQuestion(index)"
                class="card-item"
                :class="{
                'current': currentIndex === index,
                'answered': isAnswered(q.id),
                'marked': userMarks[q.id]
              }"
            >
              {{ index + 1 }}
              <span v-if="userMarks[q.id]" class="mark-dot"></span>
            </button>
          </div>
        </el-scrollbar>

        <div class="sidebar-footer">
          <div class="progress-info">
            <span>当前进度</span>
            <strong>{{ answeredCount }} / {{ paperData.questions?.length }}</strong>
          </div>
          <div class="progress-bar-bg">
            <div class="progress-bar-fill" :style="{ width: `${progressPercentage}%` }"></div>
          </div>
        </div>
      </aside>

      <!-- 中间试卷内容 -->
      <main class="exam-main">
        <div class="paper-wrapper">

          <!-- 试卷信息卡 -->
          <div class="paper-info-card">
            <h1 class="paper-title">{{ paperData.title }}</h1>
            <div class="paper-meta">
              <span class="meta-tag"><el-icon><Grid /></el-icon> 总分: {{ paperData.totalScore }}</span>
              <span class="meta-tag"><el-icon><Flag /></el-icon> 难度: {{ getDifficultyLabel(paperData.difficulty) }}</span>
              <span class="meta-tag"><el-icon><Timer /></el-icon> 时长: {{ Math.floor(paperData.duration / 60) }} 分钟</span>
            </div>
          </div>

          <!-- 题目列表 -->
          <div
              v-for="(question, index) in paperData.questions"
              :key="question.id"
              :id="'q-' + index"
              class="question-card"
              :class="{ 'active-card': currentIndex === index }"
              @click="currentIndex = index"
          >
            <!-- 题目头部 -->
            <div class="q-header">
              <div class="q-index">{{ index + 1 }}</div>
              <div class="q-meta">
                <span class="q-type">{{ getQuestionTypeName(question.type) }}</span>
                <span class="q-score">{{ question.score }} 分</span>
              </div>
              <button class="mark-btn" :class="{ 'is-marked': userMarks[question.id] }" @click.stop="toggleMark(question.id)">
                <el-icon><Flag /></el-icon> {{ userMarks[question.id] ? '已标记' : '标记' }}
              </button>
            </div>

            <!-- 题目内容 -->
            <div class="q-content">
              <div class="q-text" v-html="formatContent(question.content)"></div>

              <div v-if="question.imageUrl" class="q-image">
                <el-image :src="question.imageUrl" :preview-src-list="[question.imageUrl]" fit="contain" />
              </div>

              <!-- 答题区 -->
              <div class="q-answer-area">

                <!-- 单选 / 判断 -->
                <div v-if="[1, 3].includes(question.type)" class="options-list">
                  <div
                      v-for="opt in parseOptions(question.options)"
                      :key="opt.key"
                      class="option-item"
                      :class="{ 'selected': userAnswers[question.id] === opt.key }"
                      @click="userAnswers[question.id] = opt.key; handleAnswerChange()"
                  >
                    <div class="opt-key">{{ opt.key }}</div>
                    <div class="opt-val">{{ opt.value }}</div>
                  </div>
                </div>

                <!-- 多选 -->
                <div v-if="question.type === 2" class="options-list">
                  <div
                      v-for="opt in parseOptions(question.options)"
                      :key="opt.key"
                      class="option-item"
                      :class="{ 'selected': isChecked(question.id, opt.key) }"
                      @click="toggleCheckbox(question.id, opt.key); handleAnswerChange()"
                  >
                    <div class="opt-key checkbox-key">{{ opt.key }}</div>
                    <div class="opt-val">{{ opt.value }}</div>
                  </div>
                </div>

                <!-- 简答 / 填空 -->
                <div v-if="[4, 5].includes(question.type)" class="text-answer">
                  <textarea
                      v-model="userAnswers[question.id]"
                      rows="6"
                      placeholder="请输入您的答案..."
                      @input="handleAnswerChange"
                  ></textarea>
                  <span class="char-count">{{ (userAnswers[question.id] || '').length }} 字符</span>
                </div>

              </div>
            </div>
          </div>

          <!-- 底部交卷区 -->
          <div class="paper-footer">
            <div class="divider"></div>
            <p>已到达试卷底部</p>
            <button class="submit-btn-large" @click="confirmSubmit">确认提交试卷</button>
          </div>

        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { Timer, Document, Grid, Flag, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import request from '@/utils/request'
import { useProctor } from '@/hooks/useProctor'

// ---------------- 类型定义 ----------------
interface QuestionOption { key: string; value: string; }
interface Question {
  id: number;
  type: 1 | 2 | 3 | 4 | 5;
  content: string;
  score: number;
  options: string;
  imageUrl?: string;
  savedAnswer?: string | null;
}
interface PaperData {
  recordId: number | null;
  paperId: number | null;
  title: string;
  totalScore: number;
  duration: number;
  difficulty: number;
  questions: Question[];
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const currentIndex = ref(0)
const remainingTime = ref(0)
const userMarks = ref<Record<number, boolean>>({})
// 修改点 2: 增加提交中状态
const isSubmitting = ref(false)
let timerInterval: any = null

const paperData = ref<PaperData>({
  recordId: null,
  paperId: null,
  title: '',
  totalScore: 0,
  duration: 0,
  difficulty: 1,
  questions: []
})

const userAnswers = ref<Record<number, any>>({})

const { isFullscreen, enterFullscreen } = useProctor({
  recordId: () => paperData.value.recordId,
  onAutoSubmit: () => submitExam(true),
  saveAnswers: () => saveProgressToLocal()
})

const handleResume = () => {
  enterFullscreen()
}

// ---------------- 核心逻辑 ----------------
const initExam = async () => {
  const publishId = route.query.publishId || route.params.publishId
  if (!publishId) {
    ElMessage.error('参数错误: 缺少 publishId')
    router.replace('/student/exam-list')
    return
  }

  try {
    const res = await request.post<any>(`/exam/start/${publishId}`)
    paperData.value = res.data || res
    // 兼容后端可能返回的字段
    remainingTime.value = (res.remainingSeconds !== undefined) ? res.remainingSeconds : (res.duration * 60)

    if (paperData.value.questions) {
      paperData.value.questions.forEach((q: Question) => {
        // 恢复答案
        if (q.savedAnswer) {
          if (q.type === 2) {
            // 多选转数组
            userAnswers.value[q.id] = q.savedAnswer.split(',').map(String)
          } else {
            userAnswers.value[q.id] = q.savedAnswer
          }
        } else if (q.type === 2) {
          userAnswers.value[q.id] = []
        }
      })
      loadProgressFromLocal()
    }

    startTimer()
    loading.value = false
  } catch (error) {
    console.error(error)
    ElMessage.error('试卷加载失败或考试已结束')
    router.replace('/student/exam-list')
  }
}

const startTimer = () => {
  if (timerInterval) clearInterval(timerInterval)
  timerInterval = setInterval(() => {
    if (remainingTime.value > 0) {
      remainingTime.value--
    } else {
      clearInterval(timerInterval)
      ElMessageBox.alert('考试时间到，系统将自动交卷', '提示', {
        confirmButtonText: '确定',
        showClose: false,
        type: 'warning',
        callback: () => submitExam(true)
      })
    }
  }, 1000)
}

const formattedTime = computed(() => {
  const h = Math.floor(remainingTime.value / 3600)
  const m = Math.floor((remainingTime.value % 3600) / 60)
  const s = remainingTime.value % 60
  if (h > 0) return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
})

// 滚动定位
const scrollToQuestion = (index: number) => {
  currentIndex.value = index
  const element = document.getElementById(`q-${index}`)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

const toggleMark = (qId: number) => {
  userMarks.value[qId] = !userMarks.value[qId]
}

const getCacheKey = () => `exam_cache_${userStore.userInfo?.id || 'guest'}_${paperData.value.recordId}`

const saveProgressToLocal = () => {
  if (!paperData.value.recordId) return
  localStorage.setItem(getCacheKey(), JSON.stringify(userAnswers.value))
}

const loadProgressFromLocal = () => {
  if (!paperData.value.recordId) return
  const cached = localStorage.getItem(getCacheKey())
  if (cached) {
    try {
      const localAnswers = JSON.parse(cached)
      Object.keys(localAnswers).forEach(key => {
        const qId = Number(key)
        if (!userAnswers.value[qId] || (Array.isArray(userAnswers.value[qId]) && userAnswers.value[qId].length === 0)) {
          userAnswers.value[qId] = localAnswers[qId]
        }
      })
    } catch (e) {
      console.error('Local cache parse error', e)
    }
  }
}

const handleAnswerChange = () => {
  saveProgressToLocal()
}

// 多选处理
const toggleCheckbox = (qid: number, key: string) => {
  const current = userAnswers.value[qid] || []
  const idx = current.indexOf(key)
  if (idx > -1) {
    current.splice(idx, 1)
  } else {
    current.push(key)
  }
  userAnswers.value[qid] = [...current] // 触发更新
}

const isChecked = (qid: number, key: string) => {
  const ans = userAnswers.value[qid]
  if (Array.isArray(ans)) {
    return ans.includes(key)
  }
  return false
}

const isAnswered = (qid: number) => {
  const val = userAnswers.value[qid]
  if (Array.isArray(val)) return val.length > 0
  return val !== undefined && val !== null && String(val).trim() !== ''
}

const answeredCount = computed(() => {
  return paperData.value.questions?.filter((q) => isAnswered(q.id)).length || 0
})

const progressPercentage = computed(() => {
  if (!paperData.value.questions?.length) return 0
  return Math.min(100, Math.round((answeredCount.value / paperData.value.questions.length) * 100))
})

const confirmSubmit = () => {
  const total = paperData.value.questions.length
  const answered = answeredCount.value
  const msg = answered < total
      ? `还有 <span style="color:#ef4444;font-weight:bold">${total - answered}</span> 道题未作答，确定交卷？`
      : '确定要提交试卷吗？'

  ElMessageBox.confirm(msg, '交卷确认', {
    confirmButtonText: '确定交卷',
    cancelButtonText: '再检查一下',
    type: 'warning',
    dangerouslyUseHTMLString: true,
    center: true,
    roundButton: true
  }).then(() => {
    submitExam(false)
  })
}

const submitExam = async (force = false) => {
  isSubmitting.value = true // 修改点 3: 标记开始提交，抑制全屏检测

  const loadingInstance = ElLoading.service({
    lock: true,
    text: force ? '时间到，自动收卷中...' : '正在加密提交...',
    background: 'rgba(255, 255, 255, 0.95)',
  })

  try {
    const answersList = Object.entries(userAnswers.value).map(([qId, val]) => {
      const q = paperData.value.questions.find(i => i.id == Number(qId))
      if (!q) return null
      let submitVal = ''
      if (Array.isArray(val)) {
        submitVal = JSON.stringify(val.sort())
      } else {
        submitVal = String(val)
      }
      return { questionId: Number(qId), userAnswer: submitVal }
    }).filter(Boolean)

    await request.post('/exam/submit', {
      recordId: paperData.value.recordId,
      answers: answersList
    })

    ElMessage.success('交卷成功！')

    // 修改点 4: 提交成功后，主动退出全屏
    if (document.fullscreenElement) {
      await document.exitFullscreen().catch(() => {})
    }

    localStorage.removeItem(getCacheKey())
    router.replace(`/student/exam-result?recordId=${paperData.value.recordId}`)
  } catch (error: any) {
    console.error(error)
    ElMessage.error(error.message || '交卷失败')
    isSubmitting.value = false // 失败后恢复全屏检测
  } finally {
    loadingInstance.close()
  }
}

const formatContent = (content: string) => {
  if(!content) return ''
  return content.replace(/\n/g, '<br/>')
}

const parseOptions = (jsonStr: string): QuestionOption[] => {
  try {
    const arr = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr
    if (Array.isArray(arr)) {
      if (typeof arr[0] === 'string') {
        return arr.map((item: string, idx: number) => ({
          key: String.fromCharCode(65 + idx),
          value: item
        }))
      }
      return arr
    }
    return []
  } catch (e) {
    return []
  }
}

const getQuestionTypeTag = (type: number) => {
  // 这里仅保留逻辑，样式在CSS中定义
  return ''
}

const getQuestionTypeName = (type: number) => {
  const map: Record<number, string> = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '简答题', 5: '填空题' }
  return map[type] || '未知'
}

const getDifficultyLabel = (diff: number) => {
  const map: Record<number, string> = { 1: '简单', 2: '中等', 3: '困难' }
  return map[diff] || '普通'
}

onMounted(() => {
  initExam()
  // 防止意外刷新
  window.onbeforeunload = (e) => {
    e = e || window.event;
    if (e) {
      e.returnValue = '考试进行中';
    }
    return '考试进行中';
  };
})

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
  window.onbeforeunload = null
})
</script>

<style scoped>
/* ----------------------------------------
  原生 CSS 样式重构 - Native Styles
  风格：干净、专业、类似 Notion/Linear
  ----------------------------------------
*/

/* 全局容器：使用 fixed 定位强行覆盖整个视口，
   Z-Index 设为 2000 以高于 Layout 的 Sidebar/Header */
.exam-app-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  background-color: #f7f9fc; /* 必须设置背景色，防止底层透出 */
  color: #1a202c;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  overflow: hidden;
}

/* ---------------- HEADER ---------------- */
.exam-header {
  height: 64px;
  background-color: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
  z-index: 100;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 300px;
}

.logo-box {
  width: 36px;
  height: 36px;
  background-color: #4f46e5; /* Indigo */
  color: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.title-box h1 {
  font-size: 16px;
  font-weight: 700;
  margin: 0;
  line-height: 1.2;
  color: #2d3748;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 220px;
}

.subtitle {
  font-size: 12px;
  color: #718096;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.timer-capsule {
  display: flex;
  align-items: center;
  gap: 10px;
  background-color: #fff;
  border: 1px solid #e2e8f0;
  padding: 6px 16px;
  border-radius: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
  transition: all 0.3s ease;
}

.timer-capsule.urgent {
  border-color: #feb2b2;
  background-color: #fff5f5;
  color: #c53030;
  animation: pulse 2s infinite;
}

.timer-capsule .el-icon {
  font-size: 20px;
  color: #718096;
}

.timer-capsule.urgent .el-icon {
  color: #f56565;
}

.time-info {
  display: flex;
  flex-direction: column;
  line-height: 1;
}

.time-info .label {
  font-size: 10px;
  color: #a0aec0;
  text-transform: uppercase;
  font-weight: 700;
}

.time-info .value {
  font-family: 'Roboto Mono', monospace;
  font-size: 18px;
  font-weight: 700;
  color: #2d3748;
}

.timer-capsule.urgent .time-info .value {
  color: #c53030;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
  width: 300px;
  justify-content: flex-end;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: right;
}

.user-info .text-info {
  display: flex;
  flex-direction: column;
}

.user-info .name {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
}

.user-info .id {
  font-size: 12px;
  color: #a0aec0;
}

.submit-btn {
  background-color: #4f46e5;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
}

.submit-btn:hover {
  background-color: #4338ca;
}

/* ---------------- BODY & SIDEBAR ---------------- */
.exam-body {
  display: flex;
  flex: 1;
  overflow: hidden; /* 防止双滚动条 */
}

.exam-sidebar {
  width: 280px;
  background-color: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #f7fafc;
}

.sidebar-header h3 {
  font-size: 14px;
  font-weight: 700;
  margin: 0 0 12px 0;
  color: #4a5568;
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #718096;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
  margin-right: 4px;
}

.dot.uncheck { background: #fff; border: 1px solid #cbd5e0; }
.dot.checked { background: #ebf4ff; border: 1px solid #4299e1; }
.dot.current { background: #fffff0; border: 1px solid #ecc94b; }

.sidebar-scroll {
  flex: 1;
}

.card-grid {
  padding: 20px;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.card-item {
  height: 36px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background-color: #fff;
  color: #718096;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
}

.card-item:hover {
  border-color: #a0aec0;
  color: #2d3748;
}

.card-item.answered {
  background-color: #ebf4ff;
  border-color: #bee3f8;
  color: #3182ce;
}

.card-item.current {
  background-color: #ed8936;
  border-color: #ed8936;
  color: #fff;
  box-shadow: 0 2px 6px rgba(237, 137, 54, 0.4);
  transform: scale(1.05);
  z-index: 1;
}

.mark-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  background-color: #f56565;
  border: 1px solid #fff;
  border-radius: 50%;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid #e2e8f0;
  background-color: #f8fafc;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #718096;
  margin-bottom: 8px;
}

.progress-bar-bg {
  height: 8px;
  background-color: #edf2f7;
  border-radius: 4px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #4f46e5, #9f7aea);
  transition: width 0.5s ease;
}

/* ---------------- MAIN CONTENT ---------------- */
.exam-main {
  flex: 1;
  overflow-y: auto;
  padding: 40px;
  scroll-behavior: smooth;
}

.paper-wrapper {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 30px;
  padding-bottom: 100px;
}

/* 试卷头部信息卡 */
.paper-info-card {
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.02);
  border: 1px solid #edf2f7;
}

.paper-title {
  font-size: 24px;
  color: #1a202c;
  margin: 0 0 16px 0;
}

.paper-meta {
  display: flex;
  gap: 16px;
}

.meta-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  background-color: #f7fafc;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #4a5568;
  border: 1px solid #edf2f7;
}

/* 题目卡片 */
.question-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
  border: 1px solid #edf2f7;
  overflow: hidden;
  transition: box-shadow 0.3s, border-color 0.3s;
}

.question-card.active-card {
  border-color: #ed8936;
  box-shadow: 0 4px 12px rgba(237, 137, 54, 0.1);
}

.q-header {
  padding: 20px 30px;
  background-color: #fbfdff;
  border-bottom: 1px solid #edf2f7;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.q-index {
  width: 32px;
  height: 32px;
  background-color: #2d3748;
  color: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  margin-right: 16px;
}

.q-meta {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.q-type {
  font-size: 13px;
  font-weight: 700;
  color: #4a5568;
  background: #edf2f7;
  padding: 4px 8px;
  border-radius: 4px;
}

.q-score {
  font-size: 13px;
  color: #718096;
  font-weight: 500;
}

.mark-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  color: #cbd5e0;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  transition: color 0.2s;
}

.mark-btn:hover { color: #a0aec0; }
.mark-btn.is-marked { color: #ed8936; }

.q-content {
  padding: 30px;
}

.q-text {
  font-size: 17px;
  line-height: 1.8;
  color: #2d3748;
  margin-bottom: 20px;
}

.q-image {
  margin-bottom: 20px;
  padding: 10px;
  background: #f7fafc;
  border-radius: 8px;
  border: 1px solid #edf2f7;
  display: inline-block;
}

/* 选项区域 */
.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border: 2px solid #edf2f7;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.option-item:hover {
  border-color: #cbd5e0;
  background-color: #f7fafc;
}

.option-item.selected {
  border-color: #667eea;
  background-color: #ebf4ff;
}

.opt-key {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid #e2e8f0;
  color: #718096;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  margin-right: 16px;
  flex-shrink: 0;
  transition: all 0.2s;
}

.checkbox-key {
  border-radius: 6px; /* 多选为方框 */
}

.option-item:hover .opt-key {
  border-color: #cbd5e0;
  color: #4a5568;
}

.option-item.selected .opt-key {
  background-color: #4f46e5;
  border-color: #4f46e5;
  color: white;
}

.opt-val {
  font-size: 16px;
  line-height: 1.6;
  color: #4a5568;
  padding-top: 2px;
}

.option-item.selected .opt-val {
  color: #2d3748;
  font-weight: 500;
}

/* 文本答题 */
.text-answer {
  position: relative;
}

.text-answer textarea {
  width: 100%;
  padding: 16px;
  border: 2px solid #edf2f7;
  border-radius: 10px;
  font-size: 16px;
  line-height: 1.6;
  color: #2d3748;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s;
  background-color: #fbfdff;
}

.text-answer textarea:focus {
  border-color: #667eea;
  background-color: white;
}

.char-count {
  position: absolute;
  bottom: 12px;
  right: 12px;
  font-size: 12px;
  color: #a0aec0;
  pointer-events: none;
}

/* 底部 */
.paper-footer {
  text-align: center;
  margin-top: 40px;
}

.paper-footer .divider {
  width: 60px;
  height: 4px;
  background-color: #e2e8f0;
  margin: 0 auto 16px;
  border-radius: 2px;
}

.paper-footer p {
  color: #a0aec0;
  font-size: 14px;
  margin-bottom: 24px;
}

.submit-btn-large {
  background-color: #2d3748;
  color: white;
  padding: 14px 40px;
  font-size: 18px;
  font-weight: 700;
  border-radius: 30px;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 15px rgba(45, 55, 72, 0.2);
  transition: transform 0.2s, box-shadow 0.2s;
}

.submit-btn-large:hover {
  background-color: #1a202c;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(26, 32, 44, 0.3);
}

.submit-btn-large:active {
  transform: translateY(1px);
}

/* ---------------- OVERLAY ---------------- */
.fullscreen-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(26, 32, 44, 0.95);
  backdrop-filter: blur(10px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mask-content {
  background: #2d3748;
  padding: 40px;
  border-radius: 16px;
  text-align: center;
  color: white;
  max-width: 400px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.5);
}

.mask-icon {
  font-size: 48px;
  color: #ecc94b;
  margin-bottom: 20px;
}

.mask-content h2 {
  margin: 0 0 10px;
  font-size: 24px;
}

.mask-content p {
  color: #a0aec0;
  margin-bottom: 30px;
  line-height: 1.6;
}

.resume-btn {
  width: 100%;
  padding: 12px;
  background-color: #4f46e5;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
}

.resume-btn:hover {
  background-color: #4338ca;
}

/* ---------------- ANIMATIONS ---------------- */
@keyframes pulse {
  0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(229, 62, 62, 0.7); }
  70% { transform: scale(1.05); box-shadow: 0 0 0 10px rgba(229, 62, 62, 0); }
  100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(229, 62, 62, 0); }
}

/* 响应式微调 */
@media (max-width: 768px) {
  .exam-sidebar {
    display: none; /* 移动端暂藏侧边栏，或改为抽屉 */
  }
  .exam-main {
    padding: 20px;
  }
  .header-center {
    display: none; /* 小屏隐藏倒计时或移至次级 */
  }
}
</style>