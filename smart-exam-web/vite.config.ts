import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 自动导入配置
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  // 路径别名配置
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  // 开发服务器代理配置
  server: {
    proxy: {
      // 将 /api 开头的请求代理到 http://localhost:8080
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 保留 /api 前缀，因为后端接口已经包含 /api
        rewrite: (path) => path,
      },
    },
  },
  // 构建配置
  build: {
    // 提高构建速度，减少构建时间
    cssCodeSplit: true,
    // 生成 sourcemap 用于调试（生产环境可关闭）
    sourcemap: false,
    // 调整 chunk 大小警告限制
    chunkSizeWarningLimit: 1000,
  },
})
