import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 自定义 Axios 实例类型
interface CustomAxiosInstance extends AxiosInstance {
  // 覆盖请求方法类型，使其返回数据而不是 AxiosResponse
  get<T>(url: string, config?: InternalAxiosRequestConfig): Promise<T>
  post<T>(url: string, data?: any, config?: InternalAxiosRequestConfig): Promise<T>
  put<T>(url: string, data?: any, config?: InternalAxiosRequestConfig): Promise<T>
  delete<T>(url: string, config?: InternalAxiosRequestConfig): Promise<T>
}

// 创建 Axios 实例
const service: CustomAxiosInstance = axios.create({
  baseURL: '/api', // API 基础路径
  timeout: 15000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
}) as CustomAxiosInstance

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')
    if (token) {
      // 添加 Authorization 头
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: any) => {
    // 处理请求错误
    ElMessage.error('请求出错，请稍后重试')
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    // 解构响应数据
    const { code, message, data } = response.data
    
    // 检查响应状态码
    if (code === 200) {
      // 成功响应，直接返回 data
      return data
    } else {
      // 业务错误，显示错误信息
      ElMessage.error(message || '操作失败')
      return Promise.reject(new Error(message || '操作失败'))
    }
  },
  (error: any) => {
    // 处理网络错误
    if (error.response) {
      // HTTP 错误状态码处理
      const { status, data } = error.response
      
      switch (status) {
        case 401:
          // 未授权，清除 token 并跳转到登录页
          localStorage.removeItem('token')
          ElMessage.error(data?.message || '登录已过期，请重新登录')
          // 保存当前页面路径，登录后可跳转回来
          router.push({
            path: '/login',
            query: { redirect: router.currentRoute.value.fullPath }
          })
          break
        case 403:
          // 权限不足
          ElMessage.error(data?.message || '没有操作权限')
          break
        case 404:
          // 资源不存在
          ElMessage.error(data?.message || '请求的资源不存在')
          break
        case 500:
          // 服务器错误
          ElMessage.error(data?.message || '服务器内部错误')
          break
        default:
          // 其他错误
          ElMessage.error(data?.message || `请求错误（${status}）`)
      }
    } else if (error.request) {
      // 请求已发出，但没有收到响应
      ElMessage.error('网络异常，请检查网络连接')
    } else {
      // 请求配置错误
      ElMessage.error('请求配置错误')
    }
    
    return Promise.reject(error)
  }
)

export default service
