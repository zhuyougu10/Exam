<template>
  <div class="proctor-container">
    <!-- 顶部信息栏 -->
    <el-card shadow="never" class="header-card">
      <div class="header-content">
        <div class="header-left">
          <div class="header-icon">
            <el-icon size="20"><Monitor /></el-icon>
          </div>
          <div>
            <h2 class="header-title">在线监考</h2>
            <p class="header-subtitle">{{ publishId ? '实时监控考试状态，及时处理异常情况' : '选择进行中的考试进入监考' }}</p>
          </div>
        </div>
        <div class="header-right">
          <template v-if="publishId">
            <el-tag :type="wsConnected ? 'success' : 'danger'" effect="dark" round>
              <el-icon class="icon-margin-right"><Connection /></el-icon>
              {{ wsConnected ? '已连接' : '未连接' }}
            </el-tag>
            <el-button :icon="Refresh" circle @click="refreshData" />
            <el-button type="primary" plain @click="exitProctor">
              <el-icon class="icon-margin-right"><Back /></el-icon>返回列表
            </el-button>
          </template>
          <template v-else>
            <el-button :icon="Refresh" circle @click="fetchExamList" />
          </template>
        </div>
      </div>
    </el-card>

    <!-- 考试选择列表 -->
    <template v-if="!publishId">
      <el-card shadow="never" class="content-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">进行中的考试</span>
            <el-tag type="info" size="small">{{ examList.length }} 场考试</el-tag>
          </div>
        </template>
        <div v-if="examListLoading" class="empty-log">
          <el-icon class="is-loading"><Refresh /></el-icon> 加载中...
        </div>
        <div v-else-if="examList.length === 0" class="empty-log">
          暂无进行中的考试
        </div>
        <div v-else class="exam-list">
          <div
            v-for="exam in examList"
            :key="exam.id"
            class="exam-item"
            @click="enterProctor(exam.id)"
          >
            <div class="exam-info">
              <div class="exam-title">{{ exam.title }}</div>
              <div class="exam-meta">
                <span>{{ exam.paperTitle }}</span>
                <span class="exam-separator">|</span>
                <span>{{ exam.className }}</span>
              </div>
            </div>
            <div class="exam-stats">
              <div class="exam-stat">
                <span class="exam-stat-value">{{ exam.totalCount || 0 }}</span>
                <span class="exam-stat-label">总人数</span>
              </div>
              <div class="exam-stat">
                <span class="exam-stat-value stat-green">{{ exam.inProgressCount || 0 }}</span>
                <span class="exam-stat-label">答题中</span>
              </div>
              <div class="exam-stat">
                <span class="exam-stat-value stat-gray">{{ exam.submittedCount || 0 }}</span>
                <span class="exam-stat-label">已交卷</span>
              </div>
            </div>
            <div class="exam-time">
              <div>{{ formatTime(exam.startTime) }}</div>
              <div>至 {{ formatTime(exam.endTime) }}</div>
            </div>
            <el-button type="primary" size="small">进入监考</el-button>
          </div>
        </div>
      </el-card>
    </template>

    <!-- 监考界面 -->
    <template v-else>
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <el-card shadow="never" class="stat-card">
        <div class="stat-content">
          <div class="stat-value stat-blue">{{ stats.total }}</div>
          <div class="stat-label">总人数</div>
        </div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-content">
          <div class="stat-value stat-green">{{ stats.inProgress }}</div>
          <div class="stat-label">答题中</div>
        </div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-content">
          <div class="stat-value stat-gray">{{ stats.submitted }}</div>
          <div class="stat-label">已交卷</div>
        </div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-content">
          <div class="stat-value stat-orange">{{ stats.notStarted }}</div>
          <div class="stat-label">未开始</div>
        </div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-content">
          <div class="stat-value stat-red">{{ stats.totalWarnings }}</div>
          <div class="stat-label">异常警告</div>
        </div>
      </el-card>
    </div>

    <!-- 主内容区 -->
    <div class="main-grid">
      <!-- 学生列表 -->
      <el-card shadow="never" class="content-card student-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">考生状态</span>
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
      <el-card shadow="never" class="content-card log-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">
              <el-icon class="bell-icon"><Bell /></el-icon>
              实时警告
            </span>
            <el-badge :value="realtimeLogs.length" :max="99" type="danger" />
          </div>
        </template>
        <div class="log-container" ref="logContainer">
          <div v-if="realtimeLogs.length === 0" class="empty-log">
            暂无异常记录
          </div>
          <div
            v-for="log in realtimeLogs"
            :key="log.id"
            class="log-item"
            :class="getLogClass(log.actionType)"
          >
            <div class="log-header">
              <div>
                <span class="log-student-name">{{ log.studentName }}</span>
                <el-tag :type="getLogTagType(log.actionType)" size="small" class="log-tag">
                  {{ log.actionLabel }}
                </el-tag>
              </div>
              <span class="log-time">{{ formatTime(log.happenTime) }}</span>
            </div>
            <div v-if="log.content" class="log-content">{{ log.content }}</div>
            <div v-if="log.imgSnapshot" class="log-snapshot">
              <el-image
                :src="log.imgSnapshot"
                :preview-src-list="[log.imgSnapshot]"
                fit="cover"
                class="snapshot-image"
              />
            </div>
          </div>
        </div>
      </el-card>
    </div>
    </template>

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
              class="dialog-snapshot"
            />
            <span v-else class="no-snapshot">-</span>
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

const examList = ref<any[]>([])
const examListLoading = ref(false)

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
  if (pid) {
    publishId.value = Number(pid)
    refreshData()
    connectWebSocket()
    startStatsPolling()
  } else {
    fetchExamList()
  }
})

const fetchExamList = async () => {
  examListLoading.value = true
  try {
    const res: any = await request.get('/exam/publish/list', {
      params: { status: 1 }
    })
    examList.value = res?.records || res || []
  } catch (error) {
    console.error('获取考试列表失败', error)
    examList.value = []
  } finally {
    examListLoading.value = false
  }
}

const enterProctor = (id: number) => {
  publishId.value = id
  refreshData()
  connectWebSocket()
  startStatsPolling()
}

const exitProctor = () => {
  disconnectWebSocket()
  stopStatsPolling()
  publishId.value = 0
  Object.assign(stats, { total: 0, inProgress: 0, submitted: 0, notStarted: 0, totalWarnings: 0, proctorCount: 0 })
  students.value = []
  realtimeLogs.value = []
  fetchExamList()
}

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
    case 'switch_screen': return 'log-switch-screen'
    case 'leave_page': return 'log-leave-page'
    case 'env_abnormal': return 'log-env-abnormal'
    default: return 'log-default'
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
/* 容器 */
.proctor-container {
  padding: 24px;
  background-color: #f9fafb;
  min-height: calc(100vh - 64px);
}

/* 顶部信息栏 */
.header-card {
  border: none;
  border-radius: 12px;
  margin-bottom: 16px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  padding: 8px;
  background-color: #fee2e2;
  border-radius: 8px;
  color: #dc2626;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
  color: #1f2937;
  margin: 0;
}

.header-subtitle {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon-margin-right {
  margin-right: 4px;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  border: none;
  border-radius: 12px;
}

.stat-content {
  text-align: center;
}

.stat-value {
  font-size: 30px;
  font-weight: bold;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  margin-top: 4px;
}

.stat-blue { color: #2563eb; }
.stat-green { color: #16a34a; }
.stat-gray { color: #4b5563; }
.stat-orange { color: #ea580c; }
.stat-red { color: #dc2626; }

/* 主内容网格 */
.main-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.content-card {
  border: none;
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-weight: bold;
}

.bell-icon {
  color: #ef4444;
  margin-right: 4px;
}

/* 考试列表 */
.exam-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.exam-item {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 16px;
  background-color: #f9fafb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.exam-item:hover {
  background-color: #eff6ff;
  transform: translateX(4px);
}

.exam-info {
  flex: 1;
  min-width: 0;
}

.exam-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.exam-meta {
  font-size: 13px;
  color: #6b7280;
}

.exam-separator {
  margin: 0 8px;
  color: #d1d5db;
}

.exam-stats {
  display: flex;
  gap: 24px;
}

.exam-stat {
  text-align: center;
}

.exam-stat-value {
  display: block;
  font-size: 20px;
  font-weight: bold;
  color: #1f2937;
}

.exam-stat-label {
  font-size: 12px;
  color: #9ca3af;
}

.exam-time {
  font-size: 12px;
  color: #6b7280;
  text-align: right;
  min-width: 140px;
}

/* 日志容器 */
.log-container {
  max-height: 500px;
  overflow-y: auto;
}

.empty-log {
  text-align: center;
  color: #9ca3af;
  padding: 40px 0;
}

.log-item {
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  border-left: 4px solid;
  transition: all 0.3s;
}

.log-item:hover {
  transform: translateX(4px);
}

.log-switch-screen {
  border-left-color: #f97316;
  background-color: #fff7ed;
}

.log-leave-page {
  border-left-color: #ef4444;
  background-color: #fef2f2;
}

.log-env-abnormal {
  border-left-color: #a855f7;
  background-color: #faf5ff;
}

.log-default {
  border-left-color: #d1d5db;
  background-color: #f9fafb;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.log-student-name {
  font-weight: 500;
  color: #1f2937;
}

.log-tag {
  margin-left: 8px;
}

.log-time {
  font-size: 12px;
  color: #9ca3af;
}

.log-content {
  font-size: 14px;
  color: #4b5563;
  margin-top: 4px;
}

.log-snapshot {
  margin-top: 8px;
}

.snapshot-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  cursor: pointer;
}

.dialog-snapshot {
  width: 48px;
  height: 48px;
  border-radius: 4px;
  cursor: pointer;
}

.no-snapshot {
  color: #9ca3af;
}

/* 表格警告行 */
:deep(.warning-row) {
  background-color: #fff7ed !important;
}

:deep(.warning-row-high) {
  background-color: #fef2f2 !important;
}

:deep(.warning-row-high td) {
  color: #dc2626;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .main-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .header-content {
    flex-direction: column;
    gap: 16px;
  }
  
  .header-right {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
