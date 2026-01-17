import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import './assets/css/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 将ElementPlus的消息组件挂载到window全局，方便axios拦截器使用
app.config.globalProperties.$message = ElMessage
if (typeof window !== 'undefined') {
  window.$message = ElMessage
  window.ElMessage = ElMessage
}

app.mount('#app')