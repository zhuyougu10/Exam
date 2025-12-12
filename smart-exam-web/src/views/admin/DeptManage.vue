<template>
  <div class="app-container">
    <el-card shadow="never" class="filter-card">
      <div class="header-box">
        <!-- 搜索区 -->
        <div class="header-left">
          <el-input
              v-model="filterText"
              placeholder="请输入部门名称过滤"
              clearable
              prefix-icon="Search"
              class="filter-input"
              @input="handleFilter"
          />

          <!-- 新增：按类型筛选 -->
          <el-radio-group v-model="filterCategory" @change="handleFilter" class="ml-4">
            <el-radio-button :value="0">全部</el-radio-button>
            <el-radio-button :value="1">学院</el-radio-button>
            <el-radio-button :value="2">系/专业</el-radio-button>
            <el-radio-button :value="3">班级</el-radio-button>
          </el-radio-group>

          <el-button type="info" plain icon="Sort" @click="toggleExpandAll" class="ml-4">
            {{ isExpandAll ? '全部折叠' : '全部展开' }}
          </el-button>
        </div>

        <!-- 操作区 -->
        <div class="header-right">
          <el-button type="primary" icon="Plus" @click="handleAdd()">新增部门</el-button>
        </div>
      </div>
    </el-card>

    <!-- 部门树形表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
          v-if="refreshTable"
          v-loading="loading"
          :data="deptList"
          row-key="id"
          :default-expand-all="isExpandAll"
          :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
          :header-cell-style="{ background: '#f8f9fa', color: '#333', fontWeight: 'bold' }"
          border
      >
        <el-table-column prop="deptName" label="部门名称" min-width="260">
          <template #default="scope">
            <div class="dept-name-cell">
              <!-- 根据 category 显示不同图标 -->
              <el-icon :class="getCategoryIconClass(scope.row.category)">
                <component :is="getCategoryIcon(scope.row.category)" />
              </el-icon>
              <span>{{ scope.row.deptName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="category" label="类型" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getCategoryType(scope.row.category)" size="small" effect="plain">
              {{ getCategoryLabel(scope.row.category) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="sortOrder" label="排序" width="80" align="center">
          <template #default="scope">
            <el-tag type="info" size="small">{{ scope.row.sortOrder }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="leader" label="负责人" width="150" align="center">
          <template #default="scope">
            <div v-if="scope.row.leader" class="leader-cell">
              <el-avatar :size="24" class="leader-avatar">{{ scope.row.leader.charAt(0) }}</el-avatar>
              <span>{{ scope.row.leader }}</span>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" align="center" prop="createTime" width="180">
          <template #default="scope">
            <span class="time-text">{{ formatTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="220" fixed="right">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleEdit(scope.row)">修改</el-button>
            <el-button link type="success" icon="Plus" @click="handleAdd(scope.row)">新增</el-button>
            <el-popconfirm title="确定要删除该部门吗?" width="200" @confirm="handleDelete(scope.row)">
              <template #reference>
                <el-button link type="danger" icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加或修改部门对话框 -->
    <el-dialog
        :title="title"
        v-model="open"
        width="600px"
        class="custom-dialog"
        align-center
        destroy-on-close
    >
      <el-form ref="deptFormRef" :model="form" :rules="rules" label-width="90px" class="dialog-form">
        <el-row :gutter="24">
          <el-col :span="24" v-if="form.parentId !== 0">
            <el-form-item label="上级部门" prop="parentId">
              <el-tree-select
                  v-model="form.parentId"
                  :data="deptOptions"
                  :props="{ value: 'id', label: 'deptName', children: 'children' }"
                  value-key="id"
                  placeholder="请选择上级部门"
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
            <el-form-item label="部门类型" prop="category">
              <el-select v-model="form.category" placeholder="请选择类型" class="w-full">
                <el-option label="学院" :value="1" />
                <el-option label="系/专业" :value="2" />
                <el-option label="班级" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="显示排序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" controls-position="right" :min="0" class="w-full" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="负责人" prop="leader">
              <el-select
                  v-model="form.leader"
                  placeholder="请选择负责人"
                  filterable
                  clearable
                  class="w-full"
                  :loading="userLoading"
                  @focus="fetchUsers"
              >
                <el-option
                    v-for="user in userOptions"
                    :key="user.id"
                    :label="user.realName"
                    :value="user.realName"
                >
                  <span style="float: left">{{ user.realName }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">{{ user.username }}</span>
                </el-option>
              </el-select>
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
import { Search, Plus, Sort, Edit, Delete, User, FolderOpened, Document, School, Management, Grid } from '@element-plus/icons-vue';
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
const filterCategory = ref(0); // 0-全部，1-学院，2-系，3-班级
const rawDeptList = ref<any[]>([]);

// 用户选择相关
const userLoading = ref(false);
const userOptions = ref<any[]>([]);
const isUsersLoaded = ref(false);

const deptFormRef = ref();

const initialForm = {
  id: undefined,
  parentId: undefined,
  deptName: undefined,
  sortOrder: 0,
  leader: undefined,
  category: 1 // 默认为学院
};

const form = reactive({ ...initialForm });

const rules = {
  parentId: [{ required: true, message: "上级部门不能为空", trigger: "change" }],
  deptName: [{ required: true, message: "部门名称不能为空", trigger: "blur" }],
  sortOrder: [{ required: true, message: "显示排序不能为空", trigger: "blur" }],
  category: [{ required: true, message: "部门类型不能为空", trigger: "change" }]
};

const formatTime = (time: string) => {
  if (!time) return '-';
  return time.replace('T', ' ').substring(0, 16);
};

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

// 获取用户列表（用于选择负责人）
const fetchUsers = async () => {
  if (isUsersLoaded.value) return;

  userLoading.value = true;
  try {
    const res: any = await request.get('/admin/user/list', {
      params: { page: 1, size: 1000 }
    });
    userOptions.value = res.records || [];
    isUsersLoaded.value = true;
  } catch (error) {
    console.error("获取用户列表失败", error);
  } finally {
    userLoading.value = false;
  }
};

const handleFilter = () => {
  // 如果没有筛选条件，还原原始数据
  if (!filterText.value && filterCategory.value === 0) {
    deptList.value = rawDeptList.value;
    return;
  }

  // 递归过滤函数
  const filterTree = (data: any[]): any[] => {
    return data.reduce((acc, item) => {
      // 1. 类型匹配 (如果选了全部(0)则忽略此条件)
      const categoryMatch = filterCategory.value === 0 || item.category === filterCategory.value;

      // 2. 名称匹配
      const nameMatch = !filterText.value || item.deptName.includes(filterText.value);

      // 3. 递归处理子节点
      let filteredChildren: any[] = [];
      if (item.children && item.children.length > 0) {
        filteredChildren = filterTree(item.children);
      }

      // 逻辑：
      // 如果当前节点匹配，则保留（哪怕子节点不匹配，也要显示该节点）
      // 如果当前节点不匹配，但有匹配的子节点，则保留当前节点（作为路径展示）
      if ((categoryMatch && nameMatch) || filteredChildren.length > 0) {
        // 深拷贝避免影响原始数据引用
        const newItem = { ...item };
        if (filteredChildren.length > 0) {
          newItem.children = filteredChildren;
        } else {
          // 如果是因为自身匹配保留的，但子节点被过滤光了，根据业务需求可能需要清空 children
          // 这里为了展示完整树结构，保留空 children 也是合理的
          newItem.children = undefined;
        }
        acc.push(newItem);
      }

      return acc;
    }, [] as any[]);
  };

  const copyData = JSON.parse(JSON.stringify(rawDeptList.value));
  deptList.value = filterTree(copyData);
};

const toggleExpandAll = () => {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => {
    refreshTable.value = true;
  });
};

const cancel = () => {
  open.value = false;
  reset();
};

const reset = () => {
  Object.assign(form, initialForm);
};

const handleAdd = (row?: any) => {
  reset();
  if (row != undefined) {
    form.parentId = row.id;
    // 智能预设子部门类型
    if (row.category === 1) form.category = 2; // 父是学院 -> 子默认为系
    else if (row.category === 2) form.category = 3; // 父是系 -> 子默认为班级
    else form.category = 3;
  } else {
    form.parentId = 0 as any; // 根节点
    form.category = 1; // 根节点默认为学院
  }
  open.value = true;
  title.value = "添加部门";
  deptOptions.value = rawDeptList.value;
};

const handleEdit = async (row: any) => {
  reset();
  Object.assign(form, row);
  open.value = true;
  title.value = "修改部门";
  deptOptions.value = rawDeptList.value;
};

const submitForm = async () => {
  if (!deptFormRef.value) return;
  await deptFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      submitLoading.value = true;
      try {
        if (form.id != undefined) {
          await request.put(`/admin/dept/${form.id}`, form);
          ElMessage.success("修改成功");
        } else {
          await request.post('/admin/dept', form);
          ElMessage.success("新增成功");
        }
        open.value = false;
        getList();
      } catch (error) {
        // error handled
      } finally {
        submitLoading.value = false;
      }
    }
  });
};

const handleDelete = async (row: any) => {
  try {
    await request.delete(`/admin/dept/${row.id}`);
    ElMessage.success("删除成功");
    getList();
  } catch (error) {
    // error handled
  }
};

// --- 辅助显示方法 ---
const getCategoryLabel = (val: number) => {
  const map: Record<number, string> = { 1: '学院', 2: '系/专业', 3: '班级' };
  return map[val] || '未知';
}

const getCategoryType = (val: number) => {
  const map: Record<number, string> = { 1: 'danger', 2: 'primary', 3: 'success' };
  return map[val] || 'info';
}

const getCategoryIcon = (val: number) => {
  if (val === 1) return School;
  if (val === 2) return Management;
  if (val === 3) return Grid;
  return FolderOpened;
}

const getCategoryIconClass = (val: number) => {
  if (val === 1) return 'text-red-500 text-lg mr-2';
  if (val === 2) return 'text-blue-500 text-lg mr-2';
  if (val === 3) return 'text-green-500 text-lg mr-2';
  return 'text-gray-400 text-lg mr-2';
}

onMounted(() => {
  getList();
});
</script>

<style scoped>
.app-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}
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
}
.header-left {
  display: flex;
  gap: 12px;
  align-items: center;
}
.filter-input {
  width: 240px;
}
.ml-4 {
  margin-left: 16px;
}

/* 树形表格样式 */
.dept-name-cell {
  display: flex;
  align-items: center;
}
.leader-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.leader-avatar {
  background-color: #409eff;
  font-size: 12px;
}
.time-text {
  font-size: 12px;
  color: #909399;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}
.w-full {
  width: 100%;
}
</style>