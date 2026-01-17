import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

export default defineConfig(({ mode }) => ({
  // 开发环境使用根路径，生产环境使用 /static 路径
  base: mode === 'development' ? '/' : '/static',
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver({
        importStyle: 'css'
      })],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: false, // 禁用 DTS 生成避免循环依赖
      eslintrc: {
        enabled: false
      }
    }),
    Components({
      resolvers: [ElementPlusResolver({
        importStyle: 'css'
      })],
      dts: false, // 禁用 DTS 生成避免循环依赖
      types: []
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:9080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api')
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: undefined,
        inlineDynamicImports: false,
        exports: 'named'
      }
    },
    chunkSizeWarningLimit: 2000,
    target: 'es2015',
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
        passes: 2
      }
    },
    commonjsOptions: {
      include: [/node_modules/]
    }
  },
  optimizeDeps: {
    include: ['element-plus', 'vue', 'vue-router', 'pinia', 'sockjs-client', '@stomp/stompjs'],
    exclude: ['@element-plus/icons-vue'],
    force: true
  },
  define: {
    __VUE_OPTIONS_API__: true,
    __VUE_PROD_DEVTOOLS__: false,
    global: 'globalThis'
  }
}))
