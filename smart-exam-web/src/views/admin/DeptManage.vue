<template>
  <div class="p-6">
    <el-card shadow="hover">
      <!-- 页面标题和操作按钮 -->
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-xl font-bold">组织架构</h2>
        <el-button type="success" @click="handleAddRootDept">新增根部门</el-button>
      </div>

      <!-- 部门树形结构 -->
      <el-tree
        :data="deptTree"
        :props="deptTreeProps"
        :expand-on-click-node="false"
        :default-expand-all="true"
        ref="treeRef"
        class="w-full"
      >
        <template #default="{ node, data }">
          <div class="flex items-center justify-between w-full">
            <span>{{ node.label }}</span>
            <div class="flex gap-2">
              <el-button type="primary" size="small" link @click.stop="handleAddChildDept(data)">
                新增子部门
              </el-button>
              <el-button type="warning" size="small" link @click.stop="handleEditDept(data)">
                编辑
              </el-button>
              <el-popconfirm
                title="确定要删除该部门吗？"
                @confirm="handleDeleteDept(data.id)"
                :disabled="data.children && data.children.length > 0"
              >
                <template #reference>
                  <el-button
                    type="danger"
                    size="small"
                    link
                    :disabled="data.children && data.children.length > 0"
                  >
                    删除
                  </el-button>
                </template>
                <template #disabled>
                  <el-button type="danger" size="small" link disabled>
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </template>
      </el-tree>

      <!-- 新增/编辑部门对话框 -->
      <el-dialog
        :title="dialogTitle"
        v-model="dialogVisible"
        width="500px"
      >
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-form-item label="部门名称" prop="deptName">
            <el-input v-model="form.deptName" placeholder="请输入部门名称" />
          </el-form-item>
          <el-form-item label="上级部门" prop="parentId">
            <el-select v-model="form.parentId" placeholder="请选择上级部门">
              <el-option
                v-for="dept in deptOptions"
                :key="dept.id"
                :label="dept.deptName"
                :value="dept.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="排序" prop="sortOrder">
            <el-input-number v-model="form.sortOrder" :min="0" :max="999" placeholder="请输入排序值" />
          </el-form-item>
          <el-form-item label="负责人">
            <el-input v-model="form.leader" placeholder="请输入负责人姓名" />
          </el-form-item>
          <el-form-item label="类别">
            <el-select v-model="form.category" placeholder="请选择部门类别">
              <el-option label="学院" :value="1" />
              <el-option label="系" :value="2" />
              <el-option label="班级" :value="3" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import type { TreeProps } from 'element-plus'

// 数据定义
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const treeRef = ref()

const deptTree = ref<any[]>([])
const deptList = ref<any[]>([])

// 部门树形结构配置
const deptTreeProps: TreeProps = {
  label: 'deptName',
  children: 'children',
  value: 'id'
}

// 表单数据
const form = reactive({
  id: undefined,
  deptName: '',
  parentId: 0,
  sortOrder: 0,
  leader: '',
  category: 1
})

// 表单验证规则
const rules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  parentId: [{ required: true, message: '请选择上级部门', trigger: 'change' }],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'change' }]
}

// 部门下拉选项（用于选择上级部门）
const deptOptions = computed(() => {
  // 过滤掉当前编辑的部门及其子部门，防止循环引用
  const filteredDepts = deptList.value.filter(dept => {
    if (form.id === undefined) return true
    // 简单实现：如果是编辑模式，不显示当前部门作为上级部门
    return dept.id !== form.id
  })
  return filteredDepts
})

// 初始化
onMounted(() => {
  fetchDeptTree()
})

// 获取部门树形结构
const fetchDeptTree = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/api/admin/dept/tree')
    deptList.value = res
    // 构建树形结构
    deptTree.value = buildTree(res, 0)
  } catch (error) {
    ElMessage.error('获取部门列表失败')
  } finally {
    loading.value = false
  }
}

// 构建树形结构
const buildTree = (list: any[], parentId: number): any[] => {
  return list
    .filter(item => item.parentId === parentId)
    .map(item => ({
      ...item,
      children: buildTree(list, item.id)
    }))
}

// 新增根部门
const handleAddRootDept = () => {
  dialogTitle.value = '新增根部门'
  Object.assign(form, {
    id: undefined,
    deptName: '',
    parentId: 0,
    sortOrder: 0,
    leader: '',
    category: 1
  })
  dialogVisible.value = true
}

// 新增子部门
const handleAddChildDept = (parentDept: any) => {
  dialogTitle.value = '新增子部门'
  Object.assign(form, {
    id: undefined,
    deptName: '',
    parentId: parentDept.id,
    sortOrder: 0,
    leader: '',
    category: parentDept.category + 1 || 1
  })
  dialogVisible.value = true
}

// 编辑部门
const handleEditDept = (dept: any) => {
  dialogTitle.value = '编辑部门'
  Object.assign(form, dept)
  dialogVisible.value = true
}

// 删除部门
const handleDeleteDept = async (deptId: number) => {
  try {
    await request.delete(`/api/admin/dept/${deptId}`)
    ElMessage.success('部门删除成功')
    fetchDeptTree()
  } catch (error) {
    // error handled by interceptor
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        if (form.id) {
          await request.put(`/api/admin/dept/${form.id}`, form)
        } else {
          await request.post('/api/admin/dept', form)
        }
        ElMessage.success(form.id ? '部门更新成功' : '部门创建成功')
        dialogVisible.value = false
        fetchDeptTree()
      } catch (error) {
        // error handled by interceptor
      }
    }
  })
}
</script>
