<template>
  <div class="page-container">
    <div class="content-wrapper">
      <!-- 顶部筛选与操作栏 -->
      <el-card shadow="never" class="header-card">
        <div class="header-content">
          <div class="title-section">
            <div class="icon-box">
              <el-icon size="24"><Collection /></el-icon>
            </div>
            <div class="text-box">
              <h2 class="main-title">课程知识库</h2>
              <p class="sub-title">管理课程的 RAG 检索资料 (PDF/Word/MD)</p>
            </div>
          </div>

          <!-- 筛选表单 -->
          <div class="filter-section">
            <el-select
                v-model="queryParams.courseId"
                placeholder="请选择课程"
                filterable
                class="filter-select"
                @change="handleCourseChange"
            >
              <el-option
                  v-for="item in courseOptions"
                  :key="item.id"
                  :label="item.courseName"
                  :value="item.id"
              />
            </el-select>

            <el-input
                v-model="queryParams.keyword"
                placeholder="搜索文件名"
                prefix-icon="Search"
                clearable
                class="filter-input"
                @input="handleSearch"
            />

            <el-button :icon="Refresh" circle @click="refreshData" />
          </div>
        </div>
      </el-card>

      <!-- 主要内容区域 (Grid Layout) -->
      <div class="page-grid">

        <!-- 左侧/上方：上传区域 -->
        <div class="grid-upload">
          <el-card shadow="never" class="upload-card">
            <div class="upload-wrapper">
              <el-upload
                  class="custom-upload-dragger"
                  drag
                  action="#"
                  multiple
                  :http-request="customUpload"
                  :show-file-list="false"
                  :disabled="!queryParams.courseId || uploadingCount > 0"
                  accept=".pdf,.doc,.docx,.md,.txt"
              >
                <div class="upload-placeholder">
                  <el-icon class="upload-icon"><upload-filled /></el-icon>
                  <div class="upload-text">
                    将文件拖到此处，或 <em class="highlight">点击上传</em>
                  </div>
                  <div class="upload-subtext">(支持批量上传)</div>
                </div>

                <template #tip>
                  <div class="upload-tip">
                    <div v-if="!queryParams.courseId" class="warning-tip">
                      <el-icon><Warning /></el-icon> 请先在上方选择课程
                    </div>
                    <div v-else class="info-tip">
                      支持 PDF, Word, Markdown (Max 10MB)
                    </div>
                  </div>
                </template>
              </el-upload>

              <!-- 上传进度提示 -->
              <div v-if="uploadingCount > 0" class="upload-progress">
                <el-progress
                    :percentage="100"
                    status="warning"
                    :indeterminate="true"
                    :duration="2"
                    :format="() => `正在上传 ${uploadingCount} 个文件...`"
                />
              </div>
            </div>
          </el-card>
        </div>

        <!-- 右侧/下方：文件列表 -->
        <div class="grid-list">
          <el-card shadow="never" class="list-card">
            <el-table
                v-loading="loading && uploadingCount === 0"
                :data="filteredFileList"
                style="width: 100%; height: 100%;"
                :header-cell-style="{ background: '#f9fafb', color: '#374151', fontWeight: '600' }"
                class="file-table"
            >
              <el-table-column label="文件名" min-width="250">
                <template #default="{ row }">
                  <div class="file-info">
                    <div class="file-icon-box">
                      <el-icon :class="getFileIconColor(row.fileName)">
                        <component :is="getFileIcon(row.fileName)" />
                      </el-icon>
                    </div>
                    <div class="file-details">
                      <span class="file-name" :title="row.fileName">{{ row.fileName }}</span>
                      <span class="file-id">ID: {{ row.difyDocumentId || 'Pending...' }}</span>
                    </div>
                  </div>
                </template>
              </el-table-column>

              <el-table-column prop="createTime" label="上传时间" width="180">
                <template #default="{ row }">
                  <span class="time-text">{{ formatTime(row.createTime) }}</span>
                </template>
              </el-table-column>

              <el-table-column label="状态" width="120" align="center">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)" effect="light" round size="small">
                    <div class="status-tag-content">
                      <el-icon v-if="row.status === 1" class="is-loading"><Loading /></el-icon>
                      {{ getStatusLabel(row.status) }}
                    </div>
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="操作" width="100" align="center" fixed="right">
                <template #default="{ row }">
                  <el-popconfirm
                      title="确定要删除该文件吗？"
                      width="220"
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
                <div class="empty-state">
                  <el-empty description="该课程暂无知识库文件" />
                </div>
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

// --- State ---
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

onMounted(async () => {
  await fetchCourses()
  if (courseOptions.value.length > 0) {
    queryParams.courseId = courseOptions.value[0].id
    fetchFiles()
  }
})

onUnmounted(() => {
  stopPolling()
})

const fetchCourses = async () => {
  try {
    const res: any = await request.get('/admin/course/list', { params: { size: 100 } })
    if (res && res.records) {
      courseOptions.value = res.records
    }
  } catch (error) {
    console.error('获取课程失败', error)
  }
}

const fetchFiles = async (isPolling = false) => {
  if (!queryParams.courseId) return

  if (!isPolling) loading.value = true
  try {
    const res: any = await request.get('/knowledge/list', {
      params: { courseId: queryParams.courseId }
    })
    fileList.value = res || []

    const hasIndexing = fileList.value.some(file => file.status === 1)
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
    await request.post('/knowledge/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    fetchFiles(true)
    ElMessage.success(`文件 ${options.file.name} 上传成功`)
  } catch (error: any) {
    ElMessage.error(`文件 ${options.file.name} 上传失败: ${error.message || '未知错误'}`)
  } finally {
    uploadingCount.value--
  }
}

const handleDelete = async (row: any) => {
  try {
    await request.delete(`/knowledge/${row.id}`)
    ElMessage.success('删除成功')
    fetchFiles()
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
}

const handleCourseChange = () => {
  fetchFiles()
}

const handleSearch = () => { }

const refreshData = () => {
  fetchFiles()
}

const startPolling = () => {
  if (pollingTimer.value) return
  pollingTimer.value = setInterval(() => {
    fetchFiles(true)
  }, 5000)
}

const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
}

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
  return 'Document'
}

const getFileIconColor = (fileName: string) => {
  if (fileName.endsWith('.pdf')) return 'text-red-500'
  if (fileName.endsWith('.doc') || fileName.endsWith('.docx')) return 'text-blue-500'
  if (fileName.endsWith('.md')) return 'text-gray-700'
  return 'text-gray-500'
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
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Header */
.header-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (min-width: 768px) {
  .header-content {
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }
}

.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-box {
  padding: 8px;
  background-color: #e0e7ff;
  border-radius: 8px;
  color: #4f46e5;
  display: flex;
}

.main-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
  line-height: 1.2;
}

.sub-title {
  font-size: 12px;
  color: #6b7280;
  margin: 4px 0 0 0;
}

/* Filter Section */
.filter-section {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.filter-select {
  width: 100%;
}

.filter-input {
  width: 100%;
}

@media (min-width: 768px) {
  .filter-select {
    width: 240px;
  }
  .filter-input {
    width: 208px;
  }
}

/* Grid Layout */
.page-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

@media (min-width: 1024px) {
  .page-grid {
    grid-template-columns: 1fr 3fr;
  }
}

/* Upload Area */
.upload-card {
  height: 100%;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  min-height: 300px;
  display: flex;
  flex-direction: column;
}

.upload-wrapper {
  height: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

:deep(.custom-upload-dragger .el-upload-dragger) {
  width: 100%;
  height: 100%;
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px dashed #e5e7eb;
  background-color: #f9fafb;
  border-radius: 8px;
  transition: all 0.3s;
}

:deep(.custom-upload-dragger .el-upload-dragger:hover) {
  border-color: #6366f1;
  background-color: #eef2ff;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 0;
}

.upload-icon {
  font-size: 60px;
  color: #9ca3af;
  margin-bottom: 16px;
}

.upload-text {
  font-size: 16px;
  color: #4b5563;
}

.highlight {
  color: #6366f1;
  font-weight: 600;
  font-style: normal;
}

.upload-subtext {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 8px;
}

.upload-tip {
  margin-top: 16px;
  text-align: center;
  min-height: 24px;
}

.warning-tip {
  color: #f97316;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.info-tip {
  color: #9ca3af;
  font-size: 12px;
}

.upload-progress {
  margin-top: 16px;
  padding: 0 16px;
  text-align: center;
}

/* List Area */
.list-card {
  height: 100%;
  min-height: 500px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}

:deep(.el-card__body) {
  flex: 1;
  padding: 0; /* Remove default padding for full-width table */
  display: flex;
  flex-direction: column;
}

.file-table {
  flex: 1;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.file-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background-color: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.file-details {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.file-name {
  font-weight: 500;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.file-id {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
  margin-top: 2px;
}

.time-text {
  font-size: 14px;
  color: #6b7280;
}

.status-tag-content {
  display: flex;
  align-items: center;
  gap: 4px;
}

.empty-state {
  padding: 48px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

/* Text Colors */
.text-red-500 { color: #ef4444; }
.text-blue-500 { color: #3b82f6; }
.text-gray-700 { color: #374151; }
.text-gray-500 { color: #6b7280; }
</style>