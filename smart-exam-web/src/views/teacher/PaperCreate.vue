<template>
  <div class="page-container">
    <div class="content-wrapper">

      <!-- 顶部步骤条 -->
      <div class="step-card">
        <el-steps :active="activeStep" finish-status="success" align-center class="custom-steps">
          <el-step title="基本信息" description="设置试卷元数据" />
          <el-step :title="baseForm.mode === 'random' ? '策略配置' : '题目挑选'" description="配置试卷内容" />
          <el-step title="预览确认" description="最终检查" />
        </el-steps>
      </div>

      <!-- 步骤 1: 基本信息 -->
      <div v-show="activeStep === 0" class="step-center">
        <el-card shadow="hover" class="base-info-card">
          <template #header>
            <div class="card-header-title">
              <el-icon class="header-icon"><DocumentCopy /></el-icon>
              试卷基本信息
            </div>
          </template>

          <el-form ref="baseFormRef" :model="baseForm" :rules="baseRules" label-width="100px" size="large" class="base-form">
            <el-form-item label="试卷标题" prop="title">
              <el-input v-model="baseForm.title" placeholder="例如：2025年春季Java期末考试A卷" />
            </el-form-item>

            <el-form-item label="所属课程" prop="courseId">
              <el-select v-model="baseForm.courseId" placeholder="请选择课程" class="full-width" filterable>
                <el-option
                    v-for="item in courseOptions"
                    :key="item.id"
                    :label="item.courseName"
                    :value="item.id"
                />
              </el-select>
            </el-form-item>

            <div class="form-grid">
              <el-form-item label="考试时长" prop="duration">
                <el-input-number v-model="baseForm.duration" :min="10" :step="10" :max="480" class="full-width">
                  <template #suffix>分钟</template>
                </el-input-number>
              </el-form-item>

              <el-form-item label="及格分数" prop="passScore">
                <el-input-number v-model="baseForm.passScore" :min="0" :max="1000" class="full-width">
                  <template #suffix>分</template>
                </el-input-number>
              </el-form-item>
            </div>

            <el-form-item label="组卷模式" prop="mode">
              <el-radio-group v-model="baseForm.mode" size="large" class="mode-radio-group">
                <el-radio-button value="random" class="radio-btn">
                  <div class="radio-content">
                    <el-icon><MagicStick /></el-icon> 智能组卷 (随机抽题)
                  </div>
                </el-radio-button>
                <el-radio-button value="manual" class="radio-btn">
                  <div class="radio-content">
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
        <el-card shadow="never" class="strategy-card">
          <template #header>
            <div class="strategy-header">
              <span class="strategy-title">组卷策略配置</span>
              <div class="score-summary">
                当前总分: <span class="score-number">{{ totalScore }}</span> 分
                <span v-if="baseForm.passScore > totalScore" class="score-warning">
                  (及格分 {{baseForm.passScore}} > 总分!)
                </span>
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
              系统将根据下方配置的规则，从题库中随机抽取题目。请确保题库中有足够数量的题目。
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
                <el-input-number v-model="row.count" :min="0" :max="100" class="full-width" />
              </template>
            </el-table-column>

            <el-table-column label="单题分值" width="200">
              <template #default="{ row }">
                <el-input-number v-model="row.score" :min="0" :max="100" :precision="1" class="full-width" />
              </template>
            </el-table-column>

            <el-table-column label="小计" align="center">
              <template #default="{ row }">
                <span class="subtotal-text">{{ (row.count * row.score).toFixed(1) }} 分</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="100" align="center">
              <template #default="{ $index }">
                <el-button type="danger" icon="Delete" circle @click="removeRule($index)" />
              </template>
            </el-table-column>
          </el-table>

          <div class="add-rule-btn-container">
            <el-button type="primary" plain icon="Plus" class="add-rule-btn" @click="addRule">添加规则</el-button>
          </div>
        </el-card>
      </div>

      <!-- 步骤 2: 题目挑选 (手动组卷) -->
      <div v-show="activeStep === 1 && baseForm.mode === 'manual'" class="manual-layout">
        <div class="manual-grid">

          <!-- 左侧：题库列表 -->
          <el-card shadow="never" class="manual-card left-card">
            <template #header>
              <div class="manual-card-header">待选题目库</div>
            </template>

            <!-- 筛选栏 -->
            <div class="manual-filter">
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

            <!-- 列表内容 -->
            <div class="list-container custom-scrollbar">
              <div v-for="q in questionList" :key="q.id" class="question-list-item group">
                <div class="question-content">
                  <div class="question-main">
                    <div class="question-tags">
                      <el-tag size="small" :type="getTypeTag(q.type)">{{ getTypeLabel(q.type) }}</el-tag>
                      <el-tag size="small" type="info" effect="plain">难度: {{ q.difficulty }}</el-tag>
                    </div>
                    <div class="question-text" :title="q.content">{{ q.content }}</div>
                  </div>
                  <el-button
                      type="primary"
                      size="small"
                      icon="Plus"
                      circle
                      class="add-btn"
                      :disabled="isQuestionSelected(q.id)"
                      @click="addQuestion(q)"
                  />
                </div>
              </div>
              <el-empty v-if="questionList.length === 0" description="暂无题目" :image-size="60" />
            </div>

            <!-- 底部页码 -->
            <div class="manual-pagination">
              <el-pagination
                  small
                  layout="prev, pager, next"
                  :total="questionTotal"
                  v-model:current-page="questionQuery.page"
                  @current-change="handleQuestionSearch"
              />
            </div>
          </el-card>

          <!-- 右侧：已选列表 -->
          <el-card shadow="never" class="manual-card right-card">
            <template #header>
              <div class="manual-card-header-flex">
                <span class="selected-title">已选题目 ({{ manualQuestions.length }})</span>
                <div class="selected-summary">
                  <span class="summary-text">总分: <span class="summary-score">{{ totalScore }}</span></span>
                  <span v-if="baseForm.passScore > totalScore" class="summary-warning">
                    及格分{{baseForm.passScore}} > 总分!
                  </span>
                </div>
              </div>
            </template>

            <div class="selected-container custom-scrollbar">
              <draggable
                  v-model="manualQuestions"
                  item-key="id"
                  ghost-class="ghost"
                  class="draggable-area"
                  v-if="manualQuestions.length > 0"
              >
                <template #item="{ element, index }">
                  <div class="selected-item">
                    <div class="item-index">
                      {{ index + 1 }}
                    </div>
                    <div class="item-content-wrapper">
                      <div class="item-tags">
                        <el-tag size="small" :type="getTypeTag(element.type)" effect="light">{{ getTypeLabel(element.type) }}</el-tag>
                      </div>
                      <div class="item-text-truncate">{{ element.content }}</div>
                    </div>
                    <div class="item-score-input">
                      <el-input-number
                          v-model="element.score"
                          size="small"
                          :min="0.5"
                          :step="0.5"
                          controls-position="right"
                          class="full-width"
                      />
                    </div>
                    <el-button type="danger" link icon="Close" @click="removeQuestion(index)" />
                  </div>
                </template>
              </draggable>

              <div v-else class="empty-selected">
                <el-icon size="40" class="empty-icon"><DocumentAdd /></el-icon>
                <p>请从左侧添加题目</p>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 步骤 3: 预览确认 -->
      <div v-show="activeStep === 2" class="step-center">
        <div class="preview-container">
          <!-- 试卷头 -->
          <div class="preview-header">
            <h1 class="preview-title">{{ baseForm.title }}</h1>
            <div class="preview-meta">
              <span>课程: {{ getCourseName(baseForm.courseId) }}</span>
              <span>时长: {{ baseForm.duration }}分钟</span>
              <span class="meta-score">总分: {{ totalScore }}分</span>
              <span>及格: {{ baseForm.passScore }}分</span>
            </div>

            <el-alert
                v-if="baseForm.passScore > totalScore"
                title="参数错误：及格分数高于总分，无法提交！"
                type="error"
                show-icon
                class="mt-4 preview-alert"
                :closable="false"
            />
          </div>

          <!-- 智能组卷预览 -->
          <div v-if="baseForm.mode === 'random'" class="preview-content">
            <div class="ai-preview-placeholder">
              <el-icon size="60" class="ai-icon"><MagicStick /></el-icon>
              <h3 class="ai-title">智能组卷策略已就绪</h3>
              <p class="ai-desc">
                系统将在提交后，根据以下策略从题库中随机抽取试题生成试卷。请确认策略无误。
              </p>
            </div>

            <div class="strategy-summary">
              <h4 class="strategy-summary-title">题型分布结构</h4>
              <div v-for="(rule, idx) in randomRules" :key="idx" class="strategy-summary-row">
                <span class="row-type">{{ getTypeLabel(rule.type) }}</span>
                <span class="row-value">
                  {{ rule.count }}题 × {{ rule.score }}分 = {{ (rule.count * rule.score).toFixed(1) }}分
                </span>
              </div>
              <div class="strategy-summary-total">
                <span>合计</span>
                <span class="total-value">{{ totalScore }} 分</span>
              </div>
            </div>
          </div>

          <!-- 手动组卷预览 -->
          <div v-else class="preview-content scrollable">
            <div v-for="(group, type) in groupedQuestions" :key="type" class="question-group">
              <div class="group-header">
                <div class="group-bar"></div>
                <h3 class="group-title">
                  {{ getGroupTitle(Number(type)) }}
                  <span class="group-meta">
                    (共 {{ group.length }} 题，{{ group.reduce((sum: number, q: any) => sum + q.score, 0) }} 分)
                  </span>
                </h3>
              </div>

              <div v-for="(q, idx) in group" :key="q.id" class="question-preview-item">
                <div class="preview-item-flex">
                  <span class="preview-index">{{ idx + 1 }}.</span>
                  <div class="preview-body">
                    <p class="preview-text">{{ q.content }}</p>
                    <div class="preview-footer">
                      <span>(分值: {{ q.score }}分)</span>
                      <span class="preview-id">ID: {{ q.id }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部按钮栏 -->
      <div class="floating-footer">
        <div class="footer-content">
          <el-button v-if="activeStep > 0" @click="prevStep" :icon="ArrowLeft" round size="large">上一步</el-button>
          <el-button v-if="activeStep < 2" type="primary" @click="nextStep" round size="large">
            下一步 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
          <el-button
              v-if="activeStep === 2"
              type="success"
              :loading="submitting"
              :disabled="baseForm.passScore > totalScore || totalScore <= 0"
              @click="submitPaper"
              :icon="Check"
              round
              size="large"
          >
            确认创建试卷
          </el-button>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
// (JS 逻辑部分保持不变，直接复用原有的即可)
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, Mouse, Delete, Plus, Search, DocumentAdd, Close, ArrowLeft, ArrowRight, Check, InfoFilled, DocumentCopy } from '@element-plus/icons-vue'
import request from '@/utils/request'
import draggable from 'vuedraggable'

const router = useRouter()

const activeStep = ref(0)
const submitting = ref(false)
const courseOptions = ref<any[]>([])
const baseFormRef = ref()

const baseForm = reactive({
  title: '',
  courseId: undefined as number | undefined,
  duration: 90,
  passScore: 60,
  mode: 'random'
})

const baseRules = {
  title: [{ required: true, message: '请输入试卷标题', trigger: 'blur' }],
  courseId: [{ required: true, message: '请选择所属课程', trigger: 'change' }],
  duration: [
    { required: true, message: '请输入考试时长', trigger: 'blur' },
    { type: 'number', min: 1, max: 480, message: '时长范围 1-480 分钟', trigger: 'blur' }
  ],
  passScore: [
    { required: true, message: '请输入及格分数', trigger: 'blur' },
    { type: 'number', min: 0, message: '及格分不能为负数', trigger: 'blur' }
  ]
}

const randomRules = ref([
  { type: 1, count: 5, score: 2 },
  { type: 2, count: 2, score: 5 }
])

const manualQuestions = ref<any[]>([])
const questionList = ref<any[]>([])
const questionTotal = ref(0)
const questionQuery = reactive({
  page: 1,
  size: 20,
  content: '',
  type: undefined as number | undefined
})

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
  return groups
})

onMounted(() => {
  fetchCourses()
})

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

const nextStep = async () => {
  if (activeStep.value === 0) {
    if (!baseFormRef.value) return
    await baseFormRef.value.validate((valid: boolean) => {
      if (valid) {
        if (baseForm.mode === 'manual' && questionList.value.length === 0) {
          handleQuestionSearch()
        }
        activeStep.value++
      }
    })
  }
  else if (activeStep.value === 1) {
    if (totalScore.value <= 0) {
      ElMessage.warning('试卷总分不能为0，请添加题目或规则')
      return
    }

    if (baseForm.passScore > totalScore.value) {
      ElMessage.error(`参数不合理：及格分 (${baseForm.passScore}) 不能高于总分 (${totalScore.value})`)
      return
    }

    activeStep.value++
  }
}

const prevStep = () => {
  activeStep.value--
}

const addRule = () => {
  randomRules.value.push({ type: 1, count: 0, score: 0 })
}
const removeRule = (index: number) => {
  randomRules.value.splice(index, 1)
}

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
  const q = { ...question, score: getDefaultScore(question.type) }
  manualQuestions.value.push(q)
}

const removeQuestion = (index: number) => {
  manualQuestions.value.splice(index, 1)
}

const getDefaultScore = (type: number) => {
  switch (type) {
    case 1: return 2
    case 2: return 4
    case 3: return 2
    case 4: return 10
    case 5: return 2
    default: return 2
  }
}

const submitPaper = async () => {
  if (baseForm.passScore > totalScore.value) {
    ElMessage.error('及格分设置过高，请返回第一步修改或增加题目分值')
    return
  }

  submitting.value = true
  try {
    const url = baseForm.mode === 'random' ? '/paper/random-create' : '/paper/manual-create'

    const payload: any = {
      courseId: baseForm.courseId,
      title: baseForm.title,
      duration: baseForm.duration,
      passScore: baseForm.passScore
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
    router.push('/teacher/paper-list')
  } catch (error: any) {
    console.error('Create paper failed:', error)
  } finally {
    submitting.value = false
  }
}

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

<style scoped>
/* Main Layout */
.page-container {
  padding: 24px;
  background-color: #f9fafb;
  min-height: 100vh;
  box-sizing: border-box;
}

.content-wrapper {
  max-width: 1152px;
  margin: 0 auto;
}

/* Steps */
.step-card {
  margin-bottom: 32px;
  background-color: white;
  padding: 32px;
  border-radius: 12px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  border: 1px solid #f3f4f6;
}

:deep(.el-step__title) {
  font-weight: 500;
}
:deep(.el-step__head.is-success) {
  color: #6366f1;
  border-color: #6366f1;
}
:deep(.el-step__title.is-success) {
  color: #6366f1;
}

/* Step 1: Base Info */
.step-center {
  display: flex;
  justify-content: center;
  width: 100%;
}

.base-info-card {
  width: 100%;
  max-width: 768px;
  border-radius: 12px;
  border: none;
}

.card-header-title {
  font-weight: 700;
  font-size: 18px;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  color: #4f46e5;
}

.base-form {
  padding: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

@media (min-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr 1fr;
  }
}

.full-width {
  width: 100%;
}

.mode-radio-group {
  width: 100%;
  display: flex;
  gap: 16px;
}

.radio-btn {
  flex: 1;
}

:deep(.mode-radio-group .el-radio-button__inner) {
  width: 100%;
  border-radius: 4px !important;
  border: 1px solid #dcdfe6;
}

.radio-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 16px;
}

/* Step 2: Random Strategy */
.strategy-card {
  border-radius: 12px;
  border: none;
}

.strategy-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.strategy-title {
  font-weight: 700;
  font-size: 18px;
  color: #1f2937;
}

.score-summary {
  font-size: 14px;
  background-color: #f3f4f6;
  padding: 8px 16px;
  border-radius: 8px;
}

.score-number {
  font-size: 20px;
  font-weight: 700;
  color: #4f46e5;
}

.score-warning {
  color: #ef4444;
  font-weight: 700;
  margin-left: 8px;
  font-size: 12px;
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .5; }
}

.mb-6 { margin-bottom: 24px; }

.subtotal-text {
  font-weight: 700;
  color: #374151;
}

.add-rule-btn-container {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.add-rule-btn {
  width: 100%;
  border-style: dashed;
}

/* Step 2: Manual Layout */
.manual-layout {
  height: 650px;
}

.manual-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  height: 100%;
}

.manual-card {
  border-radius: 12px;
  border: none;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.left-card {
  background-color: white;
}

.right-card {
  background-color: #f9fafb; /* gray-50/50 approx */
}

:deep(.manual-card .el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px;
}

.manual-card-header {
  font-weight: 700;
  color: #1f2937;
}

.manual-card-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.selected-title {
  font-weight: 700;
  color: #4338ca; /* indigo-700 */
}

.selected-summary {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.summary-text {
  font-size: 14px;
  font-weight: 700;
}

.summary-score {
  color: #4f46e5;
  font-size: 18px;
}

.summary-warning {
  color: #ef4444;
  font-size: 12px;
  font-weight: 700;
  animation: pulse 2s infinite;
}

.manual-filter {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.flex-1 { flex: 1; }
.w-32 { width: 128px; }

.list-container, .selected-container {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}

.draggable-area {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* Question Items */
.question-list-item {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background-color: white;
  transition: all 0.2s;
  margin-bottom: 12px;
}

.question-list-item:hover {
  border-color: #a5b4fc; /* indigo-300 */
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

.question-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.question-main {
  flex: 1;
  min-width: 0;
}

.question-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
}

.question-text {
  font-size: 14px;
  color: #374151;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.add-btn {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.question-list-item:hover .add-btn {
  opacity: 1;
}

/* Selected Items */
.selected-item {
  padding: 12px;
  background-color: white;
  border: 1px solid #e0e7ff;
  border-radius: 8px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: move;
  transition: border-color 0.2s;
}

.selected-item:hover {
  border-color: #a5b4fc;
}

.item-index {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #f3f4f6;
  color: #6b7280;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.item-content-wrapper {
  flex: 1;
  min-width: 0;
}

.item-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
}

.item-text-truncate {
  font-size: 14px;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-score-input {
  width: 96px;
  flex-shrink: 0;
}

.empty-selected {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}

.empty-icon {
  margin-bottom: 8px;
  opacity: 0.5;
}

.manual-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  border-top: 1px solid #f3f4f6;
  padding-top: 12px;
}

/* Preview Step */
.preview-container {
  background-color: white;
  padding: 32px;
  border-radius: 12px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  border: 1px solid #f3f4f6;
  width: 100%;
  max-width: 800px;
  min-height: 600px;
  display: flex;
  flex-direction: column;
}

.preview-header {
  text-align: center;
  border-bottom: 2px solid #1f2937;
  padding-bottom: 24px;
  margin-bottom: 32px;
}

.preview-title {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 16px;
}

.preview-meta {
  display: flex;
  justify-content: center;
  gap: 24px;
  font-size: 14px;
  color: #4b5563;
}

.meta-score {
  font-weight: 700;
  color: #4f46e5;
}

.preview-alert {
  margin-top: 16px;
  max-width: 448px;
  margin-left: auto;
  margin-right: auto;
}

.preview-content {
  flex: 1;
}

.preview-content.scrollable {
  overflow-y: auto;
  padding-right: 16px;
}

.ai-preview-placeholder {
  text-align: center;
  padding: 40px 0;
}

.ai-icon {
  color: #c7d2fe; /* indigo-200 */
  margin-bottom: 16px;
}

.ai-title {
  font-size: 18px;
  font-weight: 700;
  color: #374151;
}

.ai-desc {
  color: #6b7280;
  max-width: 448px;
  margin: 8px auto 0;
}

.strategy-summary {
  max-width: 512px;
  margin: 0 auto;
  background-color: #f9fafb;
  padding: 24px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.strategy-summary-title {
  font-weight: 700;
  margin-bottom: 16px;
  color: #374151;
}

.strategy-summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #e5e7eb;
}

.strategy-summary-row:last-child {
  border-bottom: none;
}

.row-type {
  color: #4b5563;
}

.row-value {
  font-weight: 500;
  color: #111827;
}

.strategy-summary-total {
  display: flex;
  justify-content: space-between;
  padding-top: 16px;
  margin-top: 8px;
  border-top: 2px solid #d1d5db;
  font-size: 18px;
  font-weight: 700;
}

.total-value {
  color: #4f46e5;
}

/* Grouped Preview */
.question-group {
  margin-bottom: 32px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.group-bar {
  width: 4px;
  height: 24px;
  background-color: #1f2937;
}

.group-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.group-meta {
  font-size: 14px;
  font-weight: 400;
  color: #6b7280;
  margin-left: 8px;
}

.question-preview-item {
  padding-left: 16px;
  margin-bottom: 16px;
}

.preview-item-flex {
  display: flex;
  gap: 8px;
}

.preview-index {
  font-weight: 700;
  color: #374151;
  user-select: none;
}

.preview-body {
  flex: 1;
}

.preview-text {
  color: #1f2937;
  margin-bottom: 4px;
}

.preview-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #9ca3af;
}

.preview-id {
  opacity: 0;
  transition: opacity 0.2s;
}

.preview-item-flex:hover .preview-id {
  opacity: 1;
}

/* Floating Footer */
.floating-footer {
  position: fixed;
  bottom: 24px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  z-index: 50;
  pointer-events: none;
}

.footer-content {
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border: 1px solid #e5e7eb;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  border-radius: 9999px;
  padding: 16px 32px;
  display: flex;
  gap: 16px;
  pointer-events: auto;
  transition: transform 0.3s;
}

.footer-content:hover {
  transform: scale(1.05);
}

/* Misc */
.ghost {
  opacity: 0.5;
  background: #eef2ff !important;
  border: 2px dashed #6366f1 !important;
}

.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: #d1d5db;
  border-radius: 3px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background-color: transparent;
}
</style>