import axios from 'axios'

// 获取API基础URL
const getApiBase = () => {
  // 如果在开发环境且通过Vite代理，使用相对路径
  if (import.meta.env.DEV) {
    return '/api'
  }
  // 生产环境使用当前域名
  return window.location.origin
}

// 创建axios实例
const authClient = axios.create({
  baseURL: getApiBase(),
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 添加Token
authClient.interceptors.request.use(
  (config) => {
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

// 响应拦截器 - 处理错误
authClient.interceptors.response.use(
  (response) => response,
  (error) => {
    let errorMessage = '请求失败'

    if (error.response?.status === 401) {
      // Token过期或无效，清除本地存储
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      // 跳转到登录页
      window.location.href = '/login'
      return Promise.reject(error)
    }

    if (error.response) {
      // 服务器返回了错误状态码
      const { status, data } = error.response

      // 获取错误信息 - 优先使用后端返回的 message
      if (typeof data === 'string') {
        // 后端返回纯字符串（当前后端的实现）
        errorMessage = data
      } else if (data && typeof data === 'object') {
        // 后端返回 JSON 对象
        if (data.message) {
          errorMessage = data.message
        } else if (data.error) {
          errorMessage = data.error
        } else if (data.errors && Array.isArray(data.errors)) {
          errorMessage = data.errors.map(e => e.defaultMessage || e.message).join(', ')
        }
      }

      // 如果状态码是400但还没设置错误信息
      if (status === 400 && errorMessage === '请求失败') {
        errorMessage = '请求参数错误'
      } else if (status === 403) {
        errorMessage = errorMessage === '请求失败' ? '拒绝访问' : errorMessage
      } else if (status === 404) {
        errorMessage = errorMessage === '请求失败' ? '请求的资源不存在' : errorMessage
      } else if (status === 500) {
        errorMessage = errorMessage === '请求失败' ? '服务器内部错误' : errorMessage
      }

      // 在控制台输出详细错误信息
      console.error(`API请求失败 [${status}]:`, errorMessage, data)
    } else if (error.request) {
      // 请求已发出但没有收到响应
      errorMessage = '网络连接失败，请检查网络设置'
      console.error('网络错误:', errorMessage)
    } else {
      // 其他错误
      errorMessage = error.message || '请求失败'
      console.error('请求错误:', errorMessage)
    }

    // 将提取的错误信息设置到 error 对象的 message 属性
    error.message = errorMessage

    return Promise.reject(error)
  }
)

/**
 * 用户登录
 */
export const login = async (username, password) => {
  const response = await authClient.post('/api/auth/login', {
    username,
    password
  })
  return response.data
}

/**
 * 用户注册
 */
export const register = async (username, password) => {
  const response = await authClient.post('/api/auth/register', {
    username,
    password
  })
  return response.data
}

/**
 * 修改密码
 */
export const changePassword = async (currentPassword, newPassword) => {
  const response = await authClient.post('/api/user/change-password', {
    currentPassword,
    newPassword
  })
  return response.data
}

/**
 * 检查是否已初始化（是否有用户）
 */
export const checkSetup = async () => {
  const response = await authClient.get('/api/auth/check-setup')
  return response.data.hasUsers
}

/**
 * 获取当前用户信息
 */
export const getCurrentUser = async () => {
  const response = await authClient.get('/api/user/me')
  return response.data
}

/**
 * 用户登出
 */
export const logout = async () => {
  const response = await authClient.post('/api/user/logout')
  return response.data
}

export default authClient
