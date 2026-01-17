<template>
  <!-- 转换队列操作区域 - 始终显示 -->
  <div class="conversion-action-area" :class="{ 'mobile-mode': isMobile }">
    <el-card class="conversion-action-card" shadow="hover">
      <div class="conversion-action-content">
        <!-- 未选择文件时的提示 -->
        <div v-if="conversionStore.sourceFiles.length === 0" class="empty-state">
          <el-icon class="empty-icon"><Document /></el-icon>
          <div class="empty-text">
            <h3>请先在文件浏览器中勾选媒体文件</h3>
            <p>支持常见的视频和音频格式，选择后可进行转换设置</p>
          </div>
        </div>
        
        <!-- 已选择文件时的操作区域 -->
        <div v-else class="selected-state">
          <div class="selected-files-info">
            <el-icon class="files-icon"><Document /></el-icon>
            <div class="files-text">
              <h3>已选择 {{ conversionStore.sourceFiles.length }} 个文件</h3>
              <p>文件已准备好，请点击下方按钮添加到转换队列</p>
            </div>
          </div>
          <div class="action-buttons">
            <el-button 
              type="primary" 
              size="large"
              @click="proceedToSettings"
              class="proceed-btn"
            >
              <el-icon class="btn-icon"><CaretRight /></el-icon>
              继续设置转换参数
            </el-button>
            <el-button 
              size="large"
              @click="clearAllFiles"
              class="clear-all-btn"
            >
              <el-icon class="btn-icon"><CloseBold /></el-icon>
              重新选择
            </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Document, CloseBold, Plus, CaretRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useConversionStore } from '@/stores/conversion'

const conversionStore = useConversionStore()
const isMobile = ref(false)

// 移动设备检测
const checkDevice = () => {
  isMobile.value = window.innerWidth <= 768
  document.body.classList.toggle('mobile', isMobile.value)
  document.body.classList.toggle('desktop', !isMobile.value)
}

// 继续设置转换参数 - 滚动到转换设置区域
const proceedToSettings = () => {
  // 滚动到转换设置区域
  const formElement = document.querySelector('.conversion-settings .el-form')
  if (formElement) {
    formElement.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
  
  ElMessage.success('请在下方设置转换参数')
}

// 清空所有文件
const clearAllFiles = () => {
  // 清空conversionStore
  conversionStore.setSourceFiles([])
  // 同时需要触发FileBrowser清空选择，但这里我们通过事件总线来实现
  // 先清空store，FileBrowser会通过watcher同步
  ElMessage.success('已清空选择')
}

onMounted(() => {
  checkDevice()
  window.addEventListener('resize', checkDevice)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkDevice)
})
</script>

<style scoped>
/* 转换队列操作区域样式 */
.conversion-action-area {
  margin-bottom: 20px;
  animation: slideInUp 0.3s ease-out;
}

.conversion-action-area:not(.mobile-mode) {
  position: relative;
  width: 100%;
}

.conversion-action-area.mobile-mode {
  margin-top: 20px;
  margin-bottom: 20px;
  position: relative;
  width: 100%;
}

.conversion-action-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border: 2px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
}

.conversion-action-card:hover {
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.conversion-action-content {
  padding: 20px 24px;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 空状态样式 */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  width: 100%;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  color: var(--text-placeholder);
  opacity: 0.6;
}

.empty-text h3 {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-regular);
}

.empty-text p {
  margin: 0;
  font-size: 14px;
  color: var(--text-placeholder);
}

/* 已选择状态样式 */
.selected-state {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  width: 100%;
  flex-wrap: wrap;
}

.selected-files-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 200px;
}

.files-icon {
  font-size: 48px;
  color: var(--primary-color);
  opacity: 0.8;
}

.files-text h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.files-text p {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.action-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
}

.proceed-btn {
  background: linear-gradient(135deg, var(--primary-color) 0%, #409eff 100%);
  border: none;
  font-size: 16px;
  font-weight: 600;
  padding: 12px 24px;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.3);
  transition: all 0.3s ease;
  min-width: 180px;
}

.proceed-btn:hover {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4);
}

.clear-all-btn {
  background: transparent;
  border: 2px solid var(--border-color);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
  padding: 10px 24px;
  border-radius: 8px;
  transition: all 0.3s ease;
  min-width: 180px;
}

  .clear-all-btn:hover {
    border-color: var(--danger-color);
    color: var(--danger-color);
    background: rgba(245, 108, 108, 0.05);
  }

  .btn-icon {
    margin-right: 8px;
    font-size: 18px;
  }

  .action-buttons {
    flex-direction: column;
    gap: 12px;
    width: 100%;
  }

  .proceed-btn,
  .clear-all-btn {
    width: 100%;
    min-width: unset;
  }

/* 动画效果 */
@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .conversion-action-content {
    flex-direction: column;
    align-items: stretch;
    text-align: center;
    padding: 16px 20px;
  }
  
  .selected-files-info {
    justify-content: center;
    text-align: center;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 16px;
  }
  
  .files-icon {
    font-size: 40px;
  }
  
  .files-text h3 {
    font-size: 18px;
  }
  
  .files-text p {
    font-size: 14px;
  }
  
  .clear-all-btn {
    width: 100%;
    margin-top: 8px;
    padding: 14px 20px;
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .conversion-action-content {
    padding: 14px 16px;
  }
  
  .files-text h3 {
    font-size: 16px;
  }
  
  .files-text p {
    font-size: 13px;
  }
  
  .clear-all-btn {
    font-size: 15px;
    padding: 12px 16px;
  }
}
</style>