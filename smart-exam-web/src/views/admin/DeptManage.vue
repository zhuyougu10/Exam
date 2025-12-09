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
          <el-button type="info" plain icon="Sort" @click="toggleExpandAll">
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
              <el-icon :class="['folder-icon', scope.row.children?.length ? 'has-child' : '']">
                <component :is="scope.row.children?.length ? 'FolderOpened' : 'Document'" />
              </el-icon>
              <span>{{ scope.row.deptName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="sortOrder" label="排序" width="80" align="center">
          <template #default="scope">
            <el-tag type="info" size="small">{{ scope.row.sortOrder }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" effect="plain">
              {{ scope.row.status === 1 ? '正常' : '停用' }}
            </el-tag>
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
            <el-form-item label="显示排序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" controls-position="right" :min="0" class="w-full" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="负责人" prop="leader">
              <el-input v-model="form.leader" placeholder="请输入负责人" prefix-icon="User" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" prefix-icon="Phone" maxlength="11" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" prefix-icon="Message" maxlength="50" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="部门状态">
              <el-radio-group v-model="form.status" class="w-full">
                <el-radio-button :value="1">正常</el-radio-button>
                <el-radio-button :value="0">停用</el-radio-button>
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
import { Search, Plus, Sort, Edit, Delete, User, FolderOpened, Document, Phone, Message } from '@element-plus/icons-vue';
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
const rawDeptList = ref<any[]>([]);

const deptFormRef = ref();

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

const rules = {
  parentId: [{ required: true, message: "上级部门不能为空", trigger: "change" }],
  deptName: [{ required: true, message: "部门名称不能为空", trigger: "blur" }],
  sortOrder: [{ required: true, message: "显示排序不能为空", trigger: "blur" }],
  email: [{ type: 'email', message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }]
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

const handleFilter = (val: string) => {
  if (!val) {
    deptList.value = rawDeptList.value;
    return;
  }
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
  const copyData = JSON.parse(JSON.stringify(rawDeptList.value));
  deptList.value = filterTree(copyData, val);
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

/* 树形表格样式 */
.dept-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.folder-icon {
  font-size: 16px;
  color: #909399;
}
.folder-icon.has-child {
  color: #e6a23c; /* 文件夹颜色 */
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