<template>
  <div class="p-6">
    <el-card shadow="hover">
      <!-- 搜索栏 -->
      <div class="flex justify-between items-center mb-4">
        <div class="flex gap-4">
          <el-input v-model="queryParams.courseName" placeholder="课程名称搜索" class="w-48" clearable @clear="handleSearch" />
          <el-select v-model="queryParams.deptId" placeholder="选择部门" class="w-48" clearable @clear="handleSearch">
            <el-option v-for="item in deptOptions" :key="item.id" :label="item.deptName" :value="item.id" />
          </el-select>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
        <el-button type="success" @click="handleAdd">新增课程</el-button>
      </div>

      <!-- 课程列表 -->
      <el-table :data="courseList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="courseName" label="课程名称" />
        <el-table-column prop="courseCode" label="课程代码" />
        <el-table-column prop="deptId" label="所属部门">
          <template #default="{ row }">
            {{ getDeptName(row.deptId) }}
          </template>
        </el-table-column>
        <el-table-column prop="credits" label="学分" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定要删除该课程吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="form.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="课程代码" prop="courseCode">
          <el-input v-model="form.courseCode" :disabled="!!form.id" placeholder="请输入课程代码" />
        </el-form-item>
        <el-form-item label="所属部门" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择所属部门">
            <el-option v-for="item in deptOptions" :key="item.id" :label="item.deptName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学分" prop="credits">
          <el-input-number v-model="form.credits" :min="0" :max="10" :step="0.5" placeholder="请输入学分" />
        </el-form-item>
        <el-form-item label="课程描述">
          <el-input v-model="form.description" type="textarea" rows="3" placeholder="请输入课程描述" />
        </el-form-item>
        <el-form-item label="课程封面">
          <el-upload
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :http-request="handleImageUpload"
            :before-upload="beforeAvatarUpload"
          >
            <img v-if="form.coverImg" :src="form.coverImg" class="el-upload--picture-card" style="width: 100px; height: 100px" />
            <el-icon v-else class="el-icon-plus avatar-uploader-icon" style="width: 100px; height: 100px" />
          </el-upload>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

// 数据定义
const loading = ref(false)
const courseList = ref([])
const total = ref(0)
const deptOptions = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()

const queryParams = reactive({
  page: 1,
  size: 10,
  courseName: '',
  deptId: undefined
})

const form = reactive({
  id: undefined,
  courseName: '',
  courseCode: '',
  deptId: undefined,
  credits: 0,
  coverImg: '',
  description: '',
  status: 1
})

const rules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  courseCode: [{ required: true, message: '请输入课程代码', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
  credits: [{ required: true, message: '请输入学分', trigger: 'change' }]
}

// 初始化
onMounted(() => {
  fetchDepts()
  fetchData()
})

// 获取部门列表
const fetchDepts = async () => {
  try {
    const res = await request.get('/api/admin/dept/tree')
    deptOptions.value = res as any
  } catch (error) {
    console.error(error)
  }
}

// 获取课程列表
const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/api/admin/course/list', { params: queryParams })
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

// 图片上传处理（模拟）
const handleImageUpload = () => {
  // 这里只是模拟，实际项目中需要调用真实的上传接口
  ElMessage.info('图片上传功能待实现')
}

// 图片上传前验证
const beforeAvatarUpload = (file: File) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG) {
    ElMessage.error('上传图片只能是 JPG/PNG 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('上传图片大小不能超过 2MB!')
  }
  return isJPG && isLt2M
}

// 操作方法
const handleAdd = () => {
  dialogTitle.value = '新增课程'
  Object.assign(form, {
    id: undefined,
    courseName: '',
    courseCode: '',
    deptId: undefined,
    credits: 0,
    coverImg: '',
    description: '',
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑课程'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (id: number) => {
  try {
    await request.delete(`/api/admin/course/${id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    // error handled by interceptor
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        if (form.id) {
          await request.put(`/api/admin/course/${form.id}`, form)
        } else {
          await request.post('/api/admin/course', form)
        }
        ElMessage.success(form.id ? '更新成功' : '创建成功')
        dialogVisible.value = false
        fetchData()
      } catch (error) {
        // error handled by interceptor
      }
    }
  })
}

// 辅助方法
const getDeptName = (deptId: number) => {
  const dept = deptOptions.value.find(d => d.id === deptId)
  return dept ? dept.deptName : '-'
}
</script>
