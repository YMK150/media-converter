import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let stompClient = null
let reconnectAttempts = 0
const maxReconnectAttempts = 5
const reconnectDelay = 3000

/**
 * WebSocket服务
 */
export class WebSocketService {
  constructor() {
    this.subscriptions = new Map()
    this.isConnected = false
    this.listeners = new Map()
  }

  /**
   * 连接WebSocket
   */
  connect() {
    if (stompClient && stompClient.connected) {
      console.log('WebSocket已连接')
      return Promise.resolve()
    }

    return new Promise((resolve, reject) => {
      try {
        // 获取WebSocket URL
        const wsUrl = this.getWebSocketUrl()
        
        stompClient = new Client({
          webSocketFactory: () => new SockJS(wsUrl),
          reconnectDelay: reconnectDelay,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          onConnect: (frame) => {
            console.log('WebSocket连接成功:', frame)
            this.isConnected = true
            reconnectAttempts = 0
            
            // 重新订阅所有已注册的订阅
            this.resubscribeAll()
            
            resolve()
          },
          onStompError: (frame) => {
            console.error('WebSocket STOMP错误:', frame)
            this.isConnected = false
            this.handleReconnect()
            reject(new Error(frame.headers['message'] || 'WebSocket连接错误'))
          },
          onWebSocketClose: () => {
            console.log('WebSocket连接关闭')
            this.isConnected = false
            this.handleReconnect()
          },
          onDisconnect: () => {
            console.log('WebSocket断开连接')
            this.isConnected = false
          }
        })

        stompClient.activate()
      } catch (error) {
        console.error('WebSocket连接失败:', error)
        reject(error)
      }
    })
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (stompClient) {
      // 取消所有订阅
      this.subscriptions.forEach((subscription) => {
        subscription.unsubscribe()
      })
      this.subscriptions.clear()
      
      stompClient.deactivate()
      stompClient = null
      this.isConnected = false
      console.log('WebSocket已断开')
    }
  }

  /**
   * 订阅主题
   */
  subscribe(destination, callback) {
    if (!stompClient || !stompClient.connected) {
      console.warn('WebSocket未连接，无法订阅:', destination)
      return null
    }

    // 如果已经订阅过，先取消
    if (this.subscriptions.has(destination)) {
      this.subscriptions.get(destination).unsubscribe()
    }

    const subscription = stompClient.subscribe(destination, (message) => {
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch (error) {
        console.error('解析WebSocket消息失败:', error, message.body)
        callback(message.body)
      }
    })

    this.subscriptions.set(destination, subscription)
    console.log('已订阅主题:', destination)
    return subscription
  }

  /**
   * 取消订阅
   */
  unsubscribe(destination) {
    const subscription = this.subscriptions.get(destination)
    if (subscription) {
      subscription.unsubscribe()
      this.subscriptions.delete(destination)
      console.log('已取消订阅:', destination)
    }
  }

  /**
   * 重新订阅所有主题
   */
  resubscribeAll() {
    // 这个方法会在重新连接后调用，但需要外部保存回调函数
    // 这里只是占位，实际由外部管理
    console.log('重新订阅所有主题')
  }

  /**
   * 处理重连
   */
  handleReconnect() {
    if (reconnectAttempts < maxReconnectAttempts) {
      reconnectAttempts++
      console.log(`尝试重连 (${reconnectAttempts}/${maxReconnectAttempts})...`)
      setTimeout(() => {
        this.connect().catch((error) => {
          console.error('重连失败:', error)
        })
      }, reconnectDelay)
    } else {
      console.error('达到最大重连次数，停止重连')
    }
  }

  /**
   * 获取WebSocket URL
   */
  getWebSocketUrl() {
    // 从当前页面URL获取协议和主机
    const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:'
    const host = window.location.host
    
    // 开发环境使用代理，生产环境直接使用
    if (import.meta.env.DEV) {
      // 开发环境通过代理访问
      return `${protocol}//${host}/api/ws`
    }
    
    // 生产环境
    return `${protocol}//${host}/api/ws`
  }

  /**
   * 获取连接状态
   */
  getConnectionState() {
    return this.isConnected && stompClient && stompClient.connected
  }
}

// 创建单例
export const webSocketService = new WebSocketService()

