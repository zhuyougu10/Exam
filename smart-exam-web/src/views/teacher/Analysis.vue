<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'
import { Search, Trophy, DataLine, Timer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// 状态
const loading = ref(false)
const examOptions = ref<any[]>([])
const currentExamId = ref<number | undefined>(undefined)

const stats = ref({
  avgScore: 0,
  maxScore: 0,
  minScore: 0,
  passRate: 0,
  attendeeCount: 0
})

const wrongQuestionList = ref<any[]>([])

const barChartRef = ref<HTMLElement | null>(null)
const radarChartRef = ref<HTMLElement | null>(null)
let barChart: echarts.ECharts | null = null
let radarChart: echarts.ECharts | null = null

// 初始化加载
onMounted(() => {
  fetchExamList()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  radarChart?.dispose()
})

const handleResize = () => {
  barChart?.resize()
  radarChart?.resize()
}

// 1. 获取考试列表
const fetchExamList = async () => {
  try {
    const res: any = await request.get('/analysis/exam/list')
    if (res && res.length > 0) {
      examOptions.value = res.map((item: any) => ({
        id: item.id,
        label: item.title + (item.status === 2 ? ' (已结束)' : ' (进行中)')
      }))
      // 默认选中第一个
      currentExamId.value = res[0].id
      handleExamChange()
    } else {
      ElMessage.info('暂无考试记录')
    }
  } catch (error) {
    console.error('Fetch exams failed', error)
  }
}

// 2. 获取具体分析数据
const handleExamChange = async () => {
  if (!currentExamId.value) return
  loading.value = true
  try {
    const res: any = await request.get(`/analysis/exam/${currentExamId.value}`)
    if (res) {
      // 核心指标
      stats.value = {
        avgScore: res.avgScore || 0,
        maxScore: res.maxScore || 0,
        minScore: res.minScore || 0,
        passRate: res.passRate || 0,
        attendeeCount: res.attendeeCount || 0
      }

      // 错题榜
      wrongQuestionList.value = res.wrongTop10 || []

      // 更新图表
      nextTick(() => {
        updateCharts(res)
      })
    }
  } catch (error) {
    ElMessage.error('获取分析数据失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 初始化/更新图表
const updateCharts = (data: any) => {
  // 柱状图：分数分布
  if (barChartRef.value) {
    if (!barChart) barChart = echarts.init(barChartRef.value)
    barChart.setOption({
      title: { text: '成绩分数段分布', left: 'center', textStyle: { fontSize: 16 } },
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: data.scoreRanges || [] },
      yAxis: { type: 'value' },
      series: [
        {
          data: data.scoreCounts || [],
          type: 'bar',
          itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
          barWidth: '40%',
          label: { show: true, position: 'top' }
        }
      ]
    })
  }

  // 雷达图：知识点
  if (radarChartRef.value) {
    if (!radarChart) radarChart = echarts.init(radarChartRef.value)

    const indicators = (data.knowledgeRadar || []).map((item: any) => ({
      name: item.name,
      max: item.max || 100
    }))

    const radarValues = (data.knowledgeRadar || []).map((item: any) => item.value)

    radarChart.setOption({
      title: { text: '知识点掌握热力图', left: 'center', textStyle: { fontSize: 16 } },
      tooltip: {},
      radar: {
        indicator: indicators.length > 0 ? indicators : [{ name: '暂无数据', max: 100 }],
        radius: '65%'
      },
      series: [
        {
          name: '平均得分率',
          type: 'radar',
          areaStyle: { color: 'rgba(103, 194, 58, 0.4)' },
          itemStyle: { color: '#67c23a' },
          data: [
            {
              value: radarValues.length > 0 ? radarValues : [0],
              name: '班级平均'
            }
          ]
        }
      ]
    })
  }
}
</script>

<template>
  <div class="analysis-container" v-loading="loading">
    <!-- 顶部筛选 -->
    <div class="filter-header card">
      <div class="filter-item">
        <span class="label">选择考试场次：</span>
        <el-select
            v-model="currentExamId"
            placeholder="请选择"
            style="width: 300px"
            @change="handleExamChange"
        >
          <el-option
              v-for="item in examOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
          />
        </el-select>
      </div>
      <el-button type="primary" :icon="Search" @click="handleExamChange">刷新分析</el-button>
    </div>

    <!-- 核心指标卡片 -->
    <div class="stats-row">
      <div class="stat-card card blue-gradient">
        <div class="stat-icon">
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">平均分 / 参考人数</div>
          <div class="stat-value">{{ stats.avgScore }} <span style="font-size:14px">/ {{ stats.attendeeCount }}人</span></div>
        </div>
      </div>
      <div class="stat-card card purple-gradient">
        <div class="stat-icon">
          <el-icon><Trophy /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">最高分 / 最低分</div>
          <div class="stat-value">{{ stats.maxScore }} <span style="font-size:14px">/ {{ stats.minScore }}</span></div>
        </div>
      </div>
      <div class="stat-card card green-gradient">
        <div class="stat-icon">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">及格率</div>
          <div class="stat-value">{{ stats.passRate }}%</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <div class="chart-col card">
        <div ref="barChartRef" class="chart-container"></div>
      </div>
      <div class="chart-col card">
        <div ref="radarChartRef" class="chart-container"></div>
      </div>
    </div>

    <!-- 错题表格 -->
    <div class="table-section card">
      <div class="section-title">
        <h3>高频错题 TOP 10</h3>
        <span class="subtitle">针对错误率较高的题目建议重点讲解</span>
      </div>
      <el-table :data="wrongQuestionList" style="width: 100%" stripe>
        <el-table-column type="index" label="排名" width="80" align="center">
          <template #default="scope">
            <span :class="['rank-badge', scope.$index < 3 ? 'top-rank' : '']">
              {{ scope.$index + 1 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="题目内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="type" label="题型" width="100" align="center">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="knowledge" label="关联知识点" width="150" align="center" />
        <el-table-column prop="errorRate" label="错误率" width="120" align="center">
          <template #default="scope">
            <span class="error-text">{{ scope.row.errorRate }}%</span>
            <el-progress
                :percentage="scope.row.errorRate"
                :show-text="false"
                status="exception"
                :stroke-width="6"
                style="margin-top: 4px"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.analysis-container {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  padding: 20px;
}

/* 筛选头 */
.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-item {
  display: flex;
  align-items: center;
}

.filter-item .label {
  font-size: 14px;
  color: #606266;
  margin-right: 12px;
}

/* 统计卡片 */
.stats-row {
  display: flex;
  gap: 24px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  color: white;
  padding: 24px;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.blue-gradient {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.purple-gradient {
  background: linear-gradient(135deg, #a855f7 0%, #7c3aed 100%);
}

.green-gradient {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.stat-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
}

/* 图表行 */
.charts-row {
  display: flex;
  gap: 24px;
}

.chart-col {
  flex: 1;
  min-width: 0;
}

.chart-container {
  width: 100%;
  height: 350px;
}

/* 表格区域 */
.section-title {
  margin-bottom: 16px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.section-title h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.section-title .subtitle {
  font-size: 12px;
  color: #909399;
}

.rank-badge {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  background-color: #f0f2f5;
  color: #909399;
  font-size: 12px;
  font-weight: bold;
}

.rank-badge.top-rank {
  background-color: #ff9800;
  color: white;
}

.error-text {
  color: #f56c6c;
  font-weight: bold;
}

@media (max-width: 768px) {
  .stats-row, .charts-row {
    flex-direction: column;
  }
}
</style>