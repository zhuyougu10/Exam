/**
 * WebSocket客户端
 * 提供WebSocket连接管理、自动重连、心跳检测功能
 */

export interface WebSocketMessage {
  type: string
  message: string
  data?: any
  timestamp?: string
  success?: boolean
}

export interface WebSocketOptions {
  url: string
  token: string
  onMessage?: (message: WebSocketMessage) => void
  onOpen?: () => void
  onClose?: (event: CloseEvent) => void
  onError?: (error: Event) => void
  reconnectInterval?: number
  maxReconnectAttempts?: number
  heartbeatInterval?: number
}

export class WebSocketClient {
  private ws: WebSocket | null = null
  private options: WebSocketOptions
  private reconnectAttempts = 0
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private isManualClose = false
  private messageHandlers: Map<string, Set<(data: any) => void>> = new Map()

  constructor(options: WebSocketOptions) {
    this.options = {
      reconnectInterval: 3000,
      maxReconnectAttempts: 10,
      heartbeatInterval: 30000,
      ...options
    }
  }

  /**
   * 建立WebSocket连接
   */
  connect(): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      console.log('[WebSocket] 已连接，无需重复连接')
      return
    }

    this.isManualClose = false
    const url = `${this.options.url}?token=${this.options.token}`
    
    try {
      this.ws = new WebSocket(url)
      this.setupEventHandlers()
    } catch (error) {
      console.error('[WebSocket] 连接创建失败:', error)
      this.scheduleReconnect()
    }
  }

  /**
   * 设置WebSocket事件处理器
   */
  private setupEventHandlers(): void {
    if (!this.ws) return

    this.ws.onopen = () => {
      console.log('[WebSocket] 连接成功')
      this.reconnectAttempts = 0
      this.startHeartbeat()
      this.options.onOpen?.()
    }

    this.ws.onmessage = (event: MessageEvent) => {
      try {
        const message: WebSocketMessage = JSON.parse(event.data)
        console.log('[WebSocket] 收到消息:', message.type)
        
        // 调用全局消息处理器
        this.options.onMessage?.(message)
        
        // 调用类型特定的消息处理器
        const handlers = this.messageHandlers.get(message.type)
        if (handlers) {
          handlers.forEach(handler => handler(message.data))
        }
      } catch (error) {
        console.error('[WebSocket] 消息解析失败:', error)
      }
    }

    this.ws.onclose = (event: CloseEvent) => {
      console.log('[WebSocket] 连接关闭:', event.code, event.reason)
      this.stopHeartbeat()
      this.options.onClose?.(event)
      
      if (!this.isManualClose) {
        this.scheduleReconnect()
      }
    }

    this.ws.onerror = (error: Event) => {
      console.error('[WebSocket] 连接错误:', error)
      this.options.onError?.(error)
    }
  }

  /**
   * 启动心跳检测
   */
  private startHeartbeat(): void {
    this.stopHeartbeat()
    
    this.heartbeatTimer = setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.send({ type: 'ping', message: '心跳检测' })
      }
    }, this.options.heartbeatInterval)
  }

  /**
   * 停止心跳检测
   */
  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 计划重连
   */
  private scheduleReconnect(): void {
    if (this.isManualClose) return
    
    if (this.reconnectAttempts >= (this.options.maxReconnectAttempts || 10)) {
      console.error('[WebSocket] 达到最大重连次数，停止重连')
      return
    }

    this.reconnectAttempts++
    const delay = this.options.reconnectInterval! * Math.min(this.reconnectAttempts, 5)
    
    console.log(`[WebSocket] 将在 ${delay}ms 后进行第 ${this.reconnectAttempts} 次重连`)
    
    this.reconnectTimer = setTimeout(() => {
      console.log(`[WebSocket] 正在进行第 ${this.reconnectAttempts} 次重连...`)
      this.connect()
    }, delay)
  }

  /**
   * 发送消息
   */
  send(message: WebSocketMessage): boolean {
    if (this.ws?.readyState !== WebSocket.OPEN) {
      console.warn('[WebSocket] 连接未打开，无法发送消息')
      return false
    }

    try {
      this.ws.send(JSON.stringify(message))
      return true
    } catch (error) {
      console.error('[WebSocket] 发送消息失败:', error)
      return false
    }
  }

  /**
   * 注册消息类型处理器
   */
  on(type: string, handler: (data: any) => void): void {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, new Set())
    }
    this.messageHandlers.get(type)!.add(handler)
  }

  /**
   * 移除消息类型处理器
   */
  off(type: string, handler: (data: any) => void): void {
    const handlers = this.messageHandlers.get(type)
    if (handlers) {
      handlers.delete(handler)
    }
  }

  /**
   * 关闭连接
   */
  close(): void {
    this.isManualClose = true
    this.stopHeartbeat()
    
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    
    if (this.ws) {
      this.ws.close(1000, '主动关闭连接')
      this.ws = null
    }
    
    this.messageHandlers.clear()
    console.log('[WebSocket] 连接已关闭')
  }

  /**
   * 获取连接状态
   */
  get readyState(): number {
    return this.ws?.readyState ?? WebSocket.CLOSED
  }

  /**
   * 是否已连接
   */
  get isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }
}

// 单例实例
let wsInstance: WebSocketClient | null = null

/**
 * 获取WebSocket实例
 */
export function getWebSocketInstance(): WebSocketClient | null {
  return wsInstance
}

/**
 * 创建WebSocket实例
 */
export function createWebSocket(options: WebSocketOptions): WebSocketClient {
  if (wsInstance) {
    wsInstance.close()
  }
  wsInstance = new WebSocketClient(options)
  return wsInstance
}

/**
 * 关闭WebSocket实例
 */
export function closeWebSocket(): void {
  if (wsInstance) {
    wsInstance.close()
    wsInstance = null
  }
}
