<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
// 导入所需图标
import { Trophy, ArrowLeft, Share, Warning, Check, Close } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()

// 响应式数据
const resultData = ref({
  paperTitle: '',
  totalScore: 0,
  paperTotalScore: 100,
  beatRate: 0,
  startTime: '',
  duration: '',
  radarData: [] as any[],
  questions: [] as any[]
})

// 计算错题 (根据 studentScore < score 判断)
const wrongQuestions = computed(() => {
  return resultData.value.questions.filter(q => q.studentScore < q.score)
})

const radarChartRef = ref<HTMLElement | null>(null)
let myChart: echarts.ECharts | null = null;

const initChart = () => {
  if (!radarChartRef.value) return
  if (resultData.value.radarData.length === 0) return // 无数据不渲染

  if (myChart != null) {
    myChart.dispose();
  }
  myChart = echarts.init(radarChartRef.value)

  const option = {
    radar: {
      indicator: resultData.value.radarData.map(item => ({ name: item.name, max: item.max })),
      radius: '65%',
      splitArea: {
        areaStyle: {
          color: ['#f8d5d5', '#f3f6f9', '#ffffff', '#e6f7ff'],
          shadowColor: 'rgba(0, 0, 0, 0.1)',
          shadowBlur: 10
        }
      }
    },
    series: [
      {
        name: '知识点掌握度',
        type: 'radar',
        data: [
          {
            value: resultData.value.radarData.map(item => item.value),
            name: '掌握度',
            areaStyle: {
              color: 'rgba(64, 158, 255, 0.4)'
            },
            itemStyle: {
              color: '#409eff'
            }
          }
        ]
      }
    ]
  }
  myChart.setOption(option)

  window.addEventListener('resize', () => myChart?.resize())
}

// 格式化答案显示 (核心修改)
const formatAnswer = (answer: string | null, type: number) => {
  if (answer === null || answer === undefined || answer === '') return '未作答'

  // 清理可能存在的数组符号
  const clean = String(answer).replace(/[\[\]"]/g, '')

  if (type === 1) { // 单选: 0->A
    const val = parseInt(clean)
    return isNaN(val) ? answer : String.fromCharCode(65 + val)
  }
  if (type === 2) { // 多选: 0,1 -> A,B
    const parts = clean.split(',')
    return parts.map(p => {
      const val = parseInt(p.trim())
      return isNaN(val) ? p : String.fromCharCode(65 + val)
    }).join(',')
  }
  if (type === 3) { // 判断: 0->错误, 1->正确
    if (clean === '0') return '错误'
    if (clean === '1') return '正确'
    return clean
  }
  return answer
}

// 获取选项状态（是否选中、是否正确）
const getOptionStatus = (question: any, opt: any) => {
  const sAns = question.studentAnswer || ''
  const cAns = question.correctAnswer || ''

  let isSelected = false
  let isCorrect = false

  if (question.type === 3) {
    // 判断题：比对内容 (因为答案已格式化为 '正确'/'错误')
    // 假设选项内容也是 '正确' 或 '错误'
    isSelected = sAns === opt.content
    isCorrect = cAns === opt.content
  } else {
    // 选择题：比对 Key (A, B, C...)
    isSelected = sAns.includes(opt.key)
    isCorrect = cAns.includes(opt.key)
  }

  return { isSelected, isCorrect }
}

// 获取选项样式类名
const getOptionClass = (question: any, opt: any) => {
  const { isSelected, isCorrect } = getOptionStatus(question, opt)

  if (isCorrect) {
    return 'option-correct'
  }
  if (isSelected && !isCorrect) {
    return 'option-wrong'
  }
  return 'option-default'
}

// 解析后端选项 JSON 字符串
const parseOptions = (optionsStr: string) => {
  try {
    const opts = JSON.parse(optionsStr)
    // 假设后端存的是 ["A选项", "B选项"] 这样的数组，需要转为 [{key:'A', content:'...'}, ...]
    if (Array.isArray(opts)) {
      return opts.map((content, index) => ({
        key: String.fromCharCode(65 + index),
        content: content
      }))
    }
    return opts
  } catch (e) {
    return []
  }
}

const fetchExamResult = async () => {
  const recordId = route.params.id || route.query.id || route.query.recordId
  const publishId = route.query.publishId

  if (!recordId && !publishId) {
    ElMessage.error('参数错误：缺少考试记录ID或发布ID')
    return
  }

  try {
    let res: any
    if (recordId) {
      res = await request.get(`/exam/result/${recordId}`)
    } else {
      res = await request.get(`/exam/result/publish/${publishId}`)
    }

    // 数据映射：将后端 VO 映射到前端 resultData 结构
    resultData.value = {
      paperTitle: res.title,
      totalScore: res.userScore,
      paperTotalScore: res.totalScore,
      beatRate: res.beatRate || 0,
      startTime: res.startTime,
      duration: res.duration,
      radarData: res.radarData || [],
      questions: res.questionList.map((q: any) => ({
        id: q.id,
        questionNo: q.questionNo,
        type: q.type,
        content: q.content,
        options: parseOptions(q.options),
        // 在此处进行答案格式化
        studentAnswer: formatAnswer(q.studentAnswer, q.type),
        correctAnswer: formatAnswer(q.correctAnswer, q.type),
        score: q.maxScore,
        studentScore: q.score,
        analysis: q.analysis
      }))
    }

    nextTick(() => {
      initChart()
    })

  } catch (error) {
    console.error('获取成绩失败', error)
  }
}

const goBack = () => {
  router.push('/student/exam-list')
}

onMounted(() => {
  fetchExamResult()
})
</script>

<template>
  <div class="result-page">
    <!-- Header Banner -->
    <div class="banner">
      <div class="banner-content">
        <div class="banner-top">
          <el-button link class="back-btn" @click="goBack">
            <el-icon class="mr-1"><ArrowLeft /></el-icon> 返回列表
          </el-button>
          <div class="exam-time">考试时间: {{ resultData.startTime }}</div>
        </div>

        <div class="banner-main">
          <div class="exam-info">
            <h1 class="paper-title">{{ resultData.paperTitle }}</h1>
            <p class="exam-meta">耗时 {{ resultData.duration }} | 满分 {{ resultData.paperTotalScore }}</p>
          </div>

          <div class="score-card-glass">
            <div class="score-item border-right">
              <div class="score-label">最终得分</div>
              <div class="score-value">{{ resultData.totalScore }}</div>
            </div>
            <div class="score-item">
              <div class="score-label">击败考生</div>
              <div class="beat-rate">
                <el-icon class="trophy-icon"><Trophy /></el-icon>
                {{ resultData.beatRate }}%
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Decorative circles -->
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
    </div>

    <!-- Main Content -->
    <div class="main-container">
      <div class="grid-layout">

        <!-- Left Column: Chart & Stats -->
        <div class="left-column">
          <!-- Radar Chart Card -->
          <div class="card" v-if="resultData.radarData && resultData.radarData.length > 0">
            <h3 class="card-title blue-border">能力雷达</h3>
            <div ref="radarChartRef" class="chart-container"></div>
            <div class="chart-hint">基于本次考试知识点得分率生成</div>
          </div>

          <!-- Basic Info Card -->
          <div class="card">
            <h3 class="card-title purple-border">答题概况</h3>
            <div class="stats-list">
              <div class="stat-row">
                <span class="stat-label">题目总数</span>
                <span class="stat-value">{{ resultData.questions.length }} 题</span>
              </div>
              <div class="stat-row">
                <span class="stat-label">错题数量</span>
                <span class="stat-value text-red">{{ wrongQuestions.length }} 题</span>
              </div>
              <div class="stat-row">
                <span class="stat-label">正确率</span>
                <el-progress
                    :percentage="resultData.questions.length > 0 ? Math.round(((resultData.questions.length - wrongQuestions.length) / resultData.questions.length) * 100) : 0"
                    status="success"
                    class="progress-bar"
                />
              </div>
            </div>
            <div class="share-btn-wrapper">
              <el-button class="full-width-btn" :icon="Share">分享成绩单</el-button>
            </div>
          </div>
        </div>

        <!-- Right Column: Mistake Analysis -->
        <div class="right-column">
          <div class="card min-h-card">
            <div class="mistake-header">
              <h3 class="card-title red-border">错题解析 (仅展示扣分题目)</h3>
              <el-tag type="danger" effect="plain">{{ wrongQuestions.length }} 道错题</el-tag>
            </div>

            <div v-if="wrongQuestions.length > 0" class="mistake-list">
              <div
                  v-for="(question) in wrongQuestions"
                  :key="question.id"
                  class="mistake-item"
              >
                <!-- Question Header -->
                <div class="question-title-row">
                  <span class="q-type-badge">
                    {{ question.type === 1 ? '单选' : question.type === 2 ? '多选' : question.type === 3 ? '判断' : '简答' }}
                  </span>
                  <div class="q-content">
                    {{ question.questionNo }}. {{ question.content }}
                    <span class="q-score">({{ question.score }}分)</span>
                  </div>
                  <div class="lost-score">
                    -{{ question.score - question.studentScore }}
                  </div>
                </div>

                <!-- Options -->
                <div class="options-list" v-if="question.type <= 3">
                  <div
                      v-for="opt in question.options"
                      :key="opt.key"
                      class="option-item"
                      :class="getOptionClass(question, opt)"
                  >
                    <span class="opt-key">{{ opt.key }}.</span>
                    <span class="opt-content">{{ opt.content }}</span>

                    <!-- Icons: 使用 helper 函数统一逻辑 -->
                    <el-icon v-if="getOptionStatus(question, opt).isCorrect" class="status-icon green"><Check /></el-icon>
                    <el-icon v-else-if="getOptionStatus(question, opt).isSelected" class="status-icon red"><Close /></el-icon>
                  </div>
                </div>

                <!-- Analysis Box -->
                <div class="analysis-box">
                  <div class="analysis-title">
                    <el-icon><Warning /></el-icon> 解析
                  </div>
                  <p class="analysis-text">
                    {{ question.analysis || '暂无解析' }}
                  </p>
                  <div class="analysis-footer">
                    <span class="label">正确答案：</span>
                    <span class="val green">{{ question.correctAnswer }}</span>
                    <span class="divider">|</span>
                    <span class="label">您的答案：</span>
                    <span class="val red strike">{{ question.studentAnswer || '未作答' }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Empty State -->
            <div v-else class="empty-state">
              <el-image src="https://cdni.iconscout.com/illustration/premium/thumb/success-achievement-5670876-4728568.png" class="empty-img" />
              <h3 class="empty-title">全对啦！太棒了！</h3>
              <p class="empty-desc">本次考试没有错题，继续保持哦 ~</p>
            </div>

          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Reset & Layout */
.result-page {
  min-height: 100vh;
  background-color: #f9fafb;
  padding-bottom: 40px;
}

/* Banner Styles */
.banner {
  position: relative;
  background: linear-gradient(to right, #2563eb, #4338ca);
  color: white;
  padding-top: 48px;
  padding-bottom: 96px;
  padding-left: 24px;
  padding-right: 24px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  overflow: hidden; /* For circles */
}

.banner-content {
  max-width: 1152px; /* 6xl */
  margin: 0 auto;
  position: relative;
  z-index: 10;
}

.banner-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.back-btn {
  color: white !important;
  font-size: 14px;
}

.back-btn:hover {
  color: #dbeafe !important; /* blue-100 */
}

.exam-time {
  font-size: 14px;
  opacity: 0.8;
}

.banner-main {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-start;
}

@media (min-width: 768px) {
  .banner-main {
    flex-direction: row;
    align-items: center;
  }
}

.paper-title {
  font-size: 30px;
  font-weight: bold;
  margin-bottom: 8px;
  margin-top: 0;
}

.exam-meta {
  color: #dbeafe;
  margin: 0;
}

.score-card-glass {
  margin-top: 24px;
  display: flex;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

@media (min-width: 768px) {
  .score-card-glass {
    margin-top: 0;
  }
}

.score-item {
  text-align: center;
  padding: 0 16px;
}

.border-right {
  border-right: 1px solid rgba(255, 255, 255, 0.2);
}

.score-label {
  font-size: 14px;
  color: #dbeafe;
  margin-bottom: 4px;
}

.score-value {
  font-size: 48px;
  font-weight: 800;
  color: #fde047; /* yellow-300 */
  line-height: 1;
}

.beat-rate {
  font-size: 24px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
}

.trophy-icon {
  margin-right: 4px;
  color: #fde047;
}

/* Decorative Circles */
.circle {
  position: absolute;
  border-radius: 50%;
}

.circle-1 {
  top: -40px;
  right: -40px;
  width: 256px;
  height: 256px;
  background-color: rgba(255, 255, 255, 0.05);
  filter: blur(64px);
}

.circle-2 {
  bottom: -40px;
  left: -40px;
  width: 192px;
  height: 192px;
  background-color: rgba(96, 165, 250, 0.2); /* blue-400 */
  filter: blur(48px);
}

/* Main Content Grid */
.main-container {
  max-width: 1152px;
  margin: -64px auto 0;
  padding: 0 16px;
  position: relative;
  z-index: 20;
}

.grid-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

@media (min-width: 1024px) {
  .grid-layout {
    grid-template-columns: 1fr 2fr;
  }
}

/* Cards */
.card {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  padding: 24px;
  margin-bottom: 24px;
}

.left-column .card:last-child {
  margin-bottom: 0;
}

.card-title {
  font-size: 18px;
  font-weight: bold;
  color: #1f2937;
  margin-top: 0;
  margin-bottom: 16px;
  padding-left: 12px;
  border-left-width: 4px;
  border-left-style: solid;
}

.blue-border { border-left-color: #3b82f6; }
.purple-border { border-left-color: #a855f7; }
.red-border { border-left-color: #ef4444; }

.chart-container {
  width: 100%;
  height: 300px;
}

.chart-hint {
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
  margin-top: 8px;
}

.stats-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  color: #4b5563;
}

.stat-value {
  font-weight: 600;
  color: #111827;
}

.text-red { color: #ef4444; }

.progress-bar {
  width: 128px;
}

.share-btn-wrapper {
  margin-top: 24px;
}

.full-width-btn {
  width: 100%;
}

.min-h-card {
  min-height: 500px;
}

/* Mistake List */
.mistake-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.mistake-list {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.mistake-item {
  border-bottom: 1px solid #f3f4f6;
  padding-bottom: 32px;
}

.mistake-item:last-child {
  border-bottom: none;
}

.question-title-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.q-type-badge {
  background-color: #f3f4f6;
  color: #4b5563;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  white-space: nowrap;
}

.q-content {
  font-weight: 500;
  color: #1f2937;
  line-height: 1.6;
  flex: 1;
}

.q-score {
  color: #9ca3af;
  font-size: 14px;
  margin-left: 8px;
}

.lost-score {
  color: #ef4444;
  font-weight: bold;
  white-space: nowrap;
}

.options-list {
  padding-left: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  font-size: 14px;
  transition: all 0.2s;
}

.opt-key {
  width: 24px;
  font-weight: bold;
}

.status-icon {
  margin-left: auto;
  font-size: 18px;
}

.status-icon.green { color: #16a34a; }
.status-icon.red { color: #dc2626; }

/* Option Classes */
.option-correct {
  background-color: #dcfce7; /* green-100 */
  border-color: #22c55e; /* green-500 */
  color: #15803d; /* green-700 */
  font-weight: 500;
}

.option-wrong {
  background-color: #fee2e2; /* red-100 */
  border-color: #ef4444; /* red-500 */
  color: #b91c1c; /* red-700 */
  font-weight: 500;
}

.option-default {
  background-color: white;
  color: #4b5563;
}

/* Analysis Box */
.analysis-box {
  margin-top: 16px;
  background-color: #fff7ed; /* orange-50 */
  border: 1px solid #ffedd5; /* orange-100 */
  border-radius: 8px;
  padding: 16px;
}

.analysis-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #ea580c; /* orange-600 */
  font-weight: bold;
  font-size: 14px;
  margin-bottom: 4px;
}

.analysis-text {
  color: #4b5563;
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
}

.analysis-footer {
  margin-top: 8px;
  font-size: 14px;
}

.analysis-footer .label { color: #6b7280; }
.analysis-footer .val { font-weight: bold; }
.analysis-footer .val.green { color: #16a34a; }
.analysis-footer .val.red { color: #ef4444; }
.analysis-footer .strike { text-decoration: line-through; }
.analysis-footer .divider { margin: 0 8px; color: #d1d5db; }

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
  text-align: center;
}

.empty-img {
  width: 192px;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 20px;
  font-weight: bold;
  color: #1f2937;
  margin: 0;
}

.empty-desc {
  color: #6b7280;
  margin-top: 8px;
}

/* Deep override for Element Plus progress bar */
:deep(.el-progress-bar__inner) {
  background-color: #10b981;
}
</style>