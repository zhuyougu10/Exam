import { ref, onMounted, onUnmounted, watch } from 'vue'
import type { WebSocketMessage } from '@/utils/websocket'
import { 
  WebSocketClient, 
  createWebSocket, 
  getWebSocketInstance,
  closeWebSocket 
} from '@/utils/websocket'

/**
 * WebSocket连接状态
 */
export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'error'

/**
 * WebSocket Hook
 * 提供响应式的WebSocket连接管理
 */
export function useWebSocket() {
  const status = ref<ConnectionStatus>('disconnected')
  const isConnected = ref(false)
  const lastMessage = ref<WebSocketMessage | null>(null)
  
  let wsClient: WebSocketClient | null = null

  /**
   * 初始化WebSocket连接
   */
  const connect = (token: string) => {
    if (!token) {
      console.warn('[useWebSocket] 未提供token，无法连接')
      return
    }

    // 确定WebSocket URL (开发环境使用代理，生产环境使用相对路径)
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsHost = window.location.host
    const wsUrl = `${wsProtocol}//${wsHost}/ws/notice`

    status.value = 'connecting'
    
    wsClient = createWebSocket({
      url: wsUrl,
      token,
      onOpen: () => {
        status.value = 'connected'
        isConnected.value = true
      },
      onClose: () => {
        status.value = 'disconnected'
        isConnected.value = false
      },
      onError: () => {
        status.value = 'error'
        isConnected.value = false
      },
      onMessage: (message) => {
        lastMessage.value = message
      }
    })

    wsClient.connect()
  }

  /**
   * 断开WebSocket连接
   */
  const disconnect = () => {
    closeWebSocket()
    wsClient = null
    status.value = 'disconnected'
    isConnected.value = false
  }

  /**
   * 发送消息
   */
  const send = (message: WebSocketMessage): boolean => {
    const instance = getWebSocketInstance()
    if (instance) {
      return instance.send(message)
    }
    return false
  }

  /**
   * 注册消息处理器
   */
  const on = (type: string, handler: (data: any) => void) => {
    const instance = getWebSocketInstance()
    if (instance) {
      instance.on(type, handler)
    }
  }

  /**
   * 移除消息处理器
   */
  const off = (type: string, handler: (data: any) => void) => {
    const instance = getWebSocketInstance()
    if (instance) {
      instance.off(type, handler)
    }
  }

  return {
    status,
    isConnected,
    lastMessage,
    connect,
    disconnect,
    send,
    on,
    off
  }
}

/**
 * 通知WebSocket Hook
 * 专门用于处理系统通知的WebSocket连接
 */
export function useNoticeWebSocket() {
  const { status, isConnected, connect, disconnect, on, off } = useWebSocket()
  
  const unreadCount = ref(0)
  const notices = ref<any[]>([])

  /**
   * 处理新通知
   */
  const handleNewNotice = (data: any) => {
    if (data) {
      notices.value.unshift({
        ...data,
        isRead: false
      })
      unreadCount.value++
    }
  }

  /**
   * 处理未读数量更新
   */
  const handleUnreadCount = (count: number) => {
    unreadCount.value = count
  }

  /**
   * 初始化连接并注册处理器
   */
  const init = (token: string) => {
    connect(token)
    
    // 等待连接建立后注册处理器
    const checkConnection = setInterval(() => {
      const instance = getWebSocketInstance()
      if (instance?.isConnected) {
        clearInterval(checkConnection)
        instance.on('notice', handleNewNotice)
        instance.on('exam_notice', handleNewNotice)
        instance.on('unread_count', handleUnreadCount)
      }
    }, 100)

    // 超时清除
    setTimeout(() => clearInterval(checkConnection), 10000)
  }

  /**
   * 清理连接和处理器
   */
  const cleanup = () => {
    const instance = getWebSocketInstance()
    if (instance) {
      instance.off('notice', handleNewNotice)
      instance.off('exam_notice', handleNewNotice)
      instance.off('unread_count', handleUnreadCount)
    }
    disconnect()
  }

  /**
   * 标记所有为已读
   */
  const markAllRead = () => {
    notices.value.forEach(n => n.isRead = true)
    unreadCount.value = 0
  }

  /**
   * 标记单个为已读
   */
  const markRead = (noticeId: number) => {
    const notice = notices.value.find(n => n.id === noticeId)
    if (notice && !notice.isRead) {
      notice.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  /**
   * 设置通知列表
   */
  const setNotices = (list: any[]) => {
    notices.value = list
  }

  /**
   * 设置未读数量
   */
  const setUnreadCount = (count: number) => {
    unreadCount.value = count
  }

  return {
    status,
    isConnected,
    unreadCount,
    notices,
    init,
    cleanup,
    markAllRead,
    markRead,
    setNotices,
    setUnreadCount
  }
}
