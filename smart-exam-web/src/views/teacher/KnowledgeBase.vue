<template>
  <div class="app-container p-6 bg-gray-50 min-h-[calc(100vh-64px)] flex flex-col">
    <!-- 顶部筛选栏 -->
    <el-card shadow="never" class="border-0 rounded-xl mb-4 shrink-0">
      <div class="flex flex-wrap justify-between items-center gap-4">
        <div class="flex items-center gap-3">
          <div class="p-2 bg-indigo-100 rounded-lg text-indigo-600">
            <el-icon size="20"><Collection /></el-icon>
          </div>
          <div>
            <h2 class="text-lg font-bold text-gray-800">课程知识库</h2>
            <p class="text-xs text-gray-500">管理 RAG 检索资料，支撑 AI 出题与问答</p>
          </div>
        </div>

        <el-form :inline="true" class="!m-0 flex flex-wrap items-center gap-2">
          <el-form-item class="!mr-0 !mb-0">
            <el-select
                v-model="queryParams.courseId"
                placeholder="请选择课程"
                filterable
                class="w-60"
                @change="handleCourseChange"
            >
              <el-option
                  v-for="item in courseOptions"
                  :key="item.id"
                  :label="item.courseName"
                  :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item class="!mr-0 !mb-0">
            <el-input
                v-model="queryParams.keyword"
                placeholder="搜索文件名"
                prefix-icon="Search"
                clearable
                class="w-52"
                @input="handleSearch"
            />
          </el-form-item>
          <el-form-item class="!mr-0 !mb-0">
            <el-button :icon="Refresh" circle @click="refreshData" />
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 主体内容：Grid 布局自动撑满高度 -->
    <div class="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-4 gap-6">

      <!-- 左侧：上传区域 (高度自适应) -->
      <div class="lg:col-span-1 h-full">
        <el-card shadow="never" class="border-0 rounded-xl h-full flex flex-col">
          <div class="flex-1 flex flex-col justify-center items-center p-4 border-2 border-dashed border-gray-200 rounded-lg bg-gray-50 hover:bg-indigo-50 hover:border-indigo-300 transition-colors relative">
            <el-upload
                class="absolute inset-0 w-full h-full opacity-0 z-10 cursor-pointer"
                drag
                action="#"
                multiple
                :http-request="customUpload"
                :show-file-list="false"
                :disabled="!queryParams.courseId || uploadingCount > 0"
                accept=".pdf,.doc,.docx,.md,.txt"
            >
            </el-upload>

            <!-- 自定义显示内容 -->
            <div class="text-center z-0 pointer-events-none">
              <div class="w-16 h-16 bg-white rounded-full shadow-sm flex items-center justify-center mx-auto mb-4 text-indigo-500">
                <el-icon size="32"><UploadFilled /></el-icon>
              </div>
              <h3 class="text-lg font-medium text-gray-700 mb-1">点击或拖拽上传</h3>
              <p class="text-sm text-gray-400 mb-6">支持 PDF, Word, Markdown</p>

              <div v-if="!queryParams.courseId" class="inline-flex items-center px-3 py-1 bg-orange-100 text-orange-600 rounded-full text-xs">
                <el-icon class="mr-1"><Warning /></el-icon> 请先选择课程
              </div>
              <div v-else class="text-xs text-gray-400">
                单个文件最大 10MB
              </div>
            </div>

            <!-- 上传进度覆盖层 -->
            <div v-if="uploadingCount > 0" class="absolute inset-0 bg-white/90 z-20 flex flex-col items-center justify-center">
              <el-progress type="circle" :percentage="100" status="warning" :indeterminate="true" :width="80" />
              <p class="mt-4 text-gray-600 font-medium">正在上传 {{ uploadingCount }} 个文件...</p>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧：文件列表 (高度自适应) -->
      <div class="lg:col-span-3 h-full">
        <el-card shadow="never" class="border-0 rounded-xl h-full flex flex-col table-card-body-full">
          <el-table
              v-loading="loading && uploadingCount === 0"
              :data="filteredFileList"
              style="width: 100%; height: 100%;"
              :header-cell-style="{ background: '#f9fafb', color: '#374151', fontWeight: '600' }"
              height="100%"
          >
            <el-table-column label="文件名" min-width="300">
              <template #default="{ row }">
                <div class="flex items-center gap-3 overflow-hidden py-1">
                  <div class="w-10 h-10 rounded-lg flex items-center justify-center bg-gray-100 text-2xl flex-shrink-0">
                    <el-icon :class="getFileIconColor(row.fileName)">
                      <component :is="getFileIcon(row.fileName)" />
                    </el-icon>
                  </div>
                  <div class="flex flex-col overflow-hidden min-w-0">
                    <span class="font-medium text-gray-700 truncate" :title="row.fileName">{{ row.fileName }}</span>
                    <span class="text-xs text-gray-400 truncate font-mono">ID: {{ row.difyDocumentId || 'Pending...' }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="createTime" label="上传时间" width="180" align="center">
              <template #default="{ row }">
                <span class="text-sm text-gray-500 font-mono">{{ formatTime(row.createTime) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="状态" width="140" align="center">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" effect="light" round size="small" class="px-3">
                  <div class="flex items-center gap-1.5">
                    <el-icon v-if="row.status === 1" class="is-loading"><Loading /></el-icon>
                    <span class="w-2 h-2 rounded-full" :class="getStatusDotColor(row.status)" v-else></span>
                    {{ getStatusLabel(row.status) }}
                  </div>
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-popconfirm
                    title="确定删除？Dify 索引也将被移除。"
                    width="220"
                    confirm-button-type="danger"
                    @confirm="handleDelete(row)"
                >
                  <template #reference>
                    <el-button type="danger" link :disabled="row.status === 1">
                      <el-icon size="18"><Delete /></el-icon>
                    </el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty description="该课程暂无知识库文件" :image-size="100" />
            </template>
          </el-table>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Collection, Search, Refresh, UploadFilled, Delete, Loading, Warning, Document } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getWebSocketInstance } from '@/utils/websocket'

const loading = ref(false)
const uploadingCount = ref(0)
const courseOptions = ref<any[]>([])
const fileList = ref<any[]>([])
const pollingTimer = ref<any>(null)

const queryParams = reactive({
  courseId: undefined as number | undefined,
  keyword: ''
})

const filteredFileList = computed(() => {
  if (!queryParams.keyword) return fileList.value
  return fileList.value.filter(file =>
      file.fileName.toLowerCase().includes(queryParams.keyword.toLowerCase())
  )
})

// WebSocket文件状态处理
const handleFileStatus = (data: any) => {
  if (data && data.id) {
    const fileIndex = fileList.value.findIndex((f: any) => f.id === data.id)
    if (fileIndex >= 0) {
      fileList.value[fileIndex].status = data.status
      if (data.difyDocumentId) {
        fileList.value[fileIndex].difyDocumentId = data.difyDocumentId
      }
      // 状态变更后检查是否还需要轮询
      if (!fileList.value.some(file => file.status === 1)) {
        stopPolling()
      }
    } else {
      // 新文件，刷新列表
      fetchFiles(true)
    }
  }
}

// 注册WebSocket事件处理器
const registerWsHandlers = () => {
  const wsInstance = getWebSocketInstance()
  if (wsInstance?.isConnected) {
    wsInstance.on('file_status', handleFileStatus)
  }
}

// 移除WebSocket事件处理器
const unregisterWsHandlers = () => {
  const wsInstance = getWebSocketInstance()
  if (wsInstance) {
    wsInstance.off('file_status', handleFileStatus)
  }
}

onMounted(async () => {
  await fetchCourses()
  if (courseOptions.value.length > 0) {
    queryParams.courseId = courseOptions.value[0].id
    fetchFiles()
  }
  registerWsHandlers()
})

onUnmounted(() => {
  stopPolling()
  unregisterWsHandlers()
})

const fetchCourses = async () => {
  try {
    // 只获取当前用户已加入的课程
    const res: any = await request.get('/course/user/my-courses')
    courseOptions.value = res || []
  } catch (error) { console.error(error) }
}

const fetchFiles = async (isPolling = false) => {
  if (!queryParams.courseId) return
  if (!isPolling) loading.value = true
  try {
    const res: any = await request.get('/knowledge/list', { params: { courseId: queryParams.courseId } })
    fileList.value = res || []
    if (fileList.value.some(file => file.status === 1)) {
      startPolling()
    } else {
      stopPolling()
    }
  } catch (error) {
    stopPolling()
  } finally {
    if (!isPolling) loading.value = false
  }
}

const customUpload = async (options: any) => {
  if (!queryParams.courseId) {
    ElMessage.warning('请先选择课程')
    return
  }
  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('courseId', queryParams.courseId.toString())

  uploadingCount.value++
  try {
    await request.post('/knowledge/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    ElMessage.success(`${options.file.name} 上传成功`)
    fetchFiles(true)
  } catch (error: any) {
    ElMessage.error(`${options.file.name} 失败: ${error.message}`)
  } finally {
    uploadingCount.value--
  }
}

const handleDelete = async (row: any) => {
  try {
    await request.delete(`/knowledge/${row.id}`)
    ElMessage.success('删除成功')
    fetchFiles()
  } catch (error: any) { ElMessage.error(error.message) }
}

const handleCourseChange = () => fetchFiles()
const handleSearch = () => {}
const refreshData = () => fetchFiles()

const startPolling = () => {
  if (pollingTimer.value) return
  pollingTimer.value = setInterval(() => fetchFiles(true), 5000)
}

const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
}

const formatTime = (time: string) => time ? time.replace('T', ' ').substring(0, 16) : '-'
const getStatusLabel = (status: number) => ({ 0: '上传中', 1: '索引中', 2: '可用', 3: '失败' } as any)[status] || '未知'
const getStatusType = (status: number) => ({ 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' } as any)[status] || 'info'
const getStatusDotColor = (status: number) => ({ 0: 'bg-gray-400', 1: 'bg-orange-400', 2: 'bg-green-500', 3: 'bg-red-500' } as any)[status]
const getFileIcon = () => 'Document'
const getFileIconColor = (name: string) => {
  if (name.endsWith('.pdf')) return 'text-red-500'
  if (name.match(/\.doc(x)?$/)) return 'text-blue-500'
  return 'text-gray-500'
}
</script>

<style scoped>
/* 强制表格容器撑满卡片 */
.table-card-body-full :deep(.el-card__body) {
  height: 100%;
  padding: 0;
  display: flex;
  flex-direction: column;
}
/* 去除上传组件默认边框，使用自定义容器边框 */
:deep(.el-upload) {
  width: 100%;
  height: 100%;
}
:deep(.el-upload-dragger) {
  width: 100%;
  height: 100%;
  border: none;
  background: transparent;
}
</style>