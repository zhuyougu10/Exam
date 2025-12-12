<template>
  <div class="app-container p-6 bg-gray-50 min-h-[calc(100vh-64px)]">
    <el-card shadow="never" class="border-0 rounded-xl mb-4">
      <div class="flex flex-wrap justify-between items-center gap-4">
        <div class="flex items-center gap-3">
          <div class="p-2 bg-blue-100 rounded-lg text-blue-600">
            <el-icon size="20"><DocumentCopy /></el-icon>
          </div>
          <div>
            <h2 class="text-lg font-bold text-gray-800">试卷管理</h2>
            <p class="text-xs text-gray-500">查看、预览及发布试卷</p>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-3">
          <el-select
              v-model="queryParams.courseId"
              placeholder="按课程筛选"
              clearable
              filterable
              class="w-48"
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
              v-model="queryParams.title"
              placeholder="搜索试卷标题"
              prefix-icon="Search"
              clearable
              class="w-60"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
          />
          <el-button type="primary" icon="Plus" @click="$router.push('/teacher/paper-create')">新建试卷</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="border-0 rounded-xl table-card">
      <el-table
          v-loading="loading"
          :data="paperList"
          stripe
          style="width: 100%"
          :header-cell-style="{ background: '#f9fafb', color: '#374151', fontWeight: '600' }"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />

        <el-table-column prop="title" label="试卷标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-medium text-gray-700">{{ row.title }}</span>
          </template>
        </el-table-column>

        <el-table-column label="所属课程" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag type="info" effect="plain" class="border-0 bg-gray-100 text-gray-600">
              {{ getCourseName(row.courseId) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="总分 / 及格" width="140" align="center">
          <template #default="{ row }">
            <span class="text-gray-900 font-bold">{{ row.totalScore }}</span>
            <span class="text-gray-400 mx-1">/</span>
            <span class="text-gray-500">{{ row.passScore }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="duration" label="时长" width="100" align="center">
          <template #default="{ row }">
            {{ row.duration }} 分钟
          </template>
        </el-table-column>

        <el-table-column prop="difficulty" label="难度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDifficultyType(row.difficulty)" size="small">
              {{ getDifficultyLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="160" align="center">
          <template #default="{ row }">
            <span class="text-xs text-gray-500">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="success" link icon="Timer" @click="handlePublish(row)">
              发布
            </el-button>
            <el-divider direction="vertical" />
            <el-button type="primary" link icon="View" @click="handlePreview(row)">
              预览
            </el-button>
            <el-divider direction="vertical" />
            <el-popconfirm
                title="确定删除该试卷吗？"
                width="200"
                @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button type="danger" link icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex justify-end mt-4">
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

    <el-dialog v-model="previewVisible" title="试卷预览" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="试卷ID">{{ currentPaper.id }}</el-descriptions-item>
        <el-descriptions-item label="试卷标题">{{ currentPaper.title }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ currentPaper.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="及格分">{{ currentPaper.passScore }}</el-descriptions-item>
        <el-descriptions-item label="难度">{{ getDifficultyLabel(currentPaper.difficulty) }}</el-descriptions-item>
      </el-descriptions>
      <div class="mt-4 text-center text-gray-400 text-sm">
        （完整预览功能将在答题界面实现）
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Plus, Delete, View, Timer, DocumentCopy } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const paperList = ref([])
const total = ref(0)
const courseOptions = ref<any[]>([])
const previewVisible = ref(false)
const currentPaper = ref<any>({})

const queryParams = reactive({
  page: 1,
  size: 10,
  courseId: undefined as number | undefined,
  title: ''
})

onMounted(() => {
  fetchCourses()
  fetchData()
})

const fetchCourses = async () => {
  try {
    const res: any = await request.get('/admin/course/list', { params: { size: 100 } })
    courseOptions.value = res.records
  } catch (error) { console.error(error) }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/paper/list', { params: queryParams })
    paperList.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

const handleSizeChange = (val: number) => {
  queryParams.size = val
  fetchData()
}

const handleCurrentChange = (val: number) => {
  queryParams.page = val
  fetchData()
}

const handlePublish = (row: any) => {
  router.push({
    path: '/teacher/exam-publish',
    query: {
      paperId: row.id,
      paperTitle: row.title
    }
  })
}

const handlePreview = (row: any) => {
  currentPaper.value = row
  previewVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await request.delete(`/paper/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    // error handled by interceptor
  }
}

// 辅助方法
const getCourseName = (id: number) => {
  const course = courseOptions.value.find(c => c.id === id)
  return course ? course.courseName : `课程ID:${id}`
}

const getDifficultyLabel = (diff: number) => {
  return { 1: '简单', 2: '中等', 3: '困难' }[diff] || '未知'
}

const getDifficultyType = (diff: number) => {
  return { 1: 'success', 2: 'warning', 3: 'danger' }[diff] || 'info'
}

const formatTime = (time: string) => {
  return time ? time.replace('T', ' ').substring(0, 16) : '-'
}
</script>

<style scoped>
.table-card {
  min-height: 500px;
}
</style>