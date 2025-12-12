import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import router from './router'

import ElementPlus from 'element-plus'
// 1. 先引入 Element Plus 样式
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 2. 后引入自定义样式（Tailwind），确保 Tailwind 的 Utility 类（如 mt-4）能覆盖组件库样式
import './style.css'

// 引入路由守卫配置
import './router/permission'

// 导入根组件
import App from './App.vue'

// 创建 Pinia 实例
const pinia = createPinia()
// 使用 Pinia 持久化插件
pinia.use(piniaPluginPersistedstate)

// 创建 Vue 应用实例
const app = createApp(App)

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册插件
app.use(router)
app.use(pinia)
app.use(ElementPlus)

// 挂载应用
app.mount('#app')