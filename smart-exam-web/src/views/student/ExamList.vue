<template>
  <div class="p-6 bg-gray-50 min-h-[calc(100vh-64px)]">
    <!-- 头部 -->
    <div class="mb-6 flex justify-between items-center">
      <div>
        <h2 class="text-2xl font-bold text-gray-800">我的考试</h2>
        <p class="text-gray-500 text-sm mt-1">查看所有待参加及历史考试记录</p>
      </div>
      <el-button circle :icon="Refresh" @click="fetchData" :loading="loading" />
    </div>

    <!-- 状态 Tabs -->
    <el-tabs v-model="activeTab" class="custom-tabs mb-6" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="进行中" name="running" />
      <el-tab-pane label="未开始" name="upcoming" />
      <el-tab-pane label="已结束" name="history" />
    </el-tabs>

    <!-- 列表内容 -->
    <div v-loading="loading">
      <div v-if="filteredList.length > 0" class="grid grid-cols-1 gap-4">
        <el-card
            v-for="exam in filteredList"
            :key="exam.id"
            shadow="hover"
            class="border-0 rounded-xl transition-all hover:shadow-md group"
            :body-style="{ padding: '20px' }"
        >
          <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">

            <!-- 左侧：图标与信息 -->
            <div class="flex items-start gap-4 flex-1">
              <!-- 状态图标 -->
              <div
                  class="w-14 h-14 rounded-2xl flex items-center justify-center shrink-0 text-2xl shadow-sm transition-colors"
                  :class="getStatusStyle(exam).bgClass"
              >
                <el-icon :class="getStatusStyle(exam).textClass">
                  <component :is="getStatusStyle(exam).icon" />
                </el-icon>
              </div>

              <!-- 信息 -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-1">
                  <h3 class="text-lg font-bold text-gray-800 truncate group-hover:text-indigo-600 transition-colors">
                    {{ exam.title }}
                  </h3>
                  <el-tag :type="getStatusStyle(exam).tagType" size="small" effect="plain" round>
                    {{ getStatusText(exam) }}
                  </el-tag>
                </div>

                <div class="flex flex-wrap items-center gap-x-6 gap-y-2 text-sm text-gray-500 mt-2">
                  <div class="flex items-center gap-1.5">
                    <el-icon><Calendar /></el-icon>
                    <span>{{ formatTimeRange(exam.startTime, exam.endTime) }}</span>
                  </div>
                  <div class="flex items-center gap-1.5">
                    <el-icon><Timer /></el-icon>
                    <span>{{ exam.duration }} 分钟</span>
                  </div>
                  <div class="flex items-center gap-1.5" v-if="exam.limitCount > 0">
                    <el-icon><RefreshLeft /></el-icon>
                    <span>剩余次数: {{ Math.max(0, exam.limitCount - (exam.triedCount || 0)) }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 右侧：操作按钮 -->
            <div class="flex items-center gap-3 md:justify-end w-full md:w-auto mt-2 md:mt-0 border-t md:border-0 pt-4 md:pt-0 border-gray-100">
              <!-- 状态 1: 进行中 -->
              <template v-if="exam.status === 1">
                <div class="flex flex-col items-end mr-2 hidden md:flex">
                  <span class="text-xs text-green-500 font-medium animate-pulse">● 考试进行中</span>
                  <span class="text-[10px] text-gray-400">请保持网络畅通</span>
                </div>
                <el-button type="primary" size="large" round class="px-8 shadow-md shadow-indigo-200" @click="handleEnterExam(exam)">
                  进入考试
                  <el-icon class="ml-2"><Right /></el-icon>
                </el-button>
              </template>

              <!-- 状态 0: 未开始 -->
              <template v-else-if="exam.status === 0">
                <el-button type="info" size="large" round plain disabled class="px-6 bg-gray-50">
                  <el-icon class="mr-2"><Clock /></el-icon>
                  {{ formatStartTime(exam.startTime) }} 开始
                </el-button>
              </template>

              <!-- 状态 2: 已结束 -->
              <template v-else>
                <div class="text-right mr-2 hidden md:block">
                  <div class="text-xs text-gray-400">已提交 {{ exam.triedCount || 0 }} 次</div>
                </div>
                <el-button size="large" round @click="handleViewResult(exam)">
                  查看结果
                </el-button>
              </template>
            </div>

          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <div v-else class="flex flex-col items-center justify-center py-20 bg-white rounded-xl">
        <el-empty description="暂无相关考试安排" :image-size="160" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Refresh, Calendar, Timer, RefreshLeft, Right, Clock,
  VideoPlay, CircleCheck, Lock
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import dayjs from 'dayjs'

const router = useRouter()
const loading = ref(false)
const activeTab = ref('all')
const examList = ref<any[]>([])

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get<any[]>('/exam/my-list')
    examList.value = res || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 过滤列表
const filteredList = computed(() => {
  if (activeTab.value === 'all') return examList.value

  const statusMap: Record<string, number> = {
    'upcoming': 0,
    'running': 1,
    'history': 2
  }
  return examList.value.filter(item => item.status === statusMap[activeTab.value])
})

// Tab 切换
const handleTabChange = () => {
  // 可以在这里重新请求后端带状态的接口，目前采用前端过滤
}

// 进入考试
const handleEnterExam = (exam: any) => {
  // 路由传参：传递 publishId
  router.push({
    path: `/student/exam-paper`,
    query: { publishId: exam.id }
  })
}

// 查看结果
const handleViewResult = (exam: any) => {
  // 注意：后端暂时没有根据 publishId 直接获取 recordId 的接口
  // 这里暂时跳转到结果页，并带上 publishId，让结果页去处理（或提示用户）
  // 理想情况：exam 对象里应该包含 lastRecordId

  // 临时逻辑：如果考试已结束，尝试查找结果。
  // 由于数据缺失，暂跳转到错题本或列表页提示
  // 为了闭环，这里假设 ExamResult 可以接受 publishId 并查询最新记录
  // router.push({ path: '/student/exam-result', query: { publishId: exam.id } })

  ElMessage.info('查看历史试卷功能正在升级中，请前往“仪表盘”查看统计或“错题本”回顾。')
}

// ------ 样式与格式化 ------

const getStatusStyle = (exam: any) => {
  switch (exam.status) {
    case 1: // 进行中
      return {
        bgClass: 'bg-green-100',
        textClass: 'text-green-600',
        icon: VideoPlay,
        tagType: 'success'
      }
    case 2: // 已结束
      return {
        bgClass: 'bg-gray-100',
        textClass: 'text-gray-500',
        icon: CircleCheck,
        tagType: 'info'
      }
    default: // 0: 未开始
      return {
        bgClass: 'bg-blue-50',
        textClass: 'text-blue-500',
        icon: Lock,
        tagType: 'primary'
      }
  }
}

const getStatusText = (exam: any) => {
  const map = { 0: '未开始', 1: '进行中', 2: '已结束' }
  return (map as any)[exam.status] || '未知'
}

const formatTimeRange = (start: string, end: string) => {
  const s = dayjs(start)
  const e = dayjs(end)
  // 如果是同一天，显示 "MM-DD HH:mm - HH:mm"
  if (s.isSame(e, 'day')) {
    return `${s.format('MM-DD HH:mm')} ~ ${e.format('HH:mm')}`
  }
  return `${s.format('MM-DD HH:mm')} ~ ${e.format('MM-DD HH:mm')}`
}

const formatStartTime = (time: string) => {
  return dayjs(time).format('MM-DD HH:mm')
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.custom-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #f3f4f6;
}
.custom-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  color: #6b7280;
}
.custom-tabs :deep(.el-tabs__item.is-active) {
  font-weight: 600;
  color: #4f46e5;
}
.custom-tabs :deep(.el-tabs__active-bar) {
  background-color: #4f46e5;
}
</style>