<template>
  <div class="p-6">
    <el-card shadow="hover">
      <!-- 搜索栏 -->
      <div class="flex justify-between items-center mb-4">
        <div class="flex gap-4">
          <el-input v-model="queryParams.username" placeholder="用户名搜索" class="w-48" clearable @clear="handleSearch" />
          <el-select v-model="queryParams.deptId" placeholder="选择部门" class="w-48" clearable @clear="handleSearch">
            <el-option v-for="item in deptOptions" :key="item.id" :label="item.deptName" :value="item.id" />
          </el-select>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>
        <el-button type="success" @click="handleAdd">新增用户</el-button>
      </div>

      <!-- 用户列表 -->
      <el-table :data="userList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="真实姓名" />
        <el-table-column label="角色">
          <template #default="{ row }">
            <el-tag :type="getRoleTag(row.role)">{{ getRoleName(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deptId" label="部门">
          <template #default="{ row }">
            {{ getDeptName(row.deptId) }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定要删除该用户吗？" @confirm="handleDelete(row.id)">
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
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!form.id">
          <el-input v-model="form.password" type="password" show-password placeholder="默认 123456" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" class="w-full">
            <el-option label="学生" :value="1" />
            <el-option label="教师" :value="2" />
            <el-option label="管理员" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-select v-model="form.deptId" class="w-full">
            <el-option v-for="item in deptOptions" :key="item.id" :label="item.deptName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
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
const userList = ref([])
const total = ref(0)
const deptOptions = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()

const queryParams = reactive({
  page: 1,
  size: 10,
  username: '',
  deptId: undefined
})

const form = reactive({
  id: undefined,
  username: '',
  realName: '',
  password: '',
  role: 1,
  deptId: undefined,
  phone: '',
  status: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// 初始化
onMounted(() => {
  fetchDepts()
  fetchData()
})

// 获取部门列表
const fetchDepts = async () => {
  try {
    const res = await request.get('/admin/dept/tree')
    deptOptions.value = res as any
  } catch (error) {
    console.error(error)
  }
}

// 获取用户列表
const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/admin/user/list', { params: queryParams })
    userList.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  fetchData()
}

// 操作方法
const handleAdd = () => {
  dialogTitle.value = '新增用户'
  Object.assign(form, {
    id: undefined,
    username: '',
    realName: '',
    password: '',
    role: 1,
    deptId: undefined,
    phone: '',
    status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑用户'
  Object.assign(form, row)
  form.password = '' // 编辑时不回显密码
  dialogVisible.value = true
}

const handleDelete = async (id: number) => {
  try {
    await request.delete(`/admin/user/${id}`)
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
          await request.put(`/admin/user/${form.id}`, form)
        } else {
          await request.post('/admin/user', form)
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
const getRoleName = (role: number) => {
  const map: Record<number, string> = { 1: '学生', 2: '教师', 3: '管理员' }
  return map[role] || '未知'
}

const getRoleTag = (role: number) => {
  const map: Record<number, string> = { 1: '', 2: 'warning', 3: 'danger' }
  return map[role] || 'info'
}

const getDeptName = (deptId: number) => {
  const dept = deptOptions.value.find(d => d.id === deptId)
  return dept ? dept.deptName : '-'
}
</script>