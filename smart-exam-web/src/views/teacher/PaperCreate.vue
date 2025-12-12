<template>
  <div class="app-container p-4 h-[calc(100vh-64px)] flex flex-col bg-gray-50">
    <!-- 顶部步骤条 -->
    <div class="bg-white p-4 rounded-xl shadow-sm mb-4 shrink-0">
      <el-steps :active="activeStep" finish-status="success" align-center simple>
        <el-step title="基本信息" icon="Edit" />
        <el-step :title="baseForm.mode === 'random' ? '策略配置' : '题目挑选'" :icon="baseForm.mode === 'random' ? 'MagicStick' : 'Mouse'" />
        <el-step title="预览确认" icon="View" />
      </el-steps>
    </div>

    <!-- 内容区域容器: Flex 自动填充剩余高度 -->
    <div class="flex-1 min-h-0 overflow-hidden flex flex-col">

      <!-- 步骤 1: 基本信息 -->
      <div v-show="activeStep === 0" class="h-full flex justify-center items-start overflow-y-auto custom-scrollbar">
        <el-card shadow="hover" class="w-full max-w-4xl border-0 rounded-xl mt-4">
          <template #header>
            <div class="font-bold text-lg text-gray-800">试卷基本信息</div>
          </template>

          <el-form ref="baseFormRef" :model="baseForm" :rules="baseRules" label-width="100px" size="large">
            <el-form-item label="试卷标题" prop="title">
              <el-input v-model="baseForm.title" placeholder="例如：2025年春季Java期末考试A卷" maxlength="50" show-word-limit />
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
              <el-radio-group v-model="baseForm.mode" class="w-full">
                <el-row :gutter="20" class="w-full">
                  <el-col :span="12">
                    <div
                        class="mode-card"
                        :class="{ 'active': baseForm.mode === 'random' }"
                        @click="baseForm.mode = 'random'"
                    >
                      <el-icon size="24" class="mb-2"><MagicStick /></el-icon>
                      <div class="font-bold">智能组卷</div>
                      <div class="text-xs text-gray-500 mt-1">配置规则，随机抽题</div>
                      <el-radio value="random" class="hidden-radio" />
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div
                        class="mode-card"
                        :class="{ 'active': baseForm.mode === 'manual' }"
                        @click="baseForm.mode = 'manual'"
                    >
                      <el-icon size="24" class="mb-2"><Mouse /></el-icon>
                      <div class="font-bold">手动组卷</div>
                      <div class="text-xs text-gray-500 mt-1">自由挑选，精准控制</div>
                      <el-radio value="manual" class="hidden-radio" />
                    </div>
                  </el-col>
                </el-row>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <!-- 步骤 2: 策略配置 (智能组卷) -->
      <div v-show="activeStep === 1 && baseForm.mode === 'random'" class="h-full overflow-y-auto custom-scrollbar">
        <el-card shadow="never" class="border-0 rounded-xl h-full flex flex-col">
          <template #header>
            <div class="flex justify-between items-center">
              <div class="flex items-center gap-2">
                <span class="font-bold">组卷策略配置</span>
                <el-tag type="info" size="small">当前总分: {{ totalScore }} 分</el-tag>
              </div>
              <el-button type="primary" plain icon="Plus" size="small" @click="addRule">添加规则</el-button>
            </div>
          </template>

          <el-alert
              type="info"
              show-icon
              :closable="false"
              class="mb-4"
              title="系统将根据下方配置的规则，从题库中随机抽取题目。请确保题库中有足够数量的题目。"
          />

          <el-table :data="randomRules" border style="width: 100%">
            <el-table-column label="题目类型" prop="type">
              <template #default="{ row }">
                <el-select v-model="row.type" placeholder="选择题型" class="w-full">
                  <el-option label="单选题" :value="1" />
                  <el-option label="多选题" :value="2" />
                  <el-option label="判断题" :value="3" />
                  <el-option label="简答题" :value="4" />
                  <el-option label="填空题" :value="5" />
                </el-select>
              </template>
            </el-table-column>

            <el-table-column label="题目数量" prop="count">
              <template #default="{ row }">
                <el-input-number v-model="row.count" :min="1" :max="100" class="w-full" />
              </template>
            </el-table-column>

            <el-table-column label="单题分值" prop="score">
              <template #default="{ row }">
                <el-input-number v-model="row.score" :min="0.5" :max="100" :precision="1" :step="0.5" class="w-full" />
              </template>
            </el-table-column>

            <el-table-column label="小计" align="center">
              <template #default="{ row }">
                <span class="font-bold text-gray-700">{{ (row.count * row.score).toFixed(1) }} 分</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="80" align="center">
              <template #default="{ $index }">
                <el-button type="danger" icon="Delete" circle text @click="removeRule($index)" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <!-- 步骤 2: 题目挑选 (手动组卷) -->
      <div v-show="activeStep === 1 && baseForm.mode === 'manual'" class="h-full flex gap-6 overflow-hidden max-w-[90%] mx-auto w-full">
        <!-- 左侧：题库列表 (自适应高度) -->
        <div class="flex-1 flex flex-col h-full bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          <div class="p-4 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
            <div class="flex items-center gap-2">
              <span class="font-bold text-gray-700 text-lg">待选题目库</span>
              <el-tag type="info" effect="plain" round size="small">共 {{ questionTotal }} 题</el-tag>
            </div>

            <el-pagination
                small
                layout="prev, pager, next"
                :total="questionTotal"
                v-model:current-page="questionQuery.page"
                @current-change="handleQuestionSearch"
                class="!m-0"
            />
          </div>

          <!-- 优化后的搜索栏 -->
          <div class="p-4 border-b border-gray-100 bg-white sticky top-0 z-10">
            <div class="flex gap-3">
              <el-select
                  v-model="questionQuery.type"
                  placeholder="全部题型"
                  class="w-32 shrink-0"
                  clearable
                  @change="handleQuestionSearch"
              >
                <el-option label="单选" :value="1" />
                <el-option label="多选" :value="2" />
                <el-option label="判断" :value="3" />
                <el-option label="简答" :value="4" />
                <el-option label="填空" :value="5" />
              </el-select>

              <el-input
                  v-model="questionQuery.content"
                  placeholder="请输入题目关键词搜索..."
                  prefix-icon="Search"
                  clearable
                  class="flex-1"
                  @input="debouncedSearch"
              >
                <template #append>
                  <el-button :icon="Search" @click="handleQuestionSearch" />
                </template>
              </el-input>
            </div>
          </div>

          <div class="flex-1 overflow-y-auto p-3 custom-scrollbar">
            <div v-for="q in questionList" :key="q.id" class="question-item group">
              <div class="flex justify-between items-start gap-2">
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-1">
                    <el-tag size="small" :type="getTypeTag(q.type)" effect="plain">{{ getTypeLabel(q.type) }}</el-tag>
                    <el-tag size="small" type="info" effect="plain" class="scale-90 origin-left">难度: {{ q.difficulty }}</el-tag>
                  </div>
                  <div class="text-sm text-gray-700 line-clamp-2 leading-relaxed" :title="q.content">{{ q.content }}</div>
                </div>
                <el-button
                    type="primary"
                    size="small"
                    icon="Plus"
                    circle
                    plain
                    class="shrink-0 opacity-0 group-hover:opacity-100 transition-opacity"
                    :disabled="isQuestionSelected(q.id)"
                    @click="addQuestion(q)"
                />
              </div>
            </div>
            <el-empty v-if="questionList.length === 0" description="暂无符合条件的题目" :image-size="60" />
          </div>
        </div>

        <!-- 右侧：已选列表 (自适应高度) -->
        <div class="flex-1 flex flex-col h-full bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          <div class="p-4 border-b border-gray-100 flex justify-between items-center bg-indigo-50">
            <span class="font-bold text-indigo-800">已选题目 ({{ manualQuestions.length }})</span>
            <span class="text-xs font-bold bg-indigo-100 text-indigo-600 px-2 py-1 rounded">总分: {{ totalScore }}</span>
          </div>

          <div class="flex-1 overflow-y-auto p-3 custom-scrollbar bg-gray-50/50">
            <draggable
                v-model="manualQuestions"
                item-key="id"
                ghost-class="ghost"
                class="space-y-2 h-full"
                v-if="manualQuestions.length > 0"
            >
              <template #item="{ element, index }">
                <div class="selected-item group">
                  <div class="flex items-center gap-3">
                    <span class="text-gray-400 font-mono text-xs w-4">{{ index + 1 }}</span>
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 mb-1">
                        <el-tag size="small" :type="getTypeTag(element.type)" class="scale-75 origin-left !m-0">{{ getTypeLabel(element.type) }}</el-tag>
                      </div>
                      <div class="text-xs text-gray-600 truncate">{{ element.content }}</div>
                    </div>
                  </div>
                  <div class="flex items-center gap-2 mt-2 pt-2 border-t border-gray-100">
                    <span class="text-xs text-gray-400">分值:</span>
                    <el-input-number
                        v-model="element.score"
                        size="small"
                        :min="0.5"
                        :step="0.5"
                        controls-position="right"
                        class="!w-20"
                    />
                    <div class="flex-1"></div>
                    <el-button type="danger" link size="small" icon="Close" @click="removeQuestion(index)" />
                  </div>
                </div>
              </template>
            </draggable>

            <div v-else class="h-full flex flex-col justify-center items-center text-gray-400">
              <el-icon size="40" class="mb-2 text-gray-300"><DocumentAdd /></el-icon>
              <p class="text-sm">请从左侧添加题目</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 步骤 3: 预览确认 -->
      <div v-show="activeStep === 2" class="h-full overflow-y-auto custom-scrollbar flex justify-center pb-8">
        <div class="w-full max-w-4xl bg-white rounded-xl shadow-lg border border-gray-200 min-h-full">
          <!-- 试卷头 -->
          <div class="text-center border-b-2 border-gray-100 p-8 bg-gray-50 rounded-t-xl">
            <h1 class="text-3xl font-bold text-gray-900 mb-6">{{ baseForm.title }}</h1>
            <div class="flex justify-center gap-12 text-gray-600 text-sm">
              <div class="flex flex-col items-center">
                <span class="text-gray-400 text-xs mb-1">所属课程</span>
                <span class="font-bold text-gray-800">{{ getCourseName(baseForm.courseId) }}</span>
              </div>
              <div class="w-px h-10 bg-gray-300"></div>
              <div class="flex flex-col items-center">
                <span class="text-gray-400 text-xs mb-1">考试时长</span>
                <span class="font-bold text-gray-800">{{ baseForm.duration }} 分钟</span>
              </div>
              <div class="w-px h-10 bg-gray-300"></div>
              <div class="flex flex-col items-center">
                <span class="text-gray-400 text-xs mb-1">试卷总分</span>
                <span class="font-bold text-indigo-600 text-lg">{{ totalScore }} 分</span>
              </div>
              <div class="w-px h-10 bg-gray-300"></div>
              <div class="flex flex-col items-center">
                <span class="text-gray-400 text-xs mb-1">及格分数</span>
                <span class="font-bold text-gray-800">{{ baseForm.passScore }} 分</span>
              </div>
            </div>
          </div>

          <div class="p-8">
            <!-- 智能组卷预览 -->
            <div v-if="baseForm.mode === 'random'">
              <div class="bg-blue-50 p-8 rounded-lg text-center mb-8 border border-blue-100">
                <div class="inline-block p-4 bg-white rounded-full shadow-sm mb-4">
                  <el-icon size="40" class="text-blue-500"><MagicStick /></el-icon>
                </div>
                <h3 class="font-bold text-blue-800 text-xl mb-2">智能组卷策略已就绪</h3>
                <p class="text-blue-600">系统将在提交后自动从题库中抽取试题生成试卷</p>
              </div>

              <div class="max-w-2xl mx-auto bg-white border border-gray-200 rounded-lg overflow-hidden">
                <div class="px-6 py-4 bg-gray-50 border-b border-gray-200 flex justify-between items-center">
                  <span class="font-bold text-gray-700">题型分布预览</span>
                  <el-tag type="info">共 {{ randomRules.reduce((sum, r) => sum + r.count, 0) }} 题</el-tag>
                </div>
                <div v-for="(rule, idx) in randomRules" :key="idx" class="flex justify-between items-center px-6 py-4 border-b border-gray-100 last:border-0 hover:bg-gray-50 transition-colors">
                  <div class="flex items-center gap-3">
                    <span class="w-8 h-8 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center font-bold text-sm">{{ idx + 1 }}</span>
                    <span class="text-gray-700 font-medium">{{ getTypeLabel(rule.type) }}</span>
                  </div>
                  <div class="flex items-center gap-4">
                    <span class="text-gray-500">{{ rule.count }} 题 × {{ rule.score }} 分</span>
                    <span class="font-bold text-gray-800 w-16 text-right">= {{ (rule.count * rule.score).toFixed(1) }} 分</span>
                  </div>
                </div>
                <div class="px-6 py-4 bg-gray-50 border-t border-gray-200 flex justify-between items-center">
                  <span class="font-bold text-gray-600">合计</span>
                  <span class="font-bold text-indigo-600 text-xl">{{ totalScore }} 分</span>
                </div>
              </div>
            </div>

            <!-- 手动组卷预览 -->
            <div v-else class="space-y-6">
              <template v-for="type in [1, 2, 3, 4, 5]" :key="type">
                <div v-if="groupedQuestions[type] && groupedQuestions[type].length > 0" class="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
                  <!-- 题型大标题 -->
                  <div class="bg-indigo-50/50 px-6 py-4 border-b border-indigo-100 flex justify-between items-center">
                    <div class="flex items-center gap-3">
                      <div class="h-6 w-1.5 bg-indigo-600 rounded-full"></div>
                      <h3 class="text-lg font-bold text-gray-800">{{ getGroupTitle(type) }}</h3>
                    </div>
                    <div class="text-sm">
                      <span class="text-gray-500">共 {{ groupedQuestions[type].length }} 题</span>
                      <el-divider direction="vertical" />
                      <span class="text-gray-500">计 <span class="font-bold text-indigo-600">{{ getGroupScore(groupedQuestions[type]) }}</span> 分</span>
                    </div>
                  </div>

                  <!-- 题目列表 -->
                  <div class="divide-y divide-gray-100">
                    <div v-for="(q, idx) in groupedQuestions[type]" :key="q.id" class="p-6 hover:bg-gray-50 transition-colors group relative">
                      <div class="flex gap-4">
                        <!-- 序号 -->
                        <div class="shrink-0 w-8 text-lg font-bold text-gray-400 font-mono text-right select-none">{{ idx + 1 }}.</div>

                        <!-- 主体 -->
                        <div class="flex-1 space-y-3">
                          <!-- 题干与分值 -->
                          <div class="flex justify-between items-start gap-4">
                            <div class="text-gray-800 text-base leading-relaxed font-medium text-justify">{{ q.content }}</div>
                            <div class="shrink-0 flex flex-col items-end gap-1">
                              <el-tag size="small" type="info" effect="plain" class="font-mono">ID: {{ q.id }}</el-tag>
                              <span class="text-sm font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">{{ q.score }} 分</span>
                            </div>
                          </div>

                          <!-- 选项 (仅选择题) -->
                          <div v-if="[1, 2].includes(q.type) && q.options" class="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-3 mt-2 pl-1">
                            <div v-for="(opt, oIdx) in parseOptions(q.options)" :key="oIdx"
                                 class="flex items-start gap-2 text-sm text-gray-600 group-hover:text-gray-800 transition-colors">
                              <span class="shrink-0 w-6 h-6 rounded-full border border-gray-300 flex items-center justify-center text-xs font-bold text-gray-500 bg-white group-hover:border-indigo-300 group-hover:text-indigo-600 transition-colors mt-0.5">
                                {{ String.fromCharCode(65 + oIdx) }}
                              </span>
                              <span class="leading-relaxed">{{ opt }}</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- 底部按钮栏 (悬浮) -->
    <div class="bg-white/90 backdrop-blur border-t border-gray-200 p-4 -mx-4 -mb-4 mt-4 flex justify-center gap-4 shrink-0 z-10 sticky bottom-0">
      <el-button v-if="activeStep > 0" @click="prevStep" :icon="ArrowLeft" round>上一步</el-button>
      <el-button v-if="activeStep < 2" type="primary" @click="nextStep" round>
        下一步 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
      </el-button>
      <el-button v-if="activeStep === 2" type="success" :loading="submitting" @click="submitPaper" :icon="Check" round>
        确认创建试卷
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, Mouse, Delete, Plus, Search, DocumentAdd, Close, ArrowLeft, ArrowRight, Check, Edit, View } from '@element-plus/icons-vue'
import request from '@/utils/request'
import draggable from 'vuedraggable'
import { useDebounceFn } from '@vueuse/core'

const router = useRouter()

// --- 状态定义 ---
const activeStep = ref(0)
const submitting = ref(false)
const courseOptions = ref<any[]>([])
const baseFormRef = ref()

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
  return groups
})

// --- 生命周期 ---
onMounted(() => {
  fetchCourses()
})

// --- 方法 ---
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

// 核心修复：增强 nextStep 的校验逻辑
const nextStep = async () => {
  // 步骤 0 -> 1: 校验基本信息
  if (activeStep.value === 0) {
    if (!baseFormRef.value) return
    await baseFormRef.value.validate((valid: boolean) => {
      if (valid) {
        // 如果是手动组卷且还没加载题目，先加载一次
        if (baseForm.mode === 'manual' && questionList.value.length === 0) {
          handleQuestionSearch()
        }
        activeStep.value++
      }
    })
  }
  // 步骤 1 -> 2: 校验组卷规则与分数逻辑 (核心修改点)
  else if (activeStep.value === 1) {
    // 1. 校验总分是否有效
    if (totalScore.value <= 0) {
      ElMessage.warning('试卷总分必须大于 0，请添加题目或配置规则')
      return
    }

    // 2. 校验及格分与总分的关系
    // 注意：totalScore 是计算属性，动态计算当前规则/题目的总分
    if (baseForm.passScore > totalScore.value) {
      ElMessage.error(`及格分数 (${baseForm.passScore}分) 不能高于试卷总分 (${totalScore.value}分)`)
      return
    }

    // 3. 智能组卷特定校验
    if (baseForm.mode === 'random') {
      if (randomRules.value.length === 0) {
        ElMessage.warning('请至少添加一条组卷规则')
        return
      }
      // 检查是否有无效规则 (数量或分值 <= 0)
      const invalidRule = randomRules.value.find(r => !r.count || r.count <= 0 || !r.score || r.score <= 0)
      if (invalidRule) {
        ElMessage.warning('所有规则的“题目数量”和“单题分值”都必须大于 0')
        return
      }
    }
    // 4. 手动组卷特定校验
    else {
      if (manualQuestions.value.length === 0) {
        ElMessage.warning('请从左侧题库中至少选择一道题目')
        return
      }
      // 检查是否有分值为 0 的题目
      const zeroScoreQ = manualQuestions.value.find(q => !q.score || q.score <= 0)
      if (zeroScoreQ) {
        ElMessage.warning('所有选中题目的分值必须大于 0')
        return
      }
    }

    // 所有校验通过，进入预览步骤
    activeStep.value++
  }
}

const prevStep = () => activeStep.value--

const addRule = () => randomRules.value.push({ type: 1, count: 1, score: 2 }) // 默认1题2分，避免0值校验
const removeRule = (index: number) => randomRules.value.splice(index, 1)

const handleQuestionSearch = async () => {
  if (!baseForm.courseId) return
  try {
    const res: any = await request.get('/question/list', {
      params: { ...questionQuery, courseId: baseForm.courseId }
    })
    questionList.value = res.records
    questionTotal.value = res.total
  } catch (error) { console.error(error) }
}

const debouncedSearch = useDebounceFn(() => {
  handleQuestionSearch()
}, 300)

const isQuestionSelected = (id: number) => manualQuestions.value.some(q => q.id === id)

const addQuestion = (question: any) => {
  const q = { ...question, score: getDefaultScore(question.type) }
  manualQuestions.value.push(q)
}

const removeQuestion = (index: number) => manualQuestions.value.splice(index, 1)

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

const parseOptions = (jsonStr: string) => {
  try {
    return JSON.parse(jsonStr) || []
  } catch (e) {
    return []
  }
}

const submitPaper = async () => {
  // 最终提交前的双重校验
  if (baseForm.mode === 'random') {
    if (randomRules.value.length === 0) {
      ElMessage.warning('请配置组卷规则')
      return
    }
  } else {
    if (manualQuestions.value.length === 0) {
      ElMessage.warning('请选择至少一道题目')
      return
    }
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
  } catch (error) { console.error(error) }
  finally { submitting.value = false }
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

const getGroupScore = (questions: any[]) => {
  if (!questions) return 0
  return questions.reduce((sum, q) => sum + (Number(q.score) || 0), 0)
}
</script>

<style scoped lang="scss">
/* 模式选择卡片 */
.mode-card {
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  &:hover {
    border-color: #a5b4fc;
    background-color: #f5f7ff;
  }

  &.active {
    border-color: #6366f1;
    background-color: #eef2ff;
    color: #6366f1;

    .text-gray-500 {
      color: #818cf8;
    }
  }

  .hidden-radio {
    position: absolute;
    opacity: 0;
    width: 0;
    height: 0;
  }
}

/* 题目列表样式 */
.question-item {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid transparent;
  transition: all 0.2s;
  margin-bottom: 4px;

  &:hover {
    background-color: #f3f4f6;
    border-color: #e5e7eb;
  }
}

/* 已选题目样式 */
.selected-item {
  background-color: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px;
  transition: all 0.2s;
  cursor: move;

  &:hover {
    border-color: #818cf8;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  }
}

/* 拖拽占位 */
.ghost {
  opacity: 0.5;
  background: #eef2ff;
  border: 1px dashed #6366f1;
}

/* 自定义滚动条 */
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
</style>