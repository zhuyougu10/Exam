<template>
  <div class="app-container">
    <!-- 顶部操作栏 -->
    <el-card shadow="never" class="filter-card">
      <div class="header-box">
        <!-- 左侧筛选区：使用 el-form inline 模式，保证在一行 -->
        <div class="header-left">
          <el-form :inline="true" :model="queryParams" class="filter-form">
            <el-form-item label="课程搜索" class="form-item-no-margin">
              <el-input
                  v-model="queryParams.courseName"
                  placeholder="课程名称 / 代码"
                  clearable
                  prefix-icon="Search"
                  class="filter-input"
                  @keyup.enter="handleSearch"
                  @clear="handleSearch"
              />
            </el-form-item>
            <el-form-item label="所属院系" class="form-item-no-margin">
              <!-- 修复点1：添加 value: 'id' 配置 -->
              <el-tree-select
                  v-model="queryParams.deptId"
                  :data="deptOptions"
                  :props="{ label: 'deptName', value: 'id', children: 'children' }"
                  placeholder="请选择院系"
                  clearable
                  check-strictly
                  class="filter-select"
                  @change="handleSearch"
              />
            </el-form-item>
            <el-form-item class="form-item-no-margin">
              <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 右侧操作区 -->
        <div class="header-right">
          <el-button type="success" icon="Plus" @click="handleAdd">新建课程</el-button>
        </div>
      </div>
    </el-card>

    <!-- 课程数据表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
          v-loading="loading"
          :data="courseList"
          style="width: 100%"
          :header-cell-style="{ background: '#f8f9fa', color: '#333', fontWeight: 'bold' }"
          stripe
      >
        <el-table-column type="index" label="#" width="60" align="center" />

        <el-table-column label="课程信息" min-width="260">
          <template #default="{ row }">
            <div class="course-info">
              <div class="course-cover">
                <el-image
                    class="cover-img"
                    :src="row.coverImg || 'https://via.placeholder.com/100x100?text=Course'"
                    :preview-src-list="[row.coverImg]"
                    fit="cover"
                >
                  <template #error>
                    <div class="image-slot">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
              </div>
              <div class="course-detail">
                <div class="course-name" :title="row.courseName">{{ row.courseName }}</div>
                <div class="course-code">{{ row.courseCode }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="credits" label="学分" width="100" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" type="info">{{ row.credits }} 学分</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="所属院系" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="dept-text">
              <el-icon class="icon"><School /></el-icon>
              {{ getDeptName(row.deptId) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                inline-prompt
                active-text="启用"
                inactive-text="禁用"
                @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>

        <el-table-column label="创建时间" prop="createTime" width="160" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-popconfirm title="确定要删除该课程吗？" width="220" @confirm="handleDelete(row)">
              <template #reference>
                <el-button type="danger" link icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 (Fixed) -->
      <div class="pagination-box">
        <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            background
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
        :title="dialogTitle"
        v-model="dialogVisible"
        width="600px"
        class="custom-dialog"
        align-center
        destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="85px" class="dialog-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="form.courseName" placeholder="例如：高级软件工程" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程代码" prop="courseCode">
              <el-input v-model="form.courseCode" placeholder="例如：CS2024" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属院系" prop="deptId">
              <!-- 修复点2：添加 value: 'id' 配置 -->
              <el-tree-select
                  v-model="form.deptId"
                  :data="deptOptions"
                  :props="{ label: 'deptName', value: 'id', children: 'children' }"
                  placeholder="请选择院系"
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

        <el-form-item label="封面图片" prop="coverImg">
          <el-input v-model="form.coverImg" placeholder="输入图片 URL 地址">
            <template #prefix><el-icon><Link /></el-icon></template>
          </el-input>
          <div v-if="form.coverImg" class="preview-box">
            <img :src="form.coverImg" class="preview-img" alt="预览" />
          </div>
        </el-form-item>

        <el-form-item label="课程简介" prop="description">
          <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              placeholder="请输入课程简要描述..."
              resize="none"
          />
        </el-form-item>

        <el-form-item label="初始状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">立即启用</el-radio>
            <el-radio :value="0">暂不启用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Picture, Link, School, Edit, Delete, Refresh } from '@element-plus/icons-vue'
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

// 分页处理方法 (Fix)
const handleSizeChange = (val: number) => {
  queryParams.size = val
  queryParams.page = 1 // 重置到第一页
  fetchData()
}

const handleCurrentChange = (val: number) => {
  queryParams.page = val
  fetchData()
}

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

const formatTime = (time: string) => {
  return time ? time.replace('T', ' ').substring(0, 16) : '-'
}

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

const handleAdd = () => {
  dialogTitle.value = '新建课程'
  Object.assign(form, initialForm)
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑课程'
  Object.assign(form, row)
  dialogVisible.value = true
}

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

const handleStatusChange = async (row: any, newVal: any) => {
  try {
    await request.put(`/admin/course/${row.id}`, { id: row.id, status: newVal })
    ElMessage.success('状态已更新')
  } catch (error) {
    row.status = newVal === 1 ? 0 : 1
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

/* 顶部操作栏样式 */
.filter-card {
  margin-bottom: 20px;
}
:deep(.el-card__body) {
  padding: 15px 20px;
}
.header-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap; /* 允许小屏幕换行 */
  gap: 15px;
}
.header-left .el-form-item {
  margin-bottom: 0; /* 移除表单项默认底边距 */
  margin-right: 15px;
}
/* 强制指定输入框宽度，防止被挤压或过长 */
.filter-input {
  width: 220px;
}
.filter-select {
  width: 200px;
}

/* 课程信息展示 */
.course-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.course-cover {
  width: 50px;
  height: 50px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #eee;
  flex-shrink: 0;
}
.cover-img {
  width: 100%;
  height: 100%;
}
.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
}
.course-detail {
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}
.course-name {
  font-weight: 600;
  color: #333;
  font-size: 14px;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.course-code {
  font-size: 12px;
  color: #909399;
  background-color: #f4f4f5;
  padding: 1px 4px;
  border-radius: 4px;
  display: inline-block;
  width: fit-content;
}

/* 表格其他列 */
.dept-text {
  display: flex;
  align-items: center;
  color: #606266;
}
.dept-text .icon {
  margin-right: 6px;
  color: #909399;
}
.time-text {
  font-size: 12px;
  color: #909399;
}

/* 分页 */
.pagination-box {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 弹窗样式 */
.w-full {
  width: 100%;
}
.preview-box {
  margin-top: 10px;
  width: 100%;
  height: 120px;
  background-color: #f5f7fa;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}
.preview-img {
  max-height: 100%;
  max-width: 100%;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}
</style>