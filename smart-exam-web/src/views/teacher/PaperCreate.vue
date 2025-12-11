<template>
  <div class="app-container p-6 bg-gray-50 min-h-screen">
    <div class="max-w-6xl mx-auto main-wrapper">

      <!-- 顶部步骤条 -->
      <div class="mb-8 bg-white p-6 rounded-xl shadow-sm step-card">
        <el-steps :active="activeStep" finish-status="success" align-center>
          <el-step title="基本信息" description="设置试卷元数据" />
          <el-step :title="baseForm.mode === 'random' ? '策略配置' : '题目挑选'" description="配置试卷内容" />
          <el-step title="预览确认" description="最终检查" />
        </el-steps>
      </div>

      <!-- 步骤 1: 基本信息 -->
      <div v-show="activeStep === 0" class="step-content-center">
        <el-card shadow="hover" class="base-info-card">
          <template #header>
            <div class="font-bold text-lg text-gray-800">试卷基本信息</div>
          </template>

          <el-form ref="baseFormRef" :model="baseForm" :rules="baseRules" label-width="100px" size="large">
            <el-form-item label="试卷标题" prop="title">
              <el-input v-model="baseForm.title" placeholder="例如：2025年春季Java期末考试A卷" />
            </el-form-item>

            <el-form-item label="所属课程" prop="courseId">
              <el-select v-model="baseForm.courseId" placeholder="请选择课程" class="w-full" filterable>
                <el-option
                    v-for="item in courseOptions"
                    :key="item.id"
                    :label="item.courseName"
                    :value="item.id"
                />
              </el-select>
            </el-form-item>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="考试时长" prop="duration">
                  <el-input-number v-model="baseForm.duration" :min="10" :step="10" :max="300" class="w-full">
                    <template #suffix>分钟</template>
                  </el-input-number>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="及格分数" prop="passScore">
                  <el-input-number v-model="baseForm.passScore" :min="0" :max="100" class="w-full" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="组卷模式" prop="mode">
              <el-radio-group v-model="baseForm.mode" size="large">
                <el-radio-button value="random">
                  <div class="mode-radio-content">
                    <el-icon><MagicStick /></el-icon> 智能组卷 (随机抽题)
                  </div>
                </el-radio-button>
                <el-radio-button value="manual">
                  <div class="mode-radio-content">
                    <el-icon><Mouse /></el-icon> 手动组卷 (自由挑选)
                  </div>
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <!-- 步骤 2: 策略配置 (智能组卷) -->
      <div v-show="activeStep === 1 && baseForm.mode === 'random'">
        <el-card shadow="never" class="border-0 rounded-xl">
          <template #header>
            <div class="flex-between">
              <span class="font-bold">组卷策略配置</span>
              <div class="text-sm">
                当前总分: <span class="text-xl font-bold text-indigo-600">{{ totalScore }}</span> 分
              </div>
            </div>
          </template>

          <el-alert
              type="info"
              show-icon
              :closable="false"
              class="mb-6"
          >
            <template #title>
              系统将根据下方配置的规则，从题库中随机抽取题目。请确保题库中有足够数量的题目，否则可能组卷失败。
            </template>
          </el-alert>

          <el-table :data="randomRules" border style="width: 100%">
            <el-table-column label="题目类型" width="200">
              <template #default="{ row }">
                <el-select v-model="row.type" placeholder="选择题型">
                  <el-option label="单选题" :value="1" />
                  <el-option label="多选题" :value="2" />
                  <el-option label="判断题" :value="3" />
                  <el-option label="简答题" :value="4" />
                  <el-option label="填空题" :value="5" />
                </el-select>
              </template>
            </el-table-column>

            <el-table-column label="题目数量" width="200">
              <template #default="{ row }">
                <el-input-number v-model="row.count" :min="0" :max="100" />
              </template>
            </el-table-column>

            <el-table-column label="单题分值" width="200">
              <template #default="{ row }">
                <el-input-number v-model="row.score" :min="0" :max="100" :precision="1" />
              </template>
            </el-table-column>

            <el-table-column label="小计" align="center">
              <template #default="{ row }">
                <span class="font-bold text-gray-700">{{ (row.count * row.score).toFixed(1) }} 分</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="100" align="center">
              <template #default="{ $index }">
                <el-button type="danger" icon="Delete" circle @click="removeRule($index)" />
              </template>
            </el-table-column>
          </el-table>

          <div class="mt-4">
            <el-button type="primary" plain icon="Plus" @click="addRule">添加规则</el-button>
          </div>
        </el-card>
      </div>

      <!-- 步骤 2: 题目挑选 (手动组卷) -->
      <!-- 使用 Element Plus 栅格系统替换 Tailwind Grid，防止布局堆叠 -->
      <div v-show="activeStep === 1 && baseForm.mode === 'manual'" class="manual-container">
        <el-row :gutter="24" class="h-full-row">

          <!-- 左侧：题库列表 -->
          <el-col :span="12" class="h-full-col">
            <el-card shadow="never" class="box-card h-full flex-col-card">
              <template #header>
                <div class="font-bold">待选题目库</div>
              </template>

              <div class="filter-bar">
                <el-input
                    v-model="questionQuery.content"
                    placeholder="搜索题目..."
                    prefix-icon="Search"
                    clearable
                    class="flex-1"
                    @input="handleQuestionSearch"
                />
                <el-select v-model="questionQuery.type" placeholder="题型" class="w-32" clearable @change="handleQuestionSearch">
                  <el-option label="单选" :value="1" />
                  <el-option label="多选" :value="2" />
                  <el-option label="判断" :value="3" />
                  <el-option label="简答" :value="4" />
                  <el-option label="填空" :value="5" />
                </el-select>
              </div>

              <div class="list-content custom-scrollbar">
                <div v-for="q in questionList" :key="q.id" class="question-item">
                  <div class="item-header">
                    <div class="item-info">
                      <div class="tags-row">
                        <el-tag size="small" :type="getTypeTag(q.type)">{{ getTypeLabel(q.type) }}</el-tag>
                        <el-tag size="small" type="info" effect="plain">难度: {{ q.difficulty }}</el-tag>
                      </div>
                      <div class="item-text" :title="q.content">{{ q.content }}</div>
                    </div>
                    <el-button
                        type="primary"
                        size="small"
                        icon="Plus"
                        circle
                        :disabled="isQuestionSelected(q.id)"
                        @click="addQuestion(q)"
                    />
                  </div>
                </div>
                <el-empty v-if="questionList.length === 0" description="暂无题目" :image-size="60" />
              </div>

              <!-- 简单分页 -->
              <div class="pagination-footer">
                <el-pagination
                    small
                    layout="prev, pager, next"
                    :total="questionTotal"
                    v-model:current-page="questionQuery.page"
                    @current-change="handleQuestionSearch"
                />
              </div>
            </el-card>
          </el-col>

          <!-- 右侧：已选列表 -->
          <el-col :span="12" class="h-full-col">
            <el-card shadow="never" class="box-card h-full flex-col-card bg-gray">
              <template #header>
                <div class="flex-between">
                  <span class="font-bold text-indigo-700">已选题目 ({{ manualQuestions.length }})</span>
                  <span class="text-sm font-bold">总分: <span class="text-indigo-600 text-lg">{{ totalScore }}</span></span>
                </div>
              </template>

              <div class="list-content custom-scrollbar">
                <draggable
                    v-model="manualQuestions"
                    item-key="id"
                    ghost-class="ghost"
                    class="draggable-list"
                    v-if="manualQuestions.length > 0"
                >
                  <template #item="{ element, index }">
                    <div class="selected-item">
                      <div class="item-index">{{ index + 1 }}</div>
                      <div class="item-body">
                        <div class="tags-row">
                          <el-tag size="small" :type="getTypeTag(element.type)">{{ getTypeLabel(element.type) }}</el-tag>
                        </div>
                        <div class="item-text truncate">{{ element.content }}</div>
                      </div>
                      <div class="item-score">
                        <el-input-number
                            v-model="element.score"
                            size="small"
                            :min="0.5"
                            :step="0.5"
                            controls-position="right"
                            class="w-full"
                        />
                      </div>
                      <el-button type="danger" link icon="Close" @click="removeQuestion(index)" />
                    </div>
                  </template>
                </draggable>

                <div v-else class="empty-placeholder">
                  <el-icon size="40"><DocumentAdd /></el-icon>
                  <p>请从左侧添加题目</p>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 步骤 3: 预览确认 -->
      <div v-show="activeStep === 2" class="step-content-center">
        <div class="preview-card">
          <!-- 试卷头 -->
          <div class="paper-header">
            <h1 class="paper-title">{{ baseForm.title }}</h1>
            <div class="paper-meta">
              <span>课程: {{ getCourseName(baseForm.courseId) }}</span>
              <span>时长: {{ baseForm.duration }}分钟</span>
              <span>总分: {{ totalScore }}分</span>
              <span>及格: {{ baseForm.passScore }}分</span>
            </div>
          </div>

          <!-- 智能组卷预览 -->
          <div v-if="baseForm.mode === 'random'">
            <div class="text-center py-10">
              <el-icon size="60" class="text-indigo-200 mb-4"><MagicStick /></el-icon>
              <h3 class="text-lg font-bold text-gray-700">智能组卷策略已就绪</h3>
              <p class="text-gray-500 max-w-md mx-auto mt-2">
                系统将在提交后，根据以下策略从题库中随机抽取试题生成试卷。请确认策略无误。
              </p>
            </div>

            <div class="max-w-lg mx-auto bg-gray-50 p-6 rounded-lg">
              <h4 class="font-bold mb-4 text-gray-700">题型分布结构</h4>
              <div v-for="(rule, idx) in randomRules" :key="idx" class="strategy-row">
                <span class="text-gray-600">{{ getTypeLabel(rule.type) }}</span>
                <span class="font-medium text-gray-900">
                  {{ rule.count }}题 × {{ rule.score }}分 = {{ (rule.count * rule.score).toFixed(1) }}分
                </span>
              </div>
              <div class="strategy-total">
                <span>合计</span>
                <span class="text-indigo-600">{{ totalScore }} 分</span>
              </div>
            </div>
          </div>

          <!-- 手动组卷预览 -->
          <div v-else class="preview-list">
            <div v-for="(group, type) in groupedQuestions" :key="type" class="group-section">
              <div class="group-title">
                <div class="bar"></div>
                {{ getGroupTitle(Number(type)) }}
                <span class="sub-text">
                  (共 {{ group.length }} 题，{{ group.reduce((sum: number, q: any) => sum + q.score, 0) }} 分)
                </span>
              </div>
              <div v-for="(q, idx) in group" :key="q.id" class="question-preview">
                <div class="flex gap-2">
                  <span class="font-bold text-gray-700">{{ idx + 1 }}.</span>
                  <div class="flex-1">
                    <p class="text-gray-800">{{ q.content }}</p>
                    <div class="question-meta">
                      <span>(分值: {{ q.score }}分)</span>
                      <span class="id-text">ID: {{ q.id }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部按钮栏 -->
      <div class="footer-bar">
        <div class="action-buttons">
          <el-button v-if="activeStep > 0" @click="prevStep" :icon="ArrowLeft">上一步</el-button>
          <el-button v-if="activeStep < 2" type="primary" @click="nextStep">
            下一步 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
          <el-button v-if="activeStep === 2" type="success" :loading="submitting" @click="submitPaper" :icon="Check">
            确认创建试卷
          </el-button>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, Mouse, Delete, Plus, Search, DocumentAdd, Close, ArrowLeft, ArrowRight, Check, InfoFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'
import draggable from 'vuedraggable'

const router = useRouter()

// --- 状态定义 ---
const activeStep = ref(0)
const submitting = ref(false)
const courseOptions = ref<any[]>([])
const baseFormRef = ref()

// 步骤1：基本信息
const baseForm = reactive({
  title: '',
  courseId: undefined as number | undefined,
  duration: 90,
  passScore: 60,
  mode: 'random' // 'random' | 'manual'
})

const baseRules = {
  title: [{ required: true, message: '请输入试卷标题', trigger: 'blur' }],
  courseId: [{ required: true, message: '请选择所属课程', trigger: 'change' }],
  duration: [{ required: true, message: '请输入考试时长', trigger: 'blur' }],
  passScore: [{ required: true, message: '请输入及格分数', trigger: 'blur' }]
}

// 步骤2：智能组卷规则
const randomRules = ref([
  { type: 1, count: 5, score: 2 }, // 默认 单选 5题
  { type: 2, count: 2, score: 5 }  // 默认 多选 2题
])

// 步骤2：手动组卷数据
const manualQuestions = ref<any[]>([])
const questionList = ref<any[]>([])
const questionTotal = ref(0)
const questionQuery = reactive({
  page: 1,
  size: 20,
  content: '',
  type: undefined as number | undefined
})

// --- 计算属性 ---

const totalScore = computed(() => {
  if (baseForm.mode === 'random') {
    return randomRules.value.reduce((sum, item) => sum + (item.count * item.score), 0)
  } else {
    return manualQuestions.value.reduce((sum, item) => sum + Number(item.score || 0), 0)
  }
})

const groupedQuestions = computed(() => {
  if (baseForm.mode !== 'manual') return {}
  const groups: Record<number, any[]> = {}
  manualQuestions.value.forEach(q => {
    if (!groups[q.type]) groups[q.type] = []
    groups[q.type].push(q)
  })
  return groups // 1:[], 2:[]
})

// --- 生命周期 ---
onMounted(() => {
  fetchCourses()
})

// --- 方法 ---

// 1. 初始化数据
const fetchCourses = async () => {
  try {
    const res: any = await request.get('/admin/course/list', { params: { size: 100 } })
    courseOptions.value = res.records
  } catch (error) { console.error(error) }
}

const getCourseName = (id?: number) => {
  const c = courseOptions.value.find(i => i.id === id)
  return c ? c.courseName : '未选课程'
}

// 2. 步骤控制
const nextStep = async () => {
  if (activeStep.value === 0) {
    if (!baseFormRef.value) return
    await baseFormRef.value.validate((valid: boolean) => {
      if (valid) {
        // 如果是手动模式且从未加载过题目，预加载一次
        if (baseForm.mode === 'manual' && questionList.value.length === 0) {
          handleQuestionSearch()
        }
        activeStep.value++
      }
    })
  } else if (activeStep.value === 1) {
    if (totalScore.value <= 0) {
      ElMessage.warning('试卷总分不能为0，请添加题目或规则')
      return
    }
    activeStep.value++
  }
}

const prevStep = () => {
  activeStep.value--
}

// 3. 智能组卷逻辑
const addRule = () => {
  randomRules.value.push({ type: 1, count: 0, score: 0 })
}
const removeRule = (index: number) => {
  randomRules.value.splice(index, 1)
}

// 4. 手动组卷逻辑
const handleQuestionSearch = async () => {
  if (!baseForm.courseId) return
  try {
    const res: any = await request.get('/question/list', {
      params: {
        ...questionQuery,
        courseId: baseForm.courseId
      }
    })
    questionList.value = res.records
    questionTotal.value = res.total
  } catch (error) { console.error(error) }
}

const isQuestionSelected = (id: number) => {
  return manualQuestions.value.some(q => q.id === id)
}

const addQuestion = (question: any) => {
  // 深拷贝并设置默认分值
  const q = { ...question, score: getDefaultScore(question.type) }
  manualQuestions.value.push(q)
}

const removeQuestion = (index: number) => {
  manualQuestions.value.splice(index, 1)
}

const getDefaultScore = (type: number) => {
  // 根据题型给默认分
  switch (type) {
    case 1: return 2   // 单选
    case 2: return 4   // 多选
    case 3: return 2   // 判断
    case 4: return 10  // 简答
    case 5: return 2   // 填空
    default: return 2
  }
}

// 5. 提交创建
const submitPaper = async () => {
  submitting.value = true
  try {
    const url = baseForm.mode === 'random' ? '/paper/random-create' : '/paper/manual-create'

    // 构建Payload
    const payload: any = {
      courseId: baseForm.courseId,
      title: baseForm.title,
      duration: baseForm.duration
    }

    if (baseForm.mode === 'random') {
      payload.rules = randomRules.value
    } else {
      payload.questionList = manualQuestions.value.map(q => ({
        questionId: q.id,
        score: q.score
      }))
    }

    await request.post(url, payload)
    ElMessage.success('试卷创建成功！')

    // 跳转回列表
    router.push('/teacher/paper-list')
  } catch (error: any) {
    // 拦截器已处理弹窗，此处只负责关闭loading
    // 如果需要特殊处理，可以使用 error.message
    console.error('Create paper failed:', error)
  } finally {
    submitting.value = false
  }
}

// --- Utils ---
const getTypeLabel = (type: number) => {
  const map: any = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '简答题', 5: '填空题' }
  return map[type] || '未知'
}

const getTypeTag = (type: number) => {
  const map: any = { 1: '', 2: 'success', 3: 'warning', 4: 'info', 5: 'danger' }
  return map[type] || ''
}

const getGroupTitle = (type: number) => {
  const labels = ['一', '二', '三', '四', '五']
  return `${labels[type - 1] || '其他'}、${getTypeLabel(type)}`
}
</script>

<style scoped lang="scss">
/* 通用样式重置，解决 Tailwind 冲突 */
.w-full { width: 100%; }
.mb-6 { margin-bottom: 24px; }
.mb-8 { margin-bottom: 32px; }
.mt-4 { margin-top: 16px; }
.p-6 { padding: 24px; }
.flex-between { display: flex; justify-content: space-between; align-items: center; }
.text-center { text-align: center; }

/* 步骤容器 */
.step-content-center {
  display: flex;
  justify-content: center;
  width: 100%;
}

.base-info-card {
  width: 100%;
  max-width: 700px;
  border-radius: 12px;
  border: none;
}

.mode-radio-content {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 8px;
}

/* 手动组卷布局 */
.manual-container {
  height: 650px;
}
.h-full-row {
  height: 100%;
}
.h-full-col {
  height: 100%;
}
.flex-col-card {
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  border: none;
  :deep(.el-card__body) {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    padding: 16px;
  }
}
.bg-gray {
  background-color: #f9fafb;
}

/* 筛选栏 */
.filter-bar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
  .flex-1 { flex: 1; }
  .w-32 { width: 128px; }
}

/* 列表内容 */
.list-content {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}

.question-item {
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background-color: #fff;
  transition: all 0.2s;

  &:hover {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    border-color: #c7d2fe;
  }
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.item-info {
  flex: 1;
  overflow: hidden;
}

.tags-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.item-text {
  font-size: 14px;
  color: #374151;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 已选列表项 */
.selected-item {
  padding: 12px;
  background-color: #fff;
  border: 1px solid #e0e7ff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  cursor: move;

  &:hover {
    border-color: #818cf8;
  }
}

.item-index {
  color: #9ca3af;
  font-weight: bold;
  width: 24px;
  text-align: center;
  flex-shrink: 0;
}

.item-body {
  flex: 1;
  overflow: hidden;
  .item-text {
    white-space: nowrap;
    text-overflow: ellipsis;
    display: block;
  }
}

.item-score {
  width: 100px;
  flex-shrink: 0;
}

.empty-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #9ca3af;
  font-size: 14px;
  p { margin-top: 8px; }
}

.pagination-footer {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}

/* 预览卡片 */
.preview-card {
  width: 100%;
  max-width: 800px;
  background-color: #fff;
  padding: 32px;
  border-radius: 12px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  border: 1px solid #f3f4f6;
  min-height: 600px;
}

.paper-header {
  text-align: center;
  border-bottom: 2px solid #1f2937;
  padding-bottom: 24px;
  margin-bottom: 32px;
}

.paper-title {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 12px;
}

.paper-meta {
  display: flex;
  justify-content: center;
  gap: 24px;
  font-size: 14px;
  color: #4b5563;
}

.strategy-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #e5e7eb;
}

.strategy-total {
  display: flex;
  justify-content: space-between;
  padding: 16px 0;
  margin-top: 8px;
  border-top: 2px solid #d1d5db;
  font-weight: 700;
  font-size: 18px;
}

.group-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;

  .bar { width: 4px; height: 20px; background-color: #1f2937; }
  .sub-text { font-size: 14px; font-weight: 400; color: #6b7280; }
}

.question-preview {
  margin-bottom: 16px;
  padding-left: 16px;
}

.question-meta {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
  display: flex;
  justify-content: space-between;

  .id-text { color: transparent; }
  &:hover .id-text { color: #d1d5db; }
}

/* 底部按钮栏 */
.footer-bar {
  margin-top: 32px;
  display: flex;
  justify-content: center;
  position: sticky;
  bottom: 24px;
  z-index: 10;
}

.action-buttons {
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  padding: 16px 32px;
  border-radius: 9999px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  border: 1px solid #e5e7eb;
  display: flex;
  gap: 16px;
}

/* 隐藏数字输入框的箭头，使其更简洁 */
:deep(.el-input-number .el-input__inner) {
  text-align: left;
}

/* 题目列表的自定义滚动条 */
.custom-scrollbar {
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-thumb {
    background-color: #d1d5db;
    border-radius: 3px;
  }
  &::-webkit-scrollbar-track {
    background-color: transparent;
  }
}

/* 拖拽时的占位样式 */
.ghost {
  opacity: 0.5;
  background: #edf2f7;
  border: 1px dashed #4299e1;
}
</style>