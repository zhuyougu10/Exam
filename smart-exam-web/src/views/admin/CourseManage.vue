<template>
  <div class="app-container p-6">
    <!-- 头部搜索 -->
    <el-card shadow="hover" class="filter-card mb-4">
      <div class="flex flex-wrap gap-4 items-center justify-between">
        <div class="flex gap-3 items-center flex-1">
          <el-input
              v-model="queryParams.courseName"
              placeholder="课程名称/代码"
              class="w-64"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>

          <el-tree-select
              v-model="queryParams.deptId"
              :data="deptOptions"
              :props="{ label: 'deptName', children: 'children' }"
              placeholder="所属院系"
              clearable
              check-strictly
              class="w-48"
              @change="handleSearch"
          />

          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>

        <el-button type="success" @click="handleAdd">
          <el-icon class="mr-1"><Plus /></el-icon> 新建课程
        </el-button>
      </div>
    </el-card>

    <!-- 课程列表表格 -->
    <el-card shadow="hover" class="table-card">
      <el-table
          v-loading="loading"
          :data="courseList"
          stripe
          style="width: 100%"
          :header-cell-style="{ background: '#f8f9fa', color: '#606266', fontWeight: '600' }"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />

        <el-table-column label="课程信息" min-width="240">
          <template #default="{ row }">
            <div class="flex items-center gap-3">
              <el-image
                  class="w-12 h-12 rounded bg-gray-100 object-cover"
                  :src="row.coverImg || 'https://via.placeholder.com/100x100?text=Course'"
                  :preview-src-list="[row.coverImg]"
                  fit="cover"
              >
                <template #error>
                  <div class="flex items-center justify-center w-full h-full bg-gray-200 text-gray-400">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div>
                <div class="font-medium text-base text-gray-800">{{ row.courseName }}</div>
                <div class="text-xs text-gray-500 mt-1">代码: {{ row.courseCode }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="credits" label="学分" width="80" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" type="warning">{{ row.credits }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="所属院系" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ getDeptName(row.deptId) }}
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                active-color="#13ce66"
                inactive-color="#ff4949"
                @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>

        <el-table-column label="创建时间" prop="createTime" width="180" align="center">
          <template #default="{ row }">
            <span class="text-gray-500 text-sm">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-popconfirm title="确定要删除该课程吗？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger" icon="Delete">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
        :title="dialogTitle"
        v-model="dialogVisible"
        width="600px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="pr-4">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="form.courseName" placeholder="请输入课程名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程代码" prop="courseCode">
              <el-input v-model="form.courseCode" placeholder="如 CS101" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属院系" prop="deptId">
              <el-tree-select
                  v-model="form.deptId"
                  :data="deptOptions"
                  :props="{ label: 'deptName', children: 'children' }"
                  placeholder="请选择"
                  check-strictly
                  class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学分" prop="credits">
              <el-input-number v-model="form.credits" :precision="1" :step="0.5" :min="0" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="封面链接" prop="coverImg">
          <el-input v-model="form.coverImg" placeholder="输入图片URL地址">
            <template #prefix><el-icon><Link /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item label="课程简介" prop="description">
          <el-input v-model="form.description" type="textarea" rows="3" placeholder="请输入课程简要描述..." />
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Picture, Link } from '@element-plus/icons-vue'
import request from '@/utils/request'

// 数据定义
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()

const courseList = ref([])
const total = ref(0)
const deptOptions = ref<any[]>([])

const queryParams = reactive({
  page: 1,
  size: 10,
  courseName: '',
  deptId: undefined as number | undefined
})

const initialForm = {
  id: undefined,
  courseName: '',
  courseCode: '',
  deptId: undefined,
  credits: 2.0,
  coverImg: '',
  description: '',
  status: 1
}

const form = reactive({ ...initialForm })

const rules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  courseCode: [{ required: true, message: '请输入课程代码', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择所属院系', trigger: 'change' }],
  credits: [{ required: true, message: '请输入学分', trigger: 'blur' }]
}

// 初始化
onMounted(() => {
  fetchDepts()
  fetchData()
})

// 获取部门树
const fetchDepts = async () => {
  try {
    const res = await request.get('/admin/dept/tree')
    deptOptions.value = res as any
  } catch (error) {
    console.error(error)
  }
}

// 获取课程列表
const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/admin/course/list', { params: queryParams })
    courseList.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

const resetQuery = () => {
  queryParams.courseName = ''
  queryParams.deptId = undefined
  handleSearch()
}

// 辅助方法：格式化时间
const formatTime = (time: string) => {
  return time ? time.replace('T', ' ').substring(0, 16) : '-'
}

// 辅助方法：获取部门名称（简单的递归查找）
const getDeptName = (deptId: number) => {
  if (!deptId) return '-'
  const findName = (list: any[]): string | null => {
    for (const item of list) {
      if (item.id === deptId) return item.deptName
      if (item.children) {
        const name = findName(item.children)
        if (name) return name
      }
    }
    return null
  }
  return findName(deptOptions.value) || `未知部门(${deptId})`
}

// 操作：新增
const handleAdd = () => {
  dialogTitle.value = '新建课程'
  Object.assign(form, initialForm)
  dialogVisible.value = true
}

// 操作：编辑
const handleEdit = (row: any) => {
  dialogTitle.value = '编辑课程'
  Object.assign(form, row)
  dialogVisible.value = true
}

// 操作：提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (form.id) {
          await request.put(`/admin/course/${form.id}`, form)
          ElMessage.success('更新成功')
        } else {
          await request.post('/admin/course', form)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        fetchData()
      } catch (error) {
        // error handled by interceptor
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 操作：删除
const handleDelete = async (row: any) => {
  try {
    await request.delete(`/admin/course/${row.id}`)
    ElMessage.success('删除成功')
    if (courseList.value.length === 1 && queryParams.page > 1) {
      queryParams.page--
    }
    fetchData()
  } catch (error) {
    // error handled
  }
}

// 操作：状态变更
const handleStatusChange = async (row: any, newVal: any) => {
  try {
    // 仅发送状态更新字段
    await request.put(`/admin/course/${row.id}`, { id: row.id, status: newVal })
    ElMessage.success('状态已更新')
  } catch (error) {
    row.status = newVal === 1 ? 0 : 1 // 恢复原状
  }
}
</script>

<style scoped lang="scss">
.app-container {
  min-height: calc(100vh - 84px);
}
.filter-card {
  :deep(.el-card__body) {
    padding: 12px 20px;
  }
}
</style>