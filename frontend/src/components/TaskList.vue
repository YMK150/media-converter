<template>
  <div class="task-list">
    <!-- 任务卡片 -->
    <div 
      v-for="task in tasks" 
      :key="task.id || task.sourceFilePath || Math.random()"
      class="task-card"
      :class="getTaskClass(task)"
      @click="showTaskDetail(task)"
    >
      <!-- 任务主内容区域 -->
      <div class="task-main">
        <!-- 左侧：文件信息和状态 -->
        <div class="task-content">
          <!-- 文件图标和基本信息 -->
          <div class="file-info">
            <div class="file-icon" :class="getMediaType(task.sourceFilePath)">
              <el-icon>
                <VideoPlay v-if="getMediaType(task.sourceFilePath) === 'video'" />
                <Headset v-else />
              </el-icon>
            </div>
            
            <div class="file-details">
              <div class="file-name" :title="task.sourceFilePath">
                {{ getFileName(task.sourceFilePath) }}
              </div>
              <div class="file-format">
                <el-tag size="small" type="info">
                  {{ getSourceFormat(task.sourceFilePath) }}
                </el-tag>
                <el-icon class="conversion-arrow"><ArrowRight /></el-icon>
                <el-tag size="small" type="success">
                  {{ task.targetFormat?.toUpperCase() }}
                </el-tag>
              </div>
            </div>
          </div>
          
          <!-- 状态标签 -->
          <div class="status-wrapper">
            <el-tag 
              :type="getStatusTagType(task.status)"
              size="small"
              class="status-tag"
            >
              <el-icon v-if="task.status === 'RUNNING'" class="loading-icon">
                <Loading />
              </el-icon>
              <el-icon v-else-if="task.status === 'COMPLETED'">
                <SuccessFilled />
              </el-icon>
              <el-icon v-else-if="task.status === 'FAILED'">
                <WarningFilled />
              </el-icon>
              <el-icon v-else-if="task.status === 'CANCELLED'">
                <CircleCloseFilled />
              </el-icon>
              <span class="status-text">{{ getStatusText(task.status) }}</span>
            </el-tag>
          </div>
        </div>
        
        <!-- 右侧：时间信息 -->
        <div class="time-info-section">
          <div class="time-item" v-if="task.createdAt">
            <el-icon><Clock /></el-icon>
            <span>创建: {{ formatTime(task.createdAt) }}</span>
          </div>
          <div class="time-item" v-if="task.completedAt">
            <el-icon><Check /></el-icon>
            <span>完成: {{ formatTime(task.completedAt) }}</span>
          </div>
          <div class="time-item duration" v-if="task.createdAt && task.completedAt">
            <el-icon><Timer /></el-icon>
            <span>{{ formatDuration(task.createdAt, task.completedAt) }}</span>
          </div>
        </div>
      </div>
      
      <!-- 任务详细信息区域 -->
      <div class="task-details-section">
        <!-- 进度条（仅运行中任务显示） -->
        <div v-if="task.status === 'RUNNING'" class="progress-wrapper">
          <el-progress 
            :percentage="task.progress || 0" 
            :stroke-width="6"
            :show-text="true"
            :format="(percentage) => `${percentage}%`"
          />
        </div>
        
        <!-- 成功信息（仅完成任务显示） -->
        <div v-if="task.status === 'COMPLETED'" class="success-info">
          <div class="info-row" v-if="task.outputFilePath">
            <span class="info-label">输出文件:</span>
            <span class="info-value" :title="task.outputFilePath">
              {{ getFileName(task.outputFilePath) }}
            </span>
          </div>
          
          <div class="info-row" v-if="task.fileSize && task.outputFileSize">
            <span class="info-label">文件大小:</span>
            <span class="info-value">
              {{ formatFileSize(task.fileSize) }} → {{ formatFileSize(task.outputFileSize) }}
            </span>
          </div>
        </div>
        
        <!-- 错误信息（仅失败任务显示） -->
        <div v-if="task.status === 'FAILED'" class="error-info">
          <el-alert
            :title="task.errorMessage || '转换失败'"
            type="error"
            :closable="false"
            show-icon
            size="small"
          />
        </div>
      </div>
      
      <!-- 操作按钮区域 -->
      <div class="task-actions">
        <div class="action-buttons">
          <el-button 
            v-if="task.status === 'RUNNING'"
            size="small" 
            type="warning"
            plain
            @click.stop="$emit('cancel', task)"
            :icon="CircleCloseFilled"
          >
            取消
          </el-button>
          
          <el-button 
            v-if="task.status === 'FAILED'"
            size="small" 
            type="success"
            plain
            @click.stop="$emit('retry', task)"
            :icon="Refresh"
          >
            重试
          </el-button>
          
          <el-button 
            v-if="task.status === 'FAILED'"
            size="small" 
            type="info"
            plain
            @click.stop="showErrorDetails(task)"
            :icon="View"
          >
            详情
          </el-button>
          
          <el-button 
            v-if="['COMPLETED', 'FAILED', 'CANCELLED'].includes(task.status)"
            size="small" 
            type="danger"
            plain
            @click.stop="deleteTask(task)"
            :icon="Delete"
          >
            删除
          </el-button>
        </div>
      </div>
    </div>
    
    <!-- 任务详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="`任务详情 - ${getFileName(selectedTask?.sourceFilePath)}`"
      width="800px"
      :before-close="closeDetailDialog"
      class="task-detail-dialog"
    >
      <div v-if="selectedTask" class="detail-container">
        <!-- 第一行：基本信息 -->
        <div class="detail-row">
          <div class="detail-item">
            <label>任务ID</label>
            <span>#{{ selectedTask.id }}</span>
          </div>
          <div class="detail-item">
            <label>状态</label>
            <el-tag :type="getStatusTagType(selectedTask.status)" size="small">
              <el-icon v-if="selectedTask.status === 'RUNNING'" class="loading-icon">
                <Loading />
              </el-icon>
              <el-icon v-else-if="selectedTask.status === 'COMPLETED'">
                <SuccessFilled />
              </el-icon>
              <el-icon v-else-if="selectedTask.status === 'FAILED'">
                <WarningFilled />
              </el-icon>
              <el-icon v-else-if="selectedTask.status === 'CANCELLED'">
                <CircleCloseFilled />
              </el-icon>
              {{ getStatusText(selectedTask.status) }}
            </el-tag>
          </div>
          <div class="detail-item">
            <label>格式转换</label>
            <div class="format-info">
              <el-tag size="small" type="info">{{ getSourceFormat(selectedTask.sourceFilePath) }}</el-tag>
              <span>→</span>
              <el-tag size="small" type="success">{{ selectedTask.targetFormat?.toUpperCase() }}</el-tag>
            </div>
          </div>
        </div>

        <!-- 第二行：文件信息 -->
        <div class="detail-row">
          <div class="detail-item full-width">
            <label>源文件路径</label>
            <span class="file-path" :title="selectedTask.sourceFilePath">{{ selectedTask.sourceFilePath }}</span>
          </div>
        </div>
        
        <div class="detail-row">
          <div class="detail-item" v-if="selectedTask.outputFilePath">
            <label>输出文件路径</label>
            <span class="file-path" :title="selectedTask.outputFilePath">{{ selectedTask.outputFilePath }}</span>
          </div>
        </div>

        <!-- 第三行：时间信息 -->
        <div class="detail-row">
          <div class="detail-item">
            <label>创建时间</label>
            <span>{{ formatTime(selectedTask.createdAt) }}</span>
          </div>
          <div class="detail-item" v-if="selectedTask.startedAt">
            <label>开始时间</label>
            <span>{{ formatTime(selectedTask.startedAt) }}</span>
          </div>
          <div class="detail-item" v-if="selectedTask.completedAt">
            <label>完成时间</label>
            <span>{{ formatTime(selectedTask.completedAt) }}</span>
          </div>
        </div>

        <div class="detail-row" v-if="selectedTask.createdAt && selectedTask.completedAt">
          <div class="detail-item">
            <label>耗时</label>
            <span class="duration">{{ formatDuration(selectedTask.createdAt, selectedTask.completedAt) }}</span>
          </div>
          <div class="detail-item" v-if="selectedTask.fileSize">
            <label>源文件大小</label>
            <span>{{ formatFileSize(selectedTask.fileSize) }}</span>
          </div>
          <div class="detail-item" v-if="selectedTask.outputFileSize">
            <label>输出文件大小</label>
            <span>{{ formatFileSize(selectedTask.outputFileSize) }}</span>
          </div>
        </div>

        <!-- 第四行：转换参数 -->
        <div class="detail-row" v-if="hasVideoParams()">
          <div class="detail-item" v-if="selectedTask.videoWidth">
            <label>视频尺寸</label>
            <span>{{ selectedTask.videoWidth }}×{{ selectedTask.videoHeight }}px</span>
          </div>
          <div class="detail-item" v-if="selectedTask.frameRate">
            <label>帧率</label>
            <span>{{ selectedTask.frameRate }} fps</span>
          </div>
          <div class="detail-item" v-if="selectedTask.videoBitrate">
            <label>视频比特率</label>
            <span>{{ selectedTask.videoBitrate }} kbps</span>
          </div>
        </div>

        <div class="detail-row" v-if="selectedTask.audioBitrate || (selectedTask.progress !== undefined && selectedTask.progress !== null)">
          <div class="detail-item" v-if="selectedTask.audioBitrate">
            <label>音频比特率</label>
            <span>{{ selectedTask.audioBitrate }} kbps</span>
          </div>
          <div class="detail-item" v-if="selectedTask.progress !== undefined && selectedTask.progress !== null">
            <label>进度</label>
            <div class="progress-mini">
              <el-progress 
                :percentage="selectedTask.progress" 
                :stroke-width="4"
                :show-text="false"
                style="width: 80px;"
              />
              <span class="progress-text">{{ selectedTask.progress }}%</span>
            </div>
          </div>
        </div>

        <!-- 错误信息 -->
        <div v-if="selectedTask.errorMessage" class="detail-row error-row">
          <div class="detail-item full-width">
            <label>错误信息</label>
            <el-alert
              :title="selectedTask.errorMessage"
              type="error"
              :closable="false"
              show-icon
            />
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDetailDialog">关闭</el-button>
          <el-button 
            v-if="selectedTask?.status === 'RUNNING'"
            type="warning"
            @click="handleCancelFromDetail"
            :icon="CircleCloseFilled"
          >
            取消任务
          </el-button>
          <el-button 
            v-if="selectedTask?.status === 'FAILED'"
            type="success"
            @click="handleRetryFromDetail"
            :icon="Refresh"
          >
            重试任务
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { 
  VideoPlay, Headset, Loading, SuccessFilled, 
  WarningFilled, CircleCloseFilled, Download, Refresh,
  View, FolderOpened, Document, Delete, ArrowRight,
  Clock, Timer, Check, Setting
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  tasks: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['cancel', 'download', 'retry', 'delete'])

// 响应式数据
const detailDialogVisible = ref(false)
const selectedTask = ref(null)

// 方法
const getTaskClass = (task) => {
  return `task-${task.status.toLowerCase()}`
}

const getStatusClass = (status) => {
  return `status-${status.toLowerCase()}`
}

const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '等待中',
    'RUNNING': '转换中',
    'COMPLETED': '已完成',
    'FAILED': '失败',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}



const getFileName = (filePath) => {
  if (!filePath) return '未知文件'
  return filePath.split(/[\\/]/).pop() || '未知文件'
}

const getMediaType = (filePath) => {
  if (!filePath) return 'unknown'
  
  const videoExts = ['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv', 'webm', '3gp', 'm4v']
  const ext = filePath.split('.').pop()?.toLowerCase()
  
  return videoExts.includes(ext) ? 'video' : 'audio'
}

const getSourceFormat = (filePath) => {
  if (!filePath) return '未知'
  return filePath.split('.').pop()?.toUpperCase() || '未知'
}

const formatTime = (timeString) => {
  if (!timeString) return ''
  
  const date = new Date(timeString)
  // 格式化为精确到秒的时间
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const formatDuration = (startTime, endTime) => {
  if (!startTime || !endTime) return '-'
  
  const start = new Date(startTime)
  const end = new Date(endTime)
  const durationMs = end - start
  const durationSeconds = Math.floor(durationMs / 1000)
  
  if (durationSeconds < 60) {
    return `${durationSeconds}秒`
  }
  
  const durationMinutes = Math.floor(durationSeconds / 60)
  if (durationMinutes < 60) {
    const remainingSeconds = durationSeconds % 60
    return remainingSeconds > 0 ? `${durationMinutes}分${remainingSeconds}秒` : `${durationMinutes}分钟`
  }
  
  const durationHours = Math.floor(durationMinutes / 60)
  const remainingMinutes = durationMinutes % 60
  const remainingSeconds = durationSeconds % 60
  
  if (remainingMinutes > 0 || remainingSeconds > 0) {
    return remainingSeconds > 0 ? 
      `${durationHours}小时${remainingMinutes}分${remainingSeconds}秒` : 
      `${durationHours}小时${remainingMinutes}分钟`
  }
  
  return `${durationHours}小时`
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const openFileLocation = (filePath) => {
  if (!filePath) {
    ElMessage.warning('文件路径无效')
    return
  }
  
  // 在应用中可以尝试打开文件所在目录
  ElMessage.info('文件位置: ' + filePath)
}

const showErrorDetails = (task) => {
  ElMessage.error('错误详情: ' + (task.errorMessage || '未知错误'))
}

const deleteTask = async (task) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除任务 "${getFileName(task.sourceFilePath)}" 吗？此操作不会删除您的文件！`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 发出删除事件，由父组件处理
    emit('delete', task)
    ElMessage.success('任务已删除')
  } catch (error) {
    // 用户取消
  }
}

// 显示任务详情
const showTaskDetail = (task) => {
  selectedTask.value = task
  detailDialogVisible.value = true
}

// 关闭详情弹窗
const closeDetailDialog = () => {
  detailDialogVisible.value = false
  selectedTask.value = null
}

// 从详情弹窗取消任务
const handleCancelFromDetail = async () => {
  if (selectedTask.value) {
    try {
      await ElMessageBox.confirm(
        `确定要取消任务 "${getFileName(selectedTask.value.sourceFilePath)}" 吗？`,
        '确认取消',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      
      emit('cancel', selectedTask.value)
      closeDetailDialog()
    } catch (error) {
      // 用户取消，不显示错误消息
    }
  }
}

// 从详情弹窗重试任务
const handleRetryFromDetail = async () => {
  if (selectedTask.value) {
    try {
      await ElMessageBox.confirm(
        `确定要重试任务 "${getFileName(selectedTask.value.sourceFilePath)}" 吗？`,
        '确认重试',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'info'
        }
      )
      
      emit('retry', selectedTask.value)
      closeDetailDialog()
    } catch (error) {
      // 用户取消，不显示错误消息
    }
  }
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  const typeMap = {
    'PENDING': 'info',
    'RUNNING': 'primary',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'CANCELLED': 'warning'
  }
  return typeMap[status] || 'info'
}

// 计算压缩率
const getCompressionRate = (originalSize, compressedSize) => {
  if (!originalSize || !compressedSize) return '-'
  const rate = ((originalSize - compressedSize) / originalSize * 100).toFixed(1)
  return `${rate}%`
}

// 检查是否有视频相关参数
const hasVideoParams = () => {
  return selectedTask.value && (
    selectedTask.value.videoWidth || 
    selectedTask.value.videoHeight || 
    selectedTask.value.frameRate || 
    selectedTask.value.videoBitrate
  )
}
</script>

<style scoped>
.task-list {
  max-height: 600px;
  overflow-y: auto;
  padding: 4px;
}

.task-card {
  background: white;
  border-radius: 8px;
  margin-bottom: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  overflow: hidden;
  border: 1px solid #e8eaec;
}

.task-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-1px);
}



/* 任务主内容区域 */
.task-main {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.task-content {
  flex: 1;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  min-width: 0;
}

/* 文件信息区域 */
.file-info {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.file-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: white;
  flex-shrink: 0;
}

.file-icon.video {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.file-icon.audio {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.3;
}

.file-format {
  display: flex;
  align-items: center;
  gap: 8px;
}

.conversion-arrow {
  color: #909399;
  font-size: 16px;
}

/* 状态标签区域 */
.status-wrapper {
  flex-shrink: 0;
  min-width: 80px !important;
  width: 80px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.status-tag {
  font-weight: 500;
  border-radius: 16px;
  padding: 3px 6px !important;
  font-size: 9px !important;
  white-space: nowrap !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 2px !important;
  border: 1px solid;
  background: white !important;
  line-height: 1 !important;
  flex-shrink: 0 !important;
  min-width: fit-content !important;
  height: 20px !important;
}

.status-text {
  white-space: nowrap !important;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 45px !important;
  display: inline-block !important;
  line-height: 1 !important;
  vertical-align: middle !important;
}

/* 状态颜色 */
.status-pending {
  color: #909399;
  border-color: #dcdfe6;
  background-color: #f4f4f5;
}

.status-running {
  color: #409eff;
  border-color: #b3d8ff;
  background-color: #ecf5ff;
}

.status-completed {
  color: #67c23a;
  border-color: #c2e7b0;
  background-color: #f0f9ff;
}

.status-failed {
  color: #f56c6c;
  border-color: #fbc4c4;
  background-color: #fef0f0;
}

.status-cancelled {
  color: #909399;
  border-color: #d3d4d6;
  background-color: #f5f7fa;
}

.loading-icon {
  animation: spin 1s linear infinite !important;
  font-size: 10px !important;
  flex-shrink: 0 !important;
  display: inline-block !important;
  vertical-align: middle !important;
  transform-origin: center center !important;
  width: 10px !important;
  height: 10px !important;
  line-height: 10px !important;
  text-align: center !important;
  margin: 0 auto !important;
}

.loading-icon svg {
  width: 10px !important;
  height: 10px !important;
  display: block !important;
  margin: 0 auto !important;
  transform-origin: center center !important;
}

.status-tag .el-icon {
  font-size: 10px !important;
  flex-shrink: 0 !important;
  display: inline-block !important;
  vertical-align: middle !important;
  line-height: 1 !important;
}

.status-tag .el-icon.loading-icon,
.status-tag .loading-icon .el-icon {
  animation: spin 1s linear infinite !important;
  transform-origin: center center !important;
  width: 10px !important;
  height: 10px !important;
  display: inline-block !important;
}

.status-tag .loading-icon svg,
.status-tag .el-icon.loading-icon svg {
  width: 10px !important;
  height: 10px !important;
  transform-origin: center center !important;
  display: block !important;
}

@keyframes spin {
  from { 
    transform: rotate(0deg); 
  }
  to { 
    transform: rotate(360deg); 
  }
}

/* 时间信息区域 */
.time-info-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;
  min-width: 160px;
}

.time-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #606266;
  background: #f8f9fa;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}

.time-item.duration {
  background: #e7f7ff;
  color: #1890ff;
  font-weight: 500;
}

.time-item .el-icon {
  font-size: 14px;
}

/* 任务详细信息区域 */
.task-details-section {
  border-top: 1px solid #f0f2f5;
  padding: 10px 16px;
  background: #fafbfc;
}

.progress-wrapper {
  margin-bottom: 8px;
}

.success-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
}

.info-label {
  color: #909399;
  white-space: nowrap;
  min-width: 70px;
}

.info-value {
  color: #303133;
  flex: 1;
  word-break: break-all;
  line-height: 1.4;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  background: white;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #e8eaec;
  max-height: 60px;
  overflow-y: auto;
}

.error-info {
  margin-bottom: 8px;
}

/* 操作按钮区域 */
.task-actions {
  padding: 10px 16px;
  border-top: 1px solid #f0f2f5;
  background: white;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.action-buttons .el-button:hover {
  transform: translateY(-1px);
}

/* 空状态 */
.empty-state {
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  color: #dcdfe6;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .task-card {
    margin-bottom: 12px;
    border-radius: 8px;
  }
  
  .task-main {
    flex-direction: column;
    padding: 16px;
    gap: 12px;
  }
  
  .task-content {
    flex-direction: column;
    gap: 12px;
  }
  
  .file-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }
  
  .file-name {
    font-size: 14px;
  }
  
  .status-wrapper {
    min-width: auto;
    justify-content: flex-start;
  }
  
  .status-tag {
    padding: 3px 6px;
    font-size: 9px;
    gap: 2px;
    height: 20px;
  }
  
  .time-info-section {
    min-width: auto;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 4px;
  }
  
  .time-item {
    font-size: 11px;
    padding: 2px 6px;
  }
  
  .task-details-section {
    padding: 12px 16px;
  }
  
  .task-actions {
    padding: 12px 16px;
  }
  
  .action-buttons {
    justify-content: center;
    gap: 6px;
  }
  
  .action-buttons .el-button {
    font-size: 12px;
    padding: 6px 12px;
  }
}

/* 滚动条样式 */
.task-list::-webkit-scrollbar {
  width: 6px;
}

.task-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.task-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.task-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 详情弹窗样式 */
.task-detail-dialog .el-dialog__body {
  padding: 0;
}

.detail-container {
  padding: 20px;
  max-height: 60vh;
  overflow-y: auto;
}

.detail-row {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  align-items: flex-start;
}

.detail-row:last-child {
  margin-bottom: 0;
}

.detail-item {
  flex: 1;
  min-width: 0;
}

.detail-item.full-width {
  flex: 1 1 100%;
}

.detail-item label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  font-weight: 500;
}

.detail-item > span {
  display: block;
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  line-height: 1.4;
  word-break: break-all;
}

.detail-item .file-path {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  background: #f5f7fa;
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  font-size: 13px;
  line-height: 1.4;
}

.detail-item .duration {
  color: #67c23a;
  font-weight: 600;
}

.format-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.format-info span {
  color: #606266;
  font-weight: 500;
}

.progress-mini {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-text {
  font-size: 12px;
  color: #409eff;
  font-weight: 600;
  min-width: 35px;
}

/* 详情弹窗中的状态标签图标样式 */
.detail-item .el-tag .el-icon {
  font-size: 10px !important;
  margin-right: 2px !important;
  flex-shrink: 0 !important;
  display: inline-block !important;
  vertical-align: middle !important;
}

.detail-item .el-tag {
  font-size: 10px !important;
  padding: 3px 6px !important;
  line-height: 1 !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 2px !important;
  white-space: nowrap !important;
  flex-shrink: 0 !important;
  min-width: fit-content !important;
  height: 20px !important;
}

.detail-item .el-tag .loading-icon,
.detail-item .el-tag .el-icon.loading-icon {
  animation: spin 1s linear infinite !important;
  transform-origin: center center !important;
  width: 10px !important;
  height: 10px !important;
  display: inline-block !important;
}

.detail-item .loading-icon svg,
.detail-item .el-icon.loading-icon svg {
  width: 10px !important;
  height: 10px !important;
  transform-origin: center center !important;
  display: block !important;
}

.error-row {
  background: #fef0f0;
  border-radius: 4px;
  padding: 16px;
  border: 1px solid #fbc4c4;
}

.error-row label {
  color: #f56c6c;
  margin-bottom: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 添加任务卡片点击效果 */
.task-card {
  cursor: pointer;
  user-select: none;
}

.task-card:active {
  transform: scale(0.995);
}

/* 移动端优化 */
@media (max-width: 768px) {
  .task-detail-dialog {
    width: 95% !important;
  }
  
  .detail-container {
    padding: 16px;
    max-height: 50vh;
  }
  
  .detail-row {
    flex-direction: column;
    gap: 12px;
    margin-bottom: 12px;
  }
  
  .detail-item {
    flex: 1 1 100%;
  }
  
  .dialog-footer {
    flex-direction: column-reverse;
  }
  
  .dialog-footer .el-button {
    width: 100%;
    margin: 0;
  }
  
  .format-info {
    flex-wrap: wrap;
  }
  
  .progress-mini {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }
}

/* 更小屏幕优化 */
@media (max-width: 480px) {
  .task-detail-dialog {
    width: 98% !important;
  }
  
  .detail-container {
    padding: 12px;
  }
  
  .detail-item label {
    font-size: 11px;
  }
  
  .detail-item > span {
    font-size: 13px;
  }
  
  .detail-item .file-path {
    font-size: 12px;
    padding: 4px 8px;
  }
  
  .error-row {
    padding: 12px;
  }
}

/* 滚动条优化 */
.detail-container::-webkit-scrollbar {
  width: 6px;
}

.detail-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.detail-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.detail-container::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>