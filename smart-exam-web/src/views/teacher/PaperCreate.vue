<template>
  <!-- 外层容器：使用 Flex + 内联样式确保布局稳健，防止样式编译错误导致白屏 -->
  <div
      class="app-container"
      style="display: flex; flex-direction: column; height: calc(100vh - 84px); padding: 24px; background-color: #f9fafb; box-sizing: border-box; overflow: hidden;"
  >
    <!-- 顶部筛选栏 -->
    <div style="flex-shrink: 0; background: #fff; padding: 16px 20px; border-radius: 8px; border: 1px solid #e5e7eb; display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; box-shadow: 0 1px 2px rgba(0,0,0,0.05);">
      <div style="display: flex; align-items: center; gap: 12px;">
        <div style="background-color: #e0e7ff; padding: 8px; border-radius: 8px; display: flex; align-items: center; justify-content: center;">
          <el-icon size="20" color="#4f46e5"><DocumentCopy /></el-icon>
        </div>
        <div>
          <div style="font-weight: bold; font-size: 16px; color: #1f2937;">试卷管理</div>
          <div style="font-size: 12px; color: #6b7280;">查看、预览及管理所有试卷</div>
        </div>
      </div>

      <div style="display: flex; gap: 12px;">
        <el-select v-model="queryParams.courseId" placeholder="按课程筛选" clearable style="width: 180px;" @change="handleSearch">
          <el-option v-for="item in courseOptions" :key="item.id" :label="item.courseName" :value="item.id" />
        </el-select>
        <el-input v-model="queryParams.title" placeholder="搜索试卷标题" prefix-icon="Search" clearable style="width: 240px;" @keyup.enter="handleSearch" @clear="handleSearch" />
        <!-- 跳转到智能组卷页面 -->
        <el-button type="primary" color="#4f46e5" :icon="Plus" @click="$router.push('/teacher/paper-create')">智能组卷</el-button>
      </div>
    </div>

    <!-- 表格区域 -->
    <div style="flex: 1; overflow: hidden; background: #fff; border-radius: 8px; border: 1px solid #e5e7eb; display: flex; flex-direction: column;">
      <el-table
          v-loading="loading"
          :data="paperList"
          style="width: 100%; flex: 1;"
          height="100%"
          :header-cell-style="{ background: '#f9fafb', color: '#374151', fontWeight: '600' }"
          stripe
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="title" label="试卷标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="font-weight: 500; color: #1f2937;">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="所属课程" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag type="info" effect="plain" style="border: 0; background-color: #f3f4f6; color: #4b5563;">
              {{ getCourseName(row.courseId) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总分 / 及格" width="140" align="center">
          <template #default="{ row }">
            <span style="font-weight: bold; color: #111827;">{{ row.totalScore }}</span>
            <span style="color: #9ca3af; margin: 0 4px;">/</span>
            <span style="color: #6b7280;">{{ row.passScore }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长" width="100" align="center">
          <template #default="{ row }">{{ row.duration }} 分钟</template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getDifficultyType(row.difficulty)" effect="light">{{ getDifficultyLabel(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" align="center">
          <template #default="{ row }">
            <span style="font-size: 12px; color: #6b7280;">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="Timer" @click="handlePublish(row)">发布</el-button>
            <el-divider direction="vertical" />
            <el-button type="primary" link icon="View" @click="handlePreview(row)">预览</el-button>
            <el-divider direction="vertical" />
            <el-popconfirm title="确定删除该试卷吗？" @confirm="handleDelete(row)" width="200">
              <template #reference>
                <el-button type="danger" link icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div style="padding: 12px 20px; border-top: 1px solid #f3f4f6; display: flex; justify-content: flex-end;">
        <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @size-change="fetchData"
            @current-change="fetchData"
        />
      </div>
    </div>

    <!-- 简易预览弹窗 -->
    <el-dialog v-model="previewVisible" title="试卷预览" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="试卷ID">{{ currentPaper.id }}</el-descriptions-item>
        <el-descriptions-item label="试卷标题">{{ currentPaper.title }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ currentPaper.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="及格分">{{ currentPaper.passScore }}</el-descriptions-item>
        <el-descriptions-item label="难度">{{ getDifficultyLabel(currentPaper.difficulty) }}</el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 20px; text-align: center; color: #9ca3af; font-size: 13px;">
        ( 完整预览请在“考试发布”或“答题”端查看 )
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
  courseId: undefined,
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

const handlePublish = (row: any) => {
  router.push({ path: '/teacher/exam-publish', query: { paperId: row.id, paperTitle: row.title } })
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
  } catch (error) { }
}

const getCourseName = (id: number) => {
  const c = courseOptions.value.find(i => i.id === id)
  return c ? c.courseName : `课程ID:${id}`
}

const getDifficultyLabel = (diff: number) => ({ 1: '简单', 2: '中等', 3: '困难' } as any)[diff] || '未知'
const getDifficultyType = (diff: number) => ({ 1: 'success', 2: 'warning', 3: 'danger' } as any)[diff] || 'info'
const formatTime = (time: string) => time ? time.replace('T', ' ').substring(0, 16) : '-'
</script>