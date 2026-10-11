import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // 개발 중 API 요청은 Spring Boot(8080)로 프록시 → CORS 설정 불필요
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
