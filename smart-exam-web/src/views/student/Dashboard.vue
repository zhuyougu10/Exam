<template>
  <div class="p-6 bg-gray-50 min-h-[calc(100vh-64px)] flex flex-col gap-6">
    <!-- 欢迎语 -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-bold text-gray-800">
          早安，{{ userStore.userInfo?.realName || '同学' }}！👋
        </h2>
        <p class="text-gray-500 mt-1 text-sm">
          今天也是充满希望的一天，准备好迎接新的挑战了吗？
        </p>
      </div>
      <div class="hidden md:block">
        <span class="text-sm text-gray-400">{{ currentDate }}</span>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <el-card shadow="hover" class="border-0 rounded-xl !bg-gradient-to-br from-blue-500 to-blue-600 text-white relative overflow-hidden">
        <div class="relative z-10">
          <div class="flex items-center justify-between mb-4">
            <div class="p-2 bg-white/20 rounded-lg">
              <el-icon size="24"><Trophy /></el-icon>
            </div>
            <el-tag type="info" effect="dark" size="small" class="!bg-white/20 !border-0 text-white">Total</el-tag>
          </div>
          <div class="text-3xl font-bold mb-1">{{ stats.examCount }}</div>
          <div class="text-blue-100 text-xs">已参加考试场次</div>
        </div>
        <div class="absolute -bottom-6 -right-6 w-24 h-24 bg-white/10 rounded-full blur-2xl"></div>
      </el-card>

      <el-card shadow="hover" class="border-0 rounded-xl">
        <div class="flex items-start justify-between">
          <div>
            <div class="text-gray-500 text-xs mb-1">平均分数</div>
            <div class="text-2xl font-bold text-gray-800">{{ stats.avgScore }}</div>
            <div class="mt-2 text-xs flex items-center">
              <span class="text-green-500 flex items-center" v-if="stats.avgScore >= 60">
                <el-icon><Top /></el-icon> 良好
              </span>
              <span class="text-orange-500 flex items-center" v-else>
                <el-icon><Warning /></el-icon> 需努力
              </span>
            </div>
          </div>
          <div class="p-3 bg-green-50 rounded-full text-green-600">
            <el-icon size="20"><DataLine /></el-icon>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="border-0 rounded-xl cursor-pointer transition-transform hover:-translate-y-1" @click="$router.push('/student/mistake-book')">
        <div class="flex items-start justify-between">
          <div>
            <div class="text-gray-500 text-xs mb-1">错题待练</div>
            <div class="text-2xl font-bold text-gray-800">{{ stats.mistakeCount }}</div>
            <div class="mt-2 text-xs text-gray-400">点击进入错题本复习</div>
          </div>
          <div class="p-3 bg-orange-50 rounded-full text-orange-500">
            <el-icon size="20"><Collection /></el-icon>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="border-0 rounded-xl cursor-pointer transition-transform hover:-translate-y-1" @click="$router.push('/student/exam-list')">
        <div class="flex items-start justify-between">
          <div>
            <div class="text-gray-500 text-xs mb-1">待参加考试</div>
            <div class="text-2xl font-bold text-indigo-600">{{ stats.upcomingCount || 0 }}</div>
            <div class="mt-2 text-xs text-indigo-400">最近一场即将开始</div>
          </div>
          <div class="p-3 bg-indigo-50 rounded-full text-indigo-500">
            <el-icon size="20"><Timer /></el-icon>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 图表区域 -->
    <el-row :gutter="24">
      <el-col :span="24" :lg="16">
        <el-card shadow="never" class="border-0 rounded-xl h-full">
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-bold text-gray-800">近期成绩趋势</span>
            </div>
          </template>

          <div v-if="trendData.examNames.length === 0" class="h-64 flex items-center justify-center text-gray-400">
            暂无考试数据
          </div>
          <div v-else class="h-64 w-full" ref="trendChartRef"></div>
        </el-card>
      </el-col>

      <el-col :span="24" :lg="8" class="mt-6 lg:mt-0">
        <el-card shadow="never" class="border-0 rounded-xl h-full bg-indigo-900 text-white">
          <div class="flex flex-col h-full justify-between p-2">
            <div>
              <div class="text-indigo-200 text-sm mb-1">学习建议</div>
              <h3 class="text-xl font-bold mb-4">保持良好的复习节奏</h3>
              <p class="text-indigo-100 text-sm opacity-80 leading-relaxed">
                {{ trendData.suggestion || '完成一场考试来获取专属建议' }}
              </p>
            </div>
            <el-button color="white" class="!text-indigo-900 w-full mt-6" round @click="$router.push('/student/mistake-book')">
              去复习错题
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { useUserStore } from '@/store/user'
import { Trophy, DataLine, Collection, Timer, Top, Warning } from '@element-plus/icons-vue'
import request from '@/utils/request'
import dayjs from 'dayjs'
import * as echarts from 'echarts'

const userStore = useUserStore()
const currentDate = dayjs().format('YYYY年MM月DD日 dddd')

const stats = ref({
  examCount: 0,
  avgScore: 0,
  passCount: 0,
  mistakeCount: 0,
  upcomingCount: 0
})

const trendData = ref({
  examNames: [] as string[],
  scores: [] as number[],
  suggestion: ''
})

const trendChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null

const fetchStats = async () => {
  try {
    const res: any = await request.get('/student/dashboard/stats')
    if(res) {
      stats.value = res
    }
  } catch (error) {
    console.error(error)
  }
}

const fetchTrend = async () => {
  try {
    const res: any = await request.get('/student/dashboard/trend')
    if (res) {
      trendData.value = res
      nextTick(() => {
        initTrendChart()
      })
    }
  } catch (error) {
    console.error(error)
  }
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendData.value.examNames
    },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [
      {
        name: '分数',
        type: 'line',
        smooth: true,
        data: trendData.value.scores,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(99, 102, 241, 0.5)' },
            { offset: 1, color: 'rgba(99, 102, 241, 0)' }
          ])
        },
        itemStyle: { color: '#6366f1' },
        lineStyle: { width: 3 }
      }
    ]
  })
}

const handleResize = () => {
  trendChart?.resize()
}

onMounted(() => {
  fetchStats()
  fetchTrend()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style scoped>
@reference "@/style.css";
</style>