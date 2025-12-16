<template>
  <div class="app-container">
    <!-- 顶部操作栏 -->
    <el-card shadow="never" class="filter-card">
      <div class="header-box">
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
        <el-table-column prop="credits" label="学分" width="80" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" type="info">{{ row.credits }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="授课教师" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openMemberDrawer(row, 2)">
              {{ row.teacherCount || 0 }} 人
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="选课学生" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="success" @click="openMemberDrawer(row, 1)">
              {{ row.studentCount || 0 }} 人
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="所属院系" min-width="120" show-overflow-tooltip>
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
          <div class="cover-upload-wrapper">
            <el-upload
                class="cover-uploader"
                action="/api/file/upload/image"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="handleUploadSuccess"
                :before-upload="beforeUpload"
                accept="image/*"
            >
              <div v-if="form.coverImg" class="upload-preview">
                <img :src="form.coverImg" class="preview-img" alt="预览" />
                <div class="preview-mask">
                  <el-icon><Plus /></el-icon>
                  <span>更换封面</span>
                </div>
              </div>
              <div v-else class="upload-placeholder">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <span>点击上传封面</span>
              </div>
            </el-upload>
            <el-input v-model="form.coverImg" placeholder="或输入图片URL" clearable class="url-input">
              <template #prefix><el-icon><Link /></el-icon></template>
            </el-input>
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

    <!-- 成员管理抽屉 -->
    <el-drawer
        v-model="memberDrawer.visible"
        :title="memberDrawer.title"
        size="500px"
        direction="rtl"
    >
      <div class="member-drawer-content">
        <!-- 添加成员区域 -->
        <div class="add-member-section">
          <el-tree-select
              v-if="memberDrawer.role === 2"
              v-model="memberDrawer.filterDeptId"
              :data="deptOptions"
              :props="{ label: 'deptName', value: 'id', children: 'children' }"
              placeholder="按部门筛选"
              clearable
              check-strictly
              class="dept-filter-select"
              @change="handleDeptFilterChange"
          />
          <el-select
              v-model="memberDrawer.selectedUserId"
              filterable
              :remote="memberDrawer.role !== 2 || !memberDrawer.filterDeptId"
              reserve-keyword
              :placeholder="memberDrawer.role === 2 ? (memberDrawer.filterDeptId ? '从列表选择教师' : '先选择部门或搜索') : '输入用户名或姓名搜索'"
              :remote-method="searchUsers"
              :loading="memberDrawer.searchLoading"
              class="user-select"
          >
            <el-option
                v-for="user in memberDrawer.userOptions"
                :key="user.id"
                :label="`${user.realName} (${user.username})`"
                :value="user.id"
            >
              <div class="user-option">
                <span class="user-option-name">{{ user.realName }}</span>
                <span class="user-option-username">{{ user.username }}</span>
              </div>
            </el-option>
          </el-select>
          <el-button type="primary" @click="handleAddMember" :disabled="!memberDrawer.selectedUserId">
            添加
          </el-button>
        </div>
        <!-- 批量导入班级（仅学生） -->
        <div v-if="memberDrawer.role === 1" class="import-dept-section">
          <el-tree-select
              v-model="memberDrawer.importDeptId"
              :data="deptOptions"
              :props="{ label: 'deptName', value: 'id', children: 'children' }"
              placeholder="选择班级批量导入学生"
              check-strictly
              class="dept-select"
          />
          <el-button type="success" @click="handleImportDept" :disabled="!memberDrawer.importDeptId">
            批量导入
          </el-button>
        </div>
        <!-- 成员列表 -->
        <el-table :data="memberDrawer.members" v-loading="memberDrawer.loading" class="member-table">
          <el-table-column label="用户信息" min-width="180">
            <template #default="{ row }">
              <div class="user-info">
                <el-avatar :size="32" :src="row.avatar">{{ row.realName?.charAt(0) }}</el-avatar>
                <div class="user-detail">
                  <span class="real-name">{{ row.realName }}</span>
                  <span class="username">{{ row.username }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="加入时间" width="140">
            <template #default="{ row }">
              <span class="time-text">{{ formatTime(row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ row }">
              <el-popconfirm title="确定移除该成员？" @confirm="handleRemoveMember(row)">
                <template #reference>
                  <el-button type="danger" link size="small">移除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="memberDrawer.members.length === 0 && !memberDrawer.loading" description="暂无成员" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Picture, Link, School, Edit, Delete, Refresh, User } from '@element-plus/icons-vue'
import request from '@/utils/request'

// 上传请求头（携带Token）
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

// 数据定义
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()

const courseList = ref([])
const total = ref(0)
const deptOptions = ref<any[]>([])

// 成员管理抽屉
const memberDrawer = reactive({
  visible: false,
  title: '',
  courseId: undefined as number | undefined,
  role: 1 as number, // 1-学生, 2-教师
  members: [] as any[],
  loading: false,
  selectedUserId: undefined as number | undefined,
  userOptions: [] as any[],
  searchLoading: false,
  importDeptId: undefined as number | undefined,
  filterDeptId: undefined as number | undefined
})

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

// ===================== 成员管理 =====================

// 打开成员管理抽屉
const openMemberDrawer = (row: any, role: number) => {
  memberDrawer.courseId = row.id
  memberDrawer.role = role
  memberDrawer.title = `${row.courseName} - ${role === 2 ? '授课教师' : '选课学生'}`
  memberDrawer.selectedUserId = undefined
  memberDrawer.userOptions = []
  memberDrawer.importDeptId = undefined
  memberDrawer.visible = true
  fetchCourseMembers()
}

// 获取课程成员列表
const fetchCourseMembers = async () => {
  memberDrawer.loading = true
  try {
    const res: any = await request.get('/course/user/list', {
      params: {
        courseId: memberDrawer.courseId,
        role: memberDrawer.role
      }
    })
    memberDrawer.members = res.records || []
  } catch (error) {
    console.error(error)
  } finally {
    memberDrawer.loading = false
  }
}

// 搜索用户
const searchUsers = async (query: string) => {
  if (!query) {
    memberDrawer.userOptions = []
    return
  }
  memberDrawer.searchLoading = true
  try {
    // 根据当前抽屉角色过滤：教师只搜教师(role=2)，学生只搜学生(role=1)
    const res: any = await request.get('/admin/user/list', {
      params: { 
        username: query, 
        size: 20,
        role: memberDrawer.role // 1-学生, 2-教师
      }
    })
    // 过滤掉已在课程中的成员
    const existingIds = memberDrawer.members.map((m: any) => m.userId)
    memberDrawer.userOptions = (res.records || []).filter((u: any) => !existingIds.includes(u.id))
  } catch (error) {
    console.error(error)
  } finally {
    memberDrawer.searchLoading = false
  }
}

// 添加成员
const handleAddMember = async () => {
  if (!memberDrawer.selectedUserId || !memberDrawer.courseId) return
  try {
    await request.post('/course/user/add', {
      courseId: memberDrawer.courseId,
      userId: memberDrawer.selectedUserId,
      role: memberDrawer.role
    })
    ElMessage.success('添加成功')
    memberDrawer.selectedUserId = undefined
    memberDrawer.userOptions = []
    fetchCourseMembers()
    fetchData() // 刷新课程列表以更新统计
  } catch (error) {
    // handled
  }
}

// 批量导入班级学生
const handleImportDept = async () => {
  if (!memberDrawer.importDeptId || !memberDrawer.courseId) return
  try {
    const res: any = await request.post('/course/user/import-dept', {
      courseId: memberDrawer.courseId,
      deptId: memberDrawer.importDeptId
    })
    ElMessage.success(res || '导入完成')
    memberDrawer.importDeptId = undefined
    fetchCourseMembers()
    fetchData()
  } catch (error) {
    // handled
  }
}

// 移除成员
const handleRemoveMember = async (row: any) => {
  try {
    await request.delete('/course/user/remove', {
      params: {
        courseId: memberDrawer.courseId,
        userId: row.userId
      }
    })
    ElMessage.success('移除成功')
    fetchCourseMembers()
    fetchData()
  } catch (error) {
    // handled
  }
}

// ===================== 封面上传 =====================

// 上传成功回调
const handleUploadSuccess = (response: any) => {
  if (response.code === 200 && response.data?.url) {
    form.coverImg = response.data.url
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 上传前校验
const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB')
    return false
  }
  return true
}

// ===================== 教师按部门筛选 =====================

// 部门筛选变化
const handleDeptFilterChange = async () => {
  memberDrawer.selectedUserId = undefined
  if (!memberDrawer.filterDeptId) {
    memberDrawer.userOptions = []
    return
  }
  memberDrawer.searchLoading = true
  try {
    const res: any = await request.get('/admin/user/list', {
      params: {
        deptId: memberDrawer.filterDeptId,
        role: 2,
        size: 50
      }
    })
    const existingIds = memberDrawer.members.map((m: any) => m.userId)
    memberDrawer.userOptions = (res.records || []).filter((u: any) => !existingIds.includes(u.id))
  } catch (error) {
    console.error(error)
  } finally {
    memberDrawer.searchLoading = false
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

/* 成员管理抽屉样式 */
.member-drawer-content {
  padding: 0 10px;
}

.add-member-section,
.import-dept-section {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.user-select,
.dept-select {
  flex: 1;
}

.member-table {
  margin-top: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-detail {
  display: flex;
  flex-direction: column;
}

.real-name {
  font-weight: 500;
  color: #303133;
}

.username {
  font-size: 12px;
  color: #909399;
}

/* 封面上传样式 */
.cover-upload-wrapper {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.cover-uploader {
  width: 120px;
  height: 120px;
}

.upload-preview,
.upload-placeholder {
  width: 120px;
  height: 120px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

.upload-preview:hover,
.upload-placeholder:hover {
  border-color: #409eff;
}

.upload-preview .preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
}

.upload-preview:hover .preview-mask {
  opacity: 1;
}

.upload-placeholder {
  background: #fafafa;
  color: #8c939d;
}

.upload-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.url-input {
  width: 100%;
}

.dept-filter-select {
  width: 140px;
  flex-shrink: 0;
}

/* 用户选项样式 */
.user-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.user-option-name {
  font-weight: 500;
  color: #303133;
}

.user-option-username {
  font-size: 12px;
  color: #909399;
}
</style>