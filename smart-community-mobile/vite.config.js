import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5173,
    open: false,
    proxy: {
      '/api': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        // pathRewrite 不需要，后端Controller已使用 /api 前缀
      }
    }
  }
})