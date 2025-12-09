<template>
  <div class="p-6">
    <el-card shadow="hover">
      <!-- 页面标题 -->
      <h2 class="text-xl font-bold mb-6">个人中心</h2>

      <el-tabs v-model="activeTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-card shadow="hover" class="mb-4">
            <template #header>
              <div class="flex justify-between items-center">
                <span>个人信息</span>
                <el-button type="primary" @click="handleEditBasic" :disabled="basicEditMode">
                  {{ basicEditMode ? '保存中...' : '编辑信息' }}
                </el-button>
              </div>
            </template>

            <el-form :model="userInfo" :rules="basicRules" ref="basicFormRef" label-width="120px">
              <!-- 头像 -->
              <el-form-item label="头像">
                <el-upload
                  class="avatar-uploader"
                  action="#"
                  :show-file-list="false"
                  :http-request="handleAvatarUpload"
                  :before-upload="beforeAvatarUpload"
                >
                  <img v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar" />
                  <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
                </el-upload>
              </el-form-item>

              <!-- 用户名 -->
              <el-form-item label="用户名">
                <el-input v-model="userInfo.username" disabled />
              </el-form-item>

              <!-- 真实姓名 -->
              <el-form-item label="真实姓名" prop="realName">
                <el-input v-model="userInfo.realName" :disabled="!basicEditMode" />
              </el-form-item>

              <!-- 角色 -->
              <el-form-item label="角色">
                <el-input :value="getRoleName(userInfo.role)" disabled />
              </el-form-item>

              <!-- 邮箱 -->
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="userInfo.email" type="email" :disabled="!basicEditMode" />
              </el-form-item>

              <!-- 手机号 -->
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="userInfo.phone" :disabled="!basicEditMode" />
              </el-form-item>

              <!-- 状态 -->
              <el-form-item label="账号状态">
                <el-tag :type="userInfo.status === 1 ? 'success' : 'danger'">
                  {{ userInfo.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </el-form-item>

              <!-- 操作按钮 -->
              <el-form-item v-if="basicEditMode">
                <el-button @click="cancelEditBasic">取消</el-button>
                <el-button type="primary" @click="saveBasicInfo" :loading="basicEditMode">保存</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-tab-pane>

        <!-- 密码修改 -->
        <el-tab-pane label="密码修改" name="password">
          <el-card shadow="hover">
            <template #header>
              <span>修改密码</span>
            </template>

            <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="120px">
              <!-- 原密码 -->
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
              </el-form-item>

              <!-- 新密码 -->
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
              </el-form-item>

              <!-- 确认新密码 -->
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
              </el-form-item>

              <!-- 密码规则提示 -->
              <el-form-item>
                <el-alert
                  title="密码规则：至少8位，包含字母和数字"
                  type="info"
                  show-icon
                  :closable="false"
                />
              </el-form-item>

              <!-- 操作按钮 -->
              <el-form-item>
                <el-button @click="resetPasswordForm">重置</el-button>
                <el-button type="primary" @click="savePassword" :loading="passwordSaving">保存</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { Plus } from '@element-plus/icons-vue'

// 数据定义
const activeTab = ref('basic')
const loading = ref(false)
const basicEditMode = ref(false)
const basicFormRef = ref()
const passwordFormRef = ref()
const passwordSaving = ref(false)

// 用户信息
const userInfo = reactive({
  id: undefined,
  username: '',
  realName: '',
  avatar: '',
  role: 0,
  deptId: 0,
  email: '',
  phone: '',
  status: 1,
  createTime: ''
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单验证规则
const basicRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/, message: '密码至少8位，包含字母和数字', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: (rule: any, value: any, callback: any) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }, message: '两次输入的密码不一致', trigger: 'blur' }
  ]
}

// 初始化
onMounted(() => {
  fetchUserInfo()
})

// 获取用户信息
const fetchUserInfo = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/api/profile')
    Object.assign(userInfo, res)
  } catch (error) {
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

// 获取角色名称
const getRoleName = (role: number) => {
  const roleMap: Record<number, string> = {
    1: '学生',
    2: '教师',
    3: '管理员'
  }
  return roleMap[role] || '未知角色'
}

// 编辑基本信息
const handleEditBasic = () => {
  basicEditMode.value = true
}

// 取消编辑基本信息
const cancelEditBasic = () => {
  basicEditMode.value = false
  fetchUserInfo() // 重置数据
}

// 保存基本信息
const saveBasicInfo = async () => {
  if (!basicFormRef.value) return
  await basicFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      basicEditMode.value = true
      try {
        await request.put('/api/profile', userInfo)
        ElMessage.success('基本信息更新成功')
        basicEditMode.value = false
      } catch (error) {
        // error handled by interceptor
      } finally {
        basicEditMode.value = false
      }
    }
  })
}

// 头像上传处理（模拟）
const handleAvatarUpload = () => {
  // 这里只是模拟，实际项目中需要调用真实的上传接口
  ElMessage.info('头像上传功能待实现')
}

// 头像上传前验证
const beforeAvatarUpload = (file: File) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG) {
    ElMessage.error('上传头像只能是 JPG/PNG 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('上传头像大小不能超过 2MB!')
  }
  return isJPG && isLt2M
}

// 保存密码
const savePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      passwordSaving.value = true
      try {
        await request.put('/api/profile/password', passwordForm)
        ElMessage.success('密码修改成功')
        resetPasswordForm()
      } catch (error) {
        // error handled by interceptor
      } finally {
        passwordSaving.value = false
      }
    }
  })
}

// 重置密码表单
const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.resetFields()
}
</script>

<style scoped>
.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  line-height: 120px;
  text-align: center;
}
</style>
