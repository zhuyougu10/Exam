<template>
  <div class="app-container p-6 bg-gray-50 min-h-[calc(100vh-64px)]">
    <!-- 顶部标题栏 -->
    <el-card shadow="never" class="border-0 rounded-xl mb-4">
      <div class="flex flex-wrap justify-between items-center gap-4">
        <div class="flex items-center gap-3">
          <div class="p-2 bg-indigo-100 rounded-lg text-indigo-600">
            <el-icon size="20"><Bell /></el-icon>
          </div>
          <div>
            <h2 class="text-lg font-bold text-gray-800">消息中心</h2>
            <p class="text-xs text-gray-500">查看所有系统通知和消息</p>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <el-radio-group v-model="queryParams.isRead" @change="handleSearch">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="false">未读</el-radio-button>
            <el-radio-button :value="true">已读</el-radio-button>
          </el-radio-group>
          <el-button type="primary" :icon="Check" @click="handleMarkAllRead" :disabled="unreadCount === 0">
            全部已读
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 消息列表 -->
    <el-card shadow="never" class="border-0 rounded-xl">
      <div v-loading="loading">
        <div v-if="list.length > 0" class="notice-list">
          <div
            v-for="item in list"
            :key="item.id"
            class="notice-item"
            :class="{ 'is-unread': !item.isRead }"
            @click="handleItemClick(item)"
          >
            <div class="notice-icon" :class="getTypeClass(item.type)">
              <el-icon size="20">
                <component :is="getTypeIcon(item.type)" />
              </el-icon>
            </div>
            <div class="notice-content">
              <div class="notice-header">
                <span class="notice-title">{{ item.title }}</span>
                <span class="notice-time">{{ formatTime(item.createTime) }}</span>
              </div>
              <p class="notice-desc">{{ item.content }}</p>
            </div>
            <div class="notice-status">
              <el-tag v-if="!item.isRead" type="danger" size="small" effect="light" round>未读</el-tag>
              <el-tag v-else type="info" size="small" effect="light" round>已读</el-tag>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty v-else description="暂无消息" :image-size="120" />
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="fetchNotices"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Bell, Check, Notification, Warning, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
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

const list = ref<NoticeItem[]>([])
const loading = ref(false)
const total = ref(0)
const unreadCount = ref(0)

const queryParams = reactive({
  page: 1,
  size: 20,
  isRead: undefined as boolean | undefined
})

const formatTime = (time: string) => {
  if (!time) return ''
  const date = dayjs(time)
  const now = dayjs()
  if (now.diff(date, 'day') < 7) {
    return date.fromNow()
  }
  return date.format('YYYY-MM-DD HH:mm')
}

const getTypeIcon = (type: number) => {
  switch (type) {
    case 2: return 'Warning'
    case 3: return 'InfoFilled'
    default: return 'Notification'
  }
}

const getTypeClass = (type: number) => {
  switch (type) {
    case 2: return 'type-warning'
    case 3: return 'type-info'
    default: return 'type-normal'
  }
}

const fetchNotices = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/notice/my-list', {
      params: {
        page: queryParams.page,
        size: queryParams.size,
        isRead: queryParams.isRead
      }
    })
    list.value = (res.records || []).map((item: any) => ({
      ...item,
      isRead: item.isRead || false
    }))
    total.value = res.total || 0
    unreadCount.value = res.unreadCount || 0
  } catch (error) {
    console.error('获取通知列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchNotices()
}

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

onMounted(() => {
  fetchNotices()
})
</script>

<style scoped>
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #f0f2f5;
  cursor: pointer;
  transition: all 0.2s;
}

.notice-item:hover {
  background: #fafafa;
  border-color: #e4e7ed;
}

.notice-item.is-unread {
  background: #f0f7ff;
  border-color: #d4e5ff;
}

.notice-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.notice-icon.type-normal {
  background: #e8f4ff;
  color: #409eff;
}

.notice-icon.type-warning {
  background: #fef0e6;
  color: #e6a23c;
}

.notice-icon.type-info {
  background: #f0f9eb;
  color: #67c23a;
}

.notice-content {
  flex: 1;
  min-width: 0;
}

.notice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.notice-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.notice-time {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  margin-left: 12px;
}

.notice-desc {
  font-size: 13px;
  color: #606266;
  margin: 0;
  line-height: 1.6;
}

.notice-status {
  flex-shrink: 0;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f2f5;
}
</style>
