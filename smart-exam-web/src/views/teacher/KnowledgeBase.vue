<template>
  <div class="app-container p-6 bg-gray-50 min-h-screen">
    <div class="max-w-7xl mx-auto space-y-6">
      <!-- 顶部筛选与操作栏 -->
      <el-card shadow="never" class="border-0 rounded-xl">
        <div class="flex flex-col md:flex-row justify-between items-center gap-4">
          <div class="flex items-center gap-2">
            <div class="p-2 bg-indigo-100 rounded-lg text-indigo-600">
              <el-icon size="20"><Collection /></el-icon>
            </div>
            <div>
              <h2 class="text-lg font-bold text-gray-800">课程知识库</h2>
              <p class="text-xs text-gray-500">管理课程的 RAG 检索资料 (PDF/Word/MD)</p>
            </div>
          </div>

          <el-form :inline="true" class="flex-1 flex justify-end m-0">
            <el-form-item class="!mr-3 !mb-0">
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
            <el-form-item class="!mr-3 !mb-0">
              <el-input
                  v-model="queryParams.keyword"
                  placeholder="搜索文件名"
                  prefix-icon="Search"
                  clearable
                  @input="handleSearch"
              />
            </el-form-item>
            <el-form-item class="!mb-0">
              <el-button :icon="Refresh" circle @click="refreshData" />
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <!-- 主要内容区域 -->
      <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">

        <!-- 左侧/上方：上传区域 -->
        <div class="lg:col-span-1">
          <el-card shadow="never" class="border-0 rounded-xl h-full flex flex-col">
            <div class="flex-1 flex flex-col justify-center">
              <el-upload
                  class="upload-area w-full"
                  drag
                  action="#"
                  :http-request="customUpload"
                  :show-file-list="false"
                  :disabled="!queryParams.courseId || uploading"
                  accept=".pdf,.doc,.docx,.md,.txt"
              >
                <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                <div class="el-upload__text">
                  将文件拖到此处，或 <em>点击上传</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip text-center mt-4">
                    <div v-if="!queryParams.courseId" class="text-orange-500 flex items-center justify-center gap-1">
                      <el-icon><Warning /></el-icon> 请先在上方选择课程
                    </div>
                    <div v-else class="text-gray-400">
                      支持 PDF, Word, Markdown (Max 10MB)
                    </div>
                  </div>
                </template>
              </el-upload>
            </div>
          </el-card>
        </div>

        <!-- 右侧/下方：文件列表 -->
        <div class="lg:col-span-3">
          <el-card shadow="never" class="border-0 rounded-xl min-h-[500px]">
            <el-table
                v-loading="loading"
                :data="filteredFileList"
                style="width: 100%"
                :header-cell-style="{ background: '#f9fafb', color: '#374151', fontWeight: '600' }"
            >
              <el-table-column label="文件名" min-width="250">
                <template #default="{ row }">
                  <div class="flex items-center gap-3 overflow-hidden"> <!-- Added overflow-hidden to prevent overlap -->
                    <div class="w-10 h-10 rounded-lg flex items-center justify-center bg-gray-100 text-2xl flex-shrink-0"> <!-- Added flex-shrink-0 -->
                      <el-icon :class="getFileIconColor(row.fileName)">
                        <component :is="getFileIcon(row.fileName)" />
                      </el-icon>
                    </div>
                    <div class="flex flex-col overflow-hidden min-w-0"> <!-- Added min-w-0 for truncate within flex item -->
                      <span class="font-medium text-gray-700 truncate" :title="row.fileName">{{ row.fileName }}</span>
                      <span class="text-xs text-gray-400 truncate">ID: {{ row.difyDocumentId || 'Pending...' }}</span> <!-- Added truncate -->
                    </div>
                  </div>
                </template>
              </el-table-column>

              <el-table-column prop="createTime" label="上传时间" width="180">
                <template #default="{ row }">
                  <span class="text-sm text-gray-500">{{ formatTime(row.createTime) }}</span>
                </template>
              </el-table-column>

              <el-table-column label="状态" width="120" align="center">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)" effect="light" round>
                    <div class="flex items-center gap-1">
                      <el-icon v-if="row.status === 1" class="is-loading"><Loading /></el-icon>
                      {{ getStatusLabel(row.status) }}
                    </div>
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="操作" width="100" align="center" fixed="right">
                <template #default="{ row }">
                  <el-popconfirm
                      title="确定要删除该文件吗？这将同步删除 Dify 知识库中的索引。"
                      width="240"
                      confirm-button-text="删除"
                      confirm-button-type="danger"
                      cancel-button-text="取消"
                      @confirm="handleDelete(row)"
                  >
                    <template #reference>
                      <el-button type="danger" link :disabled="row.status === 1">
                        <el-icon size="16"><Delete /></el-icon>
                      </el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>

              <template #empty>
                <el-empty description="该课程暂无知识库文件" />
              </template>
            </el-table>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Collection, Search, Refresh, UploadFilled, Delete, Loading, Warning,
  Document, Picture
} from '@element-plus/icons-vue'
import request from '@/utils/request'

// --- 状态定义 ---
const loading = ref(false)
const uploading = ref(false)
const courseOptions = ref<any[]>([])
const fileList = ref<any[]>([])
const pollingTimer = ref<any>(null)

const queryParams = reactive({
  courseId: undefined as number | undefined,
  keyword: ''
})

// --- 计算属性 ---
const filteredFileList = computed(() => {
  if (!queryParams.keyword) return fileList.value
  return fileList.value.filter(file =>
      file.fileName.toLowerCase().includes(queryParams.keyword.toLowerCase())
  )
})

// --- 生命周期 ---
onMounted(async () => {
  await fetchCourses()
  // 如果有课程，默认选中第一个并加载
  if (courseOptions.value.length > 0) {
    queryParams.courseId = courseOptions.value[0].id
    fetchFiles()
  }
})

onUnmounted(() => {
  stopPolling()
})

// --- API 方法 ---

// 1. 获取课程列表
const fetchCourses = async () => {
  try {
    // 这里暂时复用 admin 接口，如果权限不足需后端调整或提供 /api/teacher/courses
    const res: any = await request.get('/admin/course/list', { params: { size: 100 } })
    if (res && res.records) {
      courseOptions.value = res.records
    }
  } catch (error) {
    console.error('获取课程失败', error)
  }
}

// 2. 获取文件列表
const fetchFiles = async (isPolling = false) => {
  if (!queryParams.courseId) return

  if (!isPolling) loading.value = true
  try {
    const res: any = await request.get('/knowledge/list', {
      params: { courseId: queryParams.courseId }
    })
    fileList.value = res || []

    // 检查是否有"索引中"的文件，如果有则开启轮询
    const hasIndexing = fileList.value.some(file => file.status === 1) // 1-indexing
    if (hasIndexing) {
      startPolling()
    } else {
      stopPolling()
    }
  } catch (error) {
    console.error(error)
    stopPolling()
  } finally {
    if (!isPolling) loading.value = false
  }
}

// 3. 上传文件
const customUpload = async (options: any) => {
  if (!queryParams.courseId) {
    ElMessage.warning('请先选择课程')
    return
  }

  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('courseId', queryParams.courseId.toString())

  uploading.value = true
  try {
    await request.post('/knowledge/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success('上传成功，Dify 正在索引文档...')
    fetchFiles() // 刷新列表
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

// 4. 删除文件
const handleDelete = async (row: any) => {
  try {
    await request.delete(`/knowledge/${row.id}`)
    ElMessage.success('删除成功')
    fetchFiles()
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
}

// --- 辅助逻辑 ---

const handleCourseChange = () => {
  fetchFiles()
}

const handleSearch = () => {
  // 前端过滤，无需请求
}

const refreshData = () => {
  fetchFiles()
}

const startPolling = () => {
  if (pollingTimer.value) return
  pollingTimer.value = setInterval(() => {
    fetchFiles(true) // 静默刷新
  }, 5000)
}

const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
}

// --- 样式/格式化 ---

const formatTime = (time: string) => {
  return time ? time.replace('T', ' ').substring(0, 16) : '-'
}

const getStatusLabel = (status: number) => {
  const map: any = { 0: '上传中', 1: '索引中', 2: '可用', 3: '失败' }
  return map[status] || '未知'
}

const getStatusType = (status: number) => {
  const map: any = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[status] || 'info'
}

const getFileIcon = (fileName: string) => {
  return 'Document' // 简化处理，统一用文档图标
}

const getFileIconColor = (fileName: string) => {
  if (fileName.endsWith('.pdf')) return 'text-red-500'
  if (fileName.endsWith('.doc') || fileName.endsWith('.docx')) return 'text-blue-500'
  if (fileName.endsWith('.md')) return 'text-gray-700'
  return 'text-gray-500'
}
</script>

<style scoped lang="scss">
/* 上传组件样式覆盖 */
:deep(.el-upload-dragger) {
  width: 100%;
  height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-width: 2px;
  border-style: dashed;
  border-color: #e5e7eb;
  transition: all 0.3s;
  background-color: #f9fafb;

  &:hover {
    border-color: #6366f1;
    background-color: #eef2ff;

    .el-icon--upload {
      color: #6366f1;
      transform: scale(1.1);
    }
  }

  .el-icon--upload {
    font-size: 64px;
    color: #9ca3af;
    margin-bottom: 16px;
    transition: all 0.3s;
  }

  .el-upload__text {
    font-size: 16px;
    color: #4b5563;

    em {
      color: #6366f1;
      font-weight: 600;
    }
  }
}
</style>