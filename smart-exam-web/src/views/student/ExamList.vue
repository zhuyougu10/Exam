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

    <!-- 考前须知弹窗 -->
    <el-dialog
        v-model="noticeDialogVisible"
        title="考前须知"
        width="500px"
        align-center
        destroy-on-close
        class="exam-notice-dialog"
    >
      <div class="p-4">
        <div class="flex items-center justify-center mb-6">
          <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center">
            <el-icon class="text-blue-500" size="32"><Reading /></el-icon>
          </div>
        </div>

        <h3 class="text-center text-lg font-bold text-gray-800 mb-6">
          {{ currentExam?.title }}
        </h3>

        <!-- 写死的考前须知内容 -->
        <div class="space-y-4 text-gray-600 bg-gray-50 p-4 rounded-lg border border-gray-100 text-sm">
          <div class="flex gap-3">
            <span class="font-bold text-blue-500">1.</span>
            <p>请确保网络环境良好，建议使用 Chrome 或 Edge 浏览器。</p>
          </div>
          <div class="flex gap-3">
            <span class="font-bold text-blue-500">2.</span>
            <p>考试期间<span class="font-bold text-red-500">禁止切屏</span>，系统将自动记录切屏次数，超过限制可能被强制交卷。</p>
          </div>
          <div class="flex gap-3">
            <span class="font-bold text-blue-500">3.</span>
            <p>请诚信考试，独立完成，系统已开启 AI 监考及反作弊检测。</p>
          </div>
          <div class="flex gap-3">
            <span class="font-bold text-blue-500">4.</span>
            <p>答题过程中系统会自动保存答案，如遇异常请尝试刷新页面或联系老师。</p>
          </div>
        </div>

        <!-- 密码输入区域 -->
        <div v-if="currentExam?.needPassword" class="mt-6 bg-orange-50 p-4 rounded-lg border border-orange-200">
          <div class="flex items-center gap-2 mb-3">
            <el-icon class="text-orange-500"><Lock /></el-icon>
            <span class="font-medium text-orange-700">本场考试需要输入密码</span>
          </div>
          <el-input
              v-model="examPassword"
              type="password"
              placeholder="请输入考试密码"
              show-password
              size="large"
              :class="{ 'is-error': passwordError }"
              @keyup.enter="confirmStartExam"
          />
          <div v-if="passwordError" class="text-red-500 text-xs mt-2">{{ passwordError }}</div>
        </div>

        <div class="mt-6 flex items-center justify-center gap-2">
          <el-checkbox v-model="isRead" size="large">我已阅读并知晓以上规则</el-checkbox>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-center pb-4 gap-4">
          <el-button size="large" @click="noticeDialogVisible = false">再等等</el-button>
          <el-button
              type="primary"
              size="large"
              class="w-48"
              :disabled="!isRead || (currentExam?.needPassword && !examPassword)"
              :loading="verifyingPassword"
              @click="confirmStartExam"
          >
            {{ verifyingPassword ? '验证中...' : '进入考试' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus' // 引入 ElMessageBox
import {
  Refresh, Calendar, Timer, RefreshLeft, Right, Clock,
  VideoPlay, CircleCheck, Lock, Reading
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import dayjs from 'dayjs'

const router = useRouter()
const loading = ref(false)
const activeTab = ref('all')
const examList = ref<any[]>([])

// 弹窗相关状态
const noticeDialogVisible = ref(false)
const currentExam = ref<any>(null)
const isRead = ref(false)
const examPassword = ref('')
const passwordError = ref('')
const verifyingPassword = ref(false)

// ---------------- 新增：检测 IE 浏览器 ----------------
const isIEBrowser = () => {
  const ua = window.navigator.userAgent
  // MSIE: IE 10 及以下
  // Trident: IE 11
  return ua.indexOf('MSIE ') > -1 || ua.indexOf('Trident/') > -1
}

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

// 点击进入考试按钮 -> 检测 IE -> 弹出须知
const handleEnterExam = (exam: any) => {
  // 1. IE 检测逻辑
  if (isIEBrowser()) {
    ElMessageBox.alert(
        '本考试系统基于最新 Web 技术构建，不支持 Internet Explorer (IE) 浏览器。<br/><br/>为了确保考试顺利进行，请使用 <b>Chrome (谷歌)</b>、<b>Edge</b> 或 <b>Firefox</b> 浏览器重新登录系统。',
        '浏览器不受支持',
        {
          confirmButtonText: '知道了',
          type: 'error',
          dangerouslyUseHTMLString: true,
          center: true
        }
    )
    return
  }

  // 2. 正常流程 - 重置状态
  currentExam.value = exam
  isRead.value = false
  examPassword.value = ''
  passwordError.value = ''
  noticeDialogVisible.value = true
}

// 确认进入考试 -> 验证密码 -> 跳转
const confirmStartExam = async () => {
  if (!currentExam.value) return
  
  // 如果需要密码，先验证
  if (currentExam.value.needPassword) {
    if (!examPassword.value) {
      passwordError.value = '请输入考试密码'
      return
    }
    
    verifyingPassword.value = true
    passwordError.value = ''
    
    try {
      await request.post(`/exam/verify-password/${currentExam.value.id}`, {
        password: examPassword.value
      })
      // 密码验证成功，跳转
      noticeDialogVisible.value = false
      router.push({
        path: `/student/exam-paper`,
        query: { publishId: currentExam.value.id }
      })
    } catch (error: any) {
      passwordError.value = error.message || '密码错误，请重新输入'
    } finally {
      verifyingPassword.value = false
    }
  } else {
    // 无密码要求，直接跳转
    noticeDialogVisible.value = false
    router.push({
      path: `/student/exam-paper`,
      query: { publishId: currentExam.value.id }
    })
  }
}

// 查看结果
const handleViewResult = (exam: any) => {
  router.push({ path: '/student/exam-result', query: { publishId: exam.id } })
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