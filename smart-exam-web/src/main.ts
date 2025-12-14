import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import router from './router'

// 1. Element Plus 全量引入
// 注意：虽然 vite.config.ts 中配置了自动按需引入，但显式引入可以防止某些边缘情况下的样式丢失
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 2. 自定义样式 (包含 Tailwind CSS v4 配置)
// ⚠️ 务必放在 Element Plus 样式之后，确保 style.css 中的 @layer base 修复规则能正确生效
import './style.css'

// 3. 引入业务逻辑
import './router/permission' // 路由守卫 (登录鉴权)
import App from './App.vue'

const app = createApp(App)

// 注册 Element Plus 所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// Pinia 配置 (带持久化)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

// 注册插件
app.use(pinia)
app.use(router)
app.use(ElementPlus)

app.mount('#app')