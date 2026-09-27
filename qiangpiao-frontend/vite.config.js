import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 后端地址：必须与 Tomcat 中当前实际部署的上下文路径一致
// IDEA 部署 artifact 名 qiangpiao-backend:war → 上下文 /qiangpiao_backend_war
// 手动部署 qiangpiao.war → 上下文 /qiangpiao；改名 ROOT.war → 填 ''
// 可用环境变量覆盖：set VITE_BACKEND_CONTEXT=/myapp
const BACKEND_ORIGIN = process.env.VITE_BACKEND_ORIGIN || 'http://localhost:8081'
const BACKEND_CONTEXT = process.env.VITE_BACKEND_CONTEXT || '/qiangpiao_backend_war'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5174,
    open: false,
    proxy: {
      '/api': {
        target: BACKEND_ORIGIN + BACKEND_CONTEXT,
        changeOrigin: true,
        ws: false
      },
      '/druid': {
        target: BACKEND_ORIGIN + BACKEND_CONTEXT,
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500
  }
})
