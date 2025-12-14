<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// 导入所需图标
import { Trophy, ArrowLeft, Share, Warning, Check, Close } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()

// 模拟考试结果数据
const resultData = ref({
  paperTitle: 'Java 高级程序设计期末考试',
  totalScore: 88,
  paperTotalScore: 100,
  beatRate: 85,
  startTime: '2025-06-15 14:00:00',
  duration: '85分钟',
  radarData: [
    { name: '多线程', max: 100, value: 90 },
    { name: '集合框架', max: 100, value: 80 },
    { name: 'JVM', max: 100, value: 60 },
    { name: 'IO流', max: 100, value: 85 },
    { name: '反射', max: 100, value: 70 },
    { name: '网络编程', max: 100, value: 95 }
  ],
  questions: [
    {
      id: 101,
      questionNo: 1, // 原始题号
      type: 1,
      content: '下面哪个类不是实现了 List 接口?',
      options: [
        { key: 'A', content: 'ArrayList' },
        { key: 'B', content: 'LinkedList' },
        { key: 'C', content: 'HashSet' },
        { key: 'D', content: 'Vector' }
      ],
      studentAnswer: 'D',
      correctAnswer: 'C',
      score: 5,
      studentScore: 0,
      analysis: 'HashSet 实现了 Set 接口，而不是 List 接口。'
    },
    {
      id: 102,
      questionNo: 2,
      type: 1,
      content: 'Java 中线程安全的 Map 是?',
      options: [
        { key: 'A', content: 'HashMap' },
        { key: 'B', content: 'ConcurrentHashMap' },
        { key: 'C', content: 'TreeMap' },
        { key: 'D', content: 'LinkedHashMap' }
      ],
      studentAnswer: 'B',
      correctAnswer: 'B',
      score: 5,
      studentScore: 5,
      analysis: 'ConcurrentHashMap 是线程安全的。'
    },
    {
      id: 103,
      questionNo: 3,
      type: 2,
      content: '以下哪些是 Java 的基本数据类型?',
      options: [
        { key: 'A', content: 'int' },
        { key: 'B', content: 'String' },
        { key: 'C', content: 'boolean' },
        { key: 'D', content: 'Double' }
      ],
      studentAnswer: 'A,C',
      correctAnswer: 'A,C',
      score: 10,
      studentScore: 10,
      analysis: 'String 和 Double 是类，不是基本数据类型。'
    },
    {
      id: 104,
      questionNo: 4,
      type: 1,
      content: '关于 volatile 关键字，下列描述错误的是？',
      options: [
        { key: 'A', content: '保证可见性' },
        { key: 'B', content: '保证原子性' },
        { key: 'C', content: '禁止指令重排' },
        { key: 'D', content: '比 synchronized 轻量' }
      ],
      studentAnswer: 'C',
      correctAnswer: 'B',
      score: 5,
      studentScore: 0,
      analysis: 'volatile 只能保证可见性和有序性，不能保证原子性。'
    }
  ]
})

const wrongQuestions = computed(() => {
  return resultData.value.questions.filter(q => q.studentScore < q.score)
})

const radarChartRef = ref<HTMLElement | null>(null)

const initChart = () => {
  if (!radarChartRef.value) return

  const myChart = echarts.init(radarChartRef.value)
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

  window.addEventListener('resize', () => myChart.resize())
}

// 获取选项样式类名
const getOptionClass = (question: any, optionKey: string) => {
  const isSelected = question.studentAnswer.includes(optionKey)
  const isCorrect = question.correctAnswer.includes(optionKey)

  if (isCorrect) {
    return 'option-correct'
  }
  if (isSelected && !isCorrect) {
    return 'option-wrong'
  }
  return 'option-default'
}

const goBack = () => {
  router.push('/student/exam-list')
}

onMounted(() => {
  nextTick(() => {
    initChart()
  })
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

      <!-- Decorative circles (纯CSS实现) -->
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
    </div>

    <!-- Main Content -->
    <div class="main-container">
      <div class="grid-layout">

        <!-- Left Column: Chart & Stats -->
        <div class="left-column">
          <!-- Radar Chart Card -->
          <div class="card">
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
                    :percentage="Math.round(((resultData.questions.length - wrongQuestions.length) / resultData.questions.length) * 100)"
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
                    {{ question.type === 1 ? '单选' : question.type === 2 ? '多选' : '判断' }}
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
                <div class="options-list">
                  <div
                      v-for="opt in question.options"
                      :key="opt.key"
                      class="option-item"
                      :class="getOptionClass(question, opt.key)"
                  >
                    <span class="opt-key">{{ opt.key }}.</span>
                    <span class="opt-content">{{ opt.content }}</span>

                    <!-- Icons -->
                    <el-icon v-if="question.correctAnswer.includes(opt.key)" class="status-icon green"><Check /></el-icon>
                    <el-icon v-else-if="question.studentAnswer.includes(opt.key) && !question.correctAnswer.includes(opt.key)" class="status-icon red"><Close /></el-icon>
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
                    <span class="val red strike">{{ question.studentAnswer }}</span>
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