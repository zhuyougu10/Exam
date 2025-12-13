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

    <!-- 核心指标卡片 (Grid 布局) -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <!-- 考试场次 -->
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
        <!-- 装饰背景 -->
        <div class="absolute -bottom-6 -right-6 w-24 h-24 bg-white/10 rounded-full blur-2xl"></div>
      </el-card>

      <!-- 平均分 -->
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

      <!-- 错题本 -->
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

      <!-- 待参加 -->
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
              <el-radio-group v-model="chartRange" size="small">
                <el-radio-button value="week">近7次</el-radio-button>
                <el-radio-button value="month">近30次</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <!-- 手写 SVG 折线图 (替代 ECharts) -->
          <div class="h-64 w-full relative flex items-end justify-between px-4 pb-6 pt-10 bg-gray-50/50 rounded-lg border border-gray-100">
            <!-- 模拟数据点 -->
            <svg class="absolute inset-0 w-full h-full overflow-visible" preserveAspectRatio="none">
              <!-- 折线 -->
              <polyline
                  points="0,200 100,150 200,180 300,100 400,120 500,50 600,80"
                  fill="none"
                  stroke="#6366f1"
                  stroke-width="3"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  vector-effect="non-scaling-stroke"
              />
              <!-- 填充区域 (渐变) -->
              <defs>
                <linearGradient id="gradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#6366f1" stop-opacity="0.2"/>
                  <stop offset="100%" stop-color="#6366f1" stop-opacity="0"/>
                </linearGradient>
              </defs>
              <polygon
                  points="0,256 0,200 100,150 200,180 300,100 400,120 500,50 600,80 600,256"
                  fill="url(#gradient)"
                  vector-effect="non-scaling-stroke"
              />
              <!-- 数据点圆圈 -->
              <circle cx="0%" cy="78%" r="4" fill="white" stroke="#6366f1" stroke-width="2" />
              <circle cx="16.6%" cy="58%" r="4" fill="white" stroke="#6366f1" stroke-width="2" />
              <circle cx="33.2%" cy="70%" r="4" fill="white" stroke="#6366f1" stroke-width="2" />
              <circle cx="49.8%" cy="39%" r="4" fill="white" stroke="#6366f1" stroke-width="2" />
              <circle cx="66.4%" cy="46%" r="4" fill="white" stroke="#6366f1" stroke-width="2" />
              <circle cx="83%" cy="19%" r="4" fill="white" stroke="#6366f1" stroke-width="2" />
              <circle cx="100%" cy="31%" r="4" fill="white" stroke="#6366f1" stroke-width="2" />
            </svg>

            <!-- X轴标签 -->
            <div class="absolute bottom-0 left-0 w-full flex justify-between text-xs text-gray-400 px-0">
              <span>Exam 1</span><span>Exam 2</span><span>Exam 3</span><span>Exam 4</span>
              <span>Exam 5</span><span>Exam 6</span><span>Exam 7</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="24" :lg="8" class="mt-6 lg:mt-0">
        <el-card shadow="never" class="border-0 rounded-xl h-full bg-indigo-900 text-white">
          <div class="flex flex-col h-full justify-between p-2">
            <div>
              <div class="text-indigo-200 text-sm mb-1">学习建议</div>
              <h3 class="text-xl font-bold mb-4">保持良好的复习节奏</h3>
              <p class="text-indigo-100 text-sm opacity-80 leading-relaxed">
                根据您的做题记录，我们在“数据库原理”课程中发现了较多错题。建议您前往【智能错题本】进行针对性训练。
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
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { Trophy, DataLine, Collection, Timer, Top, Warning } from '@element-plus/icons-vue'
import request from '@/utils/request'
import dayjs from 'dayjs'

const userStore = useUserStore()
const chartRange = ref('week')
const currentDate = dayjs().format('YYYY年MM月DD日 dddd')

const stats = ref({
  examCount: 0,
  avgScore: 0,
  passCount: 0,
  mistakeCount: 0,
  upcomingCount: 0 // 需要后端VO支持或自行计算
})

const fetchStats = async () => {
  try {
    const res = await request.get<any>('/student/dashboard/stats')
    // 后端VO可能字段不全，做兜底
    stats.value = {
      examCount: res.examCount || 0,
      avgScore: res.avgScore || 0,
      passCount: res.passCount || 0,
      mistakeCount: res.mistakeCount || 0,
      upcomingCount: res.upcomingCount || 0
    }
  } catch (error) {
    console.error('获取仪表盘数据失败', error)
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
/* 引入主样式文件作为引用，获取上下文 (v4 最佳实践) */
@reference "@/style.css";

/* 简单的 SVG 动画 */
polyline {
  stroke-dasharray: 1000;
  stroke-dashoffset: 1000;
  animation: dash 2s ease-out forwards;
}

@keyframes dash {
  to {
    stroke-dashoffset: 0;
  }
}
</style>