import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import router from '@/router'

// 登录表单类型
export interface LoginForm {
  username: string
  password: string
}

// 登录响应类型
export interface UserLoginResponse {
  token: string
  userInfo: {
    id: number
    role: number
    deptId: number
    name: string
  }
}

// 用户信息类型
export interface UserInfo {
  id: number
  username: string
  realName: string
  role: number
  deptId: number
  email: string
  phone: string
  avatar: string
  status: number
  createTime: string
}

// 定义用户状态管理模块
export const useUserStore = defineStore('user', () => {
  // 状态定义
  const token = ref<string | null>(localStorage.getItem('token') || null)
  const userInfo = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed(() => userInfo.value?.role || 0)

  /**
   * 设置用户角色
   * @param role 角色ID
   */
  const setUserRoles = (role: number) => {
    // 根据角色ID设置角色权限
    switch (role) {
      case 1:
        // 学生
        roles.value = ['student']
        break
      case 2:
        // 教师
        roles.value = ['teacher']
        break
      case 3:
        // 管理员
        roles.value = ['admin']
        break
      default:
        roles.value = []
    }
  }

  /**
   * 用户登录
   * @param form 登录表单数据
   * @returns 登录结果
   */
  const login = async (form: LoginForm): Promise<UserLoginResponse> => {
    try {
      // 调用登录 API
      const data = await request.post<UserLoginResponse>('/auth/login', form)
      
      // 保存 token
      token.value = data.token
      localStorage.setItem('token', data.token)
      
      // 更新用户信息
      userInfo.value = {
        id: data.userInfo.id,
        username: '',
        realName: data.userInfo.name,
        role: data.userInfo.role,
        deptId: data.userInfo.deptId,
        email: '',
        phone: '',
        avatar: '',
        status: 1,
        createTime: ''
      }
      
      // 设置用户角色
      setUserRoles(data.userInfo.role)
      
      return data
    } catch (error) {
      // 登录失败，清除状态
      logout()
      throw error
    }
  }

  /**
   * 用户登出
   */
  const logout = () => {
    // 清除 token
    token.value = null
    localStorage.removeItem('token')
    
    // 重置状态
    userInfo.value = null
    roles.value = []
    
    // 直接使用 href 跳转，触发浏览器刷新
    // 既解决了权限残留问题，又避免了 router.push + reload 导致的双重闪烁
    window.location.href = '/login'
  }

  /**
   * 获取用户详细信息
   * @returns 用户信息
   */
  const getUserInfo = async (): Promise<UserInfo> => {
    try {
      // 调用获取用户信息 API
      const data = await request.get<UserInfo>('/auth/info')
      
      // 更新用户信息
      userInfo.value = data
      
      // 设置用户角色
      setUserRoles(data.role)
      
      return data
    } catch (error) {
      // 获取失败，清除状态
      logout()
      throw error
    }
  }

  return {
    // 状态
    token,
    userInfo,
    roles,
    // 计算属性
    isLoggedIn,
    userRole,
    // 方法
    login,
    logout,
    getUserInfo,
    setUserRoles
  }
}, {
  // 持久化配置
  persist: true
})