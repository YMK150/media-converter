<template>
  <div class="home responsive-container">
    <!-- 头部 -->
    <div class="app-header">
      <div class="header-left">
        <el-icon :size="24" color="#409eff"><VideoPlay /></el-icon>
        <span class="app-title">格式转换器</span>
      </div>
      <div class="header-right">
        <UserMenu />
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <el-row :gutter="20">
        <!-- 左侧文件浏览器 -->
        <el-col :xs="24" :lg="14">
          <FileBrowser />
        </el-col>

        <!-- 右侧转换设置 -->
        <el-col :xs="24" :lg="10">
          <FileConversionAction />
          <ConversionSettings />
        </el-col>
      </el-row>

      <!-- 转换进度区域 -->
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
          <ConversionProgress />
        </el-col>
      </el-row>
    </div>

    <!-- 免责声明 -->
    <div class="disclaimer">
      <el-alert
        title="免责声明"
        type="warning"
        :closable="false"
        show-icon
      >
        <template #default>
          <div class="disclaimer-content">
            <p>1. 本软件仅供个人学习和研究使用，使用本软件进行文件格式转换时，请务必遵守您所在国家或地区的法律法规。</p>
            <p>2. 请勿使用本软件转换受版权保护的内容，除非您拥有相应的使用许可或权利。</p>
            <p>3. 因使用本软件而产生的任何法律纠纷、版权纠纷或其他相关问题，均与软件作者无关，由使用者自行承担全部责任。</p>
            <p>4. 软件作者不保证本软件的永久可用性，有权在不事先通知的情况下停止维护或更新。</p>
          </div>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import { VideoPlay } from '@element-plus/icons-vue'
import FileBrowser from '@/components/FileBrowser.vue'
import FileConversionAction from '@/components/FileConversionAction.vue'
import ConversionSettings from '@/components/ConversionSettings.vue'
import ConversionProgress from '@/components/ConversionProgress.vue'
import UserMenu from '@/components/UserMenu.vue'
</script>

<style scoped>
.home {
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
}

.main-content {
  padding: 0 20px 20px;
}

.mobile .app-header {
  padding: 15px;
}

.mobile .app-title {
  font-size: 18px;
}

.mobile .main-content {
  padding: 0 10px 10px;
}

.disclaimer {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.disclaimer-content {
  line-height: 1.8;
  color: #606266;
}

.disclaimer-content p {
  margin: 8px 0;
  font-size: 14px;
}

:deep(.disclaimer .el-alert__description) {
  margin: 0;
}

.mobile .disclaimer {
  padding: 15px 10px;
}

.mobile .disclaimer-content p {
  font-size: 13px;
}
</style>