import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 自定义 Axios 实例类型
interface CustomAxiosInstance extends AxiosInstance {
  get<T>(url: string, config?: InternalAxiosRequestConfig): Promise<T>
  post<T>(url: string, data?: any, config?: InternalAxiosRequestConfig): Promise<T>
  put<T>(url: string, data?: any, config?: InternalAxiosRequestConfig): Promise<T>
  delete<T>(url: string, config?: InternalAxiosRequestConfig): Promise<T>
}

// 创建 Axios 实例
const service: CustomAxiosInstance = axios.create({
  baseURL: '/api', // API 基础路径
  timeout: 30000, // 请求超时时间 (下载文件可能需要更长时间)
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
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    },
    (error: any) => {
      ElMessage.error('请求出错，请稍后重试')
      return Promise.reject(error)
    }
)

// 响应拦截器
service.interceptors.response.use(
    (response: AxiosResponse) => {
      // 特殊处理：如果是下载文件 (blob)，直接返回 data
      if (response.config.responseType === 'blob' || response.headers['content-type']?.includes('application/vnd.ms-excel') || response.headers['content-type']?.includes('application/vnd.openxmlformats-officedocument')) {
        return response.data
      }

      // 常规 JSON 响应处理
      const { code, message, data } = response.data

      if (code === 200) {
        return data
      } else {
        ElMessage.error(message || '操作失败')
        return Promise.reject(new Error(message || '操作失败'))
      }
    },
    (error: any) => {
      if (error.response) {
        const { status, data } = error.response

        // 如果是 blob 类型请求出错，data 是 blob，需要转为 json 读取错误信息
        if (error.config && error.config.responseType === 'blob') {
          const reader = new FileReader()
          reader.onload = () => {
            try {
              const errorMsg = JSON.parse(reader.result as string)
              ElMessage.error(errorMsg.message || '下载失败')
            } catch (e) {
              ElMessage.error('下载失败')
            }
          }
          reader.readAsText(data)
          return Promise.reject(error)
        }

        switch (status) {
          case 401:
            localStorage.removeItem('token')
            ElMessage.error('登录已过期，请重新登录')
            router.push({
              path: '/login',
              query: { redirect: router.currentRoute.value.fullPath }
            })
            break
          case 403:
            ElMessage.error(data?.message || '没有操作权限')
            break
          case 404:
            ElMessage.error(data?.message || '请求的资源不存在')
            break
          case 500:
            ElMessage.error(data?.message || '服务器内部错误')
            break
          default:
            ElMessage.error(data?.message || `请求错误（${status}）`)
        }
      } else if (error.request) {
        ElMessage.error('网络异常，请检查网络连接')
      } else {
        ElMessage.error('请求配置错误')
      }

      return Promise.reject(error)
    }
)

export default service