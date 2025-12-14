<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  EditPen,
  DocumentAdd,
  DataAnalysis,
  Files,
  UserFilled,
  Timer
} from '@element-plus/icons-vue'

const router = useRouter()

// 模拟待办事项
const pendingTasks = ref([
  { id: 1, title: '《Java期中考》有 5 份主观题待批改', time: '10分钟前', urgent: true },
  { id: 2, title: '《网络协议》考试发布成功，请确认', time: '1小时前', urgent: false },
  { id: 3, title: '题库导入任务完成，成功导入 150 题', time: '昨天', urgent: false }
])

// 快捷操作跳转
const handleQuickAction = (action: string) => {
  switch (action) {
    case 'publish':
      router.push('/teacher/exam-publish')
      break
    case 'import':
      router.push('/teacher/question-bank')
      break
    case 'paper':
      router.push('/teacher/paper-list')
      break
    case 'analysis':
      router.push('/teacher/analysis')
      break
  }
}
</script>

<template>
  <div class="dashboard-container">
    <!-- 欢迎 Banner -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <h2>早安，张老师！</h2>
        <p>今天是 2025年12月14日，新的一天，祝您工作愉快。</p>
      </div>
      <div class="welcome-img">
        <!-- 简单的装饰图形，可用图片替代 -->
        <div class="decoration-circle"></div>
      </div>
    </div>

    <div class="main-grid">
      <!-- 左侧主要内容 -->
      <div class="left-col">
        <!-- 快捷入口 -->
        <div class="section-card">
          <h3 class="card-title">快捷操作</h3>
          <div class="quick-actions">
            <div class="action-item" @click="handleQuickAction('publish')">
              <div class="icon-box blue">
                <el-icon><Timer /></el-icon>
              </div>
              <span>快速发布考试</span>
            </div>
            <div class="action-item" @click="handleQuickAction('import')">
              <div class="icon-box green">
                <el-icon><DocumentAdd /></el-icon>
              </div>
              <span>题库导入</span>
            </div>
            <div class="action-item" @click="handleQuickAction('paper')">
              <div class="icon-box purple">
                <el-icon><EditPen /></el-icon>
              </div>
              <span>试卷管理</span>
            </div>
            <div class="action-item" @click="handleQuickAction('analysis')">
              <div class="icon-box orange">
                <el-icon><DataAnalysis /></el-icon>
              </div>
              <span>成绩分析</span>
            </div>
          </div>
        </div>

        <!-- 数据概览 -->
        <div class="section-card" style="margin-top: 24px;">
          <h3 class="card-title">数据概览</h3>
          <div class="data-overview">
            <div class="data-item">
              <div class="label">试卷总数</div>
              <div class="value">24 <span class="unit">套</span></div>
            </div>
            <div class="divider"></div>
            <div class="data-item">
              <div class="label">题库题目</div>
              <div class="value">1,205 <span class="unit">道</span></div>
            </div>
            <div class="divider"></div>
            <div class="data-item">
              <div class="label">累计参考人次</div>
              <div class="value">3,502 <span class="unit">人</span></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧侧边栏 -->
      <div class="right-col">
        <div class="section-card h-full">
          <div class="card-header">
            <h3 class="card-title">待办 / 通知</h3>
            <el-button link type="primary">查看全部</el-button>
          </div>
          <div class="task-list">
            <div
                v-for="task in pendingTasks"
                :key="task.id"
                class="task-item"
            >
              <div class="task-dot" :class="{ 'urgent': task.urgent }"></div>
              <div class="task-content">
                <div class="task-title">{{ task.title }}</div>
                <div class="task-time">{{ task.time }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-container {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 84px);
}

/* Welcome Banner */
.welcome-banner {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 12px;
  padding: 32px;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.welcome-content h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
}

.welcome-content p {
  margin: 0;
  opacity: 0.9;
}

.decoration-circle {
  width: 150px;
  height: 150px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  position: absolute;
  right: -20px;
  bottom: -40px;
}

/* Grid Layout */
.main-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.section-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.h-full {
  height: 100%;
}

.card-title {
  margin: 0 0 20px 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
  border-left: 4px solid #3b82f6;
  padding-left: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header .card-title {
  margin-bottom: 0;
}

/* Quick Actions */
.quick-actions {
  display: flex;
  gap: 16px;
}

.action-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.action-item:hover {
  background-color: #f5f7fa;
  border-color: #c6e2ff;
  transform: translateY(-2px);
}

.icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 12px;
}

.icon-box.blue { background-color: #ecf5ff; color: #409eff; }
.icon-box.green { background-color: #f0f9eb; color: #67c23a; }
.icon-box.purple { background-color: #f9f0ff; color: #a855f7; }
.icon-box.orange { background-color: #fdf6ec; color: #e6a23c; }

.action-item span {
  font-size: 14px;
  color: #606266;
}

/* Data Overview */
.data-overview {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 20px 0;
}

.data-item {
  text-align: center;
}

.data-item .label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.data-item .value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.data-item .unit {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}

.divider {
  width: 1px;
  height: 40px;
  background-color: #ebeef5;
}

/* Task List */
.task-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-item {
  display: flex;
  align-items: flex-start;
  padding-bottom: 16px;
  border-bottom: 1px solid #f5f7fa;
}

.task-item:last-child {
  border-bottom: none;
}

.task-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #409eff;
  margin-top: 6px;
  margin-right: 12px;
}

.task-dot.urgent {
  background-color: #f56c6c;
}

.task-content {
  flex: 1;
}

.task-title {
  font-size: 14px;
  color: #303133;
  line-height: 1.4;
  margin-bottom: 4px;
}

.task-time {
  font-size: 12px;
  color: #909399;
}

@media (max-width: 768px) {
  .main-grid {
    grid-template-columns: 1fr;
  }
  .quick-actions {
    flex-wrap: wrap;
  }
  .action-item {
    min-width: 40%;
  }
}
</style>