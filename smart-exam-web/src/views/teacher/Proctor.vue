<template>
  <div class="app-container p-6 bg-gray-50 min-h-[calc(100vh-64px)]">
    <!-- 顶部信息栏 -->
    <el-card shadow="never" class="border-0 rounded-xl mb-4">
      <div class="flex justify-between items-center">
        <div class="flex items-center gap-3">
          <div class="p-2 bg-red-100 rounded-lg text-red-600">
            <el-icon size="20"><Monitor /></el-icon>
          </div>
          <div>
            <h2 class="text-lg font-bold text-gray-800">在线监考</h2>
            <p class="text-xs text-gray-500">实时监控考试状态，及时处理异常情况</p>
          </div>
        </div>
        <div class="flex items-center gap-4">
          <el-tag :type="wsConnected ? 'success' : 'danger'" effect="dark" round>
            <el-icon class="mr-1"><Connection /></el-icon>
            {{ wsConnected ? '已连接' : '未连接' }}
          </el-tag>
          <el-button :icon="Refresh" circle @click="refreshData" />
          <el-button type="primary" plain @click="router.back()">
            <el-icon class="mr-1"><Back /></el-icon>返回
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-5 gap-4 mb-4">
      <el-card shadow="never" class="border-0 rounded-xl">
        <div class="text-center">
          <div class="text-3xl font-bold text-blue-600">{{ stats.total }}</div>
          <div class="text-sm text-gray-500 mt-1">总人数</div>
        </div>
      </el-card>
      <el-card shadow="never" class="border-0 rounded-xl">
        <div class="text-center">
          <div class="text-3xl font-bold text-green-600">{{ stats.inProgress }}</div>
          <div class="text-sm text-gray-500 mt-1">答题中</div>
        </div>
      </el-card>
      <el-card shadow="never" class="border-0 rounded-xl">
        <div class="text-center">
          <div class="text-3xl font-bold text-gray-600">{{ stats.submitted }}</div>
          <div class="text-sm text-gray-500 mt-1">已交卷</div>
        </div>
      </el-card>
      <el-card shadow="never" class="border-0 rounded-xl">
        <div class="text-center">
          <div class="text-3xl font-bold text-orange-600">{{ stats.notStarted }}</div>
          <div class="text-sm text-gray-500 mt-1">未开始</div>
        </div>
      </el-card>
      <el-card shadow="never" class="border-0 rounded-xl">
        <div class="text-center">
          <div class="text-3xl font-bold text-red-600">{{ stats.totalWarnings }}</div>
          <div class="text-sm text-gray-500 mt-1">异常警告</div>
        </div>
      </el-card>
    </div>

    <!-- 主内容区 -->
    <div class="grid grid-cols-3 gap-4">
      <!-- 学生列表 -->
      <el-card shadow="never" class="border-0 rounded-xl col-span-2">
        <template #header>
          <div class="flex justify-between items-center">
            <span class="font-bold">考生状态</span>
            <el-input
              v-model="searchKeyword"
              placeholder="搜索学生姓名"
              clearable
              style="width: 200px"
              :prefix-icon="Search"
            />
          </div>
        </template>
        <el-table
          :data="filteredStudents"
          v-loading="loading"
          :row-class-name="tableRowClassName"
          max-height="500"
        >
          <el-table-column prop="studentName" label="姓名" width="120" />
          <el-table-column prop="statusLabel" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small" effect="dark" round>
                {{ row.statusLabel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="startTime" label="开始时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.startTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="warningCount" label="警告次数" width="100" align="center">
            <template #default="{ row }">
              <el-badge :value="row.warningCount" :type="row.warningCount > 0 ? 'danger' : 'info'" />
            </template>
          </el-table-column>
          <el-table-column prop="userIp" label="IP地址" min-width="120" />
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="viewStudentLogs(row)">
                查看日志
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 实时日志 -->
      <el-card shadow="never" class="border-0 rounded-xl">
        <template #header>
          <div class="flex justify-between items-center">
            <span class="font-bold">
              <el-icon class="text-red-500 mr-1"><Bell /></el-icon>
              实时警告
            </span>
            <el-badge :value="realtimeLogs.length" :max="99" type="danger" />
          </div>
        </template>
        <div class="log-container" ref="logContainer">
          <div v-if="realtimeLogs.length === 0" class="text-center text-gray-400 py-10">
            暂无异常记录
          </div>
          <div
            v-for="log in realtimeLogs"
            :key="log.id"
            class="log-item p-3 mb-2 rounded-lg border-l-4"
            :class="getLogClass(log.actionType)"
          >
            <div class="flex justify-between items-start">
              <div>
                <span class="font-medium text-gray-800">{{ log.studentName }}</span>
                <el-tag :type="getLogTagType(log.actionType)" size="small" class="ml-2">
                  {{ log.actionLabel }}
                </el-tag>
              </div>
              <span class="text-xs text-gray-400">{{ formatTime(log.happenTime) }}</span>
            </div>
            <div v-if="log.content" class="text-sm text-gray-600 mt-1">{{ log.content }}</div>
            <div v-if="log.imgSnapshot" class="mt-2">
              <el-image
                :src="log.imgSnapshot"
                :preview-src-list="[log.imgSnapshot]"
                fit="cover"
                class="w-20 h-20 rounded cursor-pointer"
              />
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 学生日志详情弹窗 -->
    <el-dialog v-model="logDialogVisible" :title="`${selectedStudent?.studentName} 的监考日志`" width="600px">
      <el-table :data="studentLogs" max-height="400">
        <el-table-column prop="actionLabel" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getLogTagType(row.actionType)" size="small">{{ row.actionLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="150" show-overflow-tooltip />
        <el-table-column prop="happenTime" label="时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.happenTime) }}
          </template>
        </el-table-column>
        <el-table-column label="截图" width="80">
          <template #default="{ row }">
            <el-image
              v-if="row.imgSnapshot"
              :src="row.imgSnapshot"
              :preview-src-list="[row.imgSnapshot]"
              fit="cover"
              class="w-12 h-12 rounded cursor-pointer"
            />
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Monitor, Connection, Refresh, Back, Bell, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const publishId = ref<number>(0)
const loading = ref(false)
const wsConnected = ref(false)
const searchKeyword = ref('')
const logDialogVisible = ref(false)
const selectedStudent = ref<any>(null)
const studentLogs = ref<any[]>([])
const logContainer = ref<HTMLElement | null>(null)

let ws: WebSocket | null = null
let heartbeatTimer: number | null = null
let statsPollingTimer: number | null = null

const stats = reactive({
  total: 0,
  inProgress: 0,
  submitted: 0,
  notStarted: 0,
  totalWarnings: 0,
  proctorCount: 0
})

const students = ref<any[]>([])
const realtimeLogs = ref<any[]>([])

const filteredStudents = computed(() => {
  if (!searchKeyword.value) return students.value
  return students.value.filter(s => 
    s.studentName?.includes(searchKeyword.value)
  )
})

onMounted(() => {
  const pid = route.query.publishId
  if (!pid) {
    ElMessage.error('缺少考试ID参数')
    router.back()
    return
  }
  publishId.value = Number(pid)
  
  refreshData()
  connectWebSocket()
  startStatsPolling()
})

onUnmounted(() => {
  disconnectWebSocket()
  stopStatsPolling()
})

const startStatsPolling = () => {
  // 每10秒刷新一次统计数据和学生状态
  statsPollingTimer = window.setInterval(() => {
    fetchStats()
    fetchStudents()
  }, 10000)
}

const stopStatsPolling = () => {
  if (statsPollingTimer) {
    clearInterval(statsPollingTimer)
    statsPollingTimer = null
  }
}

const refreshData = async () => {
  loading.value = true
  try {
    await Promise.all([
      fetchStats(),
      fetchStudents(),
      fetchLogs()
    ])
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res: any = await request.get('/proctor/stats', {
      params: { publishId: publishId.value }
    })
    Object.assign(stats, res)
  } catch (error) {
    console.error('获取统计数据失败', error)
  }
}

const fetchStudents = async () => {
  try {
    const res: any = await request.get('/proctor/students', {
      params: { publishId: publishId.value }
    })
    students.value = res || []
  } catch (error) {
    console.error('获取学生列表失败', error)
  }
}

const fetchLogs = async () => {
  try {
    const res: any = await request.get('/proctor/logs', {
      params: { publishId: publishId.value }
    })
    // 只保留最近50条警告日志
    realtimeLogs.value = (res || [])
      .filter((l: any) => ['switch_screen', 'leave_page', 'env_abnormal'].includes(l.actionType))
      .slice(0, 50)
  } catch (error) {
    console.error('获取日志失败', error)
  }
}

const connectWebSocket = () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('未登录，无法连接监考服务')
    return
  }

  const wsUrl = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws/proctor?token=${token}&publishId=${publishId.value}`
  
  try {
    ws = new WebSocket(wsUrl)

    ws.onopen = () => {
      wsConnected.value = true
      ElMessage.success('监考连接已建立')
      startHeartbeat()
    }

    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        handleWebSocketMessage(message)
      } catch (e) {
        console.error('解析WebSocket消息失败', e)
      }
    }

    ws.onclose = () => {
      wsConnected.value = false
      stopHeartbeat()
      // 尝试重连
      setTimeout(() => {
        if (!wsConnected.value) {
          connectWebSocket()
        }
      }, 5000)
    }

    ws.onerror = (error) => {
      console.error('WebSocket错误', error)
      wsConnected.value = false
    }
  } catch (error) {
    console.error('创建WebSocket连接失败', error)
  }
}

const disconnectWebSocket = () => {
  stopHeartbeat()
  if (ws) {
    ws.close()
    ws = null
  }
}

const startHeartbeat = () => {
  heartbeatTimer = window.setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'ping' }))
    }
  }, 30000)
}

const stopHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

const handleWebSocketMessage = (message: any) => {
  switch (message.type) {
    case 'proctor_connected':
      console.log('监考连接成功')
      break
    case 'proctor_log':
      handleNewLog(message.data)
      break
    case 'student_status':
      handleStudentStatusUpdate(message.data)
      break
    case 'proctor_stats':
      Object.assign(stats, message.data)
      break
    case 'pong':
      // 心跳响应
      break
    default:
      console.log('未知消息类型:', message.type)
  }
}

const handleNewLog = (logData: any) => {
  // 添加到实时日志列表顶部
  if (['switch_screen', 'leave_page', 'env_abnormal'].includes(logData.actionType)) {
    realtimeLogs.value.unshift(logData)
    // 保持最多50条
    if (realtimeLogs.value.length > 50) {
      realtimeLogs.value.pop()
    }
    
    // 更新学生警告次数
    const student = students.value.find(s => s.studentId === logData.studentId)
    if (student) {
      student.warningCount = logData.warningCount || (student.warningCount + 1)
    }
    
    // 更新总警告数
    stats.totalWarnings++
    
    // 滚动到顶部
    nextTick(() => {
      if (logContainer.value) {
        logContainer.value.scrollTop = 0
      }
    })
  }
}

const handleStudentStatusUpdate = (statusData: any) => {
  const student = students.value.find(s => s.studentId === statusData.studentId)
  if (student) {
    Object.assign(student, statusData)
  }
  // 刷新统计
  fetchStats()
}

const viewStudentLogs = async (student: any) => {
  selectedStudent.value = student
  // 从全部日志中筛选该学生的日志
  try {
    const res: any = await request.get('/proctor/logs', {
      params: { publishId: publishId.value }
    })
    studentLogs.value = (res || []).filter((l: any) => l.recordId === student.recordId)
  } catch (error) {
    studentLogs.value = []
  }
  logDialogVisible.value = true
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const getStatusType = (status: number) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2:
    case 3: return 'warning'
    default: return 'info'
  }
}

const tableRowClassName = ({ row }: { row: any }) => {
  if (row.warningCount >= 3) return 'warning-row-high'
  if (row.warningCount > 0) return 'warning-row'
  return ''
}

const getLogClass = (actionType: string) => {
  switch (actionType) {
    case 'switch_screen': return 'border-orange-500 bg-orange-50'
    case 'leave_page': return 'border-red-500 bg-red-50'
    case 'env_abnormal': return 'border-purple-500 bg-purple-50'
    default: return 'border-gray-300 bg-gray-50'
  }
}

const getLogTagType = (actionType: string) => {
  switch (actionType) {
    case 'switch_screen': return 'warning'
    case 'leave_page': return 'danger'
    case 'env_abnormal': return 'danger'
    case 'snapshot': return 'info'
    default: return 'info'
  }
}
</script>

<style scoped>
.log-container {
  max-height: 500px;
  overflow-y: auto;
}

.log-item {
  transition: all 0.3s;
}

.log-item:hover {
  transform: translateX(4px);
}

:deep(.warning-row) {
  background-color: #fff7ed !important;
}

:deep(.warning-row-high) {
  background-color: #fef2f2 !important;
}

:deep(.warning-row-high td) {
  color: #dc2626;
}
</style>
