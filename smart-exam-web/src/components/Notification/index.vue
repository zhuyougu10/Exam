<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Check } from '@element-plus/icons-vue'
import { ElMessage, ElNotification } from 'element-plus'
import request from '@/utils/request'
import { useNoticeWebSocket } from '@/hooks/useWebSocket'
import { getWebSocketInstance } from '@/utils/websocket'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

interface NoticeItem {
  id: number
  title: string
  content: string
  createTime: string
  isRead: boolean
  type: number
}

const router = useRouter()
const list = ref<NoticeItem[]>([])
const loading = ref(false)
const unreadCount = ref(0)
const popoverRef = ref()

// 跳转到消息中心
const goToNoticeCenter = () => {
  popoverRef.value?.hide()
  router.push('/notice')
}

// WebSocket连接
const { 
  status: wsStatus, 
  isConnected: wsConnected, 
  init: initWebSocket, 
  cleanup: cleanupWebSocket,
  setNotices,
  setUnreadCount
} = useNoticeWebSocket()

// 格式化时间为相对时间
const formatTime = (time: string) => {
  if (!time) return ''
  return dayjs(time).fromNow()
}

// 获取通知列表
const fetchNotices = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/notice/my-list', {
      params: { page: 1, size: 20 }
    })
    list.value = (res.records || []).map((item: any) => ({
      ...item,
      isRead: item.isRead || false
    }))
    unreadCount.value = res.unreadCount || 0
    setNotices(list.value)
    setUnreadCount(unreadCount.value)
  } catch (error) {
    console.error('获取通知列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 标记全部已读
const handleMarkAllRead = async () => {
  try {
    await request.post('/notice/read-all')
    list.value.forEach(item => item.isRead = true)
    unreadCount.value = 0
    ElMessage.success('全部已读')
  } catch (error) {
    console.error('标记全部已读失败:', error)
  }
}

// 点击通知项
const handleItemClick = async (item: NoticeItem) => {
  if (!item.isRead) {
    try {
      const res: any = await request.post(`/notice/read/${item.id}`)
      item.isRead = true
      unreadCount.value = res.unreadCount ?? Math.max(0, unreadCount.value - 1)
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
}

// 处理WebSocket新通知
const handleNewNotice = (data: any) => {
  if (data) {
    // 添加到列表头部
    list.value.unshift({
      id: data.id,
      title: data.title,
      content: data.content,
      createTime: data.createTime,
      isRead: false,
      type: data.type
    })
    unreadCount.value++
    
    // 显示桌面通知
    ElNotification({
      title: data.title,
      message: data.content,
      type: data.type === 2 ? 'warning' : 'info',
      duration: 5000
    })
  }
}

// 初始化WebSocket连接
const initWsConnection = () => {
  const token = localStorage.getItem('token')
  if (token) {
    initWebSocket(token)
    
    // 注册消息处理器
    const checkAndRegister = setInterval(() => {
      const instance = getWebSocketInstance()
      if (instance?.isConnected) {
        clearInterval(checkAndRegister)
        instance.on('notice', handleNewNotice)
        instance.on('exam_notice', handleNewNotice)
        instance.on('unread_count', (count: number) => {
          unreadCount.value = count
        })
      }
    }, 100)
    
    setTimeout(() => clearInterval(checkAndRegister), 10000)
  }
}

onMounted(() => {
  fetchNotices()
  initWsConnection()
})

onUnmounted(() => {
  const instance = getWebSocketInstance()
  if (instance) {
    instance.off('notice', handleNewNotice)
    instance.off('exam_notice', handleNewNotice)
  }
})
</script>

<template>
  <div class="notification-wrapper">
    <el-popover
        ref="popoverRef"
        placement="bottom"
        :width="320"
        trigger="click"
        popper-class="notification-popper"
    >
      <template #reference>
        <div class="notification-trigger">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="badge-item">
            <el-icon :size="20"><Bell /></el-icon>
          </el-badge>
        </div>
      </template>

      <!-- Popover Content -->
      <div class="notification-content">
        <!-- Header -->
        <div class="notice-header">
          <span class="title">消息通知</span>
          <el-button
              v-if="unreadCount > 0"
              link
              type="primary"
              size="small"
              @click="handleMarkAllRead"
          >
            <el-icon class="icon-margin"><Check /></el-icon> 全部已读
          </el-button>
        </div>

        <!-- List -->
        <div class="notice-list-container" v-loading="loading">
          <ul v-if="list.length > 0" class="notice-list">
            <li
                v-for="item in list"
                :key="item.id"
                class="notice-item"
                @click="handleItemClick(item)"
            >
              <!-- Unread Dot -->
              <span v-if="!item.isRead" class="unread-dot"></span>

              <div class="notice-info">
                <div class="notice-top">
                  <span class="notice-title" :class="{ 'is-read': item.isRead }">
                    {{ item.title }}
                  </span>
                  <span class="notice-time">{{ formatTime(item.createTime) }}</span>
                </div>
                <p class="notice-desc">
                  {{ item.content }}
                </p>
              </div>
            </li>
          </ul>

          <!-- Empty State -->
          <div v-else class="empty-state">
            <el-empty description="暂无消息" :image-size="60" />
          </div>
        </div>

        <!-- Footer -->
        <div class="notice-footer">
          <el-button link type="primary" size="small" @click="goToNoticeCenter">查看全部消息</el-button>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<style scoped>
.notification-wrapper {
  height: 100%;
  display: flex;
  align-items: center;
}

.notification-trigger {
  cursor: pointer;
  padding: 0 12px;
  height: 100%;
  display: flex;
  align-items: center;
  transition: background-color 0.3s;
}

.notification-trigger:hover {
  background-color: #f5f7fa;
}

.badge-item {
  display: flex;
  align-items: center;
  color: #606266;
}

/* Popover 内部样式 */
.notification-content {
  display: flex;
  flex-direction: column;
}

.notice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}

.notice-header .title {
  font-weight: bold;
  font-size: 16px;
  color: #303133;
}

.icon-margin {
  margin-right: 4px;
}

.notice-list-container {
  max-height: 400px;
  overflow-y: auto;
}

.notice-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.notice-item {
  position: relative;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f2f5;
  transition: background-color 0.2s;
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-item:hover {
  background-color: #fafafa;
}

.unread-dot {
  position: absolute;
  left: 8px;
  top: 16px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #f56c6c;
}

.notice-info {
  margin-left: 8px;
}

.notice-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}

.notice-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  /* 文字截断 */
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notice-title.is-read {
  color: #909399;
}

.notice-time {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  margin-left: 8px;
}

.notice-desc {
  font-size: 12px;
  color: #606266;
  margin: 0;
  line-height: 1.5;
  /* 两行截断 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty-state {
  padding: 32px 0;
}

.notice-footer {
  border-top: 1px solid #ebeef5;
  padding: 8px 0;
  text-align: center;
}
</style>