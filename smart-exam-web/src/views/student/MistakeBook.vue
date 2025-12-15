<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Delete, View, Hide, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 错题接口定义
interface MistakeItem {
  id: number
  questionId: number
  questionContent: string
  questionType: number // 1:单选, 2:多选, 3:判断, 4:简答, 5:填空
  options: string // JSON字符串
  courseName: string
  createTime: string
  analysis: string
  correctAnswer: string
  myAnswer: string
  wrongCount: number
  showAnalysis: boolean
}

const loading = ref(false)
const list = ref<MistakeItem[]>([])
const keyword = ref('')
const total = ref(0)
const queryParams = ref({
  current: 1,
  size: 50
})

// 过滤后的列表
const filteredList = computed(() => {
  if (!keyword.value) return list.value
  return list.value.filter(item =>
      item.questionContent?.toLowerCase().includes(keyword.value.toLowerCase())
  )
})

// 题型标签
const getTypeLabel = (type: number) => {
  const labels: Record<number, string> = {
    1: '单选题',
    2: '多选题',
    3: '判断题',
    4: '简答题',
    5: '填空题'
  }
  return labels[type] || '未知'
}

// 题型标签颜色
const getTypeTagType = (type: number) => {
  const types: Record<number, string> = {
    1: '',
    2: 'warning',
    3: 'info',
    4: 'success',
    5: 'danger'
  }
  return types[type] || ''
}

// 格式化答案显示
const formatAnswer = (answer: string, type: number, options?: string) => {
  if (!answer && answer !== '0') return '-'
  
  // 判断题: 0=错误, 1=正确
  if (type === 3) {
    return answer === '1' || answer === 'true' || answer === '正确' ? '正确' : '错误'
  }
  
  // 单选/多选题：将索引转换为字母 (0=A, 1=B, 2=C, 3=D)
  if (type === 1 || type === 2) {
    try {
      // 尝试解析JSON数组格式 [0,1,2]
      const parsed = JSON.parse(answer)
      if (Array.isArray(parsed)) {
        return parsed.map(i => String.fromCharCode(65 + Number(i))).join(', ')
      }
      // 单个数字（JSON.parse("2") 返回 2）
      if (typeof parsed === 'number' && parsed >= 0 && parsed < 26) {
        return String.fromCharCode(65 + parsed)
      }
    } catch {
      // JSON解析失败，尝试其他格式
    }
    
    // 尝试逗号分隔格式 "0,1,2"
    if (answer.includes(',')) {
      return answer.split(',').map(i => String.fromCharCode(65 + Number(i.trim()))).join(', ')
    }
    
    // 单个数字字符串 "2"
    const num = parseInt(answer)
    if (!isNaN(num) && num >= 0 && num < 26) {
      return String.fromCharCode(65 + num)
    }
  }
  
  return answer
}

// 解析选项
const parseOptions = (optionsStr: string) => {
  if (!optionsStr) return []
  try {
    return JSON.parse(optionsStr)
  } catch {
    return []
  }
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 10)
}

// 加载数据
const fetchMistakes = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/mistake-book/list', { params: queryParams.value })
    const records = res.records || []
    list.value = records.map((item: any) => ({
      ...item,
      showAnalysis: false
    }))
    total.value = res.total || 0
  } catch (error) {
    console.error('获取错题本失败', error)
    list.value = []
  } finally {
    loading.value = false
  }
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
      .then(async () => {
        try {
          await request.delete(`/mistake-book/${item.id}`)
          list.value = list.value.filter(i => i.id !== item.id)
          total.value--
          ElMessage.success('移除成功')
        } catch (error) {
          ElMessage.error('移除失败')
        }
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
            v-for="item in filteredList"
            :key="item.id"
            :xs="24"
            :sm="12"
            :lg="8"
            :xl="6"
        >
          <div class="mistake-card">
            <!-- 卡片头部 -->
            <div class="card-header">
              <el-tag :type="getTypeTagType(item.questionType)" size="small">
                {{ getTypeLabel(item.questionType) }}
              </el-tag>
              <span class="exam-tag">{{ item.courseName }}</span>
            </div>

            <!-- 题目预览 -->
            <div class="card-content">
              <p class="question-text">{{ item.questionContent }}</p>
              <div class="time-info">
                收录时间：{{ formatTime(item.createTime) }}
                <span v-if="item.wrongCount > 1" class="wrong-count">（错{{ item.wrongCount }}次）</span>
              </div>
            </div>

            <!-- 选项展示（选择题） -->
            <div v-if="[1, 2].includes(item.questionType) && item.options" class="options-panel">
              <div v-for="(opt, oIdx) in parseOptions(item.options)" :key="oIdx" class="option-item">
                <span class="option-label">{{ String.fromCharCode(65 + oIdx) }}.</span>
                <span>{{ opt }}</span>
              </div>
            </div>

            <!-- 解析区域 (折叠) -->
            <transition name="el-zoom-in-top">
              <div v-if="item.showAnalysis" class="analysis-panel">
                <div class="panel-item">
                  <span class="label text-green">正确答案：</span>
                  <span class="value">{{ formatAnswer(item.correctAnswer, item.questionType) }}</span>
                </div>
                <div class="panel-item">
                  <span class="label text-red">我的答案：</span>
                  <span class="value">{{ formatAnswer(item.myAnswer, item.questionType) || '未作答' }}</span>
                </div>
                <div v-if="item.analysis" class="panel-item column">
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
      <div v-if="!loading && filteredList.length === 0" class="empty-box">
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

.wrong-count {
  color: #f56c6c;
  font-weight: 500;
}

/* Options Panel */
.options-panel {
  padding: 0 16px 12px;
  font-size: 13px;
  color: #606266;
}

.option-item {
  display: flex;
  gap: 4px;
  margin-bottom: 4px;
  line-height: 1.5;
}

.option-label {
  font-weight: 500;
  color: #909399;
  min-width: 20px;
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