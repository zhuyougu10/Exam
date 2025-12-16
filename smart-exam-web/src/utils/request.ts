import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'
import { isTokenExpired, getTokenRemainingTime } from './token'

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

// 是否正在显示过期提示（避免重复弹窗）
let isShowingExpireDialog = false

// 请求拦截器
service.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      // 从 localStorage 获取 token
      const token = localStorage.getItem('token')
      
      if (token) {
        // 检查Token是否已过期（提前60秒判定）
        if (isTokenExpired(token, 60)) {
          // Token已过期，清除并跳转登录
          localStorage.removeItem('token')
          
          if (!isShowingExpireDialog && router.currentRoute.value.path !== '/login') {
            isShowingExpireDialog = true
            ElMessageBox.alert('登录已过期，请重新登录', '提示', {
              confirmButtonText: '确定',
              type: 'warning',
              callback: () => {
                isShowingExpireDialog = false
                window.location.href = '/login'
              }
            })
          }
          
          return Promise.reject(new Error('Token已过期'))
        }
        
        // Token未过期，添加 Authorization 头
        config.headers.Authorization = `Bearer ${token}`
        
        // 如果Token即将过期（5分钟内），打印警告日志
        const remaining = getTokenRemainingTime(token)
        if (remaining > 0 && remaining < 300) {
          console.warn(`Token将在 ${remaining} 秒后过期`)
        }
      }
      return config
    },
    (error: any) => {
      // 处理请求错误
      // 请求并未发出，通常是配置错误，可以提示
      console.error('Request Error:', error)
      return Promise.reject(error)
    }
)

// 响应拦截器
service.interceptors.response.use(
    (response: AxiosResponse) => {
      // 解构响应数据
      // 兼容后端返回字段: code/msg 或 code/message
      const res = response.data
      const code = res.code
      const msg = res.msg || res.message || '操作失败'

      // 检查响应状态码
      if (code === 200) {
        // 成功响应，直接返回 data
        return res.data
      } else {
        // 业务错误，显示详细错误信息
        // 后端业务异常通常返回非200状态码，这里统一弹窗
        ElMessage.error(msg)
        // 返回带有具体错误信息的 Error 对象，方便组件层获取 msg
        return Promise.reject(new Error(msg))
      }
    },
    (error: any) => {
      // 处理网络错误或 HTTP 状态码错误
      let message = '系统未知错误'

      if (error.response) {
        // HTTP 错误状态码处理
        const { status, data } = error.response
        // 优先使用后端返回的错误消息
        const errorMsg = data?.msg || data?.message

        switch (status) {
          case 400:
            message = errorMsg || '请求参数错误'
            break
          case 401:
            // 未授权，清除 token 并跳转到登录页
            localStorage.removeItem('token')
            message = errorMsg || '登录已过期，请重新登录'
            // 避免重复跳转
            if (router.currentRoute.value.path !== '/login') {
              router.push({
                path: '/login',
                query: { redirect: router.currentRoute.value.fullPath }
              })
            }
            break
          case 403:
            message = errorMsg || '没有操作权限'
            break
          case 404:
            message = errorMsg || '请求的资源不存在'
            break
          case 500:
            message = errorMsg || '服务器内部错误'
            break
          default:
            message = errorMsg || `请求错误（${status}）`
        }
      } else if (error.request) {
        // 请求已发出，但没有收到响应
        message = '网络异常，请检查网络连接'
      } else {
        // 请求配置错误
        message = '请求配置错误'
      }

      // 统一弹出错误提示
      ElMessage.error(message)

      // 将错误继续抛出，以便组件处理 loading 状态等
      return Promise.reject(new Error(message))
    }
)

export default service