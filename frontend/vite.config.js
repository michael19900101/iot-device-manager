import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/ws': {
        target: 'ws://localhost:8889',
        ws: true
      }
    }
  }
})






