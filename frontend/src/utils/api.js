import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    // 添加认证Token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    let errorMessage = '请求失败'

    // 处理 401 或 403 错误 - Token 无效、过期或权限不足
    if (error.response?.status === 401 || error.response?.status === 403) {
      // 清除本地存储的认证信息
      localStorage.removeItem('token')
      localStorage.removeItem('user')

      // 显示提示
      if (typeof window !== 'undefined' && window.ElMessage) {
        window.ElMessage.warning('登录已过期，请重新登录')
      }

      // 跳转到登录页
      window.location.href = '/login'
      return Promise.reject(error)
    }

    if (error.response) {
      // 服务器返回了错误状态码
      const { status, data } = error.response

      // 获取错误信息
      if (data && data.message) {
        errorMessage = data.message
      } else if (data && data.error) {
        errorMessage = data.error
      } else if (data) {
        errorMessage = data
      } else {
        // 默认错误信息
        switch (status) {
          case 400:
            errorMessage = '请求参数错误'
            break
          case 401:
            errorMessage = '未授权，请登录'
            break
          case 403:
            errorMessage = '拒绝访问'
            break
          case 404:
            errorMessage = '请求的资源不存在'
            break
          case 500:
            errorMessage = '服务器内部错误'
            break
          case 502:
            errorMessage = '网关错误'
            break
          case 503:
            errorMessage = '服务不可用'
            break
          default:
            errorMessage = `请求失败 (状态码: ${status})`
        }
      }

      // 显示toast提示
      if (typeof window !== 'undefined' && window.$message) {
        // ElementPlus 全局消息组件
        window.$message.error(errorMessage)
      } else if (typeof window !== 'undefined' && window.ElMessage) {
        // ElementPlus 消息组件
        window.ElMessage.error(errorMessage)
      }

      // 在控制台输出详细错误信息
      console.error(`API请求失败 [${status}]:`, errorMessage, data)
      
    } else if (error.request) {
      // 请求已发出但没有收到响应
      errorMessage = '网络连接失败，请检查网络设置'
      if (typeof window !== 'undefined' && window.$message) {
        window.$message.error(errorMessage)
      } else if (typeof window !== 'undefined' && window.ElMessage) {
        window.ElMessage.error(errorMessage)
      }
      console.error('网络错误:', errorMessage)
    } else {
      // 其他错误
      errorMessage = error.message || '请求失败'
      if (typeof window !== 'undefined' && window.$message) {
        window.$message.error(errorMessage)
      } else if (typeof window !== 'undefined' && window.ElMessage) {
        window.ElMessage.error(errorMessage)
      }
      console.error('请求错误:', errorMessage)
    }
    
    error.message = errorMessage
    return Promise.reject(error)
  }
)

// 文件浏览器API
export const fileBrowserApi = {
  // 获取系统根目录
  getSystemRoots() {
    return api.get('/filebrowser/roots')
  },
  
  // 获取文件列表
  listFiles(path) {
    return api.get('/filebrowser/list', { params: { path } })
  },
  
  // 创建目录
  createDirectory(parentPath, directoryName) {
    return api.post('/filebrowser/directory', null, {
      params: { parentPath, directoryName }
    })
  },
  
  // 获取支持的格式
  getSupportedFormats(sourceFormat) {
    return api.get('/filebrowser/formats', {
      params: sourceFormat ? { sourceFormat } : {}
    })
  },
}

// 转换API
export const conversionApi = {
  // 创建单个转换任务
  createTask(data) {
    return api.post('/conversion/task', data)
  },
  
  // 创建批量转换任务
  createBatchTasks(data) {
    return api.post('/conversion/batch', data)
  },
  
  // 获取所有任务
  getAllTasks(page = 0, size = 20) {
    return api.get('/conversion/tasks', { params: { page, size } })
  },
  
  // 获取单个任务
  getTask(taskId) {
    return api.get(`/conversion/task/${taskId}`)
  },
  
  // 取消任务
  cancelTask(taskId) {
    return api.post(`/conversion/task/${taskId}/cancel`)
  },
  
  // 删除任务
  deleteTask(taskId) {
    return api.delete(`/conversion/task/${taskId}`)
  },
  
  // 获取队列统计
  getQueueStats() {
    return api.get('/conversion/queue/stats')
  },
  
  // 获取下载链接
  getDownloadUrl(taskId) {
    // 获取API基础URL
    const apiBase = api.defaults.baseURL
    // 如果是相对路径，补全完整URL
    if (apiBase.startsWith('/')) {
      return window.location.origin + apiBase + `/conversion/download/${taskId}`
    }
    return apiBase + `/conversion/download/${taskId}`
  },
  
  // 创建目录
  createDirectory(parentPath, directoryName) {
    return api.post('/conversion/directory', null, {
      params: { parentPath, directoryName }
    })
  },
  
  // 清理所有任务
  clearAllTasks() {
    return api.delete('/conversion/tasks/clear')
  }
}
