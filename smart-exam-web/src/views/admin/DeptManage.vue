<template>
  <div class="app-container p-6">
    <el-card shadow="hover" class="rounded-lg">
      <!-- 顶部操作栏 -->
      <div class="mb-4 flex justify-between items-center">
        <div class="flex gap-2">
          <el-input
              v-model="filterText"
              placeholder="请输入部门名称过滤"
              clearable
              prefix-icon="Search"
              style="width: 240px"
              @input="handleFilter"
          />
          <el-button type="info" plain @click="toggleExpandAll">
            <el-icon class="mr-1"><Sort /></el-icon> 展开/折叠
          </el-button>
        </div>
        <el-button type="primary" @click="handleAdd()">
          <el-icon class="mr-1"><Plus /></el-icon> 新增部门
        </el-button>
      </div>

      <!-- 部门树形表格 -->
      <el-table
          v-if="refreshTable"
          v-loading="loading"
          :data="deptList"
          row-key="id"
          :default-expand-all="isExpandAll"
          :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
          :header-cell-style="{ background: '#f8f9fa', color: '#606266', fontWeight: '600' }"
      >
        <el-table-column prop="deptName" label="部门名称" min-width="260">
          <template #default="scope">
            <span class="font-medium text-gray-700">{{ scope.row.deptName }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="sortOrder" label="排序" width="100" align="center" />

        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" effect="light" round>
              {{ scope.row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="leader" label="负责人" width="150" align="center">
          <template #default="scope">
            <div v-if="scope.row.leader" class="flex items-center justify-center text-gray-600">
              <el-icon class="mr-1"><User /></el-icon>
              {{ scope.row.leader }}
            </div>
            <span v-else class="text-gray-300">-</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" align="center" prop="createTime" width="200">
          <template #default="scope">
            <span class="text-gray-500 text-sm">{{ formatTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="220" fixed="right">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleEdit(scope.row)">修改</el-button>
            <el-button link type="primary" icon="Plus" @click="handleAdd(scope.row)">新增</el-button>
            <el-popconfirm title="确定要删除该部门吗?" @confirm="handleDelete(scope.row)">
              <template #reference>
                <el-button link type="danger" icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加或修改部门对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body destroy-on-close>
      <el-form ref="deptFormRef" :model="form" :rules="rules" label-width="80px" class="mt-2">
        <el-row :gutter="20">
          <el-col :span="24" v-if="form.parentId !== 0">
            <el-form-item label="上级部门" prop="parentId">
              <el-tree-select
                  v-model="form.parentId"
                  :data="deptOptions"
                  :props="{ value: 'id', label: 'deptName', children: 'children' }"
                  value-key="id"
                  placeholder="选择上级部门"
                  check-strictly
                  class="w-full"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="部门名称" prop="deptName">
              <el-input v-model="form.deptName" placeholder="请输入部门名称" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="显示排序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" controls-position="right" :min="0" class="w-full" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="负责人" prop="leader">
              <el-input v-model="form.leader" placeholder="请输入负责人" maxlength="20" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="部门状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">正常</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import { Search, Plus, Sort, Edit, Delete, User } from '@element-plus/icons-vue';
import request from '@/utils/request';

// 状态定义
const loading = ref(true);
const submitLoading = ref(false);
const refreshTable = ref(true);
const isExpandAll = ref(true);
const open = ref(false);
const title = ref('');
const deptList = ref<any[]>([]);
const deptOptions = ref<any[]>([]);
const filterText = ref('');
const rawDeptList = ref<any[]>([]); // 保存原始数据用于前端过滤

const deptFormRef = ref();

// 表单初始值
const initialForm = {
  id: undefined,
  parentId: undefined,
  deptName: undefined,
  sortOrder: 0,
  leader: undefined,
  phone: undefined,
  email: undefined,
  status: 1
};

const form = reactive({ ...initialForm });

// 表单校验规则
const rules = {
  parentId: [{ required: true, message: "上级部门不能为空", trigger: "blur" }],
  deptName: [{ required: true, message: "部门名称不能为空", trigger: "blur" }],
  sortOrder: [{ required: true, message: "显示排序不能为空", trigger: "blur" }]
};

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-';
  return time.replace('T', ' ');
};

// 获取部门列表
const getList = async () => {
  loading.value = true;
  try {
    const response = await request.get('/admin/dept/tree');
    rawDeptList.value = response as any;
    deptList.value = response as any;
  } finally {
    loading.value = false;
  }
};

// 前端过滤逻辑
const handleFilter = (val: string) => {
  if (!val) {
    deptList.value = rawDeptList.value;
    return;
  }
  // 简单的递归过滤
  const filterTree = (data: any[], text: string): any[] => {
    return data.filter(item => {
      const match = item.deptName.includes(text);
      if (item.children && item.children.length > 0) {
        const filteredChildren = filterTree(item.children, text);
        if (filteredChildren.length > 0) {
          item.children = filteredChildren;
          return true;
        }
      }
      return match;
    });
  };
  // 深拷贝一份数据进行过滤，避免修改原始数据
  const copyData = JSON.parse(JSON.stringify(rawDeptList.value));
  deptList.value = filterTree(copyData, val);
};

// 展开/折叠
const toggleExpandAll = () => {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => {
    refreshTable.value = true;
  });
};

// 取消按钮
const cancel = () => {
  open.value = false;
  reset();
};

// 表单重置
const reset = () => {
  Object.assign(form, initialForm);
};

// 新增按钮操作
const handleAdd = (row?: any) => {
  reset();
  if (row != undefined) {
    form.parentId = row.id;
  }
  open.value = true;
  title.value = "添加部门";
  // 获取部门下拉树，直接复用列表数据
  deptOptions.value = rawDeptList.value;
};

// 修改按钮操作
const handleEdit = async (row: any) => {
  reset();
  // 填充表单数据
  Object.assign(form, row);
  open.value = true;
  title.value = "修改部门";
  // 排除自身作为父节点（简单处理：获取全量数据作为选项）
  deptOptions.value = rawDeptList.value;
};

// 提交按钮
const submitForm = async () => {
  if (!deptFormRef.value) return;
  await deptFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true;
      try {
        if (form.id != undefined) {
          // 修改
          await request.put('/admin/dept', form);
          ElMessage.success("修改成功");
        } else {
          // 新增
          await request.post('/admin/dept', form);
          ElMessage.success("新增成功");
        }
        open.value = false;
        getList();
      } catch (error: any) {
        // request interceptor might handle error, but we can also log here
        console.error(error);
      } finally {
        submitLoading.value = false;
      }
    }
  });
};

// 删除按钮操作
const handleDelete = async (row: any) => {
  try {
    await request.delete(`/admin/dept/${row.id}`);
    ElMessage.success("删除成功");
    getList();
  } catch (error) {
    // error handled
  }
};

onMounted(() => {
  getList();
});
</script>

<style scoped>
.app-container {
  /* 使得表格卡片充满屏幕高度的视觉感 */
  min-height: calc(100vh - 84px);
}
</style>