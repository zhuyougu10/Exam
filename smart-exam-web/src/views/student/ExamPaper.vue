<template>
  <div class="fixed inset-0 z-[2000] bg-gray-50 flex flex-col h-screen w-screen overflow-hidden">

    <!-- 全屏遮罩：当退出全屏时显示 -->
    <div v-if="!isFullscreen" class="absolute inset-0 z-[3000] bg-slate-900/95 flex flex-col items-center justify-center text-white text-center backdrop-blur-sm">
      <div class="bg-slate-800 p-8 rounded-xl shadow-2xl border border-slate-700 max-w-md w-full mx-4">
        <el-icon class="text-yellow-500 text-6xl mb-4"><Warning /></el-icon>
        <h2 class="text-2xl font-bold mb-2">考试暂停</h2>
        <p class="mb-8 text-gray-300">检测到您退出了全屏模式。为了保证考试公平性，请点击下方按钮恢复全屏继续作答。</p>
        <el-button type="primary" size="large" class="w-full !text-lg !h-12" @click="handleResume">
          恢复全屏并继续
        </el-button>
      </div>
    </div>

    <!-- 顶部 Header -->
    <header class="h-16 bg-white shadow-sm flex items-center justify-between px-6 shrink-0 z-20 border-b border-gray-200">
      <div class="flex items-center gap-4 w-1/3">
        <div class="flex items-center justify-center w-10 h-10 bg-indigo-100 rounded-lg text-indigo-600">
          <el-icon size="22"><Document /></el-icon>
        </div>
        <h1 class="text-lg font-bold text-gray-800 truncate" :title="paperData.title">
          {{ paperData.title || '加载中...' }}
        </h1>
      </div>

      <!-- 中间：倒计时 -->
      <div class="flex items-center justify-center gap-3 w-1/3">
        <div class="flex items-center px-4 py-1.5 bg-red-50 rounded-full border border-red-100 shadow-sm">
          <el-icon class="text-red-500 text-xl mr-2"><Timer /></el-icon>
          <div class="flex flex-col items-start">
            <span class="text-xs text-red-400 font-medium">剩余时间</span>
            <span class="text-xl font-mono font-bold text-red-600 leading-none tracking-wider">
              {{ formattedTime }}
            </span>
          </div>
        </div>
      </div>

      <!-- 右侧：用户信息与交卷 -->
      <div class="flex items-center justify-end gap-4 w-1/3">
        <div class="flex items-center gap-3 bg-gray-50 px-3 py-1.5 rounded-full border border-gray-100">
          <el-avatar :size="32" :src="userStore.userInfo?.avatar" class="bg-indigo-100 text-indigo-600 border border-white shadow-sm">
            {{ userStore.userInfo?.realName?.charAt(0) }}
          </el-avatar>
          <div class="flex flex-col">
            <span class="text-sm font-bold text-gray-700 leading-tight">{{ userStore.userInfo?.realName }}</span>
            <span class="text-[10px] text-gray-500 leading-tight">考生</span>
          </div>
        </div>
        <el-button type="primary" color="#4f46e5" size="large" class="!px-6 shadow-md shadow-indigo-200" @click="confirmSubmit">
          交 卷
        </el-button>
      </div>
    </header>

    <!-- 主体内容 -->
    <div class="flex flex-1 overflow-hidden">
      <!-- 左侧 Sidebar: 答题卡 -->
      <aside class="w-72 bg-white border-r border-gray-200 flex flex-col shrink-0 z-10 shadow-[4px_0_24px_rgba(0,0,0,0.02)]">
        <div class="p-5 border-b border-gray-100">
          <h3 class="font-bold text-gray-800 mb-3 flex items-center">
            <el-icon class="mr-2 text-indigo-500"><Grid /></el-icon> 答题卡
          </h3>
          <div class="flex justify-between text-xs text-gray-500 bg-gray-50 p-2 rounded-lg">
            <span class="flex items-center gap-1.5"><i class="w-2.5 h-2.5 rounded-full bg-orange-50 border border-orange-500"></i> 当前</span>
            <span class="flex items-center gap-1.5"><i class="w-2.5 h-2.5 rounded-full bg-blue-50 border border-blue-500"></i> 已答</span>
            <span class="flex items-center gap-1.5"><i class="w-2.5 h-2.5 rounded-full bg-white border border-gray-300"></i> 未答</span>
          </div>
        </div>

        <el-scrollbar class="flex-1">
          <div class="p-5">
            <div class="grid grid-cols-5 gap-3">
              <button
                  v-for="(q, index) in paperData.questions"
                  :key="q.id"
                  @click="scrollToQuestion(index)"
                  :class="[
                  'h-10 rounded-lg text-sm font-bold transition-all duration-200 flex items-center justify-center border shadow-sm',
                  currentIndex === index
                    ? 'bg-orange-50 border-orange-500 text-orange-600 ring-2 ring-orange-200 ring-offset-1 z-10'
                    : isAnswered(q.id)
                      ? 'bg-blue-50 border-blue-500 text-blue-600'
                      : 'bg-white border-gray-200 text-gray-500 hover:border-gray-400 hover:text-gray-700'
                ]"
              >
                {{ index + 1 }}
              </button>
            </div>
          </div>
        </el-scrollbar>

        <div class="p-4 border-t border-gray-200 bg-gray-50">
          <div class="flex justify-between items-center text-xs text-gray-500">
            <span>总分: <span class="font-bold text-gray-700">{{ paperData.totalScore }}</span></span>
            <span>进度: <span class="font-bold text-blue-600">{{ answeredCount }}</span> / {{ paperData.questions?.length }}</span>
          </div>
          <el-progress
              :percentage="progressPercentage"
              :show-text="false"
              class="mt-2"
              :stroke-width="6"
              color="#4f46e5"
          />
        </div>
      </aside>

      <!-- 中间 Main: 题目内容 -->
      <main class="flex-1 relative bg-[#f8fafc] flex flex-col">
        <el-scrollbar ref="scrollbarRef" wrap-class="p-8 pb-32">
          <div v-if="loading" class="flex flex-col items-center justify-center py-20 space-y-8">
            <el-skeleton class="w-full max-w-4xl" :rows="8" animated />
            <el-skeleton class="w-full max-w-4xl" :rows="8" animated />
          </div>

          <div v-else class="max-w-4xl mx-auto w-full">
            <div
                v-for="(question, index) in paperData.questions"
                :key="question.id"
                :id="'q-' + index"
                class="mb-8 scroll-mt-24 bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden transition-shadow hover:shadow-md"
                :class="{ 'ring-2 ring-orange-400 ring-offset-4': currentIndex === index }"
                @mouseenter="currentIndex = index"
                @click="currentIndex = index"
            >
              <!-- 题目 Header -->
              <div class="bg-gray-50/50 border-b border-gray-100 px-6 py-4 flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <span class="flex items-center justify-center w-8 h-8 rounded-lg bg-gray-800 text-white font-bold text-sm shadow-sm">
                    {{ index + 1 }}
                  </span>
                  <el-tag :type="getQuestionTypeTag(question.type)" effect="dark" class="!border-0">
                    {{ getQuestionTypeName(question.type) }}
                  </el-tag>
                  <span class="text-sm font-medium text-gray-500 bg-gray-100 px-2 py-0.5 rounded">
                    {{ question.score }} 分
                  </span>
                </div>
                <!-- 标记疑难 (仅UI效果) -->
                <el-tooltip content="标记为疑难" placement="top">
                  <el-button link class="!text-gray-400 hover:!text-orange-500">
                    <el-icon size="18"><Flag /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>

              <div class="p-6 md:p-8">
                <!-- 题干 -->
                <div class="mb-6 text-lg text-gray-800 font-medium leading-relaxed whitespace-pre-wrap select-none">{{ question.content }}</div>

                <!-- 图片附件 -->
                <div v-if="question.imageUrl" class="mb-6">
                  <el-image
                      :src="question.imageUrl"
                      :preview-src-list="[question.imageUrl]"
                      class="max-h-80 rounded-lg border border-gray-100 bg-gray-50 p-2"
                      fit="contain"
                  />
                </div>

                <!-- 答题区域 -->
                <div class="mt-4">
                  <!-- 1. 单选 / 3. 判断 -->
                  <el-radio-group
                      v-if="[1, 3].includes(question.type)"
                      v-model="userAnswers[question.id]"
                      class="flex flex-col gap-3 w-full"
                      @change="handleAnswerChange"
                  >
                    <el-radio
                        v-for="opt in parseOptions(question.options)"
                        :key="opt.key"
                        :value="opt.key"
                        class="!ml-0 !mr-0 w-full !h-auto flex items-start p-0 group !border-0"
                    >
                      <div class="flex items-center w-full p-4 rounded-lg border transition-all duration-200 hover:bg-indigo-50 hover:border-indigo-200"
                           :class="userAnswers[question.id] === opt.key ? 'bg-indigo-50 border-indigo-500 ring-1 ring-indigo-500' : 'bg-white border-gray-200'">
                        <span class="font-bold mr-4 w-6 h-6 rounded-full flex items-center justify-center text-xs border transition-colors"
                              :class="userAnswers[question.id] === opt.key ? 'bg-indigo-500 text-white border-indigo-500' : 'bg-gray-100 text-gray-500 border-gray-300'">
                          {{ opt.key }}
                        </span>
                        <span class="whitespace-normal leading-normal text-base text-gray-700 group-hover:text-indigo-900">{{ opt.value }}</span>
                      </div>
                    </el-radio>
                  </el-radio-group>

                  <!-- 2. 多选 -->
                  <el-checkbox-group
                      v-if="question.type === 2"
                      v-model="userAnswers[question.id]"
                      class="flex flex-col gap-3 w-full"
                      @change="handleAnswerChange"
                  >
                    <el-checkbox
                        v-for="opt in parseOptions(question.options)"
                        :key="opt.key"
                        :value="opt.key"
                        class="!ml-0 !mr-0 w-full !h-auto flex items-start p-0 group !border-0"
                    >
                      <div class="flex items-center w-full p-4 rounded-lg border transition-all duration-200 hover:bg-indigo-50 hover:border-indigo-200"
                           :class="isChecked(question.id, opt.key) ? 'bg-indigo-50 border-indigo-500 ring-1 ring-indigo-500' : 'bg-white border-gray-200'">
                        <span class="font-bold mr-4 w-6 h-6 rounded flex items-center justify-center text-xs border transition-colors"
                              :class="isChecked(question.id, opt.key) ? 'bg-indigo-500 text-white border-indigo-500' : 'bg-gray-100 text-gray-500 border-gray-300'">
                          {{ opt.key }}
                        </span>
                        <span class="whitespace-normal leading-normal text-base text-gray-700 group-hover:text-indigo-900">{{ opt.value }}</span>
                      </div>
                    </el-checkbox>
                  </el-checkbox-group>

                  <!-- 4. 简答 / 5. 填空 -->
                  <div v-if="[4, 5].includes(question.type)" class="w-full">
                    <el-input
                        v-model="userAnswers[question.id]"
                        type="textarea"
                        :rows="6"
                        placeholder="在此处输入您的答案..."
                        resize="none"
                        class="!text-base custom-textarea"
                        @input="handleAnswerChange"
                    />
                  </div>
                </div>
              </div>
            </div>

            <!-- 底部提交占位，防止被遮挡 -->
            <div class="h-20 flex items-center justify-center text-gray-400 text-sm">
              - 已经到底啦，检查无误后请点击右上角交卷 -
            </div>
          </div>
        </el-scrollbar>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { Timer, Document, Grid, Flag, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElScrollbar, ElLoading } from 'element-plus'
import request from '@/utils/request'
import { useProctor } from '@/hooks/useProctor'

// ---------------- 状态定义 ----------------
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const scrollbarRef = ref<InstanceType<typeof ElScrollbar>>()

const loading = ref(true)
const currentIndex = ref(0)
const remainingTime = ref(0) // 秒
let timerInterval: any = null

// 试卷数据
const paperData = ref<any>({
  recordId: null,
  paperId: null,
  title: '',
  totalScore: 0,
  questions: []
})

// 用户答案映射
const userAnswers = ref<Record<number, any>>({})

// ---------------- 监考集成 ----------------
const { isFullscreen, enterFullscreen } = useProctor({
  recordId: () => paperData.value.recordId,
  onAutoSubmit: () => submitExam(true),
  saveAnswers: () => saveProgressToLocal()
})

const handleResume = () => {
  enterFullscreen()
}

// ---------------- 核心逻辑 ----------------

// 初始化
const initExam = async () => {
  const publishId = route.query.publishId || route.params.publishId
  if (!publishId) {
    ElMessage.error('参数错误: 缺少 publishId')
    router.replace('/student/exam-list')
    return
  }

  try {
    // 获取试卷数据 (包含断点续考答案)
    const res = await request.post<any>(`/exam/start/${publishId}`)
    paperData.value = res
    remainingTime.value = res.remainingSeconds || 0

    // 初始化答案
    if (res.questions) {
      res.questions.forEach((q: any) => {
        // 如果后端返回了已保存的答案(savedAnswer)
        if (q.savedAnswer) {
          if (q.type === 2) {
            // 多选: 字符串 "A,B" -> 数组 ["A", "B"]
            // 注意: 这里的 value 应该是选项的 Key (如 0, 1, 2)
            userAnswers.value[q.id] = q.savedAnswer.split(',').map(Number)
          } else {
            userAnswers.value[q.id] = q.savedAnswer
          }
        } else if (q.type === 2) {
          userAnswers.value[q.id] = []
        }
      })

      // 尝试加载本地缓存覆盖 (防止网络未及时保存的情况)
      loadProgressFromLocal()
    }

    startTimer()
    loading.value = false
  } catch (error) {
    console.error(error)
    ElMessage.error('试卷加载失败或考试已结束')
    router.replace('/student/exam-list')
  }
}

// 倒计时
const startTimer = () => {
  timerInterval = setInterval(() => {
    if (remainingTime.value > 0) {
      remainingTime.value--
    } else {
      clearInterval(timerInterval)
      ElMessageBox.alert('考试时间到，系统将自动交卷', '提示', {
        confirmButtonText: '确定',
        showClose: false,
        callback: () => submitExam(true)
      })
    }
  }, 1000)
}

const formattedTime = computed(() => {
  const h = Math.floor(remainingTime.value / 3600)
  const m = Math.floor((remainingTime.value % 3600) / 60)
  const s = remainingTime.value % 60
  return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
})

// ---------------- 交互逻辑 ----------------

// 滚动定位
const scrollToQuestion = (index: number) => {
  currentIndex.value = index
  const element = document.getElementById(`q-${index}`)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

// 缓存 Key
const getCacheKey = () => `exam_cache_${userStore.userInfo?.id}_${paperData.value.recordId}`

// 保存到本地
const saveProgressToLocal = () => {
  if (!paperData.value.recordId) return
  localStorage.setItem(getCacheKey(), JSON.stringify(userAnswers.value))
}

// 从本地加载
const loadProgressFromLocal = () => {
  if (!paperData.value.recordId) return
  const cached = localStorage.getItem(getCacheKey())
  if (cached) {
    try {
      const localAnswers = JSON.parse(cached)
      // 合并逻辑: 优先信赖本地 (假设本地是最新的)
      Object.assign(userAnswers.value, localAnswers)
    } catch (e) {
      console.error('Local cache parse error', e)
    }
  }
}

const handleAnswerChange = () => {
  saveProgressToLocal()
}

// 检查多选是否选中
const isChecked = (qid: number, key: string) => {
  const ans = userAnswers.value[qid]
  // 假设选项 key 是 "0", "1" 或 "A", "B"，这里统一转字符串比较比较稳妥
  // 后端返回的 options 可能是 ["A选项", "B选项"]，解析后 key 为 0, 1
  // 这里 key 是字母 (A, B) 还是索引 (0, 1)?
  // 根据 parseOptions 逻辑，key 是字母 A, B...
  // 而 el-checkbox-group v-model 绑定的是 value 数组
  // 我们需要确认 userAnswers 存的是什么。
  // 按照 QuestionExcelListener，后端存的是索引 [0,1]。
  // 前端 parseOptions 生成 key: 'A', value: 'xxx'。
  // 所以我们需要映射。为了简单，我们在 handleAnswerChange 里不做转换，提交时再转换。
  // 或者是让 parseOptions 的 key 直接就是索引?
  // 为了用户体验，显示 A/B，但数据存 0/1 比较符合后端。
  // 这里假设 userAnswers 存的是 'A', 'B' (方便前端)，提交时转索引。

  if (Array.isArray(ans)) {
    return ans.includes(key)
  }
  return false
}

// 辅助: 统计已答
const isAnswered = (qid: number) => {
  const val = userAnswers.value[qid]
  if (Array.isArray(val)) return val.length > 0
  return val !== undefined && val !== null && String(val).trim() !== ''
}

const answeredCount = computed(() => {
  return paperData.value.questions?.filter((q: any) => isAnswered(q.id)).length || 0
})

const progressPercentage = computed(() => {
  if (!paperData.value.questions?.length) return 0
  return Math.round((answeredCount.value / paperData.value.questions.length) * 100)
})

// ---------------- 提交逻辑 ----------------

const confirmSubmit = () => {
  const total = paperData.value.questions.length
  const answered = answeredCount.value

  const msg = answered < total
      ? `您还有 ${total - answered} 道题未作答，确定要交卷吗？`
      : '确定要提交试卷吗？提交后将无法修改。'

  ElMessageBox.confirm(msg, '交卷确认', {
    confirmButtonText: '确定交卷',
    cancelButtonText: '继续检查',
    type: 'warning',
    center: true
  }).then(() => {
    submitExam(false)
  })
}

const submitExam = async (force = false) => {
  const loadingInstance = ElLoading.service({
    lock: true,
    text: force ? '考试结束，正在自动收卷...' : '正在加密提交试卷...',
    background: 'rgba(255, 255, 255, 0.9)',
  })

  try {
    // 构造提交数据
    const answersList = Object.entries(userAnswers.value).map(([qId, val]) => {
      // 需要将前端的答案格式转换为后端需要的格式
      // 假设前端存储：
      // 单选: "A" -> 转为 "0" (假设 A=0)
      // 多选: ["A", "B"] -> 转为 "[0,1]" (JSON字符串)
      // 填空/简答: "文本" -> "文本"

      // 注意：这里的转换逻辑取决于 parseOptions 生成的 key 是什么
      // 下面的 parseOptions 生成的是 A, B, C...
      // 后端 QuestionExcelListener 里存的是 0, 1...
      // 我们需要反向映射。

      const q = paperData.value.questions.find((i: any) => i.id == qId)
      if (!q) return null

      let submitVal = ''
      if (q.type === 1 || q.type === 2) {
        // 客观题：字母转索引
        const mapLetterToIndex = (l: string) => l.charCodeAt(0) - 65
        if (Array.isArray(val)) {
          // 多选
          const indices = val.map(v => mapLetterToIndex(String(v))).sort((a,b) => a-b)
          submitVal = JSON.stringify(indices)
        } else {
          // 单选
          submitVal = String(mapLetterToIndex(String(val)))
        }
      } else if (q.type === 3) {
        // 判断题: 假设前端显示 A:正确 B:错误，或者直接用 1/0
        // 这里 parseOptions 对于判断题通常无需 A/B，直接 选项内容。
        // 简化起见，假设 userAnswers 存的就是 '1' 或 '0'
        submitVal = String(val)
      } else {
        // 主观题
        submitVal = String(val || '')
      }

      return {
        questionId: Number(qId),
        userAnswer: submitVal
      }
    }).filter(Boolean)

    await request.post('/exam/submit', {
      recordId: paperData.value.recordId,
      answers: answersList
    })

    ElMessage.success({
      message: '交卷成功！辛苦了',
      type: 'success',
      duration: 2000
    })

    // 清除本地缓存
    localStorage.removeItem(getCacheKey())

    // 跳转结果页
    router.replace(`/student/exam-result?recordId=${paperData.value.recordId}`)
  } catch (error: any) {
    console.error(error)
    ElMessage.error(error.message || '交卷失败，请联系监考老师')
  } finally {
    loadingInstance.close()
  }
}

// ---------------- 工具方法 ----------------

const parseOptions = (jsonStr: string) => {
  try {
    const arr = JSON.parse(jsonStr)
    // 如果是字符串数组 ["选项1", "选项2"]，转为 [{key: 'A', value: '选项1'}, ...]
    if (Array.isArray(arr)) {
      return arr.map((item, idx) => ({
        key: String.fromCharCode(65 + idx),
        value: item
      }))
    }
    return []
  } catch {
    return []
  }
}

const getQuestionTypeTag = (type: number) => {
  const map: Record<number, string> = { 1: '', 2: 'warning', 3: 'info', 4: 'success', 5: 'danger' }
  return map[type] || ''
}

const getQuestionTypeName = (type: number) => {
  const map: Record<number, string> = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '简答题', 5: '填空题' }
  return map[type] || '未知'
}

onMounted(() => {
  initExam()
})

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
})
</script>

<style scoped>
/* 禁用文本选择，防止复制题目 */
.select-none {
  user-select: none;
  -webkit-user-select: none;
}

/* 允许输入框内的文本选择 */
.custom-textarea :deep(.el-textarea__inner) {
  user-select: text;
  -webkit-user-select: text;
  font-size: 16px;
  line-height: 1.6;
  padding: 12px;
}

/* 隐藏滚动条但保留滚动功能 */
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>