<template>
  <el-container class="layout-container">
    <el-aside
        :width="isCollapse ? '64px' : '240px'"
        class="layout-sidebar"
    >
      <div class="sidebar-logo-container">
        <transition name="sidebarLogoFade">
          <router-link v-if="!isCollapse" to="/" class="sidebar-logo-link">
            <div class="logo-box">
              <el-icon class="logo-icon"><Cpu /></el-icon>
            </div>
            <h1 class="sidebar-title">Smart Exam</h1>
          </router-link>
          <router-link v-else to="/" class="sidebar-logo-link collapsed">
            <el-icon class="logo-icon"><Cpu /></el-icon>
          </router-link>
        </transition>
      </div>

      <el-scrollbar>
        <el-menu
            :default-active="activeMenu"
            :collapse="isCollapse"
            :unique-opened="true"
            background-color="#001529"
            text-color="rgba(255, 255, 255, 0.7)"
            active-text-color="#ffffff"
            class="sidebar-menu"
            :collapse-transition="false"
            router
        >
          <template v-if="hasRole('student')">
            <div class="menu-label" v-if="!isCollapse">STUDENT</div>
            <el-menu-item index="/student/dashboard">
              <el-icon><DataAnalysis /></el-icon>
              <template #title>仪表盘</template>
            </el-menu-item>
            <el-menu-item index="/student/exam-list">
              <el-icon><Document /></el-icon>
              <template #title>我的考试</template>
            </el-menu-item>
            <el-menu-item index="/student/mistake-book">
              <el-icon><Collection /></el-icon>
              <template #title>智能错题本</template>
            </el-menu-item>
          </template>

          <template v-if="hasRole('teacher')|| hasRole('admin')">
            <div class="menu-label" v-if="!isCollapse">TEACHING</div>
            <el-menu-item index="/teacher/dashboard">
              <el-icon><DataAnalysis /></el-icon>
              <template #title>工作台</template>
            </el-menu-item>
            <el-sub-menu index="teacher-resource">
              <template #title>
                <el-icon><FolderOpened /></el-icon>
                <span>资源管理</span>
              </template>
              <el-menu-item index="/teacher/knowledge-base">知识库管理</el-menu-item>
              <el-menu-item index="/teacher/question-bank">智能题库</el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="teacher-exam">
              <template #title>
                <el-icon><DocumentCopy /></el-icon>
                <span>考务管理</span>
              </template>
              <!-- 修改点：入口改为 PaperList，文案改为 试卷管理 -->
              <el-menu-item index="/teacher/paper-list">试卷管理</el-menu-item>
              <el-menu-item index="/teacher/exam-publish">考试发布</el-menu-item>
              <el-menu-item index="/teacher/review-console">AI辅助阅卷</el-menu-item>
              <el-menu-item index="/teacher/analysis">成绩统计</el-menu-item>
            </el-sub-menu>
          </template>

          <template v-if="hasRole('admin')">
            <div class="menu-label" v-if="!isCollapse">SYSTEM</div>
            <el-menu-item index="/admin/user-manage">
              <el-icon><UserFilled /></el-icon>
              <template #title>用户管理</template>
            </el-menu-item>
            <el-menu-item index="/admin/dept-manage">
              <el-icon><Management /></el-icon>
              <template #title>组织架构</template>
            </el-menu-item>
            <el-menu-item index="/admin/course-manage">
              <el-icon><Notebook /></el-icon>
              <template #title>课程管理</template>
            </el-menu-item>
            <el-menu-item index="/admin/sys-config">
              <el-icon><Setting /></el-icon>
              <template #title>系统设置</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="main-container">
      <el-header class="navbar">
        <div class="navbar-left">
          <div class="hamburger-container" @click="toggleCollapse">
            <el-icon :size="20" :class="{ 'is-active': isCollapse }">
              <component :is="isCollapse ? 'Expand' : 'Fold'" />
            </el-icon>
          </div>
          <el-breadcrumb class="breadcrumb-container" separator="/">
            <transition-group name="breadcrumb">
              <el-breadcrumb-item key="/" :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-for="item in matchedRoutes" :key="item.path">
                {{ item.meta.title }}
              </el-breadcrumb-item>
            </transition-group>
          </el-breadcrumb>
        </div>

        <div class="navbar-right">
          <el-tooltip content="消息通知" effect="dark" placement="bottom">
            <div class="right-menu-item">
              <el-badge :value="3" class="badge-item" type="danger">
                <el-icon :size="20"><Bell /></el-icon>
              </el-badge>
            </div>
          </el-tooltip>

          <el-tooltip content="全屏" effect="dark" placement="bottom">
            <div class="right-menu-item hover-effect">
              <el-icon :size="20"><FullScreen /></el-icon>
            </div>
          </el-tooltip>

          <el-dropdown class="avatar-container" trigger="click" @command="handleCommand">
            <div class="avatar-wrapper">
              <el-avatar :size="36" :src="userAvatar" class="user-avatar" :class="roleClass">
                {{ userInitial }}
              </el-avatar>
              <div class="user-info">
                <span class="user-name">{{ userStore.userInfo?.realName || 'User' }}</span>
                <span class="user-role">{{ roleName }}</span>
              </div>
              <el-icon class="el-icon--right"><CaretBottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="user-dropdown">
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>系统设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <span class="text-danger">
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  Fold, Expand, User, DataAnalysis, Document, Collection,
  UserFilled, FolderOpened, DocumentCopy,
  Setting, Management, Notebook, Cpu, Bell, FullScreen, CaretBottom, SwitchButton
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const activeMenu = computed(() => route.path)
const userAvatar = computed(() => userStore.userInfo?.avatar || '')
const userInitial = computed(() => userStore.userInfo?.realName?.charAt(0).toUpperCase() || 'U')

const roleName = computed(() => {
  if (userStore.roles.includes('admin')) return '管理员'
  if (userStore.roles.includes('teacher')) return '教师'
  return '学生'
})

const roleClass = computed(() => {
  if (userStore.roles.includes('admin')) return 'role-admin'
  if (userStore.roles.includes('teacher')) return 'role-teacher'
  return 'role-student'
})

const matchedRoutes = computed(() => {
  return route.matched.filter(item => item.meta && item.meta.title && item.path !== '/')
})

const hasRole = (role: string) => userStore.roles.includes(role)

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'logout':
      userStore.logout()
      break
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
  width: 100%;
}

/* Sidebar Styling */
.layout-sidebar {
  background-color: #001529;
  height: 100%;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.35);
  transition: width 0.3s cubic-bezier(0.25, 0.8, 0.5, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  z-index: 1001;

  :deep(.el-menu) {
    border-right: none;
  }

  /* 美化滚动条 */
  :deep(.el-scrollbar__bar.is-vertical) {
    width: 4px;
    right: 2px;
  }

  :deep(.el-scrollbar__thumb) {
    background-color: rgba(255, 255, 255, 0.2);
  }
}

.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: 64px;
  line-height: 64px;
  background: #001529;
  padding: 0 16px;
  overflow: hidden;
  display: flex;
  align-items: center;

  .sidebar-logo-link {
    height: 100%;
    width: 100%;
    display: flex;
    align-items: center;
    text-decoration: none;

    &.collapsed {
      justify-content: center;
    }

    .logo-box {
      width: 32px;
      height: 32px;
      background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 12px;

      .logo-icon {
        font-size: 20px;
        color: #fff;
      }
    }

    .sidebar-logo-link.collapsed .logo-icon {
      font-size: 24px;
      color: #1890ff;
      margin-right: 0;
    }

    .sidebar-title {
      display: inline-block;
      margin: 0;
      color: #fff;
      font-weight: 600;
      font-size: 18px;
      font-family: 'PingFang SC', 'Helvetica Neue', Helvetica, sans-serif;
      vertical-align: middle;
      white-space: nowrap;
    }
  }
}

.menu-label {
  padding: 16px 0 8px 24px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  font-weight: 600;
  letter-spacing: 1px;
}

/* 侧边栏菜单选中样式 */
.sidebar-menu {
  :deep(.el-menu-item) {
    border-left: 4px solid transparent;

    &:hover {
      background-color: rgba(255, 255, 255, 0.05) !important;
    }

    &.is-active {
      background-color: #1890ff !important;
      border-left-color: #fff;

      .el-icon {
        color: #fff;
      }
    }
  }

  :deep(.el-sub-menu__title:hover) {
    background-color: rgba(255, 255, 255, 0.05) !important;
  }
}

/* Header Styling */
.navbar {
  height: 64px;
  overflow: hidden;
  position: relative;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0;
  z-index: 1000;
  position: sticky;
  top: 0;
}

.navbar-left {
  display: flex;
  align-items: center;
  height: 100%;
}

.hamburger-container {
  padding: 0 20px;
  height: 100%;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: rgba(0, 0, 0, 0.025);
  }

  .is-active {
    transform: rotate(180deg);
  }
}

/* Right Menu Styling */
.navbar-right {
  display: flex;
  align-items: center;
  height: 100%;
  padding-right: 24px;

  .right-menu-item {
    display: flex;
    align-items: center;
    padding: 0 12px;
    height: 100%;
    cursor: pointer;
    color: #5a5e66;
    transition: all 0.3s;
    border-radius: 4px;

    &:hover {
      background: rgba(0, 0, 0, 0.025);
    }
  }
}

.avatar-container {
  margin-left: 12px;

  .avatar-wrapper {
    display: flex;
    align-items: center;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: 20px;
    transition: all 0.3s;

    &:hover {
      background: rgba(0, 0, 0, 0.025);
    }

    .user-avatar {
      font-weight: bold;
      font-size: 16px;

      &.role-admin { background-color: #f56c6c; }
      &.role-teacher { background-color: #e6a23c; }
      &.role-student { background-color: #409eff; }
    }

    .user-info {
      display: flex;
      flex-direction: column;
      margin: 0 8px;
      text-align: left;

      .user-name {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        line-height: 1.2;
      }

      .user-role {
        font-size: 11px;
        color: #909399;
        margin-top: 2px;
      }
    }
  }
}

/* Main Content Styling */
.app-main {
  background-color: #f5f7fa;
  padding: 24px;
  width: 100%;
  position: relative;
  overflow-x: hidden;
  min-height: calc(100vh - 64px);
}

/* Transitions */
.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.5, 1);
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.breadcrumb-enter-active,
.breadcrumb-leave-active {
  transition: all 0.5s;
}

.breadcrumb-enter-from,
.breadcrumb-leave-active {
  opacity: 0;
  transform: translateX(20px);
}

.breadcrumb-leave-active {
  position: absolute;
}

.text-danger {
  color: #f56c6c;
}
</style>