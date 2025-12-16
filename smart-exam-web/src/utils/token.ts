/**
 * Token工具类
 * 用于解析和验证JWT Token
 */

/**
 * 解析JWT Token的payload部分
 * @param token JWT Token字符串
 * @returns 解析后的payload对象，解析失败返回null
 */
export function parseToken(token: string): any {
  try {
    // JWT由三部分组成：header.payload.signature
    const parts = token.split('.')
    if (parts.length !== 3) {
      return null
    }
    
    // 解码payload（第二部分）
    const payload = parts[1]
    // Base64Url解码
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(decoded)
  } catch (error) {
    console.error('Token解析失败:', error)
    return null
  }
}

/**
 * 检查Token是否已过期
 * @param token JWT Token字符串
 * @param bufferSeconds 提前多少秒判定为过期（默认60秒，用于提前刷新）
 * @returns true表示已过期，false表示未过期
 */
export function isTokenExpired(token: string | null, bufferSeconds: number = 60): boolean {
  if (!token) {
    return true
  }
  
  const payload = parseToken(token)
  if (!payload || !payload.exp) {
    return true
  }
  
  // exp是Unix时间戳（秒）
  const expireTime = payload.exp * 1000 // 转换为毫秒
  const now = Date.now()
  const bufferMs = bufferSeconds * 1000
  
  // 如果当前时间 + 缓冲时间 > 过期时间，则判定为过期
  return now + bufferMs > expireTime
}

/**
 * 获取Token剩余有效时间（秒）
 * @param token JWT Token字符串
 * @returns 剩余秒数，已过期返回0，解析失败返回-1
 */
export function getTokenRemainingTime(token: string | null): number {
  if (!token) {
    return -1
  }
  
  const payload = parseToken(token)
  if (!payload || !payload.exp) {
    return -1
  }
  
  const expireTime = payload.exp * 1000
  const remaining = Math.floor((expireTime - Date.now()) / 1000)
  
  return remaining > 0 ? remaining : 0
}

/**
 * 从Token中获取用户ID
 * @param token JWT Token字符串
 * @returns 用户ID，解析失败返回null
 */
export function getUserIdFromToken(token: string | null): number | null {
  if (!token) {
    return null
  }
  
  const payload = parseToken(token)
  return payload?.userId || null
}

/**
 * 从Token中获取用户角色
 * @param token JWT Token字符串
 * @returns 角色ID，解析失败返回null
 */
export function getRoleFromToken(token: string | null): number | null {
  if (!token) {
    return null
  }
  
  const payload = parseToken(token)
  return payload?.role || null
}
