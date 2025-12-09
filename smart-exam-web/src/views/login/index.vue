<template>
  <div class="login-container">
    <div class="animated-background">
      <div class="cube"></div>
      <div class="cube"></div>
      <div class="cube"></div>
      <div class="cube"></div>
      <div class="cube"></div>
    </div>
    
    <div class="login-content">
      <div class="login-left hidden-xs-only">
        <div class="branding">
          <div class="brand-logo-box">
            <el-icon class="brand-icon"><Cpu /></el-icon>
          </div>
          
          <h1 class="brand-title">Smart Exam</h1>
          <p class="brand-slogan">智 / 能 / 考 / 试 / 系 / 统</p>
          <div class="brand-desc">
            基于 AI 技术的全流程考试解决方案<br>
            让出题更智能，让阅卷更轻松
          </div>
        </div>
      </div>

      <div class="login-right">
        <el-card class="login-card" shadow="never">
          <div class="login-header">
            <h2 class="title">欢迎登录</h2>
            <p class="subtitle">请输入您的账号和密码</p>
          </div>
          
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            class="login-form"
            size="large"
          >
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="用户名"
                :prefix-icon="User"
              />
            </el-form-item>
            
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="密码"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            
            <div class="form-options">
              <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
              <el-link type="primary" :underline="false">忘记密码？</el-link>
            </div>
            
            <el-button
              type="primary"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
              round
            >
              {{ loading ? '登录中...' : '立即登录' }}
            </el-button>
          </el-form>
          
          <div class="login-footer">
            <span>还没有账号？</span>
            <el-link type="primary" :underline="false">联系管理员</el-link>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
// 引入 Cpu 图标作为 Logo
import { User, Lock, Cpu } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login(loginForm)
        ElMessage.success('登录成功，正在跳转...')
        
        let role = userStore.roles
        let redirectPath = '/dashboard'
        if (role.includes("admin")) redirectPath = '/admin/user-manage'
        else if (role.includes("teacher")) redirectPath = '/teacher/dashboard'
        else if (role.includes("student")) redirectPath = '/student/dashboard'
        
        router.push(redirectPath)
      } catch (error: any) {
        ElMessage.error(error.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
.login-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 动态背景动画 */
.animated-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
  
  .cube {
    position: absolute;
    top: 80vh;
    left: 45vw;
    width: 10px;
    height: 10px;
    border: solid 1px rgba(255, 255, 255, 0.2);
    transform-origin: top left;
    transform: scale(0) rotate(0deg) translate(-50%, -50%);
    animation: cube 12s ease-in forwards infinite;
    
    &:nth-child(1) { animation-delay: 2s; left: 25vw; top: 40vh; border-color: rgba(255,255,255,0.3); }
    &:nth-child(2) { animation-delay: 4s; left: 75vw; top: 50vh; border-color: rgba(255,255,255,0.3); }
    &:nth-child(3) { animation-delay: 6s; left: 90vw; top: 10vh; border-color: rgba(255,255,255,0.3); }
    &:nth-child(4) { animation-delay: 8s; left: 10vw; top: 85vh; border-color: rgba(255,255,255,0.3); }
    &:nth-child(5) { animation-delay: 10s; left: 50vw; top: 10vh; border-color: rgba(255,255,255,0.3); }
  }
}

@keyframes cube {
  from { transform: scale(0) rotate(0deg) translate(-50%, -50%); opacity: 1; }
  to { transform: scale(20) rotate(960deg) translate(-50%, -50%); opacity: 0; }
}

.login-content {
  position: relative;
  z-index: 1;
  display: flex;
  width: 900px;
  height: 550px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: slideUp 0.8s cubic-bezier(0.2, 0.8, 0.2, 1);
  
  .login-left {
    flex: 1;
    background: rgba(0, 0, 0, 0.2);
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 60px;
    color: #fff;
    
    /* 替代图片的 Logo 样式 */
    .brand-logo-box {
      width: 64px;
      height: 64px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 20px;
      backdrop-filter: blur(5px);
      box-shadow: 0 4px 15px rgba(0,0,0,0.1);
      
      .brand-icon {
        font-size: 36px;
        color: #fff;
      }
    }
    
    .brand-title {
      font-size: 48px;
      font-weight: 800;
      margin-bottom: 10px;
      letter-spacing: 2px;
      text-shadow: 0 2px 4px rgba(0,0,0,0.3);
    }
    
    .brand-slogan {
      font-size: 18px;
      opacity: 0.9;
      margin-bottom: 40px;
      letter-spacing: 5px;
    }
    
    .brand-desc {
      font-size: 14px;
      line-height: 1.8;
      opacity: 0.8;
      border-left: 4px solid #fff;
      padding-left: 20px;
    }
  }
  
  .login-right {
    flex: 1;
    background: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px;
  }
}

.login-card {
  width: 100%;
  border: none !important;
  
  :deep(.el-card__body) {
    padding: 0;
  }
  
  .login-header {
    text-align: center;
    margin-bottom: 30px;
    
    .title {
      font-size: 28px;
      color: #333;
      font-weight: bold;
      margin-bottom: 10px;
    }
    
    .subtitle {
      color: #999;
      font-size: 14px;
    }
  }
  
  .login-form {
    :deep(.el-input__wrapper) {
      background-color: #f5f7fa;
      box-shadow: none !important;
      border-radius: 8px;
      padding: 10px 15px;
      transition: all 0.3s;
      
      &.is-focus {
        background-color: #fff;
        box-shadow: 0 0 0 1px #764ba2 !important;
      }
    }
    
    :deep(.el-input__inner) {
      height: 24px;
    }
  }
  
  .form-options {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 25px;
  }
  
  .login-btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 1px;
    background: linear-gradient(to right, #667eea, #764ba2);
    border: none;
    transition: transform 0.2s;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 5px 15px rgba(118, 75, 162, 0.4);
    }
    
    &:active {
      transform: translateY(0);
    }
  }
  
  .login-footer {
    margin-top: 20px;
    text-align: center;
    font-size: 14px;
    color: #666;
    
    .el-link {
      margin-left: 5px;
      font-weight: 500;
    }
  }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .login-content {
    width: 90%;
    height: auto;
    flex-direction: column;
    
    .login-left {
      display: none;
    }
    
    .login-right {
      padding: 30px 20px;
    }
  }
}
</style>