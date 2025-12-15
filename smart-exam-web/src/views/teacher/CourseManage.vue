<template>
  <div class="course-manage">
    <!-- 页面头部 -->
    <el-card shadow="never" class="header-card">
      <div class="header-content">
        <div class="header-left">
          <div class="header-icon">
            <el-icon size="20"><Reading /></el-icon>
          </div>
          <div>
            <h2 class="header-title">我的课程</h2>
            <p class="header-subtitle">管理已加入的课程，添加学生到课程中</p>
          </div>
        </div>
        <div class="header-right">
          <el-button :icon="Refresh" circle @click="fetchCourses" />
        </div>
      </div>
    </el-card>

    <!-- 课程列表 -->
    <div class="course-grid">
      <el-card
        v-for="course in courses"
        :key="course.id"
        shadow="hover"
        class="course-card"
        :class="{ active: currentCourseId === course.id }"
        @click="selectCourse(course)"
      >
        <div class="course-info">
          <div class="course-name">{{ course.courseName }}</div>
          <div class="course-code">{{ course.courseCode }}</div>
        </div>
        <el-icon class="course-arrow"><ArrowRight /></el-icon>
      </el-card>
      <div v-if="courses.length === 0 && !loading" class="empty-tip">
        暂无已加入的课程
      </div>
    </div>

    <!-- 学生管理区域 -->
    <el-card v-if="currentCourseId" shadow="never" class="member-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><User /></el-icon>
            {{ currentCourse?.courseName }} - 学生列表
          </span>
          <div class="card-actions">
            <el-select
              v-model="selectedSemester"
              placeholder="选择学期"
              size="small"
              style="width: 150px; margin-right: 8px"
              @change="fetchStudents"
            >
              <el-option label="全部学期" value="" />
              <el-option
                v-for="sem in semesters"
                :key="sem"
                :label="sem"
                :value="sem"
              />
            </el-select>
            <el-button type="primary" size="small" @click="showAddDialog">
              <el-icon><Plus /></el-icon> 添加学生
            </el-button>
            <el-button type="success" size="small" @click="showImportDialog">
              <el-icon><Upload /></el-icon> 批量导入
            </el-button>
          </div>
        </div>
      </template>

      <div v-loading="studentLoading" class="student-groups">
        <div v-for="(group, deptName) in groupedStudents" :key="deptName" class="dept-group">
          <div class="dept-header">
            <el-icon><OfficeBuilding /></el-icon>
            <span class="dept-name">{{ deptName }}</span>
            <el-tag size="small" type="info">{{ group.length }}人</el-tag>
          </div>
          <el-table :data="group" :show-header="deptName === Object.keys(groupedStudents)[0]" size="small">
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="username" label="学号" width="150" />
            <el-table-column prop="semester" label="学期" width="120" />
            <el-table-column prop="createTime" label="加入时间" min-width="160">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row }">
                <el-popconfirm title="确定移除该学生？" @confirm="removeStudent(row)">
                  <template #reference>
                    <el-button type="danger" link size="small">移除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-if="Object.keys(groupedStudents).length === 0 && !studentLoading" description="暂无学生" />
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="studentPage"
          :page-size="100"
          :total="studentTotal"
          layout="total, prev, pager, next"
          @current-change="fetchStudents"
        />
      </div>
    </el-card>

    <!-- 添加学生弹窗 -->
    <el-dialog v-model="addDialogVisible" title="添加学生" width="500px">
      <el-form label-width="80px">
        <el-form-item label="选择学生">
          <el-select
            v-model="selectedUserId"
            filterable
            remote
            :remote-method="searchUsers"
            :loading="searchLoading"
            placeholder="输入学号或姓名搜索"
            style="width: 100%"
          >
            <el-option
              v-for="user in searchResults"
              :key="user.id"
              :label="`${user.realName} (${user.username})`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择学期">
          <el-select v-model="addSemester" placeholder="选择学期" style="width: 100%">
            <el-option
              v-for="sem in semesters"
              :key="sem"
              :label="sem"
              :value="sem"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addStudent" :loading="addLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="importDialogVisible" title="批量导入学生" width="500px">
      <el-form label-width="80px">
        <el-form-item label="选择班级">
          <el-cascader
            v-model="selectedDeptIds"
            :options="deptTree"
            :props="{ value: 'id', label: 'deptName', children: 'children', checkStrictly: true }"
            placeholder="选择院系/班级"
            style="width: 100%"
            clearable
          />
        </el-form-item>
        <el-form-item label="选择学期">
          <el-select v-model="importSemester" placeholder="选择学期" style="width: 100%">
            <el-option
              v-for="sem in semesters"
              :key="sem"
              :label="sem"
              :value="sem"
            />
          </el-select>
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon>
          将导入选中部门及其所有子部门下的学生
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="importStudents" :loading="importLoading">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Reading, Refresh, ArrowRight, User, Plus, Upload, OfficeBuilding } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const courses = ref<any[]>([])
const currentCourseId = ref<number | null>(null)
const currentCourse = ref<any>(null)

const studentLoading = ref(false)
const students = ref<any[]>([])
const studentPage = ref(1)
const studentTotal = ref(0)
const semesters = ref<string[]>([])
const selectedSemester = ref('')

// 按班级分组学生
const groupedStudents = computed(() => {
  const groups: Record<string, any[]> = {}
  for (const stu of students.value) {
    const deptName = stu.deptName || '未分配班级'
    if (!groups[deptName]) {
      groups[deptName] = []
    }
    groups[deptName].push(stu)
  }
  // 按班级名称排序
  const sorted: Record<string, any[]> = {}
  Object.keys(groups).sort().forEach(key => {
    sorted[key] = groups[key]
  })
  return sorted
})

const addDialogVisible = ref(false)
const selectedUserId = ref<number | null>(null)
const searchLoading = ref(false)
const searchResults = ref<any[]>([])
const addLoading = ref(false)
const addSemester = ref('')

const importDialogVisible = ref(false)
const selectedDeptIds = ref<number[]>([])
const deptTree = ref<any[]>([])
const importLoading = ref(false)
const importSemester = ref('')

onMounted(() => {
  fetchCourses()
  fetchDeptTree()
})

const fetchCourses = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/course/user/my-courses')
    courses.value = res || []
  } catch (error) {
    console.error('获取课程失败', error)
  } finally {
    loading.value = false
  }
}

const selectCourse = (course: any) => {
  currentCourseId.value = course.id
  currentCourse.value = course
  studentPage.value = 1
  selectedSemester.value = ''
  fetchSemesters()
  fetchStudents()
}

const fetchSemesters = async () => {
  if (!currentCourseId.value) return
  try {
    const res: any = await request.get('/course/user/semesters', {
      params: { courseId: currentCourseId.value }
    })
    semesters.value = res || []
  } catch (error) {
    console.error('获取学期列表失败', error)
  }
}

const fetchStudents = async () => {
  if (!currentCourseId.value) return
  studentLoading.value = true
  try {
    const params: any = {
      courseId: currentCourseId.value,
      role: 1,
      page: studentPage.value,
      size: 100
    }
    if (selectedSemester.value) {
      params.semester = selectedSemester.value
    }
    const res: any = await request.get('/course/user/list', { params })
    students.value = res?.records || []
    studentTotal.value = res?.total || 0
  } catch (error) {
    console.error('获取学生列表失败', error)
  } finally {
    studentLoading.value = false
  }
}

const showAddDialog = () => {
  selectedUserId.value = null
  searchResults.value = []
  addSemester.value = semesters.value[0] || ''
  addDialogVisible.value = true
}

const searchUsers = async (query: string) => {
  if (!query || query.length < 2) {
    searchResults.value = []
    return
  }
  searchLoading.value = true
  try {
    const res: any = await request.get('/course/user/search-users', {
      params: { keyword: query, role: 1 }
    })
    searchResults.value = res || []
  } catch (error) {
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

const addStudent = async () => {
  if (!selectedUserId.value || !currentCourseId.value) {
    ElMessage.warning('请选择学生')
    return
  }
  if (!addSemester.value) {
    ElMessage.warning('请选择学期')
    return
  }
  addLoading.value = true
  try {
    await request.post('/course/user/add', {
      courseId: currentCourseId.value,
      userId: selectedUserId.value,
      role: 1,
      semester: addSemester.value
    })
    ElMessage.success('添加成功')
    addDialogVisible.value = false
    fetchSemesters()
    fetchStudents()
  } catch (error: any) {
    ElMessage.error(error.message || '添加失败')
  } finally {
    addLoading.value = false
  }
}

const showImportDialog = () => {
  selectedDeptIds.value = []
  importSemester.value = semesters.value[0] || ''
  importDialogVisible.value = true
}

const fetchDeptTree = async () => {
  try {
    const res: any = await request.get('/admin/dept/tree')
    deptTree.value = res || []
  } catch (error) {
    console.error('获取部门树失败', error)
  }
}

const importStudents = async () => {
  if (selectedDeptIds.value.length === 0) {
    ElMessage.warning('请选择班级')
    return
  }
  if (!importSemester.value) {
    ElMessage.warning('请选择学期')
    return
  }
  const deptId = selectedDeptIds.value[selectedDeptIds.value.length - 1]
  importLoading.value = true
  try {
    const res: any = await request.post('/course/user/import-dept', {
      courseId: currentCourseId.value,
      deptId: deptId,
      semester: importSemester.value
    })
    ElMessage.success(res || '导入成功')
    importDialogVisible.value = false
    fetchSemesters()
    fetchStudents()
  } catch (error: any) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}

const removeStudent = async (row: any) => {
  try {
    await request.delete('/course/user/remove', {
      params: {
        courseId: currentCourseId.value,
        userId: row.userId
      }
    })
    ElMessage.success('移除成功')
    fetchStudents()
  } catch (error: any) {
    ElMessage.error(error.message || '移除失败')
  }
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}
</script>

<style scoped>
.course-manage {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 64px);
}

.header-card {
  border: none;
  border-radius: 12px;
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  padding: 8px;
  background-color: #e0f2fe;
  border-radius: 8px;
  color: #0284c7;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
  color: #1f2937;
  margin: 0;
}

.header-subtitle {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.course-card {
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.course-card.active {
  border: 2px solid #409eff;
  background-color: #ecf5ff;
}

.course-info {
  flex: 1;
}

.course-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.course-code {
  font-size: 13px;
  color: #6b7280;
}

.course-arrow {
  color: #9ca3af;
  font-size: 18px;
}

.empty-tip {
  grid-column: 1 / -1;
  text-align: center;
  color: #9ca3af;
  padding: 40px 0;
}

.member-card {
  border: none;
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.student-groups {
  max-height: 500px;
  overflow-y: auto;
}

.dept-group {
  margin-bottom: 16px;
}

.dept-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background-color: #f0f9ff;
  border-radius: 6px;
  margin-bottom: 8px;
  color: #0369a1;
}

.dept-name {
  font-weight: 600;
  font-size: 14px;
}
</style>
