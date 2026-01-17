import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { conversionApi } from '@/utils/api'
import { webSocketService } from '@/utils/websocket'

export const useConversionStore = defineStore('conversion', () => {
  // 状态
  const sourceFiles = ref([])
  const conversionSettings = ref({
    targetFormat: '',
    targetDirectory: '',
    videoWidth: null,
    videoHeight: null,
    audioBitrate: null,
    videoBitrate: null,
    frameRate: null
  })
  
  const tasks = ref([])
  const queueStats = ref({
    pendingCount: 0,
    runningCount: 0,
    completedCount: 0,
    failedCount: 0,
    cancelledCount: 0,
    activeThreads: 0
  })
  
  // 分页相关状态
  const pagination = ref({
    page: 0,
    size: 20,
    total: 0,
    totalPages: 0
  })
  
  const isLoading = ref(false)
  
  // 计算属性
  const activeTasks = computed(() => 
    tasks.value.filter(task => 
      task.status === 'PENDING' || task.status === 'RUNNING'
    )
  )
  
  const completedTasks = computed(() => 
    tasks.value.filter(task => task.status === 'COMPLETED')
  )
  
  const failedTasks = computed(() => 
    tasks.value.filter(task => task.status === 'FAILED')
  )
  
  const cancelledTasks = computed(() => 
    tasks.value.filter(task => task.status === 'CANCELLED')
  )
  
  // 方法
  const setSourceFiles = (files) => {
    sourceFiles.value = files
  }
  
  const setConversionSettings = (settings) => {
    console.log('Store设置转换参数:', settings)
    conversionSettings.value = { ...conversionSettings.value, ...settings }
    console.log('Store设置后的值:', conversionSettings.value)
  }
  
  const resetConversionSettings = () => {
    conversionSettings.value = {
      targetFormat: '',
      targetDirectory: '',
      videoWidth: null,
      videoHeight: null,
      audioBitrate: null,
      videoBitrate: null,
      frameRate: null
    }
  }
  
  const createConversionTask = async (files, settings) => {
    isLoading.value = true
    try {
      if (files.length === 1) {
        // 单个任务
        const response = await conversionApi.createTask({
          sourceFiles: files,
          ...settings
        })
        return response
      } else {
        // 批量任务
        const response = await conversionApi.createBatchTasks({
          sourceFiles: files,
          ...settings
        })
        return response
      }
    } catch (error) {
      throw error
    } finally {
      isLoading.value = false
    }
  }
  
  const loadTasks = async (page = 0, size = 20, append = false) => {
    try {
      const response = await conversionApi.getAllTasks(page, size)
      
      let taskList = []
      let totalItems = 0
      
      // 处理不同的响应格式
      if (response && typeof response === 'object') {
        if (Array.isArray(response)) {
          // 直接返回数组格式
          taskList = response
          totalItems = response.length
        } else if (response.tasks && Array.isArray(response.tasks)) {
          // 分页对象格式 {tasks: [], total: n}
          taskList = response.tasks
          totalItems = response.total || response.tasks.length
        } else {
          console.warn('未知的响应格式:', response)
          taskList = []
          totalItems = 0
        }
      } else {
        console.warn('响应为空或格式错误')
        taskList = []
        totalItems = 0
      }
      
      // 更新状态
      tasks.value = append ? [...tasks.value, ...taskList] : taskList
      pagination.value = {
        page,
        size,
        total: totalItems,
        totalPages: Math.ceil(totalItems / size)
      }
      
      return response
    } catch (error) {
      console.error('加载任务列表失败:', error)
      tasks.value = []
      pagination.value = {
        page,
        size,
        total: 0,
        totalPages: 0
      }
      return []
    }
  }
  
  const getTask = async (taskId) => {
    try {
      const task = await conversionApi.getTask(taskId)
      
      // 更新本地任务状态
      const index = tasks.value.findIndex(t => t.id === taskId)
      if (index > -1) {
        tasks.value[index] = task
      } else {
        tasks.value.push(task)
      }
      
      return task
    } catch (error) {
      console.error('获取任务状态失败:', error)
      return null
    }
  }
  
  const cancelTask = async (taskId) => {
    try {
      const response = await conversionApi.cancelTask(taskId)
      
      if (response.success) {
        // 更新本地任务状态
        const index = tasks.value.findIndex(t => t.id === taskId)
        if (index > -1) {
          tasks.value[index].status = 'CANCELLED'
        }
      }
      
      return response
    } catch (error) {
      console.error('取消任务失败:', error)
      throw error
    }
  }
  
  const deleteTask = async (taskId) => {
    try {
      const response = await conversionApi.deleteTask(taskId)
      
      // 从本地任务列表中移除任务
      const index = tasks.value.findIndex(t => t.id === taskId)
      if (index > -1) {
        tasks.value.splice(index, 1)
      }
      
      return response
    } catch (error) {
      console.error('删除任务失败:', error)
      throw error
    }
  }
  
  const loadQueueStats = async () => {
    try {
      const stats = await conversionApi.getQueueStats()
      queueStats.value = stats
      return stats
    } catch (error) {
      console.error('加载队列统计失败:', error)
      return null
    }
  }
  
  const downloadFile = async (taskId) => {
    try {
      const downloadUrl = conversionApi.getDownloadUrl(taskId)
      
      // 创建隐藏的a标签来触发下载
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = ''
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
    } catch (error) {
      console.error('下载文件失败:', error)
      throw error
    }
  }
  
  const createDirectory = async (parentPath, directoryName) => {
    try {
      const response = await conversionApi.createDirectory(parentPath, directoryName)
      return response
    } catch (error) {
      console.error('创建目录失败:', error)
      throw error
    }
  }
  
  const validateSettings = () => {
    const errors = []
    
    console.log('验证 - 当前设置:', conversionSettings.value)
    console.log('验证 - 源文件数量:', sourceFiles.value.length)
    
    // 检查目标格式（去除前后空格后再检查）
    const targetFormat = conversionSettings.value.targetFormat?.trim()
    if (!targetFormat) {
      console.log('验证失败: 目标格式为空')
      errors.push('请选择目标格式')
    }
    
    // 检查目标目录（去除前后空格后再检查）
    const targetDirectory = conversionSettings.value.targetDirectory?.trim()
    if (!targetDirectory) {
      console.log('验证失败: 目标目录为空')
      errors.push('请选择目标目录')
    }
    
    if (sourceFiles.value.length === 0) {
      console.log('验证失败: 没有源文件')
      errors.push('请选择要转换的文件')
    }
    
    console.log('验证结果:', errors)
    return errors
  }
  
  const clearAllTasks = async () => {
    try {
      const response = await conversionApi.clearAllTasks()
      
      // 清理任务后，清空本地任务列表和重置分页
      tasks.value = []
      pagination.value = {
        page: 0,
        size: 20,
        total: 0,
        totalPages: 0
      }
      
      // 刷新统计信息
      await loadQueueStats()
      
      return response
    } catch (error) {
      console.error('清理所有任务失败:', error)
      throw error
    }
  }

  const reset = () => {
    sourceFiles.value = []
    resetConversionSettings()
    tasks.value = []
    isLoading.value = false
  }

  // WebSocket相关方法
  let progressSubscription = null

  /**
   * 初始化WebSocket连接并订阅进度更新
   */
  const initWebSocket = () => {
    webSocketService.connect()
      .then(() => {
        // 订阅转换进度主题
        progressSubscription = webSocketService.subscribe('/topic/conversion-progress', (taskUpdate) => {
          handleTaskUpdate(taskUpdate)
        })
        console.log('WebSocket订阅成功')
      })
      .catch((error) => {
        console.error('WebSocket连接失败:', error)
      })
  }

  /**
   * 断开WebSocket连接
   */
  const disconnectWebSocket = () => {
    if (progressSubscription) {
      webSocketService.unsubscribe('/topic/conversion-progress')
      progressSubscription = null
    }
    webSocketService.disconnect()
  }

  /**
   * 处理任务更新（来自WebSocket）
   */
  const handleTaskUpdate = (taskUpdate) => {
    if (!taskUpdate || !taskUpdate.id) {
      return
    }

    // 查找并更新任务
    const index = tasks.value.findIndex(t => t.id === taskUpdate.id)
    if (index !== -1) {
      // 更新现有任务：只更新非空字段，避免覆盖完整信息
      const existingTask = tasks.value[index]
      const updatedTask = { ...existingTask }
      
      // 只更新非空/非undefined的字段
      Object.keys(taskUpdate).forEach(key => {
        const value = taskUpdate[key]
        // 只更新非null和非undefined的值，或者明确要更新的字段（如progress、status）
        if (value !== null && value !== undefined) {
          updatedTask[key] = value
        } else if (key === 'progress' || key === 'status') {
          // progress 和 status 即使为0或特定值也要更新
          updatedTask[key] = value
        }
        // 其他null值不更新，保留原有数据
      })
      
      tasks.value[index] = updatedTask
    } else {
      // 如果是新任务且状态是运行中或等待中，添加到列表开头
      // 但需要确保任务信息完整，如果不完整则从服务器获取
      if (taskUpdate.status === 'PENDING' || taskUpdate.status === 'RUNNING') {
        // 检查任务信息是否完整（至少要有sourceFilePath）
        if (taskUpdate.sourceFilePath) {
          tasks.value.unshift(taskUpdate)
          // 如果列表超过分页大小，移除最后一个
          if (tasks.value.length > pagination.value.size) {
            tasks.value.pop()
          }
          // 更新总数
          pagination.value.total = (pagination.value.total || 0) + 1
        } else {
          // 任务信息不完整，从服务器获取完整信息
          console.log('收到新任务更新但信息不完整，从服务器获取完整信息:', taskUpdate.id)
          getTask(taskUpdate.id)
        }
      } else {
        // 其他状态的新任务，不自动添加，避免分页混乱
        console.log('收到新任务更新，但不在当前列表中:', taskUpdate.id, taskUpdate.status)
      }
    }

    // 如果任务状态改变，可能需要更新统计信息
    // 这里不直接更新统计，而是标记需要刷新（由组件定期刷新）
  }
  
  return {
    // 状态
    sourceFiles,
    conversionSettings,
    tasks,
    queueStats,
    pagination,
    isLoading,
    
    // 计算属性
    activeTasks,
    completedTasks,
    failedTasks,
    cancelledTasks,
    
    // 方法
    setSourceFiles,
    setConversionSettings,
    resetConversionSettings,
    createConversionTask,
    loadTasks,
    getTask,
    cancelTask,
    deleteTask,
    loadQueueStats,
    downloadFile,
    createDirectory,
    clearAllTasks,
    validateSettings,
    reset,
    initWebSocket,
    disconnectWebSocket,
    handleTaskUpdate
  }
})