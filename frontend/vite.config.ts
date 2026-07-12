// Vite 构建配置：开发代理与静态资源输出目录
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:30142',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
})
