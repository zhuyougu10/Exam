<template>
  <div class="flex h-[calc(100vh-84px)] bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
    <div class="w-80 flex flex-col border-r border-gray-200 bg-gray-50/50">
      <div class="p-4 bg-white border-b border-gray-100 space-y-3">
        <h2 class="text-base font-semibold text-gray-800 flex items-center gap-2">
          <el-icon><List /></el-icon> 阅卷列表
        </h2>
        <el-input
            v-model="searchQuery"
            placeholder="搜索姓名..."
            prefix-icon="Search"
            clearable
            @input="handleSearch"
        />
        <div class="flex gap-2">
          <span
              v-for="st in statusOptions"
              :key="st.value"
              @click="currentStatus = st.value; fetchList()"
              class="px-3 py-1 text-xs rounded-full cursor-pointer transition-colors border select-none"
              :class="currentStatus === st.value
              ? 'bg-indigo-600 text-white border-indigo-600'
              : 'bg-white text-gray-600 border-gray-200 hover:border-indigo-300'"
          >
            {{ st.label }}
          </span>
        </div>
      </div>

      <div class="flex-1 overflow-hidden relative">
        <el-scrollbar v-loading="listLoading">
          <div v-if="studentList.length === 0" class="p-8 text-center text-gray-400 text-sm">
            暂无数据
          </div>
          <div
              v-for="item in studentList"
              :key="item.id"
              @click="handleSelectRecord(item)"
              class="group p-4 cursor-pointer border-b border-gray-100 hover:bg-white transition-all relative"
              :class="activeRecordId === item.id ? 'bg-white shadow-sm' : ''"
          >
            <div
                class="absolute left-0 top-0 bottom-0 w-1 transition-colors"
                :class="activeRecordId === item.id ? 'bg-indigo-600' : 'bg-transparent group-hover:bg-indigo-200'"
            ></div>

            <div class="flex justify-between items-start mb-1">
              <span class="font-medium text-gray-900" :class="{'text-indigo-600': activeRecordId === item.id}">
                {{ item.studentName }}
              </span>
              <el-tag size="small" :type="getStatusType(item.status)" effect="plain" round>
                {{ item.reviewStatusDesc }}
              </el-tag>
            </div>
            <div class="text-xs text-gray-500 flex justify-between items-center mt-2">
              <span>{{ item.deptName || '未知班级' }}</span>
              <span class="font-mono font-bold text-gray-700">
                {{ item.totalScore }} <span class="font-normal text-gray-400">分</span>
              </span>
            </div>
            <div class="text-[10px] text-gray-400 mt-1 truncate">
              {{ item.paperTitle }}
            </div>
          </div>
        </el-scrollbar>
      </div>

      <div class="p-2 text-center text-xs text-gray-400 border-t border-gray-200 bg-white">
        共 {{ total }} 份试卷
      </div>
    </div>

    <div class="flex-1 flex flex-col bg-white min-w-0" v-loading="detailLoading">
      <template v-if="currentRecord">
        <div class="h-16 flex items-center justify-between px-6 border-b border-gray-100 shadow-sm z-10 bg-white/80 backdrop-blur-sm sticky top-0">
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center font-bold text-lg">
              {{ currentRecord.studentName.charAt(0) }}
            </div>
            <div>
              <div class="font-bold text-gray-800 text-lg leading-tight flex items-center gap-2">
                {{ currentRecord.studentName }}
                <span class="text-xs font-normal text-gray-500 px-2 py-0.5 bg-gray-100 rounded">
                  {{ currentRecord.deptName }}
                </span>
              </div>
              <div class="text-xs text-gray-500 mt-0.5">
                交卷时间: {{ formatTime(currentRecord.submitTime) }}
              </div>
            </div>
          </div>

          <div class="flex items-center gap-6">
            <div class="text-right">
              <div class="text-xs text-gray-400">当前得分</div>
              <div class="text-2xl font-mono font-bold text-indigo-600 leading-none">
                {{ currentTotalScore }}
              </div>
            </div>
            <el-button
                type="primary"
                size="large"
                icon="Check"
                :loading="submitting"
                :disabled="modifiedQuestions.size === 0"
                @click="submitAllReviews"
                class="!px-6 !rounded-xl shadow-indigo-200 shadow-lg"
            >
              提交复核 ({{ modifiedQuestions.size }})
            </el-button>
          </div>
        </div>

        <el-scrollbar class="flex-1 bg-gray-50/30 p-6">
          <div class="max-w-4xl mx-auto space-y-6 pb-20">
            <div
                v-for="(q, index) in questions"
                :key="q.detailId"
                class="bg-white rounded-xl border border-gray-200 shadow-sm transition-shadow hover:shadow-md overflow-hidden"
                :class="{'ring-2 ring-indigo-500 ring-offset-2': activeQuestionId === q.questionId}"
            >
              <div class="bg-gray-50 px-5 py-3 border-b border-gray-100 flex justify-between items-center">
                <div class="flex items-center gap-3">
                  <span class="bg-gray-200 text-gray-600 text-xs font-bold px-2 py-1 rounded">
                    Q{{ index + 1 }}
                  </span>
                  <el-tag size="small" :type="getQuestionTypeTag(q.type)">
                    {{ getQuestionTypeName(q.type) }}
                  </el-tag>
                  <span class="text-xs text-gray-400">满分: {{ q.maxScore }}</span>
                </div>
                <div v-if="modifiedQuestions.has(q.detailId)" class="text-xs text-amber-500 font-medium flex items-center gap-1">
                  <el-icon><EditPen /></el-icon> 已修改
                </div>
              </div>

              <div class="p-6">
                <div class="text-gray-800 text-base mb-4 leading-relaxed" v-html="q.content"></div>

                <div v-if="[1,2,3].includes(q.type)" class="space-y-2 mb-6 pl-4 border-l-2 border-gray-100">
                  <div
                      v-for="opt in parseOptions(q.options)"
                      :key="opt.key"
                      class="text-sm flex items-center gap-2"
                      :class="{
                      'text-green-600 font-bold': q.standardAnswer?.includes(opt.key),
                      'text-red-500 line-through': !q.standardAnswer?.includes(opt.key) && q.studentAnswer?.includes(opt.key)
                    }"
                  >
                    <span class="w-6 h-6 rounded-full border flex items-center justify-center text-xs"
                          :class="getOptionClass(opt.key, q.studentAnswer, q.standardAnswer)"
                    >
                      {{ opt.key }}
                    </span>
                    {{ opt.value }}
                  </div>
                </div>

                <div v-if="[4,5].includes(q.type)" class="mb-6">
                  <div class="text-xs text-gray-400 mb-1">学生作答:</div>
                  <div class="bg-gray-50 border border-gray-200 rounded-lg p-3 text-gray-700 min-h-[60px] whitespace-pre-wrap">
                    {{ q.studentAnswer || '（未作答）' }}
                  </div>
                </div>

                <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mt-6 pt-6 border-t border-gray-100 border-dashed">
                  <div class="space-y-3">
                    <div class="flex items-center justify-between">
                      <span class="text-xs font-bold text-purple-600 flex items-center gap-1">
                        <el-icon><Cpu /></el-icon> AI 智能评分
                      </span>
                      <el-button
                          v-if="q.type > 3"
                          size="small"
                          type="primary"
                          link
                          @click="acceptAiScore(q)"
                      >
                        一键采纳
                      </el-button>
                    </div>
                    <el-alert
                        :type="q.score < q.maxScore * 0.6 ? 'warning' : 'info'"
                        :closable="false"
                        class="!items-start"
                    >
                      <template #title>
                        <div class="flex items-baseline gap-2">
                          <span class="text-lg font-bold">{{ q.score }}</span>
                          <span class="text-xs opacity-70">/ {{ q.maxScore }} 分</span>
                        </div>
                      </template>
                      <div class="text-xs mt-1 leading-relaxed opacity-90">
                        {{ q.aiComment || '暂无评语' }}
                      </div>
                    </el-alert>
                  </div>

                  <div class="bg-indigo-50/50 rounded-lg p-4 border border-indigo-100">
                    <div class="text-xs font-bold text-indigo-700 mb-3 flex items-center gap-1">
                      <el-icon><User /></el-icon> 人工复核
                    </div>

                    <div class="flex items-center gap-4 mb-3">
                      <span class="text-sm text-gray-600">得分:</span>
                      <el-input-number
                          v-model="q.manualScore"
                          :min="0"
                          :max="q.maxScore"
                          :precision="1"
                          size="small"
                          class="!w-32"
                          @change="markAsModified(q)"
                      />
                    </div>

                    <el-input
                        v-model="q.manualComment"
                        type="textarea"
                        :rows="2"
                        placeholder="输入人工评语（可选）"
                        class="text-sm"
                        @input="markAsModified(q)"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </template>

      <div v-else class="flex-1 flex flex-col items-center justify-center text-gray-300 bg-white">
        <el-icon size="64" class="mb-4"><DocumentChecked /></el-icon>
        <p>请从左侧选择一份试卷开始阅卷</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, List, EditPen, Cpu, User, DocumentChecked, Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import dayjs from 'dayjs'

// --- Interfaces ---
interface ReviewListItem {
  id: number // recordId
  userId: number
  studentName: string
  deptName: string
  paperTitle: string
  totalScore: number
  status: number
  reviewStatusDesc: string
  submitTime: string
}

interface QuestionDetail {
  detailId: number
  questionId: number
  type: number
  content: string
  options: string
  standardAnswer: string
  analysis: string
  maxScore: number
  studentAnswer: string
  score: number // Current score (DB)
  aiComment: string
  isMarked: number
  // Frontend state
  manualScore?: number
  manualComment?: string
}

// --- State ---
const listLoading = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const studentList = ref<ReviewListItem[]>([])
const currentRecord = ref<ReviewListItem | null>(null)
const questions = ref<QuestionDetail[]>([])
const activeRecordId = ref<number | null>(null)
const activeQuestionId = ref<number | null>(null)
const total = ref(0)

// Search & Filter
const searchQuery = ref('')
const currentStatus = ref<number | undefined>(undefined) // undefined=All
const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待复核', value: 2 },
  { label: '已完成', value: 3 },
]

// Track modifications
const modifiedQuestions = reactive(new Set<number>()) // Stores detailId

// --- Computed ---
const currentTotalScore = computed(() => {
  if (!questions.value.length) return 0
  // Calculate based on manualScore if modified, else original score
  return questions.value.reduce((sum, q) => {
    const s = typeof q.manualScore === 'number' ? q.manualScore : q.score
    return sum + Number(s)
  }, 0).toFixed(1)
})

// --- Methods ---

const getStatusType = (status: number) => {
  return status === 3 ? 'success' : 'warning'
}

const getQuestionTypeName = (type: number) => {
  const map: Record<number, string> = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '简答题', 5: '填空题' }
  return map[type] || '未知'
}

const getQuestionTypeTag = (type: number) => {
  return [4, 5].includes(type) ? 'warning' : 'info'
}

const formatTime = (time: string) => {
  return dayjs(time).format('MM-DD HH:mm')
}

const parseOptions = (jsonStr: string) => {
  try {
    return JSON.parse(jsonStr)
  } catch {
    return []
  }
}

const getOptionClass = (key: string, studentAns: string, standardAns: string) => {
  const isSelected = studentAns?.includes(key)
  const isCorrect = standardAns?.includes(key)

  if (isCorrect) return 'bg-green-100 text-green-700 border-green-200'
  if (isSelected && !isCorrect) return 'bg-red-100 text-red-700 border-red-200'
  if (isSelected) return 'bg-indigo-100 text-indigo-700 border-indigo-200'
  return 'bg-gray-50 text-gray-400 border-gray-200'
}

// Fetch List
const fetchList = async () => {
  listLoading.value = true
  try {
    const res: any = await request.get('/review/list', {
      params: {
        page: 1,
        size: 100, // Simple implementation, practically load all for teacher
        studentName: searchQuery.value || undefined,
        status: currentStatus.value
      }
    })
    studentList.value = res.data.records
    total.value = res.data.total
  } finally {
    listLoading.value = false
  }
}

const handleSearch = () => {
  fetchList()
}

// Select Record
const handleSelectRecord = async (item: ReviewListItem) => {
  if (activeRecordId.value === item.id) return

  // Check for unsaved changes? (Optional)
  if (modifiedQuestions.size > 0) {
    // simplified: just warn or auto-discard. Ideally confirm dialog.
    modifiedQuestions.clear()
  }

  activeRecordId.value = item.id
  currentRecord.value = item
  detailLoading.value = true

  try {
    const res: any = await request.get(`/review/detail/${item.id}`)

    // Transform data for UI
    questions.value = res.data.questions.map((q: QuestionDetail) => ({
      ...q,
      manualScore: q.score, // Init manual inputs with DB values
      manualComment: q.aiComment
    }))
  } finally {
    detailLoading.value = false
  }
}

// Edit Logic
const markAsModified = (q: QuestionDetail) => {
  modifiedQuestions.add(q.detailId)
}

const acceptAiScore = (q: QuestionDetail) => {
  q.manualScore = q.score // Assuming backend `score` is what AI gave initially if not modified
  // Usually we might want to store 'originalAiScore' if 'score' was already modified.
  // But per API, detailed.score is current score.
  // We assume teacher sees current score. If they want to accept AI,
  // usually AI comment has the AI's intended score.
  // Let's assume user just wants to confirm the current score is fine or reset.
  // Actually, a better flow: q.manualScore = q.maxScore (if perfect) or just let user type.
  // Since we don't have separate 'aiScore' field in API response (it's mixed in score),
  // we just trigger modification state.
  markAsModified(q)
  ElMessage.success('已应用 AI 建议')
}

// Submit Logic
const submitAllReviews = async () => {
  if (modifiedQuestions.size === 0) return
  if (!activeRecordId.value) return

  submitting.value = true
  try {
    const promises = questions.value
        .filter(q => modifiedQuestions.has(q.detailId))
        .map(q => {
          return request.post('/review/submit', {
            recordId: activeRecordId.value,
            questionId: q.questionId,
            score: q.manualScore,
            comment: q.manualComment
          })
        })

    await Promise.all(promises)

    ElMessage.success('复核提交成功')
    modifiedQuestions.clear()

    // Refresh list to update total score and status
    fetchList()
    // Refresh current detail to ensure consistency
    if (currentRecord.value) handleSelectRecord(currentRecord.value)

  } catch (error) {
    // Global handler handles error
  } finally {
    submitting.value = false
  }
}

// Init
onMounted(() => {
  fetchList()
})
</script>

<style scoped>
/* Tailwind handles most, just minor tweaks */
:deep(.el-input-number .el-input__inner) {
  text-align: left;
}
</style>