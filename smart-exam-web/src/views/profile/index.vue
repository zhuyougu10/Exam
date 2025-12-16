<template>
  <div class="app-container p-6 bg-gray-50 min-h-screen">
    <el-row :gutter="20">
      <!-- 左侧：个人简介卡片 -->
      <el-col :span="8" :xs="24">
        <el-card shadow="never" class="border-0 rounded-xl mb-4">
          <div class="user-profile">
            <div class="text-center">
              <el-avatar
                  :size="100"
                  :src="userInfo.avatar"
                  class="border-4 border-indigo-50 shadow-sm mb-4"
              >
                {{ userInfo.realName?.charAt(0) }}
              </el-avatar>
              <div class="font-bold text-xl text-gray-800 mb-1">{{ userInfo.realName }}</div>
              <div class="text-sm text-gray-500 mb-4">@{{ userInfo.username }}</div>

              <div class="flex justify-center gap-2 mb-6">
                <el-tag :type="getRoleType(userInfo.role)" effect="dark" round>
                  {{ getRoleName(userInfo.role) }}
                </el-tag>
                <el-tag type="info" effect="plain" round v-if="userInfo.deptName">
                  {{ userInfo.deptName }}
                </el-tag>
              </div>
            </div>

            <el-divider />

            <div class="user-bio">
              <div class="bio-item">
                <el-icon><Message /></el-icon>
                <span>{{ userInfo.email || '未绑定邮箱' }}</span>
              </div>
              <div class="bio-item">
                <el-icon><Phone /></el-icon>
                <span>{{ userInfo.phone || '未绑定手机' }}</span>
              </div>
              <div class="bio-item">
                <el-icon><Calendar /></el-icon>
                <span>注册于 {{ formatTime(userInfo.createTime) }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：资料设置面板 -->
      <el-col :span="16" :xs="24">
        <el-card shadow="never" class="border-0 rounded-xl">
          <el-tabs v-model="activeTab" class="profile-tabs">
            <!-- 基本资料 Tab -->
            <el-tab-pane label="基本资料" name="info">
              <div class="py-4 pr-12">
                <el-form ref="infoFormRef" :model="infoForm" :rules="infoRules" label-width="100px">
                  <el-form-item label="用户头像">
                    <div class="flex items-center gap-4">
                      <el-avatar :size="60" :src="infoForm.avatar" />
                      <!-- 这里简化处理，实际可以使用上传组件 -->
                      <el-input v-model="infoForm.avatar" placeholder="请输入头像 URL 链接" class="w-full">
                        <template #prepend>HTTPS</template>
                      </el-input>
                    </div>
                  </el-form-item>
                  <el-form-item label="真实姓名">
                    <el-input v-model="infoForm.realName" disabled />
                  </el-form-item>
                  <el-form-item label="手机号码" prop="phone">
                    <el-input v-model="infoForm.phone" placeholder="请输入手机号码" maxlength="11" />
                  </el-form-item>
                  <el-form-item label="电子邮箱" prop="email">
                    <el-input v-model="infoForm.email" placeholder="请输入电子邮箱" />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" color="#6366f1" :loading="loading" @click="updateProfile">
                      保存修改
                    </el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>

            <!-- 账号安全 Tab -->
            <el-tab-pane label="账号安全" name="security">
              <div class="py-4 pr-12">
                <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
                  <el-form-item label="旧密码" prop="oldPassword">
                    <el-input
                        v-model="pwdForm.oldPassword"
                        type="password"
                        show-password
                        placeholder="请输入当前使用的密码"
                    />
                  </el-form-item>
                  <el-form-item label="新密码" prop="newPassword">
                    <el-input
                        v-model="pwdForm.newPassword"
                        type="password"
                        show-password
                        placeholder="请输入新密码（至少6位）"
                    />
                  </el-form-item>
                  <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input
                        v-model="pwdForm.confirmPassword"
                        type="password"
                        show-password
                        placeholder="请再次输入新密码"
                    />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="danger" :loading="pwdLoading" @click="updatePassword">
                      修改密码
                    </el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Message, Phone, Calendar, User, Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const activeTab = ref('info')
const loading = ref(false)
const pwdLoading = ref(false)

// 用户信息
const userInfo = ref<any>({})

// 表单引用
const infoFormRef = ref()
const pwdFormRef = ref()

// 资料表单
const infoForm = reactive({
  realName: '',
  phone: '',
  email: '',
  avatar: ''
})

// 密码表单
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 校验规则
const infoRules = {
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }]
}

const validatePass2 = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validatePass2, trigger: 'blur' }]
}

// 获取个人信息
const getProfile = async () => {
  try {
    const res: any = await request.get('/profile')
    userInfo.value = res
    // 同步到表单
    Object.assign(infoForm, {
      realName: res.realName,
      phone: res.phone,
      email: res.email,
      avatar: res.avatar
    })
  } catch (error) {
    console.error(error)
  }
}

// 更新资料
const updateProfile = async () => {
  if (!infoFormRef.value) return
  await infoFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        await request.put('/profile', infoForm)
        ElMessage.success('资料更新成功')
        // 更新本地状态
        await userStore.getUserInfo() // 刷新 Store
        getProfile() // 刷新页面数据
      } catch (error) {
        // error handled
      } finally {
        loading.value = false
      }
    }
  })
}

// 修改密码
const updatePassword = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      pwdLoading.value = true
      try {
        await request.put('/profile/password', {
          oldPassword: pwdForm.oldPassword,
          newPassword: pwdForm.newPassword
        })
        ElMessage.success('密码修改成功，请重新登录')
        // 登出
        setTimeout(() => {
          userStore.logout()
        }, 1500)
      } catch (error) {
        // error handled
      } finally {
        pwdLoading.value = false
      }
    }
  })
}

// 辅助方法
const formatTime = (time: string) => {
  return time ? time.split('T')[0] : '-'
}

const getRoleName = (role: number) => {
  const map: any = { 1: '学生', 2: '教师', 3: '管理员' }
  return map[role] || '未知角色'
}

const getRoleType = (role: number) => {
  const map: any = { 1: '', 2: 'warning', 3: 'danger' }
  return map[role] || 'info'
}

onMounted(() => {
  getProfile()
})
</script>

<style scoped lang="scss">
.user-profile {
  padding: 20px 0;

  .user-bio {
    padding: 0 20px;

    .bio-item {
      display: flex;
      align-items: center;
      color: #606266;
      margin-bottom: 12px;
      font-size: 14px;

      .el-icon {
        margin-right: 10px;
        font-size: 16px;
      }
    }
  }
}

.profile-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 24px;
  }
  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
    background-color: #f0f2f5;
  }
}
</style>