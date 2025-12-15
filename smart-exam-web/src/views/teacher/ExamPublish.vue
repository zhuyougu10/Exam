<template>
  <div class="app-container p-6 bg-gray-50 min-h-[calc(100vh-64px)]">
    <!-- 顶部操作栏 -->
    <el-card shadow="never" class="border-0 rounded-xl mb-4">
      <div class="flex justify-between items-center">
        <div class="flex items-center gap-3">
          <div class="p-2 bg-green-100 rounded-lg text-green-600">
            <el-icon size="20"><Timer /></el-icon>
          </div>
          <div>
            <h2 class="text-lg font-bold text-gray-800">考试发布管理</h2>
            <p class="text-xs text-gray-500">管理考试场次，监控考试状态</p>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <el-button :icon="Refresh" circle @click="fetchData" />
          <el-button type="primary" icon="Plus" @click="handleAdd">发布新考试</el-button>
        </div>
      </div>
    </el-card>

    <!-- 发布列表 -->
    <el-card shadow="never" class="border-0 rounded-xl table-card">
      <el-table
          v-loading="loading"
          :data="publishList"
          stripe
          style="width: 100%"
          :header-cell-style="{ background: '#f9fafb', color: '#374151', fontWeight: '600' }"
      >
        <el-table-column prop="title" label="考试名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-medium text-gray-800">{{ row.title }}</span>
          </template>
        </el-table-column>

        <el-table-column label="关联试卷" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-gray-600 flex items-center gap-1">
              <el-icon><Document /></el-icon>
              {{ getPaperTitle(row.paperId) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="考试时间" width="320" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2 text-sm">
              <span class="text-gray-500">{{ formatTime(row.startTime) }}</span>
              <el-icon class="text-gray-300"><Right /></el-icon>
              <span class="text-gray-500">{{ formatTime(row.endTime) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="参考对象" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-gray-600 truncate">{{ formatTargetDepts(row.targetDeptIds) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)" :class="{ 'pulse-tag': isExamRunning(row) }" effect="dark" size="small" round>
              {{ getStatusLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="DataAnalysis" @click="handleAnalysis(row)">
              查看数据
            </el-button>
            <el-divider direction="vertical" />
            <el-popconfirm title="确定撤销该场考试吗？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button type="danger" link icon="Delete">撤销</el-button>
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

    <!-- 发布考试弹窗 -->
    <el-dialog
        v-model="dialogVisible"
        title="发布考试"
        width="600px"
        align-center
        destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="py-2">
        <el-form-item label="选择试卷" prop="paperId">
          <el-select
              v-model="form.paperId"
              placeholder="请选择试卷"
              filterable
              class="w-full"
              @change="onPaperSelect"
          >
            <el-option
                v-for="item in paperOptions"
                :key="item.id"
                :label="item.title + ' (时长:' + item.duration + '分钟)'"
                :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="考试标题" prop="title">
          <el-input v-model="form.title" placeholder="例如：2025春季期末考试" />
        </el-form-item>

        <el-form-item label="考试时间" prop="timeRange">
          <el-config-provider :locale="zhCn">
            <el-date-picker
                v-model="form.timeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                class="w-full"
            />
          </el-config-provider>
        </el-form-item>

        <el-form-item label="参考班级" prop="targetDeptIds">
          <el-select
              v-model="form.targetDeptIds"
              multiple
              collapse-tags
              collapse-tags-tooltip
              :placeholder="form.paperId ? '请选择参与考试的班级' : '请先选择试卷'"
              :disabled="!form.paperId"
              class="w-full"
          >
            <el-option
                v-for="dept in courseDepts"
                :key="dept.id"
                :label="dept.deptName"
                :value="dept.id"
            />
          </el-select>
          <div v-if="form.paperId && courseDepts.length === 0" class="text-xs text-orange-500 mt-1">
            该课程暂无已加入的班级学生，请先在课程管理中添加学生
          </div>
        </el-form-item>

        <!-- 新增: 解析查看权限设置 -->
        <el-form-item label="允许看详情">
          <el-switch
              v-model="form.allowEarlyAnalysis"
              active-text="允许考前查看"
              inactive-text="考后开放"
              :active-value="1"
              :inactive-value="0"
          />
          <div class="text-xs text-gray-400 mt-1">若关闭，学生提交后仅能看到分数，在考试截止时间前<span class="text-red-400 font-bold">无法查看任何题目和解析</span>。</div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="考试限次" prop="limitCount">
              <el-input-number v-model="form.limitCount" :min="1" :max="10" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试密码" prop="password">
              <el-input v-model="form.password" placeholder="选填，留空则无密码" show-password />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            确认发布
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { Timer, Plus, Delete, DataAnalysis, Document, Right, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const publishList = ref([])
const total = ref(0)
const paperOptions = ref<any[]>([])
const deptTree = ref<any[]>([])
const deptMap = ref<Record<number, any>>({})
const courseDepts = ref<any[]>([]) // 课程关联的班级列表
const formRef = ref()

const queryParams = reactive({
  page: 1,
  size: 10
})

const form = reactive({
  paperId: undefined as number | undefined,
  title: '',
  timeRange: [] as string[],
  targetDeptIds: [] as number[],
  limitCount: 1,
  password: '',
  allowEarlyAnalysis: 1 // 默认为1：允许
})

const rules = {
  paperId: [{ required: true, message: '请选择试卷', trigger: 'change' }],
  title: [{ required: true, message: '请输入考试标题', trigger: 'blur' }],
  timeRange: [{ required: true, message: '请选择考试起止时间', trigger: 'change' }],
  targetDeptIds: [{ required: true, message: '请选择参考班级', trigger: 'change' }]
}

onMounted(() => {
  fetchPapers()
  fetchDepts()
  fetchData()
  checkRouteParams()
})

const checkRouteParams = () => {
  const { paperId, paperTitle } = route.query
  if (paperId) {
    handleAdd()
    form.paperId = Number(paperId)
    form.title = paperTitle ? `${paperTitle} - 考试` : ''
  }
}

const fetchPapers = async () => {
  try {
    const res: any = await request.get('/paper/list', { params: { size: 100 } })
    paperOptions.value = res.records
  } catch (error) { console.error(error) }
}

const fetchDepts = async () => {
  try {
    const res: any = await request.get('/admin/dept/tree')
    deptTree.value = res
    buildDeptMap(res)
  } catch (error) { console.error(error) }
}

const buildDeptMap = (nodes: any[]) => {
  nodes.forEach(node => {
    deptMap.value[node.id] = node
    if (node.children && node.children.length > 0) {
      buildDeptMap(node.children)
    }
  })
}

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/exam/publish/list', { params: queryParams })
    publishList.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const onPaperSelect = async (id: number) => {
  const paper = paperOptions.value.find(p => p.id === id)
  if (paper) {
    if (!form.title) {
      form.title = paper.title
    }
    // 根据试卷所属课程加载关联班级
    form.targetDeptIds = []
    if (paper.courseId) {
      await fetchCourseDepts(paper.courseId)
    } else {
      courseDepts.value = []
    }
  }
}

const fetchCourseDepts = async (courseId: number) => {
  try {
    const res: any = await request.get('/course/user/depts', { params: { courseId } })
    courseDepts.value = res || []
    if (courseDepts.value.length === 0) {
      ElMessage.warning('该课程暂无学生，请先在课程管理中添加学生')
    }
  } catch (error) {
    console.error(error)
    courseDepts.value = []
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      // courseDepts 已经是课程关联的班级，直接使用
      if (form.targetDeptIds.length === 0) {
        ElMessage.warning('请至少选择一个班级')
        return
      }

      // 2. 校验考试时长
      // 获取当前选中试卷的 duration (单位:分钟)
      const selectedPaper = paperOptions.value.find(p => p.id === form.paperId)
      if (selectedPaper) {
        const start = dayjs(form.timeRange[0])
        const end = dayjs(form.timeRange[1])
        const diffMinutes = end.diff(start, 'minute')

        if (diffMinutes < selectedPaper.duration) {
          ElMessage.error(`发布时间范围 (${diffMinutes}分钟) 不能小于试卷规定的考试时长 (${selectedPaper.duration}分钟)`)
          return
        }
      }

      submitLoading.value = true
      try {
        const payload = {
          ...form,
          targetDeptIds: form.targetDeptIds,
          startTime: form.timeRange[0],
          endTime: form.timeRange[1]
        }
        await request.post('/exam/publish', payload)
        ElMessage.success('考试发布成功，已通知相关学生')
        dialogVisible.value = false
        fetchData()
      } catch (error) {
        // error
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const handleAdd = () => {
  const now = dayjs()
  const endTime = now.add(2, 'hour')

  Object.assign(form, {
    paperId: undefined,
    title: '',
    timeRange: [now.format('YYYY-MM-DD HH:mm:ss'), endTime.format('YYYY-MM-DD HH:mm:ss')],
    targetDeptIds: [],
    limitCount: 1,
    password: '',
    allowEarlyAnalysis: 1 // 默认开启
  })
  courseDepts.value = [] // 重置课程班级列表
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await request.delete(`/exam/publish/${row.id}`)
    ElMessage.success('撤销成功')
    fetchData()
  } catch (error) {
    // error
  }
}

const handleAnalysis = (row: any) => {
  router.push({ path: '/teacher/analysis', query: { publishId: row.id } })
}

const handleSizeChange = (val: number) => {
  queryParams.size = val
  fetchData()
}

const handleCurrentChange = (val: number) => {
  queryParams.page = val
  fetchData()
}

// 辅助方法
const getPaperTitle = (id: number) => {
  const p = paperOptions.value.find(item => item.id === id)
  return p ? p.title : `试卷ID:${id}`
}

const formatTargetDepts = (jsonStr: string) => {
  try {
    const ids = JSON.parse(jsonStr)
    return `涉及 ${ids.length} 个班级`
  } catch (e) {
    return '-'
  }
}

const formatTime = (time: string) => {
  return time ? time.replace('T', ' ').substring(0, 16) : ''
}

const isExamRunning = (row: any) => {
  const now = dayjs()
  return now.isAfter(row.startTime) && now.isBefore(row.endTime)
}

const getStatusLabel = (row: any) => {
  const now = dayjs()
  if (now.isBefore(row.startTime)) return '未开始'
  if (now.isAfter(row.endTime)) return '已结束'
  return '进行中'
}

const getStatusType = (row: any) => {
  const now = dayjs()
  if (now.isBefore(row.startTime)) return 'info'
  if (now.isAfter(row.endTime)) return 'warning'
  return 'success'
}
</script>

<style scoped>
.pulse-tag {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.7);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(103, 194, 58, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0);
  }
}
</style>