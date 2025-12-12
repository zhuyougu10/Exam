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
                :label="item.title"
                :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="考试标题" prop="title">
          <el-input v-model="form.title" placeholder="例如：2025春季期末考试" />
        </el-form-item>

        <el-form-item label="考试时间" prop="timeRange">
          <!-- 使用 ElConfigProvider 局部配置中文 -->
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
          <!-- 修改点：移除 check-strictly 实现级联选择，添加 node-key 修复回显 -->
          <el-tree-select
              v-model="form.targetDeptIds"
              :data="deptTree"
              :props="{ label: 'deptName', value: 'id', children: 'children' }"
              multiple
              show-checkbox
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择参与考试的班级"
              node-key="id"
              class="w-full"
          />
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
const deptMap = ref<Record<number, any>>({}) // 用于快速查找部门信息
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
  password: ''
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
    // 即使是从路由跳转过来，也执行一次 handleAdd 的初始化逻辑以填充时间
    handleAdd()
    // 然后覆盖 ID 和 Title
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
    // 构建部门 Map，方便后续过滤
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

const onPaperSelect = (id: number) => {
  const paper = paperOptions.value.find(p => p.id === id)
  if (paper && !form.title) {
    form.title = paper.title
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      // 数据清洗：过滤掉非班级（category != 3）的 ID
      // el-tree 在非严格模式下，选中父节点会将父节点 ID 也包含在 v-model 中
      // 但后端只需要具体的班级 ID 才能正确关联学生
      const classIds = form.targetDeptIds.filter(id => {
        const node = deptMap.value[id]
        // 假设 category: 1=学院, 2=系/专业, 3=班级
        return node && node.category === 3
      })

      if (classIds.length === 0) {
        ElMessage.warning('请至少选择一个有效的班级（不包含学院或系）')
        return
      }

      submitLoading.value = true
      try {
        const payload = {
          ...form,
          targetDeptIds: classIds, // 使用过滤后的 ID 列表
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
  // 获取当前时间，并格式化
  const now = dayjs()
  // 默认考试时长 2小时
  const endTime = now.add(2, 'hour')

  Object.assign(form, {
    paperId: undefined,
    title: '',
    // 自动填充时间范围: [当前时间, 当前时间+2小时]
    timeRange: [now.format('YYYY-MM-DD HH:mm:ss'), endTime.format('YYYY-MM-DD HH:mm:ss')],
    targetDeptIds: [],
    limitCount: 1,
    password: ''
  })
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