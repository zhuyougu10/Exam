<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { Search, Trophy, DataLine, Timer } from '@element-plus/icons-vue'

// 模拟考试场次列表
const examOptions = [
  { id: 1, label: '2024-2025第一学期 Java高级程序设计期末考' },
  { id: 2, label: '2024-2025第一学期 数据结构期中测验' },
  { id: 3, label: '2023级 计算机网络第一次月考' }
]
const currentExamId = ref(1)

// 模拟核心指标数据
const stats = ref({
  avgScore: 78.5,
  maxScore: 98,
  minScore: 45,
  passRate: 85.2,
  attendeeCount: 120
})

// 模拟错题TOP10数据
const wrongQuestionList = ref([
  { id: 101, content: 'Java中关于Volatile关键字的描述...', type: '单选题', errorRate: 68, knowledge: '多线程' },
  { id: 102, content: '以下属于TCP三次握手过程的是...', type: '多选题', errorRate: 62, knowledge: '网络协议' },
  { id: 103, content: 'Spring Bean的生命周期包含哪些阶段...', type: '简答题', errorRate: 55, knowledge: 'Spring框架' },
  { id: 104, content: '二叉树的后序遍历序列是...', type: '单选题', errorRate: 48, knowledge: '数据结构' },
  { id: 105, content: 'MySQL索引失效的场景...', type: '多选题', errorRate: 45, knowledge: '数据库' }
])

const barChartRef = ref<HTMLElement | null>(null)
const radarChartRef = ref<HTMLElement | null>(null)
let barChart: echarts.ECharts | null = null
let radarChart: echarts.ECharts | null = null

// 初始化图表
const initCharts = () => {
  if (barChartRef.value) {
    barChart = echarts.init(barChartRef.value)
    barChart.setOption({
      title: { text: '成绩分数段分布', left: 'center', textStyle: { fontSize: 16 } },
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: ['0-60', '60-70', '70-80', '80-90', '90-100'] },
      yAxis: { type: 'value' },
      series: [
        {
          data: [5, 15, 40, 45, 15],
          type: 'bar',
          itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
          barWidth: '40%'
        }
      ]
    })
  }

  if (radarChartRef.value) {
    radarChart = echarts.init(radarChartRef.value)
    radarChart.setOption({
      title: { text: '班级知识点掌握热力', left: 'center', textStyle: { fontSize: 16 } },
      tooltip: {},
      radar: {
        indicator: [
          { name: '多线程', max: 100 },
          { name: '集合', max: 100 },
          { name: 'IO流', max: 100 },
          { name: '网络', max: 100 },
          { name: 'JVM', max: 100 },
          { name: '反射', max: 100 }
        ],
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
              value: [65, 85, 70, 90, 55, 75],
              name: '班级平均'
            }
          ]
        }
      ]
    })
  }
}

// 监听窗口大小变化
const handleResize = () => {
  barChart?.resize()
  radarChart?.resize()
}

// 模拟切换场次加载数据
const handleExamChange = () => {
  // 这里可以调用后端 API
  console.log('Switch to exam:', currentExamId.value)
  // 模拟数据变化动画
  initCharts()
}

onMounted(() => {
  nextTick(() => {
    initCharts()
    window.addEventListener('resize', handleResize)
  })
})
</script>

<template>
  <div class="analysis-container">
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
      <el-button type="primary" :icon="Search">刷新分析</el-button>
    </div>

    <!-- 核心指标卡片 -->
    <div class="stats-row">
      <div class="stat-card card blue-gradient">
        <div class="stat-icon">
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">班级平均分</div>
          <div class="stat-value">{{ stats.avgScore }}</div>
        </div>
      </div>
      <div class="stat-card card purple-gradient">
        <div class="stat-icon">
          <el-icon><Trophy /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">最高分</div>
          <div class="stat-value">{{ stats.maxScore }}</div>
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
            <el-tag size="small" :type="scope.row.type === '单选题' ? 'primary' : 'warning'">
              {{ scope.row.type }}
            </el-tag>
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
        <el-table-column label="操作" width="120" align="center">
          <template #default>
            <el-button link type="primary" size="small">查看详情</el-button>
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
  font-size: 28px;
  font-weight: bold;
}

/* 图表行 */
.charts-row {
  display: flex;
  gap: 24px;
}

.chart-col {
  flex: 1;
  min-width: 0; /* 防止Flex子项溢出 */
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