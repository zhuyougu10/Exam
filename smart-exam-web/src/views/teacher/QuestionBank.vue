<template>
  <div class="page-container">
    <div class="content-wrapper">

      <!-- 顶部工具栏 -->
      <el-card shadow="never" class="toolbar-card">
        <div class="toolbar-content">
          <!-- 左侧筛选区 -->
          <div class="filter-group">
            <div class="icon-badge">
              <el-icon size="20"><Collection /></el-icon>
            </div>

            <div class="filter-inputs">
              <el-select
                  v-model="queryParams.courseId"
                  placeholder="选择课程"
                  class="filter-item-course"
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
                  class="filter-item-search"
                  clearable
                  @keyup.enter="handleSearch"
                  @clear="handleSearch"
              />

              <el-select
                  v-model="queryParams.type"
                  placeholder="题型"
                  class="filter-item-type"
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
          </div>

          <!-- 右侧按钮组 -->
          <div class="action-group">
            <el-button type="warning" plain icon="Upload" @click="openImportDialog">
              批量导入
            </el-button>
            <el-button type="primary" color="#6366f1" icon="MagicStick" @click="openAiDialog">
              AI 智能出题
            </el-button>
            <el-button type="success" icon="Plus" @click="openManualDialog()">
              手动新增
            </el-button>
            <div class="badge-wrapper">
              <el-badge :value="runningTasksCount" :hidden="runningTasksCount === 0" type="primary">
                <el-button icon="Stopwatch" circle @click="drawerVisible = true" />
              </el-badge>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 题目列表区域 -->
      <el-card shadow="never" class="table-card">
        <el-table
            v-loading="loading"
            :data="questionList"
            row-key="id"
            style="width: 100%; height: 100%;"
            :header-cell-style="{ background: '#f9fafb', color: '#374151', fontWeight: '600' }"
            class="question-table"
        >
          <!-- 展开行：显示完整详情 -->
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="expand-wrapper">
                <el-descriptions title="题目详情" :column="1" border class="detail-descriptions">
                  <el-descriptions-item label="完整题干">
                    <div class="detail-content">
                      <span class="detail-text">{{ row.content }}</span>
                      <el-image
                          v-if="row.imageUrl"
                          :src="row.imageUrl"
                          :preview-src-list="[row.imageUrl]"
                          class="detail-image"
                          fit="contain"
                      />
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="选项" v-if="[1, 2].includes(row.type)">
                    <div class="options-list">
                      <ul class="option-ul">
                        <li v-for="(opt, idx) in parseOptions(row.options)" :key="idx"
                            class="option-li">
                          <span class="option-tag">{{ String.fromCharCode(65 + idx) }}</span>
                          <span class="option-text">{{ opt }}</span>
                          <el-tag v-if="isCorrectAnswer(row, idx)" type="success" size="small" effect="dark" class="correct-tag">正确答案</el-tag>
                        </li>
                      </ul>
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="参考答案" v-else>
                    <div class="answer-text">
                      {{ formatAnswer(row) }}
                    </div>
                  </el-descriptions-item>
                  <el-descriptions-item label="解析">
                    <div class="analysis-text">
                      {{ row.analysis || '暂无解析' }}
                    </div>
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="题干" min-width="300">
            <template #default="{ row }">
              <div class="column-content">
                <el-image
                    v-if="row.imageUrl"
                    :src="row.imageUrl"
                    class="thumbnail-image"
                    :preview-src-list="[row.imageUrl]"
                    preview-teleported
                    fit="cover"
                >
                  <template #error>
                    <div class="image-error">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div class="content-text-wrapper">
                  <p class="content-text" :title="row.content">{{ row.content }}</p>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="type" label="类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getTypeTag(row.type)" effect="plain" round size="small">
                {{ getTypeLabel(row.type) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="难度" width="140" align="center">
            <template #default="{ row }">
              <el-rate v-model="row.difficulty" disabled text-color="#ff9900" size="small" />
            </template>
          </el-table-column>

          <el-table-column label="知识点" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="tags-wrapper">
                <el-tag
                    v-for="(tag, idx) in parseTags(row.tags)"
                    :key="idx"
                    size="small"
                    type="info"
                    effect="light"
                    class="knowledge-tag"
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
        <div class="pagination-container">
          <el-pagination
              v-model:current-page="queryParams.page"
              v-model:page-size="queryParams.size"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              background
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 弹窗 1: AI 智能出题向导 -->
    <el-dialog v-model="aiDialog.visible" title="AI 智能出题向导" width="550px" destroy-on-close align-center>
      <el-form :model="aiForm" label-position="top" class="dialog-form">
        <el-form-item label="目标课程" class="form-item-spacing">
          <el-select v-model="aiForm.courseId" placeholder="请选择课程" class="full-width">
            <el-option
                v-for="item in courseOptions"
                :key="item.id"
                :label="item.courseName"
                :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="出题主题 / 知识点" class="form-item-spacing">
          <el-input v-model="aiForm.topic" placeholder="例如：Vue3 响应式原理、Java 多线程..." size="large" />
        </el-form-item>

        <el-form-item label="生成数量" class="form-item-spacing">
          <div class="slider-container">
            <el-slider v-model="aiForm.totalCount" :min="1" :max="20" class="slider-item" :marks="{1:'1', 10:'10', 20:'20'}" />
            <div class="count-display">
              {{ aiForm.totalCount }} 题
            </div>
          </div>
        </el-form-item>

        <el-form-item label="题目难度" class="form-item-spacing">
          <el-radio-group v-model="aiForm.difficulty" size="large" class="full-width-radio">
            <el-radio-button label="简单" value="简单" />
            <el-radio-button label="中等" value="中等" />
            <el-radio-button label="困难" value="困难" />
          </el-radio-group>
        </el-form-item>

        <el-form-item label="题目类型 (多选则为混合生成)" class="form-item-no-margin">
          <el-checkbox-group v-model="aiForm.types" class="type-checkbox-group">
            <el-checkbox :value="1" label="单选" border class="custom-checkbox" />
            <el-checkbox :value="2" label="多选" border class="custom-checkbox" />
            <el-checkbox :value="3" label="判断" border class="custom-checkbox" />
            <el-checkbox :value="4" label="简答" border class="custom-checkbox" />
            <el-checkbox :value="5" label="填空" border class="custom-checkbox" />
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="aiDialog.visible = false" size="large">取消</el-button>
          <el-button type="primary" :loading="aiDialog.loading" @click="submitAiTask" size="large" color="#6366f1">
            开始生成
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 弹窗 2: Excel 批量导入 -->
    <el-dialog v-model="importDialog.visible" title="Excel 批量导入题目" width="500px" align-center>
      <div class="import-dialog-content">
        <el-form label-position="top">
          <el-form-item label="导入到课程" required>
            <el-select v-model="importDialog.courseId" placeholder="请选择课程" class="full-width">
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
                class="full-width-upload"
                drag
                action="#"
                multiple
                :http-request="handleImport"
                :show-file-list="false"
                :disabled="importDialog.uploading"
                accept=".xlsx, .xls"
            >
              <div class="upload-placeholder">
                <el-icon class="upload-icon"><upload-filled /></el-icon>
                <div class="upload-text">
                  将 Excel 文件拖到此处，或 <em>点击上传</em>
                </div>
                <div class="upload-subtext">(支持多选批量上传)</div>
              </div>
              <template #tip>
                <div class="upload-tip">
                  <span class="tip-text">仅支持 .xlsx, .xls 格式</span>
                  <el-link type="primary" :underline="false" @click.stop="downloadTemplate">下载模板</el-link>
                </div>
              </template>
            </el-upload>
          </el-form-item>

          <div v-if="importDialog.uploading" class="loading-box">
            <el-icon class="is-loading"><Loading /></el-icon> 正在处理：剩余 {{ importDialog.uploadingCount }} 个文件...
          </div>
        </el-form>
      </div>
    </el-dialog>

    <!-- 抽屉: 任务进度 -->
    <el-drawer v-model="drawerVisible" :with-header="false" size="400px">
      <div class="drawer-content">
        <div class="drawer-header">
          <span class="drawer-title">AI 出题任务中心</span>
          <el-popconfirm
              v-if="hasFinishedTasks"
              title="确定清除已完成的任务记录吗？"
              @confirm="handleClearTasks"
              width="260"
          >
            <template #reference>
              <el-button type="danger" link size="small">
                <el-icon class="mr-1"><Delete /></el-icon> 清除历史
              </el-button>
            </template>
          </el-popconfirm>
        </div>

        <div class="task-list custom-scrollbar">
          <div v-if="tasks.length === 0" class="empty-task">
            <el-icon size="48" class="empty-icon"><Collection /></el-icon>
            <p>暂无任务记录</p>
          </div>
          <el-card v-for="task in tasks" :key="task.id" shadow="hover" class="task-card">
            <div class="task-header">
              <div class="task-name" :title="task.taskName">
                {{ task.taskName }}
              </div>
              <el-tag size="small" :type="getTaskStatusType(task.status)" effect="dark">
                {{ getTaskStatusLabel(task.status) }}
              </el-tag>
            </div>
            <div class="task-meta">
              <span>ID: {{ task.id }}</span>
              <span>{{ formatTime(task.createTime) }}</span>
            </div>
            <el-progress
                :percentage="calculateProgress(task)"
                :status="getTaskStatusType(task.status) === 'danger' ? 'exception' : (task.status === 2 ? 'success' : '')"
                :stroke-width="10"
                striped
                striped-flow
            />
            <div v-if="task.errorMsg" class="error-msg">
              错误: {{ task.errorMsg }}
            </div>
          </el-card>
        </div>
      </div>
    </el-drawer>

    <!-- 弹窗 3: 手动新增/编辑 -->
    <el-dialog
        v-model="manualDialog.visible"
        :title="manualForm.id ? '编辑题目' : '手动录入题目'"
        width="800px"
        destroy-on-close
        :close-on-click-modal="false"
        align-center
        class="manual-dialog"
    >
      <el-form ref="manualFormRef" :model="manualForm" :rules="manualRules" label-width="80px" class="dialog-form-padded">

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="所属课程" prop="courseId">
              <el-select v-model="manualForm.courseId" placeholder="选择课程" class="full-width">
                <el-option v-for="c in courseOptions" :key="c.id" :label="c.courseName" :value="c.id"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="题目类型" prop="type">
              <el-select v-model="manualForm.type" placeholder="选择类型" class="full-width" @change="handleTypeChange">
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
          <el-rate v-model="manualForm.difficulty" :max="3" :texts="['简单', '中等', '困难']" show-text class="mt-small" />
        </el-form-item>

        <el-form-item label="题干内容" prop="content">
          <el-input
              v-model="manualForm.content"
              type="textarea"
              :rows="4"
              placeholder="请输入题目描述..."
              resize="none"
          />
        </el-form-item>

        <el-form-item label="题目图片">
          <div class="full-width">
            <el-input v-model="manualForm.imageUrl" placeholder="输入图片 URL (支持外部链接)" class="mb-small">
              <template #prefix><el-icon><Link /></el-icon></template>
            </el-input>
            <div v-if="manualForm.imageUrl" class="image-preview-box">
              <el-image
                  :src="manualForm.imageUrl"
                  class="preview-image"
                  :preview-src-list="[manualForm.imageUrl]"
                  fit="contain"
              >
                <template #error>
                  <div class="image-load-error">
                    图片无法加载
                  </div>
                </template>
              </el-image>
            </div>
          </div>
        </el-form-item>

        <div v-if="[1, 2].includes(manualForm.type)" class="options-container">
          <div class="options-header">
            <span class="options-title">
              <el-icon class="option-icon"><Collection /></el-icon> 题目选项
            </span>
            <el-button type="primary" link size="small" @click="addOption">
              <el-icon class="mr-1"><Plus /></el-icon> 添加选项
            </el-button>
          </div>
          <div v-for="(opt, index) in manualForm.options" :key="index" class="option-input-row">
            <span class="option-label">{{ String.fromCharCode(65 + index) }}</span>
            <el-input v-model="opt.value" placeholder="输入选项内容" class="flex-1" />
            <el-button type="danger" link @click="removeOption(index)" :disabled="manualForm.options.length <= 2" class="delete-btn">
              <el-icon size="18"><Delete /></el-icon>
            </el-button>
          </div>
        </div>

        <el-form-item label="参考答案" prop="answer">
          <div class="full-width">
            <el-radio-group v-if="manualForm.type === 1" v-model="manualForm.answer" class="answer-group">
              <el-radio
                  v-for="(opt, index) in manualForm.options"
                  :key="index"
                  :value="index.toString()"
                  border
                  class="bg-white custom-radio"
              >
                选项 {{ String.fromCharCode(65 + index) }}
              </el-radio>
            </el-radio-group>

            <el-checkbox-group v-if="manualForm.type === 2" v-model="manualForm.answerArray" class="answer-group">
              <el-checkbox
                  v-for="(opt, index) in manualForm.options"
                  :key="index"
                  :value="index.toString()"
                  border
                  class="bg-white custom-checkbox"
              >
                选项 {{ String.fromCharCode(65 + index) }}
              </el-checkbox>
            </el-checkbox-group>

            <el-radio-group v-if="manualForm.type === 3" v-model="manualForm.answer" class="answer-group">
              <el-radio value="1" border class="bg-white text-green custom-radio">正确 (True)</el-radio>
              <el-radio value="0" border class="bg-white text-red custom-radio">错误 (False)</el-radio>
            </el-radio-group>

            <el-input
                v-if="[4, 5].includes(manualForm.type)"
                v-model="manualForm.answer"
                type="textarea"
                :rows="3"
                placeholder="请输入参考答案关键词或完整内容"
                resize="none"
            />
          </div>
        </el-form-item>

        <el-form-item label="解析说明">
          <el-input v-model="manualForm.analysis" type="textarea" :rows="3" placeholder="可选：输入题目解析" resize="none" />
        </el-form-item>

        <el-form-item label="标签">
          <el-select
              v-model="manualForm.tags"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="输入标签后回车 (如: 第一章)"
              class="full-width"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="manualDialog.visible = false" size="large">取消</el-button>
          <el-button type="primary" :loading="manualDialog.loading" @click="submitManual" size="large" color="#6366f1">
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
  Delete, Edit, UploadFilled, Loading, Upload, Link, Picture
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getWebSocketInstance } from '@/utils/websocket'

// ... (逻辑代码省略，请直接复用上文) ...
const loading = ref(false)
const questionList = ref([])
const courseOptions = ref<any[]>([])
const total = ref(0)
const drawerVisible = ref(false)
const tasks = ref<any[]>([])
let pollTimer: any = null

const previousRunningTaskIds = ref<Set<number>>(new Set())

const queryParams = reactive({
  page: 1,
  size: 10,
  courseId: undefined,
  content: '',
  type: undefined as number | undefined
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
  uploadingCount: 0,
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

// WebSocket任务进度处理
const handleTaskProgress = (data: any) => {
  if (data && data.taskId) {
    const taskIndex = tasks.value.findIndex((t: any) => t.id === data.taskId)
    if (taskIndex >= 0) {
      tasks.value[taskIndex].currentCount = data.currentCount
      tasks.value[taskIndex].status = data.status
      if (data.errorMsg) tasks.value[taskIndex].errorMsg = data.errorMsg
    }
  }
}

const handleTaskComplete = (data: any) => {
  if (data && data.taskId) {
    const taskIndex = tasks.value.findIndex((t: any) => t.id === data.taskId)
    if (taskIndex >= 0) {
      tasks.value[taskIndex].currentCount = data.currentCount
      tasks.value[taskIndex].status = data.status
      if (data.errorMsg) tasks.value[taskIndex].errorMsg = data.errorMsg
    }
    // 任务完成，刷新题目列表
    if (data.status === 2) {
      ElMessage.success('AI 出题任务已完成，题目列表已自动更新')
      fetchData()
    }
    fetchTasks()
  }
}

// 注册WebSocket事件处理器
const registerWsHandlers = () => {
  const wsInstance = getWebSocketInstance()
  if (wsInstance?.isConnected) {
    wsInstance.on('task_progress', handleTaskProgress)
    wsInstance.on('task_complete', handleTaskComplete)
  }
}

// 移除WebSocket事件处理器
const unregisterWsHandlers = () => {
  const wsInstance = getWebSocketInstance()
  if (wsInstance) {
    wsInstance.off('task_progress', handleTaskProgress)
    wsInstance.off('task_complete', handleTaskComplete)
  }
}

onMounted(() => {
  fetchCourses()
  fetchData()
  registerWsHandlers()
})

onUnmounted(() => {
  stopPoll()
  unregisterWsHandlers()
})

watch(drawerVisible, (val) => {
  if (val) {
    fetchTasks()
    startPoll() // 保留轮询作为WebSocket的备用方案
  } else {
    stopPoll()
  }
})

const runningTasksCount = computed(() => {
  return tasks.value.filter(t => t.status === 0 || t.status === 1).length
})

const hasFinishedTasks = computed(() => {
  return tasks.value.some(t => t.status === 2 || t.status === 3)
})

const handleSizeChange = (val: number) => {
  queryParams.size = val
  queryParams.page = 1
  fetchData()
}

const handleCurrentChange = (val: number) => {
  queryParams.page = val
  fetchData()
}

const fetchCourses = async () => {
  try {
    // 只获取当前用户已加入的课程
    const res: any = await request.get('/course/user/my-courses')
    courseOptions.value = res || []
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
    await request.post('/question/ai-generate', {
      courseId: aiForm.courseId,
      topic: aiForm.topic,
      totalCount: aiForm.totalCount,
      difficulty: aiForm.difficulty,
      types: aiForm.types
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
    const newTasks = res.records
    tasks.value = newTasks

    let needRefresh = false
    const currentRunningIds = new Set<number>()

    newTasks.forEach((t: any) => {
      if (t.status === 0 || t.status === 1) {
        currentRunningIds.add(t.id)
      } else if (t.status === 2) {
        if (previousRunningTaskIds.value.has(t.id)) {
          needRefresh = true
        }
      }
    })

    if (needRefresh) {
      ElMessage.success('AI 出题任务已完成，题目列表已自动更新')
      fetchData()
    }

    previousRunningTaskIds.value = currentRunningIds

  } catch (error) { console.error(error) }
}

const handleClearTasks = async () => {
  try {
    await request.delete('/question/task/clear')
    ElMessage.success('历史任务已清除')
    fetchTasks()
  } catch (error) {
    console.error(error)
  }
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

const openImportDialog = () => {
  importDialog.visible = true
  importDialog.courseId = queryParams.courseId
  importDialog.uploadingCount = 0
}

const downloadTemplate = async () => {
  try {
    const response = await request.get('/question/template', {
      responseType: 'blob'
    })
    const blob = new Blob([response as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '题目导入模板.xlsx'
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(link)
    ElMessage.success('模板下载成功')
  } catch (error) {
    ElMessage.error('模板下载失败')
    console.error(error)
  }
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
  importDialog.uploadingCount++

  try {
    await request.post('/question/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success(`文件 ${options.file.name} 导入成功！`)
    fetchData()
  } catch (error: any) {
    ElMessage.error(`文件 ${options.file.name} 导入失败: ${error.message || '未知错误'}`)
  } finally {
    importDialog.uploadingCount--
    if (importDialog.uploadingCount <= 0) {
      importDialog.uploading = false
      importDialog.uploadingCount = 0
    }
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
/* Page Layout */
.page-container {
  padding: 24px;
  background-color: #f9fafb;
  min-height: 100vh;
  box-sizing: border-box;
}

.content-wrapper {
  /* 移除 max-w-7xl 限制，使用 w-full 或 flex 撑满 */
  width: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  height: calc(100vh - 48px - 60px); /* 减去 padding 和可能的导航栏高度 */
}

/* Toolbar */
.toolbar-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.toolbar-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (min-width: 1024px) {
  .toolbar-content {
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }
}

.filter-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  width: 100%;
}

@media (min-width: 1024px) {
  .filter-group {
    width: auto;
  }
}

.icon-badge {
  display: none;
}

@media (min-width: 768px) {
  .icon-badge {
    display: flex;
    padding: 8px;
    background-color: #e0e7ff;
    border-radius: 8px;
    color: #4f46e5;
    flex-shrink: 0;
  }
}

.filter-inputs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
}

@media (min-width: 1024px) {
  .filter-inputs {
    width: auto;
  }
}

.filter-item-course {
  width: 100%;
}

.filter-item-search {
  width: 100%;
}

.filter-item-type {
  width: 100%;
}

@media (min-width: 640px) {
  .filter-item-course { width: 192px; }
  .filter-item-search { width: 240px; }
  .filter-item-type { width: 128px; }
}

.action-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  width: 100%;
  justify-content: flex-end;
}

@media (min-width: 1024px) {
  .action-group {
    width: auto;
  }
}

.badge-wrapper {
  margin-left: 8px;
}

/* Table */
.table-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  /* 关键修改：flex-1 让卡片撑满剩余垂直空间 */
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 防止表格溢出 */
}

:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden; /* 关键：让表格区域可滚动 */
}

.question-table {
  flex: 1;
  height: 100%;
}

/* Expand Content */
.expand-wrapper {
  padding: 24px;
  background-color: #f9fafb;
  border-radius: 8px;
  margin: 0 16px;
  border: 1px solid #f3f4f6;
}

.detail-descriptions {
  background-color: white;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 0;
}

.detail-text {
  font-size: 16px;
  color: #1f2937;
  line-height: 1.6;
}

.detail-image {
  width: 192px;
  height: auto;
  border-radius: 4px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

.options-list {
  padding: 8px 0;
}

.option-ul {
  padding: 0;
  margin: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-li {
  display: flex;
  align-items: flex-start;
  color: #374151;
  background-color: #f9fafb;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.option-li:hover {
  background-color: #f3f4f6;
}

.option-tag {
  font-weight: 700;
  margin-right: 12px;
  color: #4f46e5;
  background-color: #eef2ff;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 14px;
  flex-shrink: 0;
}

.option-text {
  flex: 1;
}

.correct-tag {
  margin-left: 12px;
}

.answer-text {
  padding: 8px 0;
  font-family: monospace;
  font-weight: 700;
  color: #16a34a;
}

.analysis-text {
  padding: 8px 0;
  color: #4b5563;
  line-height: 1.6;
}

/* Table Column Content */
.column-content {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.thumbnail-image {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  flex-shrink: 0;
  background-color: #f3f4f6;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-error {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  color: #d1d5db;
}

.content-text-wrapper {
  flex: 1;
  min-width: 0;
}

.content-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
  color: #1f2937;
  margin: 0;
  font-size: 14px;
}

.tags-wrapper {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.knowledge-tag {
  margin-bottom: 4px;
}

/* Pagination */
.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 16px;
  border-top: 1px solid #f3f4f6;
}

/* Dialogs */
.dialog-form {
  padding: 8px;
}

.dialog-form-padded {
  padding: 8px;
}

.form-item-spacing {
  margin-bottom: 20px;
}

.form-item-no-margin {
  margin-bottom: 0;
}

.full-width {
  width: 100%;
}

.slider-container {
  display: flex;
  align-items: center;
  gap: 24px;
  width: 100%;
  padding: 0 8px;
}

.slider-item {
  flex: 1;
}

.count-display {
  width: 64px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f3f4f6;
  border-radius: 4px;
  font-weight: 700;
  color: #374151;
}

.full-width-radio {
  width: 100%;
  display: flex;
}

:deep(.full-width-radio .el-radio-button__inner) {
  width: 100%;
}

.type-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* Custom Checkbox/Radio Styling Helper */
.custom-checkbox {
  margin-right: 8px;
  margin-bottom: 8px;
}
.custom-radio {
  margin-right: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}

/* Import Dialog */
.import-dialog-content {
  padding: 16px;
}

.full-width-upload {
  width: 100%;
}

:deep(.full-width-upload .el-upload-dragger) {
  width: 100%;
}

.loading-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #4f46e5;
  margin-top: 16px;
  background-color: #eef2ff;
  padding: 12px;
  border-radius: 8px;
}

/* Drawer */
.drawer-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.drawer-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.task-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-task {
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

.task-card {
  position: relative;
  border: 1px solid #f3f4f6;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.task-name {
  font-weight: 700;
  font-size: 14px;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 192px;
}

.task-meta {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
}

.error-msg {
  margin-top: 12px;
  font-size: 12px;
  color: #dc2626;
  background-color: #fef2f2;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #fee2e2;
  line-height: 1.25;
}

/* Manual Form */
.mt-small { margin-top: 8px; }
.mb-small { margin-bottom: 12px; }

.image-preview-box {
  padding: 8px;
  border: 1px dashed #d1d5db;
  border-radius: 4px;
  background-color: #f9fafb;
  display: flex;
  justify-content: center;
}

.preview-image {
  height: 160px;
  width: auto;
}

.image-load-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 160px;
  width: 160px;
  color: #9ca3af;
  background-color: #f3f4f6;
  font-size: 12px;
}

.options-container {
  background-color: #eef2ff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 24px;
  border: 1px solid #e0e7ff;
}

.options-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.options-title {
  font-size: 14px;
  font-weight: 700;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-icon {
  color: #4f46e5;
}

.option-input-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.option-input-row:last-child {
  margin-bottom: 0;
}

.option-label {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: white;
  border: 1px solid #e5e7eb;
  text-align: center;
  line-height: 32px;
  font-weight: 700;
  color: #4b5563;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

.delete-btn {
  margin-left: 0;
}

.answer-group {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.text-green { color: #16a34a; }
.text-red { color: #dc2626; }

/* Custom Scrollbar */
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