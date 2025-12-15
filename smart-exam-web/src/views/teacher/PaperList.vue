<template>
  <div
      class="app-container"
      style="display: flex; flex-direction: column; height: calc(100vh - 84px); padding: 24px; background-color: #f9fafb; box-sizing: border-box; overflow: hidden;"
  >
    <!-- 外层容器：使用 Flex + 内联样式确保布局稳健，防止样式编译错误导致白屏 -->

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

    <!-- 详细预览弹窗 -->
    <el-dialog v-model="previewVisible" title="试卷预览" width="900px" top="5vh" destroy-on-close>
      <div v-loading="previewLoading" style="max-height: 70vh; overflow-y: auto;">
        <!-- 试卷基本信息 -->
        <div style="background: #f9fafb; padding: 16px; border-radius: 8px; margin-bottom: 16px;">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <h3 style="margin: 0; font-size: 18px; color: #1f2937;">{{ paperDetail.title }}</h3>
            <div style="display: flex; gap: 12px;">
              <el-tag type="primary">总分: {{ paperDetail.totalScore }}</el-tag>
              <el-tag type="success">及格分: {{ paperDetail.passScore }}</el-tag>
              <el-tag type="warning">时长: {{ paperDetail.duration }}分钟</el-tag>
            </div>
          </div>
        </div>

        <!-- 添加题目按钮 -->
        <div style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight: 600; color: #374151;">题目列表 ({{ paperDetail.questions?.length || 0 }} 题)</span>
          <el-button type="primary" size="small" icon="Plus" @click="showAddQuestionDialog">添加题目</el-button>
        </div>

        <!-- 题目列表 -->
        <div v-if="paperDetail.questions && paperDetail.questions.length > 0" style="display: flex; flex-direction: column; gap: 12px;">
          <div v-for="(q, index) in paperDetail.questions" :key="q.id" 
               style="background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px;">
            <!-- 题目头部 -->
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
              <div style="display: flex; align-items: center; gap: 8px;">
                <span style="background: #4f46e5; color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: bold;">{{ index + 1 }}</span>
                <el-tag size="small" effect="plain">{{ getTypeLabel(q.type) }}</el-tag>
                <el-tag size="small" :type="getDifficultyType(q.difficulty)" effect="light">{{ getDifficultyLabel(q.difficulty) }}</el-tag>
                <span style="color: #ef4444; font-size: 12px; font-weight: bold;">{{ q.score }}分</span>
              </div>
              <el-popconfirm title="确定从试卷中移除该题目？" @confirm="removeQuestion(q.id)">
                <template #reference>
                  <el-button type="danger" size="small" icon="Delete" link>移除</el-button>
                </template>
              </el-popconfirm>
            </div>

            <!-- 题目内容 -->
            <div style="color: #374151; line-height: 1.6; margin-bottom: 12px;">{{ q.content }}</div>

            <!-- 选项 (单选/多选) -->
            <div v-if="[1, 2].includes(q.type) && q.options" style="margin-bottom: 12px;">
              <div v-for="(opt, oIdx) in parseOptions(q.options)" :key="oIdx" 
                   style="display: flex; gap: 8px; padding: 6px 0; color: #4b5563;">
                <span style="width: 24px; height: 24px; border-radius: 50%; border: 1px solid #d1d5db; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: bold; flex-shrink: 0;">
                  {{ String.fromCharCode(65 + oIdx) }}
                </span>
                <span>{{ opt }}</span>
              </div>
            </div>

            <!-- 答案和解析 -->
            <div style="background: #f0fdf4; padding: 12px; border-radius: 6px; border-left: 3px solid #22c55e;">
              <div style="display: flex; gap: 16px; flex-wrap: wrap;">
                <div>
                  <span style="color: #6b7280; font-size: 12px;">正确答案：</span>
                  <span style="color: #16a34a; font-weight: bold;">{{ formatAnswer(q.answer, q.type) }}</span>
                </div>
              </div>
              <div v-if="q.analysis" style="margin-top: 8px; color: #4b5563; font-size: 13px;">
                <span style="color: #6b7280;">解析：</span>{{ q.analysis }}
              </div>
            </div>
          </div>
        </div>

        <el-empty v-else description="暂无题目" />
      </div>
    </el-dialog>

    <!-- 添加题目弹窗 -->
    <el-dialog v-model="addQuestionVisible" title="添加题目到试卷" width="700px" top="10vh">
      <div style="margin-bottom: 16px; display: flex; gap: 12px;">
        <el-select v-model="questionQuery.type" placeholder="题型" clearable style="width: 120px;" @change="fetchQuestions">
          <el-option label="单选" :value="1" />
          <el-option label="多选" :value="2" />
          <el-option label="判断" :value="3" />
          <el-option label="简答" :value="4" />
          <el-option label="填空" :value="5" />
        </el-select>
        <el-input v-model="questionQuery.content" placeholder="搜索题目内容" clearable style="flex: 1;" @keyup.enter="fetchQuestions" />
        <el-button type="primary" @click="fetchQuestions">搜索</el-button>
      </div>

      <el-table v-loading="questionLoading" :data="questionList" height="400px" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="题型" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ getTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="题目内容" show-overflow-tooltip />
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-input-number v-model="row.addScore" :min="0.5" :step="0.5" size="small" style="width: 70px;" placeholder="分值" />
            <el-button type="primary" size="small" icon="Plus" @click="addQuestion(row)" :disabled="!row.addScore" style="margin-left: 8px;">添加</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 12px; display: flex; justify-content: center;">
        <el-pagination small layout="prev, pager, next" :total="questionTotal" 
                       v-model:current-page="questionQuery.page" :page-size="questionQuery.size" @current-change="fetchQuestions" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Plus, Delete, View, Timer, DocumentCopy } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const paperList = ref([])
const total = ref(0)
const courseOptions = ref<any[]>([])
const previewVisible = ref(false)
const currentPaper = ref<any>({})

// 详细预览相关
const previewLoading = ref(false)
const paperDetail = ref<any>({})

// 添加题目相关
const addQuestionVisible = ref(false)
const questionLoading = ref(false)
const questionList = ref<any[]>([])
const questionTotal = ref(0)
const questionQuery = reactive({
  page: 1,
  size: 10,
  type: undefined as number | undefined,
  content: '',
  courseId: undefined as number | undefined
})

const queryParams = reactive({
  page: 1,
  size: 10,
  courseId: undefined,
  title: ''
})

onMounted(async () => {
  await fetchCourses()
  await fetchData()
  
  // 检查是否有自动打开预览的参数
  if (route.query.previewPaperId) {
    const paperId = Number(route.query.previewPaperId)
    const paper = paperList.value.find((p: any) => p.id === paperId)
    if (paper) {
      handlePreview(paper)
    } else {
      // 如果列表中没有，直接加载预览
      currentPaper.value = { id: paperId }
      previewVisible.value = true
      await fetchPaperDetail(paperId)
    }
    // 清除URL参数
    router.replace({ query: {} })
  }
})

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

// 预览试卷详情
const handlePreview = async (row: any) => {
  currentPaper.value = row
  previewVisible.value = true
  await fetchPaperDetail(row.id)
}

const fetchPaperDetail = async (paperId: number) => {
  previewLoading.value = true
  try {
    const res: any = await request.get(`/paper/${paperId}/detail`)
    paperDetail.value = res
  } catch (error) {
    console.error(error)
  } finally {
    previewLoading.value = false
  }
}

// 显示添加题目弹窗
const showAddQuestionDialog = () => {
  questionQuery.courseId = paperDetail.value.courseId
  questionQuery.page = 1
  questionQuery.type = undefined
  questionQuery.content = ''
  addQuestionVisible.value = true
  fetchQuestions()
}

// 获取题目列表
const fetchQuestions = async () => {
  questionLoading.value = true
  try {
    const params: any = {
      page: questionQuery.page,
      size: questionQuery.size,
      courseId: questionQuery.courseId
    }
    if (questionQuery.type) params.type = questionQuery.type
    if (questionQuery.content) params.content = questionQuery.content
    
    const res: any = await request.get('/question/list', { params })
    // 过滤掉已在试卷中的题目，并初始化分值
    const existIds = new Set(paperDetail.value.questions?.map((q: any) => q.id) || [])
    questionList.value = (res.records || [])
      .filter((q: any) => !existIds.has(q.id))
      .map((q: any) => ({ ...q, addScore: 5 }))
    questionTotal.value = res.total
  } catch (error) {
    console.error(error)
  } finally {
    questionLoading.value = false
  }
}

// 添加题目到试卷
const addQuestion = async (row: any) => {
  try {
    await request.post(`/paper/${currentPaper.value.id}/add-question`, {
      questionId: row.id,
      score: row.addScore
    })
    ElMessage.success('添加成功')
    // 刷新预览
    await fetchPaperDetail(currentPaper.value.id)
    // 刷新题目列表（排除已添加的）
    await fetchQuestions()
    // 刷新主列表
    fetchData()
  } catch (error) { }
}

// 从试卷移除题目
const removeQuestion = async (questionId: number) => {
  try {
    await request.delete(`/paper/${currentPaper.value.id}/question/${questionId}`)
    ElMessage.success('移除成功')
    // 刷新预览
    await fetchPaperDetail(currentPaper.value.id)
    // 刷新主列表
    fetchData()
  } catch (error) { }
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

const getTypeLabel = (type: number) => ({ 1: '单选', 2: '多选', 3: '判断', 4: '简答', 5: '填空' } as any)[type] || '未知'
const getDifficultyLabel = (diff: number) => ({ 1: '简单', 2: '中等', 3: '困难' } as any)[diff] || '未知'
const getDifficultyType = (diff: number) => ({ 1: 'success', 2: 'warning', 3: 'danger' } as any)[diff] || 'info'
const formatTime = (time: string) => time ? time.replace('T', ' ').substring(0, 16) : '-'

// 解析选项JSON
const parseOptions = (options: string) => {
  if (!options) return []
  try {
    return JSON.parse(options)
  } catch {
    return []
  }
}

// 格式化答案显示
const formatAnswer = (answer: string, type: number) => {
  if (!answer) return '-'
  if (type === 3) {
    // 判断题
    return answer === '1' || answer === 'true' ? '正确' : '错误'
  }
  if (type === 1 || type === 2) {
    // 单选/多选：将索引转换为字母
    try {
      const indices = answer.split(',').map(Number)
      return indices.map(i => String.fromCharCode(65 + i)).join(', ')
    } catch {
      return answer
    }
  }
  return answer
}
</script>