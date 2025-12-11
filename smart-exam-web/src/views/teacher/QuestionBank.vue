<template>
  <div class="app-container p-6 bg-gray-50 min-h-screen">
    <div class="max-w-7xl mx-auto space-y-6">

      <!-- 顶部工具栏 -->
      <el-card shadow="never" class="border-0 rounded-xl">
        <div class="flex flex-wrap justify-between items-center gap-4">

          <!-- 左侧筛选区 -->
          <div class="flex flex-wrap items-center gap-2">
            <div class="hidden md:flex p-2 bg-indigo-100 rounded-lg text-indigo-600 mr-2 shrink-0">
              <el-icon size="20"><Collection /></el-icon>
            </div>

            <el-form :inline="true" :model="queryParams" class="!m-0">
              <el-form-item class="!mb-0 !mr-3">
                <el-select
                    v-model="queryParams.courseId"
                    placeholder="选择课程"
                    style="width: 200px"
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
              </el-form-item>

              <el-form-item class="!mb-0 !mr-3">
                <el-input
                    v-model="queryParams.content"
                    placeholder="搜索题目内容..."
                    prefix-icon="Search"
                    style="width: 240px"
                    clearable
                    @keyup.enter="handleSearch"
                    @clear="handleSearch"
                />
              </el-form-item>

              <el-form-item class="!mb-0">
                <el-select
                    v-model="queryParams.type"
                    placeholder="题型"
                    style="width: 140px"
                    clearable
                    @change="handleSearch"
                >
                  <el-option label="单选题" :value="1" />
                  <el-option label="多选题" :value="2" />
                  <el-option label="判断题" :value="3" />
                  <el-option label="简答题" :value="4" />
                  <el-option label="填空题" :value="5" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>

          <!-- 右侧按钮组 -->
          <div class="flex items-center gap-3 shrink-0">
            <el-button type="warning" plain icon="Upload" @click="openImportDialog">
              批量导入
            </el-button>
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
        </div>
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
                  <el-descriptions-item label="完整题干">
                    <div class="flex flex-col gap-2">
                      <span>{{ row.content }}</span>
                      <el-image
                          v-if="row.imageUrl"
                          :src="row.imageUrl"
                          :preview-src-list="[row.imageUrl]"
                          class="w-48 h-auto rounded border border-gray-200"
                          fit="contain"
                      />
                    </div>
                  </el-descriptions-item>
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
              <div class="flex items-center gap-2">
                <el-image
                    v-if="row.imageUrl"
                    :src="row.imageUrl"
                    class="w-10 h-10 rounded flex-shrink-0 bg-gray-100"
                    :preview-src-list="[row.imageUrl]"
                    preview-teleported
                >
                  <template #error>
                    <div class="flex justify-center items-center w-full h-full text-gray-400">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <span class="truncate block max-w-md" :title="row.content">{{ row.content }}</span>
              </div>
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

    <!-- 弹窗 1: AI 智能出题向导 -->
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

    <!-- 弹窗 2: Excel 批量导入 -->
    <el-dialog v-model="importDialog.visible" title="Excel 批量导入题目" width="500px">
      <el-form label-position="top">
        <el-form-item label="导入到课程" required>
          <el-select v-model="importDialog.courseId" placeholder="请选择课程" class="w-full">
            <el-option
                v-for="item in courseOptions"
                :key="item.id"
                :label="item.courseName"
                :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="上传文件">
          <el-upload
              class="w-full"
              drag
              action="#"
              :http-request="handleImport"
              :show-file-list="false"
              :disabled="importDialog.uploading"
              accept=".xlsx, .xls"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将 Excel 文件拖到此处，或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip flex justify-between items-center">
                <span>仅支持 .xlsx, .xls 格式</span>
                <el-link type="primary" :underline="false" @click.stop="downloadTemplate">下载模板</el-link>
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <div v-if="importDialog.uploading" class="flex items-center justify-center gap-2 text-indigo-600 mt-2">
          <el-icon class="is-loading"><Loading /></el-icon> 正在解析并导入数据...
        </div>
      </el-form>
    </el-dialog>

    <!-- 抽屉: 任务进度 -->
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

    <!-- 弹窗 3: 手动新增/编辑 -->
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

        <el-form-item label="题目图片">
          <el-input v-model="manualForm.imageUrl" placeholder="输入图片 URL (支持外部链接)" class="mb-2">
            <template #prefix><el-icon><Link /></el-icon></template>
          </el-input>
          <div v-if="manualForm.imageUrl" class="p-2 border border-dashed rounded bg-gray-50 inline-block">
            <el-image
                :src="manualForm.imageUrl"
                class="h-32 w-auto object-contain"
                :preview-src-list="[manualForm.imageUrl]"
                fit="contain"
            >
              <template #error>
                <div class="flex items-center justify-center h-32 w-32 text-gray-400 bg-gray-100 text-xs">
                  图片无法加载
                </div>
              </template>
            </el-image>
          </div>
        </el-form-item>

        <!-- 选项区域 -->
        <div v-if="[1, 2].includes(manualForm.type)" class="option-container bg-gray-50 p-4 rounded-lg mb-4">
          <div class="option-header flex justify-between items-center mb-2">
            <span class="text-sm font-bold text-gray-600">题目选项</span>
            <el-button type="primary" link size="small" @click="addOption">
              <el-icon><Plus /></el-icon> 添加选项
            </el-button>
          </div>
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

          <el-radio-group v-if="manualForm.type === 3" v-model="manualForm.answer">
            <el-radio value="1" border>正确 (True)</el-radio>
            <el-radio value="0" border>错误 (False)</el-radio>
          </el-radio-group>

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
  Delete, Edit, UploadFilled, Loading, Link, Picture
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
  types: [1]
})

const importDialog = reactive({
  visible: false,
  uploading: false,
  courseId: undefined
})

const manualDialog = reactive({
  visible: false,
  loading: false
})

const manualForm = reactive({
  id: undefined,
  courseId: undefined,
  type: 1,
  difficulty: 1,
  content: '',
  imageUrl: '',
  options: [{ value: '' }, { value: '' }, { value: '' }, { value: '' }],
  answer: '',
  answerArray: [] as string[],
  analysis: '',
  tags: [] as string[]
})

const manualFormRef = ref()
const manualRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  content: [{ required: true, message: '请输入题干内容', trigger: 'blur' }],
  answer: [{
    validator: (rule: any, value: any, callback: any) => {
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

watch(drawerVisible, (val) => {
  if (val) {
    fetchTasks()
    startPoll()
  } else {
    stopPoll()
  }
})

const runningTasksCount = computed(() => {
  return tasks.value.filter(t => t.status === 0 || t.status === 1).length
})

// --- Methods ---

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

// 2. AI 智能出题逻辑 - 修复：传递 types 数组而不是 type
const openAiDialog = () => {
  aiForm.topic = ''
  aiForm.totalCount = 5
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
    // 修复：直接发送 types 数组，不再转换为单数字段
    await request.post('/question/ai-generate', {
      courseId: aiForm.courseId,
      topic: aiForm.topic,
      totalCount: aiForm.totalCount,
      difficulty: aiForm.difficulty,
      types: aiForm.types // <--- 关键修复：发送 List
    })

    ElMessage.success('任务已提交，AI 正在生成中...')
    aiDialog.visible = false
    drawerVisible.value = true
    fetchTasks()
  } catch (error) {
    // error
  } finally {
    aiDialog.loading = false
  }
}

const fetchTasks = async () => {
  try {
    const res: any = await request.get('/question/task/list', { params: { size: 20 } })
    tasks.value = res.records
  } catch (error) { console.error(error) }
}

const startPoll = () => {
  if (pollTimer) return
  pollTimer = setInterval(fetchTasks, 3000)
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
    manualForm.id = row.id
    manualForm.courseId = row.courseId
    manualForm.type = row.type
    manualForm.content = row.content
    manualForm.imageUrl = row.imageUrl || ''
    manualForm.difficulty = row.difficulty
    manualForm.analysis = row.analysis
    manualForm.tags = parseTags(row.tags)

    const parsedOpts = parseOptions(row.options)
    manualForm.options = parsedOpts.map((v: string) => ({ value: v }))

    if (row.type === 2) {
      try {
        manualForm.answerArray = JSON.parse(row.answer).map(String)
      } catch (e) {
        manualForm.answerArray = []
      }
    } else {
      manualForm.answer = row.answer
    }
  } else {
    manualForm.id = undefined
    manualForm.content = ''
    manualForm.imageUrl = ''
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
          options: JSON.stringify(manualForm.options.map(o => o.value)),
          tags: JSON.stringify(manualForm.tags)
        }

        if (manualForm.type === 2) {
          const sorted = [...manualForm.answerArray].sort()
          payload.answer = JSON.stringify(sorted)
        }

        delete payload.answerArray

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

// 5. 导入功能逻辑
const openImportDialog = () => {
  importDialog.visible = true
  importDialog.courseId = queryParams.courseId
}

const downloadTemplate = () => {
  ElMessage.info('模板下载功能待实现，请联系管理员获取标准 Excel 模板')
}

const handleImport = async (options: any) => {
  if (!importDialog.courseId) {
    ElMessage.warning('请先选择导入的目标课程')
    return
  }

  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('courseId', importDialog.courseId.toString())

  importDialog.uploading = true
  try {
    await request.post('/question/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success('导入成功！')
    importDialog.visible = false
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    importDialog.uploading = false
  }
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

const isCorrectAnswer = (row: any, index: number) => {
  if (row.type === 1) {
    return row.answer === index.toString()
  } else if (row.type === 2) {
    try {
      const answers = JSON.parse(row.answer)
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
.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.option-container {
  background-color: #f9fafb;
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
:deep(.el-upload-dragger) {
  width: 100%;
  border-color: #dcdfe6;
}
:deep(.el-upload-dragger:hover) {
  border-color: #409eff;
}
</style>