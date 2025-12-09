<template>
  <div class="login-container">
    <!-- 科技感背景 SVG -->
    <div class="background-svg">
      <svg width="100%" height="100%" viewBox="0 0 1000 1000" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="bgGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#4a90e2" stop-opacity="0.1" />
            <stop offset="50%" stop-color="#50e3c2" stop-opacity="0.1" />
            <stop offset="100%" stop-color="#b8e986" stop-opacity="0.1" />
          </linearGradient>
          <filter id="glow">
            <feGaussianBlur stdDeviation="3" result="coloredBlur" />
            <feMerge>
              <feMergeNode in="coloredBlur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        </defs>
        <!-- 背景网格线 -->
        <g opacity="0.3">
          <pattern id="grid" width="50" height="50" patternUnits="userSpaceOnUse">
            <path d="M 50 0 L 0 0 0 50" fill="none" stroke="#4a90e2" stroke-width="0.5" />
          </pattern>
          <rect width="100%" height="100%" fill="url(#grid)" />
        </g>
        <!-- 装饰性圆形 -->
        <circle cx="200" cy="300" r="150" fill="url(#bgGradient)" filter="url(#glow)" opacity="0.5" />
        <circle cx="800" cy="200" r="100" fill="url(#bgGradient)" filter="url(#glow)" opacity="0.4" />
        <circle cx="150" cy="700" r="120" fill="url(#bgGradient)" filter="url(#glow)" opacity="0.3" />
        <circle cx="850" cy="750" r="180" fill="url(#bgGradient)" filter="url(#glow)" opacity="0.5" />
      </svg>
    </div>
    
    <!-- 登录卡片 -->
    <div class="login-card-wrapper">
      <el-card class="login-card" shadow="hover">
        <!-- 卡片头部 -->
        <template #header>
          <div class="login-card-header">
            <div class="logo">
              <el-icon class="logo-icon"><Cpu /></el-icon>
            </div>
            <h2 class="title">智能考试系统</h2>
            <p class="subtitle">欢迎登录，开始您的智能考试之旅</p>
          </div>
        </template>
        
        <!-- 登录表单 -->
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          class="login-form"
        >
          <!-- 账号输入框 -->
          <el-form-item prop="username">
            <template #label>
              <div class="form-label">
                <span>账号</span>
                <el-icon class="label-icon"><User /></el-icon>
              </div>
            </template>
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              clearable
              autocomplete="off"
            />
          </el-form-item>
          
          <!-- 密码输入框 -->
          <el-form-item prop="password">
            <template #label>
              <div class="form-label">
                <span>密码</span>
                <el-icon class="label-icon"><Lock /></el-icon>
              </div>
            </template>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              :show-password="showPassword"
              clearable
              autocomplete="off"
            />
          </el-form-item>
          
          <!-- 记住我和忘记密码 -->
          <div class="form-footer">
            <el-checkbox v-model="loginForm.rememberMe" class="remember-me">
              记住我
            </el-checkbox>
            <el-button type="text" class="forgot-password">
              忘记密码？
            </el-button>
          </div>
          
          <!-- 登录按钮 -->
          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              @click="handleLogin"
              class="login-button"
              :disabled="loading"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
      
      <!-- 版权信息 -->
      <div class="copyright">
        © 2025 智能考试系统 - 版权所有
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

// 引入 Element Plus 图标
import { Cpu, User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

// 表单引用
const loginFormRef = ref()

// 加载状态
const loading = ref(false)

// 密码可见性
const showPassword = ref(false)

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})

// 表单验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 32, message: '用户名长度在 4 到 32 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度在 6 到 64 个字符', trigger: 'blur' }
  ]
}

// 处理登录
const handleLogin = async () => {
  // 表单验证
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      // 开始加载
      loading.value = true
      
      try {
        // 调用登录 API
        const result = await userStore.login(loginForm)
        // 登录成功
        ElMessage.success('登录成功')
        
        let redirectPath = '/dashboard'
        let role = userStore.roles
        if (role.includes("admin")) {
          // 管理员
          redirectPath = '/admin/user-manage'
        } else if (role.includes("teacher")) {
          // 教师
          redirectPath = '/teacher/dashboard'
        } else if (role.includes("student")) {
          // 学生
          redirectPath = '/student/dashboard'
        }
        
        // 跳转到目标页面
        router.push(redirectPath)
      } catch (error: any) {
        // 登录失败
        ElMessage.error(error.message || '登录失败，请稍后重试')
      } finally {
        // 结束加载
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
// 登录页面样式重置
:deep(body),
:deep(html) {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

// 登录容器
.login-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  background-color: #f5f7fa;
  margin: 0;
  padding: 0;
}

// 背景 SVG
.background-svg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  margin: 0;
  padding: 0;
}

// 登录卡片容器
.login-card-wrapper {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
}

// 登录卡片
.login-card {
  width: 420px;
  max-width: 90%;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  animation: cardSlideIn 0.6s ease-out;
  
  // 卡片动画
  @keyframes cardSlideIn {
    from {
      opacity: 0;
      transform: translateY(-20px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
  
  // 卡片头部
  .login-card-header {
    text-align: center;
    padding: 20px 0;
    
    .logo {
      margin-bottom: 20px;
      
      .logo-icon {
        font-size: 48px;
        color: #1890ff;
        animation: logoPulse 2s ease-in-out infinite;
        
        @keyframes logoPulse {
          0%, 100% {
            transform: scale(1);
          }
          50% {
            transform: scale(1.1);
          }
        }
      }
    }
    
    .title {
      margin: 0 0 8px;
      font-size: 24px;
      font-weight: bold;
      color: #303133;
    }
    
    .subtitle {
      margin: 0;
      font-size: 14px;
      color: #909399;
    }
  }
  
  // 登录表单
  .login-form {
    padding: 0 20px 20px;
  }
  
  // 表单标签
  .form-label {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 500;
    color: #303133;
    margin-bottom: 8px;
    
    .label-icon {
      font-size: 14px;
      color: #909399;
    }
  }
  
  // 表单底部
  .form-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    .remember-me {
      font-size: 14px;
      color: #606266;
    }
    
    .forgot-password {
      font-size: 14px;
      color: #1890ff;
    }
  }
  
  // 登录按钮
  .login-button {
    width: 100%;
    height: 40px;
    font-size: 16px;
    font-weight: 500;
    border-radius: 8px;
    transition: all 0.3s ease;
    
    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
    }
    
    &:active {
      transform: translateY(0);
    }
  }
}

// 版权信息
.copyright {
  margin-top: 20px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

// 响应式设计
@media (max-width: 768px) {
  .login-card {
    width: 100%;
    margin: 0 20px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  }
  
  .login-card-header {
    padding: 16px 0;
    
    .title {
      font-size: 20px;
    }
    
    .logo .logo-icon {
      font-size: 40px;
    }
  }
  
  .login-form {
    padding: 0 16px 16px;
  }
}
</style>
