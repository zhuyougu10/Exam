<template>
  <el-container class="layout-container">
    <el-aside
      :width="isCollapse ? '64px' : '220px'"
      class="layout-sidebar"
      :class="{ 'is-collapsed': isCollapse }"
    >
      <div class="sidebar-logo-container">
        <transition name="sidebarLogoFade">
          <router-link v-if="!isCollapse" to="/" class="sidebar-logo-link">
            <el-icon class="logo-icon" :size="24"><Cpu /></el-icon>
            <h1 class="sidebar-title">智能考试系统</h1>
          </router-link>
          <router-link v-else to="/" class="sidebar-logo-link">
            <el-icon class="logo-icon" :size="24"><Cpu /></el-icon>
          </router-link>
        </transition>
      </div>

      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :unique-opened="true"
          background-color="#001529"
          text-color="#bfcbd9"
          active-text-color="#409eff"
          class="sidebar-menu"
          :collapse-transition="false"
          router
        >
          <template v-if="hasRole('student')">
            <div class="menu-label" v-if="!isCollapse">学生功能</div>
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
            <div class="menu-label" v-if="!isCollapse">教师管理</div>
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
              <el-menu-item index="/teacher/paper-create">智能组卷</el-menu-item>
              <el-menu-item index="/teacher/exam-publish">考试发布</el-menu-item>
              <el-menu-item index="/teacher/review-console">AI辅助阅卷</el-menu-item>
              <el-menu-item index="/teacher/analysis">成绩统计</el-menu-item>
            </el-sub-menu>
          </template>

          <template v-if="hasRole('admin')">
             <div class="menu-label" v-if="!isCollapse">系统设置</div>
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
              <el-badge :value="3" class="badge-item">
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
              <el-avatar :size="32" :src="userAvatar" class="user-avatar">
                {{ userInitial }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo?.realName || 'User' }}</span>
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
                  <span style="display:block;">
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
  Menu as IconMenu,
  Fold, Expand, User, DataAnalysis, Document, Collection,
  UserFilled, FolderOpened, EditPen, DocumentCopy, Reading, TrendCharts,
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

// 获取面包屑数据
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
/* 布局容器 */
.layout-container {
  height: 100vh;
  width: 100%;
}

/* 侧边栏样式 */
.layout-sidebar {
  background-color: #001529;
  height: 100%;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.35);
  transition: width 0.28s;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  z-index: 1001;
  
  /* 去掉 Element Menu 默认的右边框 */
  :deep(.el-menu) {
    border-right: none;
  }
}

/* Logo 区域 */
.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: 60px;
  line-height: 60px;
  background: #002140;
  text-align: center;
  overflow: hidden;

  .sidebar-logo-link {
    height: 100%;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    text-decoration: none;

    .logo-icon {
      color: #409eff;
      margin-right: 0;
    }

    .sidebar-title {
      display: inline-block;
      margin: 0 0 0 12px;
      color: #fff;
      font-weight: 600;
      line-height: 50px;
      font-size: 16px;
      font-family: Avenir, Helvetica Neue, Arial, Helvetica, sans-serif;
      vertical-align: middle;
      white-space: nowrap; // 防止文字换行
    }
  }
}

/* 菜单分组标签 */
.menu-label {
  padding: 12px 20px 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  line-height: 1.5;
}

/* 顶部导航栏 */
.navbar {
  height: 60px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0;
  z-index: 1000;
}

.navbar-left {
  display: flex;
  align-items: center;
  height: 100%;
}

.hamburger-container {
  padding: 0 15px;
  height: 100%;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: background 0.3s;
  
  &:hover {
    background: rgba(0, 0, 0, 0.025);
  }
}

.breadcrumb-container {
  margin-left: 8px;
}

/* 顶部右侧菜单 */
.navbar-right {
  display: flex;
  align-items: center;
  height: 100%;
  padding-right: 20px;
  
  .right-menu-item {
    display: flex;
    align-items: center;
    padding: 0 12px;
    height: 100%;
    cursor: pointer;
    color: #5a5e66;
    transition: all 0.3s;
    
    &:hover {
      background: rgba(0, 0, 0, 0.025);
    }
  }
}

/* 头像区域 */
.avatar-container {
  margin-left: 10px;
  
  .avatar-wrapper {
    display: flex;
    align-items: center;
    cursor: pointer;
    padding: 0 8px;
    
    &:hover {
      background: rgba(0, 0, 0, 0.025);
      border-radius: 4px;
    }
    
    .user-name {
      margin: 0 8px;
      font-size: 14px;
      color: #606266;
    }
    
    .el-icon--right {
      color: #909399;
    }
  }
}

/* 内容区域 */
.app-main {
  background-color: #f0f2f5; /* 浅灰色背景，突出卡片内容 */
  padding: 20px;
  width: 100%;
  position: relative;
  overflow: hidden;
  box-sizing: border-box;
}

/* 动画效果：页面切换 */
.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.4s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

/* 面包屑动画 */
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
</style>