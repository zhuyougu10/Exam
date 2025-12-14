<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Bell, Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface NoticeItem {
  id: number
  title: string
  content: string
  time: string
  isRead: boolean
  type: 'system' | 'exam' | 'course'
}

const list = ref<NoticeItem[]>([])
const loading = ref(false)

const unreadCount = computed(() => list.value.filter(item => !item.isRead).length)

const fetchNotices = () => {
  loading.value = true
  // 模拟 API 请求
  setTimeout(() => {
    list.value = [
      {
        id: 1,
        title: 'Java高级程序设计考试即将开始',
        content: '您报名的考试将在 30 分钟后开始，请做好准备。',
        time: '10分钟前',
        isRead: false,
        type: 'exam'
      },
      {
        id: 2,
        title: '系统维护通知',
        content: '系统将于今晚 02:00 进行升级维护，预计耗时 2 小时。',
        time: '2小时前',
        isRead: false,
        type: 'system'
      },
      {
        id: 3,
        title: '您的试卷《数据结构期中》已批改',
        content: '点击查看详细成绩分析。',
        time: '昨天',
        isRead: true,
        type: 'exam'
      }
    ]
    loading.value = false
  }, 500)
}

const handleMarkAllRead = () => {
  list.value.forEach(item => item.isRead = true)
  ElMessage.success('全部已读')
}

const handleItemClick = (item: NoticeItem) => {
  if (!item.isRead) {
    item.isRead = true
  }
  console.log('Clicked:', item)
}

onMounted(() => {
  fetchNotices()
})
</script>

<template>
  <div class="notification-wrapper">
    <el-popover
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
                  <span class="notice-time">{{ item.time }}</span>
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
          <el-button link type="primary" size="small">查看全部消息</el-button>
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