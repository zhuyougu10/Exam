import router from './index'
import { asyncRoutes } from './index'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

// 路由白名单（无需登录即可访问）
const whiteList = ['/login']

// 路由守卫实现
router.beforeEach(async (to, _from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 智能考试系统` : '智能考试系统'
  
  const userStore = useUserStore()
  const token = userStore.token
  
  // 检查用户是否已登录
  if (token) {
    // 已登录用户，不允许访问登录页
    if (to.path === '/login') {
      // 修复死循环：已登录用户访问 login，根据角色跳转到对应首页
      const role = userStore.roles[0]
      if (role === 'student') next({ path: '/student/dashboard' })
      else if (role === 'teacher') next({ path: '/teacher/dashboard' })
      else if (role === 'admin') next({ path: '/admin/user-manage' })
      else next({ path: '/' }) // 兜底
      return
    }
    
    // 核心修复逻辑：
    // 不仅检查 roles 是否为空，还要检查动态路由是否已挂载。
    // 我们检查 'student', 'teacher', 'admin' 其中一个路由是否存在。
    const hasDynamicRoutes = router.hasRoute('student') || router.hasRoute('teacher') || router.hasRoute('admin')

    // 如果角色为空 OR 动态路由未加载
    if (userStore.roles.length === 0 || !hasDynamicRoutes) {
      try {
        // 如果没有角色信息（比如刷新页面时），重新获取
        if (userStore.roles.length === 0) {
          await userStore.getUserInfo()
        }
        
        // 根据用户角色过滤出可访问的路由
        const accessRoutes = filterAsyncRoutes(asyncRoutes, userStore.roles)
        
        // 动态添加路由
        accessRoutes.forEach(route => {
          router.addRoute(route)
        })
        
        // 确保动态路由已加载完成，然后跳转
        // replace: true 确保导航不会留下历史记录，防止用户回退到空白页
        next({ ...to, replace: true })
        return
      } catch (error) {
        // 获取用户信息失败，清除 token，跳转登录页
        userStore.logout()
        ElMessage.error('登录已过期，请重新登录')
        next({ path: '/login', query: { redirect: to.fullPath } })
        return
      }
    }
    
    // 已获取角色信息，直接跳转
    next()
  } else {
    // 未登录用户，检查是否在白名单中
    if (whiteList.includes(to.path)) {
      // 在白名单中，允许访问
      next()
    } else {
      // 不在白名单中，跳转登录页
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
  }
})

/**
 * 根据用户角色过滤路由
 * @param routes 待过滤的路由列表
 * @param roles 用户角色数组
 * @returns 过滤后的路由列表
 */
function filterAsyncRoutes(routes: any[], roles: string[]): any[] {
  const res: any[] = []
  
  routes.forEach(route => {
    const tmp = { ...route }
    
    // 检查当前路由是否有权限访问
    if (hasPermission(tmp, roles)) {
      // 递归过滤子路由
      if (tmp.children) {
        tmp.children = filterAsyncRoutes(tmp.children, roles)
      }
      res.push(tmp)
    }
  })
  
  return res
}

/**
 * 检查路由是否有权限访问
 * @param route 路由对象
 * @param roles 用户角色数组
 * @returns 是否有权限访问
 */
function hasPermission(route: any, roles: string[]): boolean {
  // 路由没有设置 roles，默认允许访问
  if (!route.meta?.roles || route.meta.roles.length === 0) {
    return true
  }
  
  // 检查用户角色是否包含在路由的 roles 中
  return roles.some(role => route.meta.roles.includes(role))
}
