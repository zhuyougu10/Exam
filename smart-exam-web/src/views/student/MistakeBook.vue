<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Delete, View, Hide, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 错题接口定义
interface MistakeItem {
  id: number
  questionContent: string
  type: number // 1:单选, 2:多选, 3:判断, 4:简答
  typeLabel: string
  examName: string
  recordTime: string
  analysis: string
  correctAnswer: string
  myAnswer: string
  showAnalysis: boolean
}

const loading = ref(false)
const list = ref<MistakeItem[]>([])
const keyword = ref('')

// 模拟加载数据
const fetchMistakes = () => {
  loading.value = true
  setTimeout(() => {
    list.value = [
      {
        id: 1,
        questionContent: 'Java 中 ArrayList 和 LinkedList 的主要区别是什么？请简要说明其底层数据结构及应用场景。',
        type: 4,
        typeLabel: '简答题',
        examName: 'Java 高级程序设计期中',
        recordTime: '2025-05-20',
        correctAnswer: 'ArrayList基于数组，查询快增删慢；LinkedList基于链表，增删快查询慢...',
        myAnswer: 'ArrayList是链表，LinkedList是数组（记反了）',
        analysis: 'ArrayList 底层是动态数组，内存连续，适合随机访问；LinkedList 底层是双向链表，适合频繁插入删除。',
        showAnalysis: false
      },
      {
        id: 2,
        questionContent: '下列关于 Spring Boot 自动配置原理描述正确的是？',
        type: 1,
        typeLabel: '单选题',
        examName: 'Spring Boot 框架技术',
        recordTime: '2025-06-01',
        correctAnswer: 'A',
        myAnswer: 'C',
        analysis: '@EnableAutoConfiguration 利用 @Import 导入选择器，加载 META-INF/spring.factories 中的配置类。',
        showAnalysis: false
      },
      {
        id: 3,
        questionContent: 'TCP 协议通过什么机制保证数据传输的可靠性？',
        type: 2,
        typeLabel: '多选题',
        examName: '计算机网络基础',
        recordTime: '2025-06-10',
        correctAnswer: 'A,B,D',
        myAnswer: 'A,B',
        analysis: 'TCP 通过校验和、序列号、确认应答、重发控制、连接管理和窗口控制等机制保证可靠性。',
        showAnalysis: false
      },
      {
        id: 4,
        questionContent: 'HashMap 是线程安全的吗？',
        type: 3,
        typeLabel: '判断题',
        examName: 'Java 基础测验',
        recordTime: '2025-04-15',
        correctAnswer: '错误',
        myAnswer: '正确',
        analysis: 'HashMap 是非线程安全的，多线程环境下应使用 ConcurrentHashMap。',
        showAnalysis: false
      }
    ]
    loading.value = false
  }, 500)
}

// 切换解析显示
const toggleAnalysis = (item: MistakeItem) => {
  item.showAnalysis = !item.showAnalysis
}

// 移除错题
const handleRemove = (item: MistakeItem) => {
  ElMessageBox.confirm(
      '确定要将这道题从错题本中移除吗？移除后不可恢复。',
      '提示',
      {
        confirmButtonText: '确认移除',
        cancelButtonText: '取消',
        type: 'warning',
      }
  )
      .then(() => {
        list.value = list.value.filter(i => i.id !== item.id)
        ElMessage.success('移除成功')
      })
      .catch(() => {})
}

onMounted(() => {
  fetchMistakes()
})
</script>

<template>
  <div class="mistake-container">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="title-box">
        <h2>我的错题本</h2>
        <span class="subtitle">共收录 {{ list.length }} 道错题</span>
      </div>
      <div class="search-box">
        <el-input
            v-model="keyword"
            placeholder="搜索题目关键词..."
            :prefix-icon="Search"
            style="width: 240px"
        />
      </div>
    </div>

    <!-- 错题卡片网格 -->
    <div v-loading="loading" class="mistake-grid">
      <el-row :gutter="20">
        <el-col
            v-for="item in list"
            :key="item.id"
            :xs="24"
            :sm="12"
            :lg="8"
            :xl="6"
        >
          <div class="mistake-card">
            <!-- 卡片头部 -->
            <div class="card-header">
              <el-tag :type="item.type === 1 ? '' : item.type === 2 ? 'warning' : 'info'" size="small">
                {{ item.typeLabel }}
              </el-tag>
              <span class="exam-tag">{{ item.examName }}</span>
            </div>

            <!-- 题目预览 -->
            <div class="card-content">
              <p class="question-text">{{ item.questionContent }}</p>
              <div class="time-info">收录时间：{{ item.recordTime }}</div>
            </div>

            <!-- 解析区域 (折叠) -->
            <transition name="el-zoom-in-top">
              <div v-if="item.showAnalysis" class="analysis-panel">
                <div class="panel-item">
                  <span class="label text-green">正确答案：</span>
                  <span class="value">{{ item.correctAnswer }}</span>
                </div>
                <div class="panel-item">
                  <span class="label text-red">我的答案：</span>
                  <span class="value">{{ item.myAnswer }}</span>
                </div>
                <div class="panel-item column">
                  <span class="label text-orange">题目解析：</span>
                  <p class="desc">{{ item.analysis }}</p>
                </div>
              </div>
            </transition>

            <!-- 底部操作栏 -->
            <div class="card-actions">
              <div class="action-btn" @click="toggleAnalysis(item)">
                <el-icon v-if="!item.showAnalysis"><View /></el-icon>
                <el-icon v-else><Hide /></el-icon>
                <span>{{ item.showAnalysis ? '收起解析' : '查看解析' }}</span>
              </div>
              <div class="divider"></div>
              <div class="action-btn delete" @click="handleRemove(item)">
                <el-icon><Delete /></el-icon>
                <span>移除</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 空状态 -->
      <div v-if="!loading && list.length === 0" class="empty-box">
        <el-empty description="太棒了，目前没有错题！" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.mistake-container {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 84px);
}

/* Toolbar */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: white;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.title-box h2 {
  margin: 0;
  font-size: 20px;
  display: inline-block;
  margin-right: 12px;
}

.subtitle {
  color: #909399;
  font-size: 14px;
}

/* Grid & Card */
.mistake-grid {
  min-height: 400px;
}

.mistake-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  transition: all 0.3s;
  border: 1px solid #ebeef5;
}

.mistake-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  padding: 16px;
  border-bottom: 1px solid #f5f7fa;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.exam-tag {
  font-size: 12px;
  color: #909399;
  background: #f4f4f5;
  padding: 2px 6px;
  border-radius: 4px;
}

.card-content {
  padding: 16px;
  flex: 1;
}

.question-text {
  font-size: 15px;
  color: #303133;
  line-height: 1.6;
  margin: 0 0 12px 0;
  /* 限制3行 */
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 72px;
}

.time-info {
  font-size: 12px;
  color: #c0c4cc;
}

/* Analysis Panel */
.analysis-panel {
  background-color: #fafafa;
  padding: 16px;
  border-top: 1px solid #ebeef5;
}

.panel-item {
  font-size: 13px;
  margin-bottom: 8px;
  display: flex;
  flex-wrap: wrap;
}

.panel-item.column {
  flex-direction: column;
}

.panel-item:last-child {
  margin-bottom: 0;
}

.panel-item .label {
  font-weight: bold;
  margin-right: 4px;
  flex-shrink: 0;
}

.panel-item .desc {
  margin: 4px 0 0 0;
  color: #606266;
  line-height: 1.5;
}

.text-green { color: #67c23a; }
.text-red { color: #f56c6c; }
.text-orange { color: #e6a23c; }

/* Actions */
.card-actions {
  display: flex;
  border-top: 1px solid #ebeef5;
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 0;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: background-color 0.2s;
}

.action-btn:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.action-btn.delete:hover {
  color: #f56c6c;
}

.action-btn .el-icon {
  margin-right: 6px;
}

.divider {
  width: 1px;
  background-color: #ebeef5;
}

.empty-box {
  padding: 40px;
  background: white;
  border-radius: 8px;
}
</style>