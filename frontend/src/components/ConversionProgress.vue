<template>
  <div class="conversion-progress">
    <el-card class="media-card">
      <template #header>
        <div class="card-header">
          <span>转换进度</span>
          <div class="header-actions">
            <el-button 
              size="small" 
              @click="refreshTasks"
              :loading="loading"
              :icon="Refresh"
            >
              刷新
            </el-button>
            <el-button 
              size="small" 
              @click="clearAllTasks"
              :disabled="conversionStore.tasks.length === 0"
              :icon="Delete"
            >
              清理所有转换记录
            </el-button>
          </div>
        </div>
      </template>
      
      <!-- 队列统计（可点击切换页签） -->
      <div class="queue-stats">
        <el-row :gutter="16">
          <el-col :span="4" v-for="(stat, key) in queueStatsDisplay" :key="key">
            <div 
              class="stat-item" 
              :class="{ 'stat-item-clickable': stat.tabName }"
              @click="stat.tabName && handleStatClick(stat.tabName)"
            >
              <div class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <!-- 任务列表 -->
      <div class="task-list custom-scrollbar" v-loading="loading">
        <!-- 任务分类标签页（隐藏标签头部，通过统计卡片切换） -->
        <el-tabs v-model="activeTab" type="border-card" @tab-change="handleTabChange" class="hidden-tabs">
          <el-tab-pane label="全部" name="all">
            <div v-if="conversionStore.tasks.length === 0" class="empty-state">
              <el-icon class="empty-icon"><Timer /></el-icon>
              <div class="empty-text">暂无转换任务</div>
            </div>
            <TaskList 
              v-else
              :tasks="conversionStore.tasks"
              @cancel="handleCancelTask"
              @download="handleDownloadFile"
              @retry="handleRetryTask"
              @delete="handleDeleteTask"
            />
          </el-tab-pane>
          
          <el-tab-pane 
            :label="`进行中`"
            name="active"
          >
            <div v-if="conversionStore.activeTasks.length === 0" class="empty-state">
              <el-icon class="empty-icon"><Timer /></el-icon>
              <div class="empty-text">暂无进行中的任务</div>
            </div>
            <TaskList 
              v-else
              :tasks="conversionStore.activeTasks"
              @cancel="handleCancelTask"
              @download="handleDownloadFile"
              @retry="handleRetryTask"
              @delete="handleDeleteTask"
            />
          </el-tab-pane>
          
          <el-tab-pane 
            :label="`已完成`"
            name="completed"
          >
            <div v-if="conversionStore.completedTasks.length === 0" class="empty-state">
              <el-icon class="empty-icon"><Timer /></el-icon>
              <div class="empty-text">暂无已完成的任务</div>
            </div>
            <TaskList 
              v-else
              :tasks="conversionStore.completedTasks"
              @cancel="handleCancelTask"
              @download="handleDownloadFile"
              @retry="handleRetryTask"
              @delete="handleDeleteTask"
            />
          </el-tab-pane>
          
          <el-tab-pane 
            :label="`失败`"
            name="failed"
          >
            <div v-if="conversionStore.failedTasks.length === 0" class="empty-state">
              <el-icon class="empty-icon"><Timer /></el-icon>
              <div class="empty-text">暂无失败的任务</div>
            </div>
            <TaskList 
              v-else
              :tasks="conversionStore.failedTasks"
              @cancel="handleCancelTask"
              @download="handleDownloadFile"
              @retry="handleRetryTask"
              @delete="handleDeleteTask"
            />
          </el-tab-pane>
          
          <el-tab-pane 
            :label="`已取消`"
            name="cancelled"
          >
            <div v-if="conversionStore.cancelledTasks.length === 0" class="empty-state">
              <el-icon class="empty-icon"><Timer /></el-icon>
              <div class="empty-text">暂无已取消的任务</div>
            </div>
            <TaskList 
              v-else
              :tasks="conversionStore.cancelledTasks"
              @cancel="handleCancelTask"
              @download="handleDownloadFile"
              @retry="handleRetryTask"
              @delete="handleDeleteTask"
            />
          </el-tab-pane>
        </el-tabs>
        
        <!-- 分页控件 -->
        <div v-if="showPagination" class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="conversionStore.pagination.total || 0"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { 
  Refresh, Delete, Timer, VideoPlay, Headset, 
  Download, CircleCloseFilled, Loading, SuccessFilled,
  WarningFilled
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useConversionStore } from '@/stores/conversion'
import TaskList from './TaskList.vue'

const conversionStore = useConversionStore()

const activeTab = ref('all')
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
let refreshInterval = null

// 同步分页参数
const syncPaginationParams = () => {
  currentPage.value = conversionStore.pagination.page + 1 // 转换为1基页码
  pageSize.value = conversionStore.pagination.size
}

// 队列统计显示（增加点击切换功能）
const queueStatsDisplay = computed(() => {
  const stats = conversionStore.queueStats
  const totalCount = stats.pendingCount + stats.runningCount + stats.completedCount + stats.failedCount + (stats.cancelledCount || 0)
  
  return [
    { 
      label: '全部', 
      value: totalCount,
      color: '#606266',
      tabName: 'all' // 点击切换到"全部"页签
    },
    { 
      label: '等待中', 
      value: stats.pendingCount,
      color: '#909399',
      tabName: 'active' // 点击切换到"进行中"页签
    },
    { 
      label: '转换中', 
      value: stats.runningCount,
      color: '#409eff',
      tabName: 'active' // 点击切换到"进行中"页签
    },
    { 
      label: '已完成', 
      value: stats.completedCount,
      color: '#67c23a',
      tabName: 'completed' // 点击切换到"已完成"页签
    },
    { 
      label: '失败', 
      value: stats.failedCount,
      color: '#f56c6c',
      tabName: 'failed' // 点击切换到"失败"页签
    },
    { 
      label: '已取消', 
      value: stats.cancelledCount || 0,
      color: '#e6a23c',
      tabName: 'cancelled' // 点击切换到"已取消"页签
    }
  ]
})

// 是否显示分页控件
const showPagination = computed(() => {
  const total = conversionStore.pagination.total || 0
  const size = conversionStore.pagination.size || 20
  return total > size
})

// 方法
const refreshTasks = async () => {
  loading.value = true
  try {
    await Promise.all([
      conversionStore.loadTasks(currentPage.value - 1, pageSize.value),
      conversionStore.loadQueueStats()
    ])
    // 同步分页参数
    syncPaginationParams()
  } catch (error) {
    console.error('刷新任务失败:', error)
  } finally {
    loading.value = false
  }
}

const clearAllTasks = async () => {
  if (conversionStore.tasks.length === 0) {
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要清理所有 ${conversionStore.tasks.length} 个转换记录吗？此操作不会删除您的文件！`,
      '确认清理',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    loading.value = true
    try {
      const deletedCount = await conversionStore.clearAllTasks()
      
      // 清理成功后刷新任务列表和统计信息
      // store 中已经清空了任务列表，这里只需要刷新统计和重置分页
      currentPage.value = 1
      await Promise.all([
        conversionStore.loadTasks(0, pageSize.value),
        conversionStore.loadQueueStats()
      ])
      syncPaginationParams()
      
      // 显示成功消息
      if (deletedCount > 0) {
        ElMessage.success(`已成功清理 ${deletedCount} 个转换记录`)
      }
    } catch (error) {
      // 错误信息已由axios拦截器统一处理
    }
  } catch (error) {
    // 用户取消
    console.log('用户取消清理操作')
  } finally {
    loading.value = false
  }
}

const handleCancelTask = async (task) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消任务 "${task.sourceFilePath}" 吗？`,
      '确认取消',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await conversionStore.cancelTask(task.id)
    if (response.success) {
      ElMessage.success('任务已取消')
    }
  } catch (error) {
    // 错误信息已由axios拦截器统一处理
  }
}

const handleDownloadFile = async (task) => {
  try {
    await conversionStore.downloadFile(task.id)
    ElMessage.success('文件下载已开始')
  } catch (error) {
    // 错误信息已由axios拦截器统一处理
  }
}

const handleRetryTask = async (task) => {
  try {
    // 重新创建转换任务
    const files = [task.sourceFilePath]
    const settings = {
      targetFormat: task.targetFormat,
      targetDirectory: task.targetDirectory,
      videoWidth: task.videoWidth,
      videoHeight: task.videoHeight,
      audioBitrate: task.audioBitrate,
      videoBitrate: task.videoBitrate,
      frameRate: task.frameRate
    }
    
    await conversionStore.createConversionTask(files, settings)
    ElMessage.success('任务已重新添加到队列')
    
    // 刷新任务列表
    refreshTasks()
  } catch (error) {
    // 错误信息已由axios拦截器统一处理
  }
}

const handleDeleteTask = async (task) => {
  try {
    await conversionStore.deleteTask(task.id)
    //ElMessage.success('任务已删除')
  } catch (error) {
    // 错误信息已由axios拦截器统一处理
  }
}

const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  currentPage.value = 1 // 重置到第一页
  refreshTasks()
}

const handleCurrentChange = (newPage) => {
  currentPage.value = newPage
  refreshTasks()
}

const handleTabChange = (tabName) => {
  // 切换标签页时刷新数据
  currentPage.value = 1
  refreshTasks()
}

// 处理统计卡片点击
const handleStatClick = (tabName) => {
  activeTab.value = tabName
  handleTabChange(tabName)
}

// 生命周期
onMounted(() => {
  // 初始加载
  refreshTasks()
  
  // 初始化WebSocket连接
  conversionStore.initWebSocket()
  
  // 定期刷新统计信息（不刷新任务列表，任务列表由WebSocket实时更新）
  refreshInterval = setInterval(() => {
    conversionStore.loadQueueStats()
  }, 10000) // 每10秒刷新一次统计信息
})

onUnmounted(() => {
  // 清理定时器
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
  
  // 断开WebSocket连接
  conversionStore.disconnectWebSocket()
})
</script>

<style scoped>
.conversion-progress {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.queue-stats {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 12px;
  background: white;
  border-radius: 6px;
  border: 1px solid #e8eaec;
  transition: all 0.3s ease;
}

.stat-item-clickable {
  cursor: pointer;
  user-select: none;
}

.stat-item-clickable:hover {
  background: #f5f7fa;
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.task-list {
  min-height: 300px;
  padding: 4px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  padding: 16px 0;
  border-top: 1px solid #e8eaec;
  background: #fafbfc;
}

.mobile .card-header {
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
}

.mobile .header-actions {
  width: 100%;
  justify-content: space-between;
}

.mobile .queue-stats .el-col {
  margin-bottom: 8px;
}

.mobile .stat-item {
  padding: 8px;
}

.mobile .stat-value {
  font-size: 20px;
}

.mobile .el-tabs__header {
  margin: 0;
}

.mobile .el-tabs__content {
  padding: 8px;
}

/* 隐藏页签头部，只显示内容 */
.hidden-tabs :deep(.el-tabs__header) {
  display: none;
}

.hidden-tabs :deep(.el-tabs__content) {
  padding: 0;
}
</style>