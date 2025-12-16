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
              <el-button type="warning" class="action-btn" @click="handleImport">
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
    <el-dialog
        title="批量导入用户"
        v-model="importDialogVisible"
        width="550px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <div class="import-content">
        <el-alert type="info" :closable="false" show-icon class="mb-4">
          <template #title>
            <div>
              <p style="margin: 0 0 8px 0;">Excel文件格式要求：</p>
              <ul style="margin: 0; padding-left: 20px; line-height: 1.8;">
                <li><strong>用户名</strong>（必填）：唯一标识</li>
                <li><strong>真实姓名</strong>（必填）</li>
                <li><strong>密码</strong>：为空则默认 123456</li>
                <li><strong>角色</strong>：学生/教师/管理员，默认学生</li>
                <li><strong>部门/班级</strong>：需与系统中部门名称一致</li>
                <li><strong>手机号</strong>、<strong>邮箱</strong>：选填</li>
              </ul>
            </div>
          </template>
        </el-alert>

        <el-upload
            ref="uploadRef"
            class="upload-area"
            drag
            action="#"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
        >
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">
            将Excel文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              仅支持 .xlsx 或 .xls 格式文件
            </div>
          </template>
        </el-upload>

        <div class="template-download">
          <el-button type="primary" link @click="downloadTemplate">
            <el-icon class="mr-1"><Download /></el-icon> 下载导入模板
          </el-button>
        </div>

        <!-- 导入结果 -->
        <div v-if="importResult" class="import-result">
          <el-divider content-position="left">导入结果</el-divider>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="成功">
              <el-tag type="success">{{ importResult.successCount }} 条</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="失败">
              <el-tag type="danger">{{ importResult.failCount }} 条</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div v-if="importResult.errors && importResult.errors.length > 0" class="error-list">
            <p class="error-title">错误详情：</p>
            <ul>
              <li v-for="(err, idx) in importResult.errors" :key="idx" class="error-item">{{ err }}</li>
            </ul>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importLoading" @click="submitImport" :disabled="!importFile">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox, ElTree } from 'element-plus'
import { Search, Plus, Refresh, EditPen, Delete, Key, Lock, OfficeBuilding, FolderOpened, Document, Upload, Download } from '@element-plus/icons-vue'

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

// ===================== 批量导入相关 =====================
const importDialogVisible = ref(false)
const importLoading = ref(false)
const importFile = ref<File | null>(null)
const importResult = ref<any>(null)
const uploadRef = ref()

const handleImport = () => {
  importFile.value = null
  importResult.value = null
  importDialogVisible.value = true
}

const handleFileChange = (file: any) => {
  importFile.value = file.raw
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先移除已选文件')
}

const submitImport = async () => {
  if (!importFile.value) {
    ElMessage.warning('请先选择要上传的Excel文件')
    return
  }

  importLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    
    const res: any = await request.post('/admin/user/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    importResult.value = res
    ElMessage.success(`导入完成：成功 ${res.successCount} 条，失败 ${res.failCount} 条`)
    
    // 刷新用户列表
    if (res.successCount > 0) {
      fetchData()
    }
  } catch (error: any) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}

const downloadTemplate = () => {
  // 使用 xlsx 库生成模板文件
  const templateData = [
    ['用户名', '真实姓名', '密码', '角色', '部门/班级', '手机号', '邮箱'],
    ['zhangsan', '张三', '123456', '学生', '计算机科学与技术1班', '13800138001', 'zhangsan@example.com'],
    ['lisi', '李四', '', '教师', '计算机学院', '13800138002', 'lisi@example.com']
  ]
  
  // 创建工作簿
  const worksheet = templateData.map(row => row.join('\t')).join('\n')
  const blob = new Blob(['\ufeff' + worksheet], { type: 'application/vnd.ms-excel;charset=utf-8' })
  
  // 下载文件
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = '用户导入模板.xls'
  link.click()
  URL.revokeObjectURL(link.href)
  
  ElMessage.success('模板下载成功，请使用Excel打开编辑')
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
.text-gray { color: #c0c4cc; }
.w-full { width: 100%; }
.mb-4 { margin-bottom: 16px; }

// 批量导入样式
.import-content {
  .upload-area {
    margin-bottom: 16px;
    
    :deep(.el-upload-dragger) {
      padding: 30px 20px;
    }
  }
  
  .template-download {
    text-align: center;
    margin-bottom: 16px;
  }
  
  .import-result {
    margin-top: 16px;
    
    .error-list {
      margin-top: 12px;
      background: #fef0f0;
      border-radius: 4px;
      padding: 12px;
      max-height: 200px;
      overflow-y: auto;
      
      .error-title {
        color: #f56c6c;
        font-weight: 500;
        margin: 0 0 8px 0;
        font-size: 13px;
      }
      
      ul {
        margin: 0;
        padding-left: 20px;
      }
      
      .error-item {
        color: #909399;
        font-size: 12px;
        line-height: 1.8;
      }
    }
  }
}
</style>