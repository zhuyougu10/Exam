<template>
  <div class="app-container p-6 bg-gray-50 min-h-screen">
    <div class="max-w-7xl mx-auto space-y-6">

      <!-- 顶部工具栏 -->
      <el-card shadow="never" class="border-0 rounded-xl">
        <el-row justify="space-between" align="middle" class="gap-4">
          <!-- 左侧筛选 -->
          <div class="flex flex-wrap gap-3 items-center">
            <div class="p-2 bg-indigo-100 rounded-lg text-indigo-600 mr-2">
              <el-icon size="20"><Collection /></el-icon>
            </div>
            <el-select
                v-model="queryParams.courseId"
                placeholder="选择课程"
                class="w-48"
                clearable
                @change="handleSearch"
            >
              <el-option
                  v-for="item in courseOptions"
                  :key="item.id"
                  :label="item.courseName"
                  :value="item.id"
              />
            </el-select>
            <el-input
                v-model="queryParams.content"
                placeholder="搜索题目内容..."
                prefix-icon="Search"
                class="w-60"
                clearable
                @keyup.enter="handleSearch"
                @clear="handleSearch"
            />
            <el-select
                v-model="queryParams.type"
                placeholder="题型"
                class="w-32"
                clearable
                @change="handleSearch"
            >
              <el-option label="单选题" :value="1" />
              <el-option label="多选题" :value="2" />
              <el-option label="判断题" :value="3" />
              <el-option label="简答题" :value="4" />
              <el-option label="填空题" :value="5" />
            </el-select>
          </div>

          <!-- 右侧按钮组 -->
          <div class="flex items-center gap-3">
            <el-button type="primary" color="#6366f1" icon="MagicStick" @click="openAiDialog">
              AI 智能出题
            </el-button>
            <el-button type="success" icon="Plus" @click="openManualDialog()">
              手动新增
            </el-button>
            <el-badge :value="runningTasksCount" :hidden="runningTasksCount === 0" type="primary">
              <el-button icon="Stopwatch" circle @click="drawerVisible = true" />
            </el-badge>
          </div>
        </el-row>
      </el-card>

      <!-- 题目列表区域 -->
      <el-card shadow="never" class="border-0 rounded-xl min-h-[500px]">
        <el-table
            v-loading="loading"
            :data="questionList"
            row-key="id"
            style="width: 100%"
            :header-cell-style="{ background: '#f9fafb', color: '#374151', fontWeight: '600' }"
        >
          <!-- 展开行：显示完整详情 -->
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="p-4 bg-gray-50 rounded-lg mx-4">
                <el-descriptions title="题目详情" :column="1" border>
                  <el-descriptions-item label="完整题干">{{ row.content }}</el-descriptions-item>
                  <el-descriptions-item label="选项" v-if="[1, 2].includes(row.type)">
                    <ul class="list-disc pl-5">
                      <li v-for="(opt, idx) in parseOptions(row.options)" :key="idx" class="text-gray-600">
                        <span class="font-bold mr-2">{{ String.fromCharCode(65 + idx) }}.</span> {{ opt }}
                        <el-tag v-if="isCorrectAnswer(row, idx)" type="success" size="small" class="ml-2">正确答案</el-tag>
                      </li>
                    </ul>
                  </el-descriptions-item>
                  <el-descriptions-item label="参考答案" v-else>
                    {{ formatAnswer(row) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="解析">
                    <span class="text-gray-600">{{ row.analysis || '暂无解析' }}</span>
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="题干" min-width="300">
            <template #default="{ row }">
              <span class="truncate block max-w-md" :title="row.content">{{ row.content }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="type" label="类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getTypeTag(row.type)">{{ getTypeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="难度" width="150" align="center">
            <template #default="{ row }">
              <el-rate v-model="row.difficulty" disabled text-color="#ff9900" />
            </template>
          </el-table-column>

          <el-table-column label="知识点" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="flex gap-1 flex-wrap">
                <el-tag
                    v-for="(tag, idx) in parseTags(row.tags)"
                    :key="idx"
                    size="small"
                    type="info"
                    effect="plain"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link icon="Edit" @click="openManualDialog(row)">编辑</el-button>
              <el-popconfirm title="确定删除该题目吗？" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button type="danger" link icon="Delete">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="flex justify-end mt-4">
          <el-pagination
              v-model:current-page="queryParams.page"
              v-model:page-size="queryParams.size"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="fetchData"
              @current-change="fetchData"
          />
        </div>
      </el-card>
    </div>

    <!-- AI 智能出题弹窗 -->
    <el-dialog v-model="aiDialog.visible" title="AI 智能出题向导" width="500px" destroy-on-close>
      <el-form :model="aiForm" label-position="top">
        <el-form-item label="目标课程">
          <el-select v-model="aiForm.courseId" placeholder="请选择课程" class="w-full">
            <el-option
                v-for="item in courseOptions"
                :key="item.id"
                :label="item.courseName"
                :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="出题主题 / 知识点">
          <el-input v-model="aiForm.topic" placeholder="例如：Vue3 响应式原理、Java 多线程..." />
        </el-form-item>

        <el-form-item label="生成数量">
          <div class="flex items-center gap-4 w-full">
            <el-slider v-model="aiForm.totalCount" :min="1" :max="20" class="flex-1" />
            <span class="text-gray-600 w-12 text-right">{{ aiForm.totalCount }} 题</span>
          </div>
        </el-form-item>

        <el-form-item label="题目难度">
          <el-radio-group v-model="aiForm.difficulty">
            <el-radio-button label="简单" value="简单" />
            <el-radio-button label="中等" value="中等" />
            <el-radio-button label="困难" value="困难" />
          </el-radio-group>
        </el-form-item>

        <el-form-item label="题目类型 (多选则为混合生成)">
          <el-checkbox-group v-model="aiForm.types">
            <el-checkbox :value="1" label="单选" />
            <el-checkbox :value="2" label="多选" />
            <el-checkbox :value="3" label="判断" />
            <el-checkbox :value="4" label="简答" />
            <el-checkbox :value="5" label="填空" />
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="aiDialog.visible = false">取消</el-button>
          <el-button type="primary" :loading="aiDialog.loading" @click="submitAiTask">
            开始生成
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 任务进度抽屉 -->
    <el-drawer v-model="drawerVisible" title="AI 出题任务中心" size="400px">
      <div class="space-y-4">
        <div v-if="tasks.length === 0" class="text-center text-gray-400 py-10">
          暂无任务记录
        </div>
        <el-card v-for="task in tasks" :key="task.id" shadow="hover" class="relative">
          <div class="flex justify-between items-start mb-2">
            <div class="font-bold text-sm text-gray-700 truncate w-48" :title="task.taskName">
              {{ task.taskName }}
            </div>
            <el-tag size="small" :type="getTaskStatusType(task.status)">
              {{ getTaskStatusLabel(task.status) }}
            </el-tag>
          </div>
          <div class="text-xs text-gray-500 mb-2">
            ID: {{ task.id }} | {{ formatTime(task.createTime) }}
          </div>
          <el-progress
              :percentage="calculateProgress(task)"
              :status="getTaskStatusType(task.status) === 'danger' ? 'exception' : (task.status === 2 ? 'success' : '')"
              :stroke-width="8"
          />
          <div v-if="task.errorMsg" class="mt-2 text-xs text-red-500 bg-red-50 p-2 rounded">
            错误: {{ task.errorMsg }}
          </div>
        </el-card>
      </div>
    </el-drawer>

    <!-- 手动新增/编辑弹窗 -->
    <el-dialog
        v-model="manualDialog.visible"
        :title="manualForm.id ? '编辑题目' : '手动录入题目'"
        width="60%"
        destroy-on-close
        :close-on-click-modal="false"
    >
      <el-form ref="manualFormRef" :model="manualForm" :rules="manualRules" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属课程" prop="courseId">
              <el-select v-model="manualForm.courseId" placeholder="选择课程" class="w-full">
                <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="题目类型" prop="type">
              <el-select v-model="manualForm.type" placeholder="选择类型" class="w-full" @change="handleTypeChange">
                <el-option label="单选题" :value="1" />
                <el-option label="多选题" :value="2" />
                <el-option label="判断题" :value="3" />
                <el-option label="简答题" :value="4" />
                <el-option label="填空题" :value="5" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="难度等级">
          <el-rate v-model="manualForm.difficulty" :max="3" :texts="['简单', '中等', '困难']" show-text />
        </el-form-item>

        <el-form-item label="题干内容" prop="content">
          <el-input
              v-model="manualForm.content"
              type="textarea"
              :rows="3"
              placeholder="请输入题目描述..."
          />
        </el-form-item>

        <!-- 选项区域 (修复布局塌陷问题) -->
        <div v-if="[1, 2].includes(manualForm.type)" class="option-container bg-gray-50 p-4 rounded-lg mb-4">
          <div class="option-header flex justify-between items-center mb-2">
            <span class="text-sm font-bold text-gray-600">题目选项</span>
            <el-button type="primary" link size="small" @click="addOption">
              <el-icon><Plus /></el-icon> 添加选项
            </el-button>
          </div>
          <!-- 使用 flex-row 强制横向排列，配合 CSS 兜底 -->
          <div v-for="(opt, index) in manualForm.options" :key="index" class="option-row flex items-center gap-2 mb-2">
            <span class="option-label w-6 text-center font-bold text-gray-500">{{ String.fromCharCode(65 + index) }}</span>
            <el-input v-model="opt.value" placeholder="输入选项内容" class="flex-1" />
            <el-button type="danger" link @click="removeOption(index)" :disabled="manualForm.options.length <= 2">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- 答案设置区域 -->
        <el-form-item label="参考答案" prop="answer">
          <!-- 单选 -->
          <el-radio-group v-if="manualForm.type === 1" v-model="manualForm.answer">
            <el-radio
                v-for="(opt, index) in manualForm.options"
                :key="index"
                :value="index.toString()"
                border
            >
              {{ String.fromCharCode(65 + index) }}
            </el-radio>
          </el-radio-group>

          <!-- 多选 -->
          <el-checkbox-group v-if="manualForm.type === 2" v-model="manualForm.answerArray">
            <el-checkbox
                v-for="(opt, index) in manualForm.options"
                :key="index"
                :value="index.toString()"
                border
            >
              {{ String.fromCharCode(65 + index) }}
            </el-checkbox>
          </el-checkbox-group>

          <!-- 判断 -->
          <el-radio-group v-if="manualForm.type === 3" v-model="manualForm.answer">
            <el-radio value="1" border>正确 (True)</el-radio>
            <el-radio value="0" border>错误 (False)</el-radio>
          </el-radio-group>

          <!-- 简答/填空 -->
          <el-input
              v-if="[4, 5].includes(manualForm.type)"
              v-model="manualForm.answer"
              type="textarea"
              placeholder="请输入参考答案关键词或完整内容"
          />
        </el-form-item>

        <el-form-item label="解析说明">
          <el-input v-model="manualForm.analysis" type="textarea" placeholder="可选：输入题目解析" />
        </el-form-item>

        <el-form-item label="标签">
          <el-select
              v-model="manualForm.tags"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="输入标签后回车 (如: 第一章)"
              class="w-full"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="manualDialog.visible = false">取消</el-button>
          <el-button type="primary" :loading="manualDialog.loading" @click="submitManual">
            保存题目
          </el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Collection, Search, MagicStick, Plus, Stopwatch,
  Delete, Edit
} from '@element-plus/icons-vue'
import request from '@/utils/request'

// --- State ---
const loading = ref(false)
const questionList = ref([])
const courseOptions = ref<any[]>([])
const total = ref(0)
const drawerVisible = ref(false)
const tasks = ref<any[]>([])
let pollTimer: any = null

const queryParams = reactive({
  page: 1,
  size: 10,
  courseId: undefined,
  content: '',
  type: undefined
})

const aiDialog = reactive({
  visible: false,
  loading: false
})

const aiForm = reactive({
  courseId: undefined,
  topic: '',
  totalCount: 5,
  difficulty: '中等',
  types: [1] // 默认选中单选
})

const manualDialog = reactive({
  visible: false,
  loading: false
})

// 手动录入表单
const manualForm = reactive({
  id: undefined,
  courseId: undefined,
  type: 1,
  difficulty: 1,
  content: '',
  options: [{ value: '' }, { value: '' }, { value: '' }, { value: '' }], // 默认4个选项
  answer: '', // 单选/判断/简答 的存储值
  answerArray: [] as string[], // 多选的中间存储值
  analysis: '',
  tags: [] as string[]
})

const manualFormRef = ref()
const manualRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  content: [{ required: true, message: '请输入题干内容', trigger: 'blur' }],
  answer: [{
    validator: (rule: any, value: any, callback: any) => {
      // 多选题校验 answerArray，其他校验 answer
      if (manualForm.type === 2) {
        if (!manualForm.answerArray || manualForm.answerArray.length === 0) callback(new Error('请选择答案'))
        else callback()
      } else {
        if (!value) callback(new Error('请输入或选择答案'))
        else callback()
      }
    },
    trigger: 'change'
  }]
}

// --- Lifecycle ---
onMounted(() => {
  fetchCourses()
  fetchData()
})

onUnmounted(() => {
  stopPoll()
})

// 监听抽屉打开状态，控制轮询
watch(drawerVisible, (val) => {
  if (val) {
    fetchTasks()
    startPoll()
  } else {
    stopPoll()
  }
})

// 计算正在进行的任务数 (用于 Badge)
const runningTasksCount = computed(() => {
  return tasks.value.filter(t => t.status === 0 || t.status === 1).length
})

// --- Methods ---

// 1. 基础数据获取
const fetchCourses = async () => {
  try {
    const res: any = await request.get('/admin/course/list', { params: { size: 100 } })
    courseOptions.value = res.records
  } catch (error) { console.error(error) }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/question/list', { params: queryParams })
    questionList.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

// 2. AI 智能出题逻辑
const openAiDialog = () => {
  aiForm.topic = ''
  aiForm.totalCount = 5
  // 默认选中当前筛选的课程
  if (queryParams.courseId) aiForm.courseId = queryParams.courseId
  aiDialog.visible = true
}

const submitAiTask = async () => {
  if (!aiForm.courseId || !aiForm.topic) {
    ElMessage.warning('请补全课程和主题信息')
    return
  }

  aiDialog.loading = true
  try {
    // 处理题型逻辑: 选中1个传ID，否则传null(混合)
    const typeParam = aiForm.types.length === 1 ? aiForm.types[0] : null

    await request.post('/question/ai-generate', {
      courseId: aiForm.courseId,
      topic: aiForm.topic,
      totalCount: aiForm.totalCount,
      difficulty: aiForm.difficulty,
      type: typeParam
    })

    ElMessage.success('任务已提交，AI 正在生成中...')
    aiDialog.visible = false
    drawerVisible.value = true // 自动打开任务抽屉
    fetchTasks()
  } catch (error) {
    // error
  } finally {
    aiDialog.loading = false
  }
}

// 3. 任务轮询逻辑
const fetchTasks = async () => {
  try {
    const res: any = await request.get('/question/task/list', { params: { size: 20 } })
    tasks.value = res.records
  } catch (error) { console.error(error) }
}

const startPoll = () => {
  if (pollTimer) return
  pollTimer = setInterval(fetchTasks, 3000) // 3秒轮询一次
}

const stopPoll = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const calculateProgress = (task: any) => {
  if (task.totalCount === 0) return 0
  return Math.min(Math.round((task.currentCount / task.totalCount) * 100), 100)
}

// 4. 手动录入逻辑
const openManualDialog = (row?: any) => {
  if (row) {
    // 编辑模式
    manualForm.id = row.id
    manualForm.courseId = row.courseId
    manualForm.type = row.type
    manualForm.content = row.content
    manualForm.difficulty = row.difficulty
    manualForm.analysis = row.analysis
    manualForm.tags = parseTags(row.tags)

    // 解析选项
    const parsedOpts = parseOptions(row.options)
    manualForm.options = parsedOpts.map((v: string) => ({ value: v }))

    // 解析答案
    if (row.type === 2) {
      // 多选答案通常存为 JSON "[0,1]" 或 字符串 "0,1"
      try {
        manualForm.answerArray = JSON.parse(row.answer).map(String)
      } catch (e) {
        manualForm.answerArray = []
      }
    } else {
      manualForm.answer = row.answer
    }
  } else {
    // 新增模式
    manualForm.id = undefined
    manualForm.content = ''
    manualForm.analysis = ''
    manualForm.options = [{ value: '' }, { value: '' }, { value: '' }, { value: '' }]
    manualForm.answer = ''
    manualForm.answerArray = []
    manualForm.tags = []
    if (queryParams.courseId) manualForm.courseId = queryParams.courseId
  }
  manualDialog.visible = true
}

const handleTypeChange = () => {
  // 切换类型时重置答案
  manualForm.answer = ''
  manualForm.answerArray = []
}

const addOption = () => {
  manualForm.options.push({ value: '' })
}

const removeOption = (index: number) => {
  manualForm.options.splice(index, 1)
}

const submitManual = async () => {
  if (!manualFormRef.value) return
  await manualFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      manualDialog.loading = true
      try {
        const payload: any = {
          ...manualForm,
          // 转换选项为纯字符串数组
          options: JSON.stringify(manualForm.options.map(o => o.value)),
          tags: JSON.stringify(manualForm.tags)
        }

        // 处理答案
        if (manualForm.type === 2) {
          // 多选需排序后存 JSON，保证比对一致性
          const sorted = [...manualForm.answerArray].sort()
          payload.answer = JSON.stringify(sorted)
        }

        delete payload.answerArray // 移除中间变量

        if (manualForm.id) {
          await request.put('/question/update', payload)
          ElMessage.success('更新成功')
        } else {
          await request.post('/question/create', payload)
          ElMessage.success('创建成功')
        }

        manualDialog.visible = false
        fetchData()
      } catch (error) {
        // error
      } finally {
        manualDialog.loading = false
      }
    }
  })
}

const handleDelete = async (id: number) => {
  try {
    await request.delete('/question/batch', { data: [id] })
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    // error
  }
}

// --- Utils ---
const getTypeLabel = (type: number) => {
  const map: any = { 1: '单选', 2: '多选', 3: '判断', 4: '简答', 5: '填空' }
  return map[type] || '未知'
}

const getTypeTag = (type: number) => {
  const map: any = { 1: '', 2: 'success', 3: 'warning', 4: 'info', 5: 'danger' }
  return map[type] || ''
}

const getTaskStatusLabel = (status: number) => {
  const map: any = { 0: '等待中', 1: '进行中', 2: '已完成', 3: '失败' }
  return map[status]
}

const getTaskStatusType = (status: number) => {
  const map: any = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[status]
}

const formatTime = (time: string) => {
  return time ? time.replace('T', ' ').substring(0, 16) : ''
}

// 解析后端存储的 JSON 字符串
const parseOptions = (jsonStr: string) => {
  try {
    return JSON.parse(jsonStr) || []
  } catch (e) {
    return []
  }
}

const parseTags = (jsonStr: string) => {
  try {
    return JSON.parse(jsonStr) || []
  } catch (e) {
    return []
  }
}

// 判断选项是否为正确答案
const isCorrectAnswer = (row: any, index: number) => {
  if (row.type === 1) {
    return row.answer === index.toString()
  } else if (row.type === 2) {
    try {
      const answers = JSON.parse(row.answer)
      // 兼容字符串或数字类型的索引比较
      return answers.includes(index) || answers.includes(index.toString())
    } catch (e) { return false }
  }
  return false
}

const formatAnswer = (row: any) => {
  if (row.type === 3) return row.answer === '1' ? '正确' : '错误'
  return row.answer
}
</script>

<style scoped>
/* 强制覆盖，确保 flex 生效 */
.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.option-container {
  background-color: #f9fafb; /* 对应 bg-gray-50 */
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}
.option-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.list-disc {
  list-style-type: disc;
}
</style>