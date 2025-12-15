import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// 路由类型定义
export type AppRouteRecordRaw = RouteRecordRaw & {
  meta?: {
    title?: string
    roles?: string[]
    hidden?: boolean
  }
  children?: AppRouteRecordRaw[]
}

// 公共路由（无需权限验证）
export const constantRoutes: AppRouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: {
      title: '登录',
      hidden: true
    }
  },
  {
    path: '/profile',
    component: () => import('@/layout/index.vue'),
    hidden: true, // 不在侧边栏显示
    children: [
      {
        path: '', // 空路径表示 /profile 直接渲染此组件
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: {
          title: '个人中心',
          icon: 'User'
        }
      }
    ]
  },
  {
    path: '/notice',
    component: () => import('@/layout/index.vue'),
    hidden: true,
    children: [
      {
        path: '',
        name: 'NoticeCenter',
        component: () => import('@/views/common/NoticeCenter.vue'),
        meta: {
          title: '消息中心'
        }
      }
    ]
  },
  {
    path: '/404',
    name: '404',
    component: () => import('@/views/error/404.vue'),
    meta: {
      title: '页面不存在',
      hidden: true
    }
  },
  {
    path: '/',
    redirect: '/login'
  }
]

// 动态路由（需要权限验证）
export const asyncRoutes: AppRouteRecordRaw[] = [
  // 学生路由
  {
    path: '/student',
    name: 'student',
    component: () => import('@/layout/index.vue'),
    redirect: '/student/dashboard',
    meta: {
      title: '学生中心',
      roles: ['student']
    },
    children: [
      {
        path: 'dashboard',
        name: 'student-dashboard',
        component: () => import('@/views/student/Dashboard.vue'),
        meta: {
          title: '仪表盘'
        }
      },
      {
        path: 'exam-list',
        name: 'student-exam-list',
        component: () => import('@/views/student/ExamList.vue'),
        meta: {
          title: '我的考试'
        }
      },
      {
        path: 'exam-paper',
        name: 'student-exam-paper',
        component: () => import('@/views/student/ExamPaper.vue'),
        meta: {
          title: '在线答题',
          hidden: true
        }
      },
      {
        path: 'exam-result',
        name: 'student-exam-result',
        component: () => import('@/views/student/ExamResult.vue'),
        meta: {
          title: '成绩解析',
          hidden: true
        }
      },
      {
        path: 'mistake-book',
        name: 'student-mistake-book',
        component: () => import('@/views/student/MistakeBook.vue'),
        meta: {
          title: '智能错题本'
        }
      }
    ]
  },
  // 教师路由
  {
    path: '/teacher',
    name: 'teacher',
    component: () => import('@/layout/index.vue'),
    redirect: '/teacher/dashboard',
    meta: {
      title: '教师中心',
      roles: ['teacher', 'admin']
    },
    children: [
      {
        path: 'dashboard',
        name: 'teacher-dashboard',
        component: () => import('@/views/teacher/Dashboard.vue'),
        meta: {
          title: '工作台'
        }
      },
      {
        path: 'knowledge-base',
        name: 'teacher-knowledge-base',
        component: () => import('@/views/teacher/KnowledgeBase.vue'),
        meta: {
          title: '知识库管理'
        }
      },
      {
        path: 'question-bank',
        name: 'teacher-question-bank',
        component: () => import('@/views/teacher/QuestionBank.vue'),
        meta: {
          title: '智能题库'
        }
      },
      {
        path: 'paper-list',
        name: 'teacher-paper-list',
        component: () => import('@/views/teacher/PaperList.vue'),
        meta: {
          title: '试卷管理'
        }
      },
      {
        path: 'paper-create',
        name: 'teacher-paper-create',
        component: () => import('@/views/teacher/PaperCreate.vue'),
        meta: {
          title: '智能组卷'
        }
      },
      {
        path: 'exam-publish',
        name: 'teacher-exam-publish',
        component: () => import('@/views/teacher/ExamPublish.vue'),
        meta: {
          title: '考试发布'
        }
      },
      {
        path: 'review-console',
        name: 'teacher-review-console',
        component: () => import('@/views/teacher/ReviewConsole.vue'),
        meta: {
          title: 'AI辅助阅卷'
        }
      },
      {
        path: 'analysis',
        name: 'teacher-analysis',
        component: () => import('@/views/teacher/Analysis.vue'),
        meta: {
          title: '成绩统计'
        }
      },
      {
        path: 'proctor',
        name: 'teacher-proctor',
        component: () => import('@/views/teacher/Proctor.vue'),
        meta: {
          title: '在线监考'
        }
      },
      {
        path: 'course-manage',
        name: 'teacher-course-manage',
        component: () => import('@/views/teacher/CourseManage.vue'),
        meta: {
          title: '课程管理'
        }
      }
    ]
  },
  // 管理员路由
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/layout/index.vue'),
    redirect: '/admin/user-manage',
    meta: {
      title: '系统管理',
      roles: ['admin']
    },
    children: [
      {
        path: 'user-manage',
        name: 'admin-user-manage',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: {
          title: '用户管理'
        }
      },
      {
        path: 'dept-manage',
        name: 'admin-dept-manage',
        component: () => import('@/views/admin/DeptManage.vue'),
        meta: {
          title: '组织架构'
        }
      },
      {
        path: 'course-manage',
        name: 'admin-course-manage',
        component: () => import('@/views/admin/CourseManage.vue'),
        meta: {
          title: '课程管理'
        }
      },
      {
        path: 'sys-config',
        name: 'admin-sys-config',
        component: () => import('@/views/admin/SysConfig.vue'),
        meta: {
          title: '系统设置'
        }
      }
    ]
  },
  // 404路由必须放在最后
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
    meta: {
      hidden: true
    }
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

export default router