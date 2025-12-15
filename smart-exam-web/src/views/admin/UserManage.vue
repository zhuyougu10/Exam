<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 左侧部门树结构 -->
      <el-col :span="5" :xs="24">
        <el-card class="box-card dept-tree-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>组织架构</span>
            </div>
          </template>
          <div class="head-container">
            <el-input
                v-model="deptName"
                placeholder="请输入部门名称"
                clearable
                prefix-icon="Search"
                class="mb-4"
            />
          </div>
          <div class="tree-container">
            <el-tree
                ref="deptTreeRef"
                :data="deptOptions"
                :props="{ children: 'children', label: 'deptName' }"
                :expand-on-click-node="false"
                :filter-node-method="filterNode"
                node-key="id"
                default-expand-all
                highlight-current
                @node-click="handleNodeClick"
            >
              <template #default="{ node, data }">
                <span class="custom-tree-node">
                  <el-icon class="mr-1"><component :is="data.children && data.children.length > 0 ? 'FolderOpened' : 'Document'" /></el-icon>
                  <span>{{ node.label }}</span>
                </span>
              </template>
            </el-tree>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧用户数据列表 -->
      <el-col :span="19" :xs="24">
        <!-- 头部搜索与操作栏 -->
        <el-card class="filter-container" shadow="never">
          <div class="filter-wrapper">
            <div class="filter-left">
              <el-input
                  v-model="queryParams.username"
                  placeholder="输入用户名/姓名搜索"
                  class="filter-item w-240"
                  clearable
                  @clear="handleSearch"
                  @keyup.enter="handleSearch"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>

              <el-select
                  v-model="queryParams.deptId"
                  placeholder="选择所属部门"
                  class="filter-item w-200"
                  clearable
                  @change="handleSearch"
                  @clear="handleSearch"
              >
                <template #prefix><el-icon><OfficeBuilding /></el-icon></template>
                <el-option
                    v-for="item in flattenDepts(deptOptions)"
                    :key="item.id"
                    :label="item.deptName"
                    :value="item.id"
                />
              </el-select>

              <el-button type="primary" @click="handleSearch">
                <el-icon class="mr-1"><Search /></el-icon> 查询
              </el-button>
              <el-button @click="resetQuery">
                <el-icon class="mr-1"><Refresh /></el-icon> 重置
              </el-button>
            </div>

            <div class="filter-right">
              <el-button type="warning" class="action-btn" @click="showImportDialog">
                <el-icon class="mr-1"><Upload /></el-icon> 批量导入
              </el-button>
              <el-button type="success" class="action-btn" @click="handleAdd">
                <el-icon class="mr-1"><Plus /></el-icon> 新增用户
              </el-button>
            </div>
          </div>
        </el-card>

        <!-- 数据表格区域 -->
        <el-card class="table-container" shadow="never">
          <el-table
              v-loading="loading"
              :data="userList"
              style="width: 100%"
              :header-cell-style="{ background: '#f8f9fa', color: '#606266', height: '50px' }"
              stripe
          >
            <el-table-column prop="id" label="ID" width="80" align="center" sortable />

            <el-table-column label="用户信息" min-width="200">
              <template #default="{ row }">
                <div class="user-info-cell">
                  <el-avatar :size="40" :src="row.avatar" :class="getRoleClass(row.role)">
                    {{ row.realName?.charAt(0).toUpperCase() }}
                  </el-avatar>
                  <div class="text-info">
                    <div class="realname">{{ row.realName }}</div>
                    <div class="username">@{{ row.username }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="角色" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="getRoleType(row.role)" effect="light" round size="small">
                  {{ getRoleName(row.role) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="deptId" label="所属部门" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="dept-cell" v-if="getDeptName(row.deptId) !== '-'">
                  <el-icon class="dept-icon"><OfficeBuilding /></el-icon>
                  <span>{{ getDeptName(row.deptId) }}</span>
                </div>
                <span v-else class="text-gray">-</span>
              </template>
            </el-table-column>

            <el-table-column prop="phone" label="手机号" width="150" align="center" />

            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <div class="status-cell">
                  <span :class="['status-dot', row.status === 1 ? 'bg-success' : 'bg-danger']"></span>
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </div>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="180" align="center" fixed="right">
              <template #default="{ row }">
                <el-tooltip content="编辑" placement="top" :enterable="false">
                  <el-button type="primary" text circle @click="handleEdit(row)">
                    <el-icon :size="16"><EditPen /></el-icon>
                  </el-button>
                </el-tooltip>

                <el-tooltip content="重置密码" placement="top" :enterable="false">
                  <el-button type="warning" text circle @click="handleResetPwd(row)">
                    <el-icon :size="16"><Key /></el-icon>
                  </el-button>
                </el-tooltip>

                <el-tooltip content="删除" placement="top" :enterable="false">
                  <el-popconfirm
                      title="确定要删除该用户吗？此操作不可恢复。"
                      confirm-button-text="确定"
                      cancel-button-text="取消"
                      confirm-button-type="danger"
                      width="220"
                      @confirm="handleDelete(row.id)"
                  >
                    <template #reference>
                      <el-button type="danger" text circle>
                        <el-icon :size="16"><Delete /></el-icon>
                      </el-button>
                    </template>
                  </el-popconfirm>
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页组件 (Fixed) -->
          <div class="pagination-wrapper">
            <el-pagination
                v-model:current-page="queryParams.page"
                v-model:page-size="queryParams.size"
                :total="total"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                background
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增/编辑对话框 -->
    <el-dialog
        :title="dialogTitle"
        v-model="dialogVisible"
        width="580px"
        class="user-dialog"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="dialog-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" :disabled="!!form.id" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="登录密码" prop="password" v-if="!form.id">
          <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="默认密码：123456"
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属部门" prop="deptId">
              <el-tree-select
                  v-model="form.deptId"
                  :data="deptOptions"
                  :props="{ label: 'deptName', value: 'id', children: 'children' }"
                  placeholder="请选择部门"
                  check-strictly
                  class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色权限" prop="role">
              <el-select v-model="form.role" class="w-full" placeholder="请选择角色">
                <el-option label="学生" :value="1" />
                <el-option label="教师" :value="2" />
                <el-option label="管理员" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号码" maxlength="11" />
        </el-form-item>

        <el-form-item label="账号状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1" border>正常启用</el-radio>
            <el-radio :value="0" border>禁用锁定</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            确认{{ form.id ? '修改' : '新增' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="批量导入用户" width="700px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon class="mb-4">
        <template #title>
          导入说明：每行一个用户，格式为 <b>学号/用户名,姓名,角色(1学生/2教师)</b>，默认密码为 123456
        </template>
      </el-alert>
      <el-form label-width="100px">
        <el-form-item label="选择部门">
          <el-tree-select
            v-model="importDeptId"
            :data="deptOptions"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            placeholder="可选，导入用户的所属部门"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="用户数据">
          <el-input
            v-model="importText"
            type="textarea"
            :rows="10"
            placeholder="每行一个用户，格式：学号,姓名,角色&#10;例如：&#10;2024001,张三,1&#10;2024002,李四,1&#10;T001,王老师,2"
          />
        </el-form-item>
      </el-form>
      <div v-if="importResult" class="import-result">
        <el-tag type="success">成功: {{ importResult.successCount }}</el-tag>
        <el-tag type="warning" class="ml-2">跳过: {{ importResult.skipCount }}</el-tag>
        <el-tag type="danger" class="ml-2">失败: {{ importResult.errorCount }}</el-tag>
        <div v-if="importResult.errors?.length" class="error-list">
          <div v-for="(err, i) in importResult.errors" :key="i" class="text-danger">{{ err }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="handleImport">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox, ElTree } from 'element-plus'
import { Search, Plus, Refresh, EditPen, Delete, Key, Lock, OfficeBuilding, FolderOpened, Document, Upload } from '@element-plus/icons-vue'

// 数据定义
const loading = ref(false)
const submitLoading = ref(false)
const userList = ref([])
const total = ref(0)
const deptOptions = ref<any[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()

// 树相关
const deptName = ref('')
const deptTreeRef = ref<InstanceType<typeof ElTree>>()

const queryParams = reactive({
  page: 1,
  size: 10,
  username: '',
  deptId: undefined as number | undefined
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

// 监听部门搜索
watch(deptName, (val) => {
  deptTreeRef.value!.filter(val)
})

// 初始化
onMounted(() => {
  fetchDepts()
  fetchData()
})

// 分页处理方法
const handleSizeChange = (val: number) => {
  queryParams.size = val
  queryParams.page = 1 // 重置到第一页
  fetchData()
}

const handleCurrentChange = (val: number) => {
  queryParams.page = val
  fetchData()
}

// 部门树过滤逻辑
const filterNode = (value: string, data: any) => {
  if (!value) return true
  return data.deptName.includes(value)
}

// 部门树节点点击
const handleNodeClick = (data: any) => {
  queryParams.deptId = data.id
  handleSearch()
}

// 获取部门列表
const fetchDepts = async () => {
  try {
    const res = await request.get('/admin/dept/tree')
    deptOptions.value = res as any
  } catch (error) {
    console.error(error)
  }
}

// 扁平化部门数据用于Select Option (简单实现，实际建议使用 TreeSelect)
const flattenDepts = (depts: any[]): any[] => {
  let res: any[] = []
  depts.forEach(dept => {
    res.push(dept)
    if (dept.children) {
      res = res.concat(flattenDepts(dept.children))
    }
  })
  return res
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

const resetQuery = () => {
  queryParams.username = ''
  queryParams.deptId = undefined
  deptTreeRef.value?.setCurrentKey(null as any) // 清除树选中
  handleSearch()
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

const handleResetPwd = (row: any) => {
  ElMessageBox.confirm(
      `确定重置用户 "${row.username}" 的密码为 123456 吗?`,
      '重置密码',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(async () => {
    ElMessage.info('功能开发中，请稍后')
  })
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
      submitLoading.value = true
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
        // error handled
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 辅助方法
const getRoleName = (role: number) => {
  const map: Record<number, string> = { 1: '学生', 2: '教师', 3: '管理员' }
  return map[role] || '未知'
}

const getRoleType = (role: number) => {
  const map: Record<number, string> = { 1: 'info', 2: 'warning', 3: 'danger' }
  return map[role] || ''
}

const getRoleClass = (role: number) => {
  const map: Record<number, string> = { 1: 'role-student', 2: 'role-teacher', 3: 'role-admin' }
  return map[role] || ''
}

const getDeptName = (deptId: number) => {
  const findName = (depts: any[], id: number): string | null => {
    for (const dept of depts) {
      if (dept.id === id) return dept.deptName
      if (dept.children) {
        const name = findName(dept.children, id)
        if (name) return name
      }
    }
    return null
  }
  return findName(deptOptions.value, deptId) || '-'
}

// 批量导入相关
const importDialogVisible = ref(false)
const importLoading = ref(false)
const importText = ref('')
const importDeptId = ref<number | undefined>(undefined)
const importResult = ref<any>(null)

const showImportDialog = () => {
  importText.value = ''
  importDeptId.value = undefined
  importResult.value = null
  importDialogVisible.value = true
}

const handleImport = async () => {
  if (!importText.value.trim()) {
    ElMessage.warning('请输入用户数据')
    return
  }
  
  // 解析文本数据
  const lines = importText.value.trim().split('\n')
  const users: any[] = []
  
  for (const line of lines) {
    const parts = line.split(',').map(s => s.trim())
    if (parts.length >= 2) {
      users.push({
        username: parts[0],
        realName: parts[1],
        role: parts[2] ? parseInt(parts[2]) : 1,
        deptId: importDeptId.value
      })
    }
  }
  
  if (users.length === 0) {
    ElMessage.warning('未解析到有效数据')
    return
  }
  
  importLoading.value = true
  try {
    const res: any = await request.post('/admin/user/batch-import', users)
    importResult.value = res
    ElMessage.success(res.message || `成功导入 ${res.successCount} 个用户`)
    fetchData()
  } catch (error) {
    console.error(error)
  } finally {
    importLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
}

.dept-tree-card {
  height: 100%;
  min-height: calc(100vh - 120px);

  :deep(.el-card__header) {
    padding: 14px 16px;
    border-bottom: 1px solid #f0f2f5;
  }

  .card-header {
    font-weight: 600;
    color: #303133;
  }

  :deep(.el-card__body) {
    padding: 16px;
  }

  .mb-4 {
    margin-bottom: 16px;
  }

  .custom-tree-node {
    display: flex;
    align-items: center;
    font-size: 14px;

    .el-icon {
      color: #909399;
    }
  }
}

.filter-container {
  margin-bottom: 20px;
  border-radius: 8px;

  :deep(.el-card__body) {
    padding: 16px 20px;
  }
}

.filter-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;

  .filter-left {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }

  .w-240 { width: 240px; }
  .w-200 { width: 200px; }
}

.table-container {
  border-radius: 8px;
  min-height: 600px;
  display: flex;
  flex-direction: column;
}

.user-info-cell {
  display: flex;
  align-items: center;

  .text-info {
    margin-left: 12px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    line-height: 1.3;

    .realname {
      font-weight: 600;
      color: #303133;
      font-size: 14px;
    }

    .username {
      font-size: 12px;
      color: #909399;
    }
  }

  .el-avatar {
    border: 2px solid transparent;
    transition: all 0.3s;
    background-color: #f0f2f5;
    color: #909399;
    font-weight: bold;

    &.role-admin { border-color: #f56c6c; color: #f56c6c; background-color: #fef0f0; }
    &.role-teacher { border-color: #e6a23c; color: #e6a23c; background-color: #fdf6ec; }
    &.role-student { border-color: #409eff; color: #409eff; background-color: #ecf5ff; }
  }
}

.dept-cell {
  display: flex;
  align-items: center;
  color: #606266;

  .dept-icon {
    margin-right: 6px;
    color: #909399;
  }
}

.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;

  .status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    margin-right: 6px;

    &.bg-success { background-color: #67c23a; box-shadow: 0 0 0 2px rgba(103, 194, 58, 0.2); }
    &.bg-danger { background-color: #f56c6c; box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.2); }
  }
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.mr-1 { margin-right: 4px; }
.ml-2 { margin-left: 8px; }
.mb-4 { margin-bottom: 16px; }
.text-gray { color: #c0c4cc; }
.text-danger { color: #f56c6c; font-size: 12px; }
.w-full { width: 100%; }

.import-result {
  margin-top: 16px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  
  .error-list {
    margin-top: 8px;
    max-height: 150px;
    overflow-y: auto;
  }
}
</style>