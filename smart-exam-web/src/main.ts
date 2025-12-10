import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import router from './router'
import ElementPlus from 'element-plus'
// 核心修复：引入 Element Plus 的全局样式文件
// 解决 ElMessage, ElMessageBox 等函数式组件无样式/不显示的问题
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 引入自定义样式（放在 Element Plus 样式之后，以便覆盖）
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