<template>
  <div class="app-container p-6 bg-gray-50 min-h-screen">
    <!-- 顶部工具栏 -->
    <el-card shadow="never" class="border-0 rounded-xl mb-4" :body-style="{ padding: '16px' }">
      <div class="flex flex-col lg:flex-row justify-between lg:items-center gap-4">

        <!-- 左侧：筛选表单 -->
        <el-form :inline="true" :model="queryParams" class="flex-1 !m-0 filter-form" @submit.prevent>
          <div class="flex items-center flex-wrap gap-2">
            <!-- 视觉引导标签 -->
            <div class="flex items-center gap-2 mr-3 bg-gray-100 px-3 py-1.5 rounded-md select-none shrink-0">
              <el-icon class="text-gray-500"><Filter /></el-icon>
              <span class="font-bold text-gray-700 text-sm">筛选</span>
            </div>

            <!-- 课程选择 -->
            <el-form-item class="!mb-0 !mr-2">
              <el-select
                  v-model="queryParams.courseId"
                  placeholder="全部课程"
                  clearable
                  filterable
                  style="width: 220px"
                  @change="handleSearch"
              >
                <el-option
                    v-for="item in courseOptions"
                    :key="item.id"
                    :label="item.courseName"
                    :value="item.id"
                />
              </el-select>
            </el-form-item>

            <!-- 题型选择 -->
            <el-form-item class="!mb-0 !mr-2">
              <el-select
                  v-model="queryParams.type"
                  placeholder="全部题型"
                  clearable
                  style="width: 140px"
                  @change="handleSearch"
              >
                <el-option label="单选题" :value="1" />
                <el-option label="多选题" :value="2" />
                <el-option label="判断题" :value="3" />
                <el-option label="简答题" :value="4" />
                <el-option label="填空题" :value="5" />
              </el-select>
            </el-form-item>

            <!-- 关键词搜索 -->
            <el-form-item class="!mb-0 !mr-2">
              <el-input
                  v-model="queryParams.content"
                  placeholder="搜索题干关键词..."
                  prefix-icon="Search"
                  clearable
                  style="width: 240px"
                  @keyup.enter="handleSearch"
                  @clear="handleSearch"
              />
            </el-form-item>
          </div>
        </el-form>

        <!-- 右侧：功能按钮组 -->
        <div class="flex items-center gap-3 shrink-0">
          <el-tooltip content="刷新列表" placement="top">
            <el-button :icon="Refresh" circle @click="fetchData" />
          </el-tooltip>

          <el-divider direction="vertical" class="hidden lg:block !h-6 !mx-1" />

          <el-button type="primary" color="#6366f1" icon="MagicStick" @click="openAiDialog" class="shadow-sm">
            AI 出题
          </el-button>

          <el-button type="success" plain icon="Plus" @click="openManualDialog()">
            手动录入
          </el-button>

          <el-tooltip content="生成任务进度" placement="bottom">
            <div class="relative inline-flex" @click="drawerVisible = true">
              <el-button icon="Timer" circle />
              <!-- 呼吸灯红点，仅当有任务时显示 -->
              <span v-if="runningTaskCount > 0" class="absolute -top-1 -right-1 flex h-3 w-3">
                <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-indigo-400 opacity-75"></span>
                <span class="relative inline-flex rounded-full h-3 w-3 bg-indigo-500"></span>
              </span>
            </div>
          </el-tooltip>
        </div>
      </div>
    </el-card>

    <!-- 题目列表表格 -->
    <el-card shadow="never" class="border-0 rounded-xl">
      <el-table
          v-loading="loading"
          :data="questionList"
          style="width: 100%"
          :header-cell-style="{ background: '#f9fafb', color: '#374151', fontWeight: '600' }"
          row-key="id"
      >
        <!-- 展开行：显示详情 -->
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="p-4 bg-gray-50 rounded-lg mx-4 border border-gray-100">
              <el-descriptions :column="1" border size="small" class="bg-white">
                <el-descriptions-item label="完整题干">
                  <div class="whitespace-pre-wrap leading-relaxed py-1">{{ row.content }}</div>
                </el-descriptions-item>

                <el-descriptions-item label="选项详情" v-if="[1, 2].includes(row.type)">
                  <div class="flex flex-col gap-2 py-1">
                    <div
                        v-for="(opt, idx) in parseOptions(row.options)"
                        :key="idx"
                        class="flex items-center bg-gray-50 px-3 py-2 rounded border border-gray-100 text-sm hover:bg-gray-100 transition-colors"
                    >
                      <span class="text-gray-700 font-medium">{{ opt }}</span>
                    </div>
                  </div>
                </el-descriptions-item>

                <el-descriptions-item label="参考答案">
                  <span v-if="row.type === 3">
                    <el-tag :type="String(row.answer) === '1' ? 'success' : 'danger'">
                      {{ String(row.answer) === '1' ? '正确' : '错误' }}
                    </el-tag>
                  </span>
                  <span v-else class="font-mono text-green-600 font-bold bg-green-50 px-2 py-0.5 rounded border border-green-100">
                    {{ row.answer }}
                  </span>
                </el-descriptions-item>
                <el-descriptions-item label="题目解析">
                  <div class="text-gray-600 leading-relaxed py-1">{{ row.analysis || '暂无解析' }}</div>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="content" label="题干内容" min-width="300">
          <template #default="{ row }">
            <div class="truncate text-gray-700 font-medium cursor-pointer" :title="row.content">
              {{ row.content }}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="type" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getQuestionTypeTag(row.type)" effect="light" round size="small">
              {{ getQuestionTypeName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="difficulty" label="难度" width="140" align="center">
          <template #default="{ row }">
            <el-rate
                v-model="row.difficulty"
                disabled
                text-color="#ff9900"
                size="small"
                :max="3"
                :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
            />
          </template>
        </el-table-column>

        <el-table-column label="标签" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="flex gap-1 flex-wrap">
              <el-tag
                  v-for="(tag, idx) in parseTags(row.tags)"
                  :key="idx"
                  size="small"
                  type="info"
                  effect="plain"
                  class="!border-gray-200"
              >
                {{ tag }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2">
              <el-button type="primary" link size="small" icon="Edit" @click="openManualDialog(row)">编辑</el-button>
              <div class="w-px h-3 bg-gray-200"></div>
              <el-popconfirm title="确定删除该题目吗？" width="200" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button type="danger" link size="small" icon="Delete">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="mt-6 flex justify-end">
        <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.size"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @size-change="fetchData"
            @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- AI 智能出题弹窗 -->
    <el-dialog
        v-model="aiDialogVisible"
        title="AI 智能出题向导"
        width="500px"
        destroy-on-close
        align-center
        class="rounded-xl"
    >
      <el-form ref="aiFormRef" :model="aiForm" :rules="aiRules" label-position="top">
        <el-form-item label="选择课程" prop="courseId">
          <el-select v-model="aiForm.courseId" placeholder="请选择目标课程" class="w-full">
            <el-option
                v-for="item in courseOptions"
                :key="item.id"
                :label="item.courseName"
                :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="出题主题 / 知识点" prop="topic">
          <el-input
              v-model="aiForm.topic"
              placeholder="例如：数据库范式、Java多线程、TCP三次握手"
              prefix-icon="Search"
          />
          <div class="text-xs text-gray-400 mt-1 flex items-center gap-1">
            <el-icon><InfoFilled /></el-icon> AI 将结合知识库资料生成相关题目
          </div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="题目类型" prop="type">
              <el-select v-model="aiForm.type" placeholder="随机混合" class="w-full" clearable>
                <el-option label="单选题" :value="1" />
                <el-option label="多选题" :value="2" />
                <el-option label="判断题" :value="3" />
                <el-option label="简答题" :value="4" />
                <el-option label="填空题" :value="5" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度等级" prop="difficulty">
              <el-select v-model="aiForm.difficulty" class="w-full">
                <el-option label="简单 (1)" value="1" />
                <el-option label="中等 (2)" value="2" />
                <el-option label="困难 (3)" value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="生成数量" prop="totalCount">
          <div class="flex items-center gap-4 bg-gray-50 p-3 rounded-lg border border-gray-100">
            <span class="text-xs text-gray-500">1</span>
            <el-slider v-model="aiForm.totalCount" :min="1" :max="20" class="flex-1" />
            <span class="text-xs text-gray-500">20</span>
            <span class="font-bold text-indigo-600 bg-white px-2 py-1 rounded shadow-sm min-w-[30px] text-center border">
              {{ aiForm.totalCount }}
            </span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-between items-center w-full">
          <span class="text-xs text-gray-400">预计耗时 10-30 秒</span>
          <div>
            <el-button @click="aiDialogVisible = false">取消</el-button>
            <el-button type="primary" color="#6366f1" :loading="aiSubmitLoading" @click="handleAiSubmit">
              <el-icon class="mr-1"><MagicStick /></el-icon> 开始生成
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 手动新增/编辑弹窗 -->
    <el-dialog
        v-model="manualDialogVisible"
        :title="manualForm.id ? '编辑题目' : '手动录入题目'"
        width="650px"
        destroy-on-close
        :close-on-click-modal="false"
        class="rounded-xl manual-dialog"
        top="5vh"
    >
      <el-form ref="manualFormRef" :model="manualForm" :rules="manualRules" label-position="top">
        <div class="bg-gray-50 p-4 rounded-lg mb-4 border border-gray-100">
          <div class="text-sm font-bold text-gray-700 mb-3 flex items-center gap-1">
            <el-icon><Setting /></el-icon> 基础信息
          </div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="所属课程" prop="courseId" class="!mb-0">
                <el-select
                    v-model="manualForm.courseId"
                    placeholder="选择课程"
                    class="w-full"
                    :disabled="!!manualForm.id"
                >
                  <el-option v-for="item in courseOptions" :key="item.id" :label="item.courseName" :value="item.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="题目类型" prop="type" class="!mb-0">
                <el-select
                    v-model="manualForm.type"
                    placeholder="选择题型"
                    class="w-full"
                    @change="handleTypeChange"
                >
                  <el-option label="单选题" :value="1" />
                  <el-option label="多选题" :value="2" />
                  <el-option label="判断题" :value="3" />
                  <el-option label="简答题" :value="4" />
                  <el-option label="填空题" :value="5" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <el-form-item label="题干内容" prop="content">
          <el-input
              v-model="manualForm.content"
              type="textarea"
              :rows="3"
              placeholder="请输入题目内容（如果是填空题，请使用 ____ 表示填空位置）"
              class="!text-base"
          />
        </el-form-item>

        <!-- 3. 动态选项区 (单选/多选) -->
        <div v-if="[1, 2].includes(manualForm.type)" class="mb-6">
          <div class="flex justify-between items-center mb-2">
            <div class="text-sm font-bold text-gray-700">选项设置</div>
            <el-button type="primary" link size="small" @click="addOption" :disabled="manualForm.options.length >= 8">
              <el-icon><Plus /></el-icon> 添加选项
            </el-button>
          </div>

          <div class="space-y-3">
            <div
                v-for="(opt, idx) in manualForm.options"
                :key="idx"
                class="flex items-center gap-3 group"
                style="display: flex; align-items: center; width: 100%; margin-bottom: 12px;"
            >
              <!-- 选项标识 + 正确答案开关 -->
              <div
                  class="flex items-center justify-center rounded-lg border cursor-pointer transition-all shrink-0"
                  :class="isOptionSelected(idx) ? 'bg-green-100 border-green-500 text-green-700 font-bold' : 'bg-white border-gray-200 text-gray-500 hover:border-gray-400'"
                  style="width: 40px; height: 40px; flex-shrink: 0; display: flex; align-items: center; justify-content: center;"
                  @click="toggleOptionAnswer(idx)"
                  title="点击设为正确答案"
              >
                {{ String.fromCharCode(65 + idx) }}
                <el-icon v-if="isOptionSelected(idx)" class="ml-1"><Check /></el-icon>
              </div>

              <!-- 输入框 -->
              <el-input
                  v-model="manualForm.options[idx]"
                  placeholder="请输入选项内容"
                  class="flex-1"
                  style="flex: 1;"
              />

              <!-- 删除按钮 -->
              <el-button
                  type="danger"
                  link
                  icon="Delete"
                  @click="removeOption(idx)"
                  :disabled="manualForm.options.length <= 2"
                  class="shrink-0"
                  style="margin-left: 8px;"
              />
            </div>
          </div>
          <div class="text-xs text-gray-400 mt-2 ml-1">
            <el-icon><InfoFilled /></el-icon> 点击左侧字母即可将其设为正确答案
          </div>
        </div>

        <!-- 4. 参考答案区 (判断/简答/填空) -->
        <div v-if="![1, 2].includes(manualForm.type)" class="mb-6">
          <el-form-item label="参考答案" prop="answer">
            <!-- 判断题：自定义按钮组，修复点击和高亮问题 -->
            <div v-if="manualForm.type === 3" class="flex gap-4 w-full">
              <div
                  class="flex-1 border rounded-lg p-3 cursor-pointer flex items-center justify-center gap-2 transition-colors select-none"
                  :class="String(manualForm.answer) === '1' ? 'border-green-500 bg-green-50 text-green-700 font-bold' : 'border-gray-200 hover:bg-gray-50'"
                  @click="selectAnswer('1')"
              >
                <el-icon><Check /></el-icon> 正确
              </div>
              <div
                  class="flex-1 border rounded-lg p-3 cursor-pointer flex items-center justify-center gap-2 transition-colors select-none"
                  :class="String(manualForm.answer) === '0' ? 'border-red-500 bg-red-50 text-red-700 font-bold' : 'border-gray-200 hover:bg-gray-50'"
                  @click="selectAnswer('0')"
              >
                <el-icon><Close /></el-icon> 错误
              </div>
            </div>

            <!-- 简答/填空：文本域 -->
            <el-input
                v-else
                v-model="manualForm.answer"
                type="textarea"
                :rows="3"
                placeholder="请输入参考答案（填空题多个答案用分号;分隔）"
            />
          </el-form-item>
        </div>

        <div class="mt-4 pt-4 border-t border-gray-100">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="难度等级" prop="difficulty" class="!mb-0">
                <el-rate v-model="manualForm.difficulty" :max="3" :texts="['简单', '中等', '困难']" show-text />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="题目解析" prop="analysis" class="!mb-0">
                <el-input v-model="manualForm.analysis" placeholder="（选填）输入解析，帮助学生理解" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="manualDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleManualSubmit" color="#6366f1">
            保存题目
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 任务进度抽屉 -->
    <el-drawer
        v-model="drawerVisible"
        title="AI 任务生成进度"
        direction="rtl"
        size="400px"
        @open="startPolling"
        @close="stopPolling"
    >
      <div v-if="tasks.length === 0" class="flex flex-col items-center justify-center h-40 text-gray-400">
        <el-icon size="40" class="mb-2"><Document /></el-icon>
        <p>暂无任务记录</p>
      </div>

      <div class="space-y-4">
        <el-card v-for="task in tasks" :key="task.id" shadow="hover" class="border-l-4 rounded-lg" :class="getTaskBorderClass(task.status)">
          <div class="flex justify-between items-start mb-2">
            <div class="flex flex-col">
              <h4 class="font-bold text-gray-700 truncate w-48 text-sm" :title="task.taskName">{{ task.taskName }}</h4>
              <span class="text-xs text-gray-400 mt-1">{{ formatTime(task.createTime) }}</span>
            </div>
            <el-tag size="small" :type="getTaskStatusType(task.status)">{{ getTaskStatusText(task.status) }}</el-tag>
          </div>

          <div class="flex items-center gap-2 mt-3">
            <el-progress
                :percentage="calculateProgress(task)"
                :status="getTaskStatusType(task.status) === 'danger' ? 'exception' : (task.status === 2 ? 'success' : '')"
                class="flex-1"
                :stroke-width="8"
            />
            <span class="text-xs text-gray-500 font-mono">
              {{ task.currentCount }}/{{ task.totalCount }}
            </span>
          </div>

          <div v-if="task.errorMsg" class="mt-3 text-xs text-red-500 bg-red-50 p-2 rounded border border-red-100 flex items-start gap-1">
            <el-icon class="mt-0.5"><Warning /></el-icon>
            <span>{{ task.errorMsg }}</span>
          </div>
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, Plus, MagicStick, Refresh, Timer, Filter,
  Edit, Delete, Document, List, InfoFilled, Warning,
  Setting, Check, Close
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

// --- 状态 ---
const loading = ref(false)
const aiDialogVisible = ref(false)
const manualDialogVisible = ref(false)
const drawerVisible = ref(false)
const aiSubmitLoading = ref(false)
const submitLoading = ref(false)

const courseOptions = ref<any[]>([])
const questionList = ref<any[]>([])
const tasks = ref<any[]>([])
const total = ref(0)
const pollingTimer = ref<any>(null)

// --- 查询参数 ---
const queryParams = reactive({
  page: 1,
  size: 10,
  courseId: undefined as number | undefined,
  content: '',
  type: undefined as number | undefined
})

// --- AI 表单 ---
const aiFormRef = ref()
const aiForm = reactive({
  courseId: undefined as number | undefined,
  topic: '',
  totalCount: 5,
  difficulty: '2',
  type: undefined as number | undefined
})
const aiRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  topic: [{ required: true, message: '请输入知识点', trigger: 'blur' }],
  totalCount: [{ required: true, message: '请设置数量', trigger: 'change' }]
}

// --- 手动表单 ---
const manualFormRef = ref()
const manualForm = reactive({
  id: undefined,
  courseId: undefined,
  type: 1,
  content: '',
  options: ['', '', '', ''],
  answer: '', // 单选/判断/简答用
  answerArr: [] as string[], // 多选用
  difficulty: 2,
  analysis: ''
})
const manualRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  content: [{ required: true, message: '请输入题干', trigger: 'blur' }],
  answer: [{ required: true, message: '请设置答案', trigger: 'change' }]
}

// --- 生命周期 ---
onMounted(async () => {
  await fetchCourses()
  fetchData()
  fetchTasks() // 初始化加载一次任务
})

onUnmounted(() => {
  stopPolling()
})

// --- API 方法 ---

const fetchCourses = async () => {
  try {
    const res: any = await request.get('/admin/course/list', { params: { size: 100 } })
    courseOptions.value = res.records
    // 默认选中第一个
    if (courseOptions.value.length > 0 && !queryParams.courseId) {
      queryParams.courseId = courseOptions.value[0].id
    }
  } catch (error) {
    console.error(error)
  }
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

const fetchTasks = async () => {
  try {
    const res: any = await request.get('/question/task/list', { params: { size: 20 } })
    tasks.value = res.records
  } catch (error) {
    console.error(error)
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

const handleDelete = async (id: number) => {
  try {
    await request.delete('/question/batch', { data: [id] })
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    // error handled
  }
}

// --- AI 逻辑 ---

const openAiDialog = () => {
  aiForm.courseId = queryParams.courseId // 默认带入当前筛选
  aiForm.topic = ''
  aiForm.totalCount = 5
  aiDialogVisible.value = true
}

const handleAiSubmit = async () => {
  await aiFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      aiSubmitLoading.value = true
      try {
        await request.post('/question/ai-generate', aiForm)
        ElMessage.success('任务已启动，正在后台生成...')
        aiDialogVisible.value = false
        drawerVisible.value = true // 自动打开抽屉
        fetchTasks() // 立即刷新任务列表
      } catch (error) {
        // error handled
      } finally {
        aiSubmitLoading.value = false
      }
    }
  })
}

// --- 轮询逻辑 ---
const runningTaskCount = computed(() => tasks.value.filter(t => t.status === 1).length)

const startPolling = () => {
  fetchTasks() // 打开时先请求一次
  if (!pollingTimer.value) {
    pollingTimer.value = setInterval(() => {
      fetchTasks()
    }, 3000)
  }
}

const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
}

// --- 手动录入逻辑 ---

const openManualDialog = (row?: any) => {
  if (row) {
    // 编辑
    Object.assign(manualForm, row)
    // 处理特殊字段
    if (row.type === 2) {
      manualForm.answerArr = row.answer ? row.answer.split(',') : []
    }
    // 选项处理: 数据库存的是 JSON 字符串，需转回数组
    try {
      manualForm.options = JSON.parse(row.options)
    } catch {
      manualForm.options = []
    }
  } else {
    // 新增
    manualForm.id = undefined
    manualForm.courseId = queryParams.courseId
    manualForm.type = 1
    manualForm.content = ''
    manualForm.options = ['', '', '', '']
    manualForm.answer = ''
    manualForm.answerArr = []
    manualForm.difficulty = 2
    manualForm.analysis = ''
  }
  manualDialogVisible.value = true
}

const handleTypeChange = () => {
  manualForm.answer = ''
  manualForm.answerArr = []
  if ([3, 4, 5].includes(manualForm.type)) {
    manualForm.options = []
  } else if (manualForm.options.length === 0) {
    manualForm.options = ['', '', '', '']
  }
}

const addOption = () => {
  manualForm.options.push('')
}

const removeOption = (idx: number) => {
  manualForm.options.splice(idx, 1)
  const char = String.fromCharCode(65 + idx)
  if (manualForm.type === 1 && manualForm.answer === char) manualForm.answer = ''
  if (manualForm.type === 2 && manualForm.answerArr.includes(char)) {
    manualForm.answerArr = manualForm.answerArr.filter(c => c !== char)
  }
}

// 新增：判断选项是否为答案
const isOptionSelected = (idx: number) => {
  const char = String.fromCharCode(65 + idx)
  if (manualForm.type === 1) return manualForm.answer === char
  if (manualForm.type === 2) return manualForm.answerArr.includes(char)
  return false
}

// 新增：切换选项答案状态
const toggleOptionAnswer = (idx: number) => {
  const char = String.fromCharCode(65 + idx)
  if (manualForm.type === 1) {
    manualForm.answer = char
  } else if (manualForm.type === 2) {
    const i = manualForm.answerArr.indexOf(char)
    if (i > -1) {
      manualForm.answerArr.splice(i, 1)
    } else {
      manualForm.answerArr.push(char)
    }
  }
}

// 新增：选择判断题答案
const selectAnswer = (val: string) => {
  manualForm.answer = val
  // 清除校验状态
  if (manualFormRef.value) {
    manualFormRef.value.clearValidate('answer')
  }
}

const handleManualSubmit = async () => {
  await manualFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      const payload = { ...manualForm }

      if ([1, 2].includes(payload.type)) {
        // 选项是纯内容，展示时加前缀，提交时不加，保持原样
        const formattedOptions = payload.options.map((opt, i) => `${opt}`)
        payload.options = JSON.stringify(formattedOptions)
      } else {
        payload.options = '[]'
      }

      if (payload.type === 2) {
        payload.answer = payload.answerArr.sort().join(',')
      }

      submitLoading.value = true
      try {
        if (payload.id) {
          await request.put('/question/update', payload)
          ElMessage.success('更新成功')
        } else {
          await request.post('/question/create', payload)
          ElMessage.success('创建成功')
        }
        manualDialogVisible.value = false
        fetchData()
      } catch (error) {
        // error
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// --- 辅助显示 ---

const parseOptions = (optionsStr: string) => {
  try {
    return JSON.parse(optionsStr)
  } catch {
    return []
  }
}

const parseTags = (tagsStr: string) => {
  try {
    const arr = JSON.parse(tagsStr)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

const getQuestionTypeName = (type: number) => {
  const map: any = { 1: '单选', 2: '多选', 3: '判断', 4: '简答', 5: '填空' }
  return map[type] || '未知'
}

const getQuestionTypeTag = (type: number) => {
  const map: any = { 1: '', 2: 'warning', 3: 'danger', 4: 'info', 5: 'success' }
  return map[type] || ''
}

const getTaskStatusText = (status: number) => {
  const map: any = { 0: '等待中', 1: '生成中', 2: '完成', 3: '失败' }
  return map[status] || '未知'
}
const getTaskStatusType = (status: number) => {
  const map: any = { 0: 'info', 1: 'primary', 2: 'success', 3: 'danger' }
  return map[status] || 'info'
}
const getTaskBorderClass = (status: number) => {
  const map: any = { 0: 'border-gray-300', 1: 'border-blue-500', 2: 'border-green-500', 3: 'border-red-500' }
  return map[status] || ''
}
const calculateProgress = (task: any) => {
  if (task.status === 2) return 100
  if (task.totalCount === 0) return 0
  return Math.floor((task.currentCount / task.totalCount) * 100)
}
const formatTime = (time: string) => {
  return time ? time.replace('T', ' ').substring(0, 16) : ''
}
</script>

<style scoped lang="scss">
/* 隐藏横向滚动条但保留滚动功能 */
.hide-scrollbar {
  -ms-overflow-style: none;  /* IE and Edge */
  scrollbar-width: none;  /* Firefox */
}
.hide-scrollbar::-webkit-scrollbar {
  display: none;
}

:deep(.el-drawer__body) {
  padding-top: 10px;
}

/* 手动录入弹窗微调 */
.manual-dialog {
  :deep(.el-dialog__body) {
    padding-top: 10px;
    padding-bottom: 20px;
  }
}
</style>