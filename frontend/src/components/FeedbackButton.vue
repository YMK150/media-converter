<template>
  <div class="feedback-button">
    <el-button
      type="primary"
      size="large"
      @click="showFeedbackDialog"
      class="feedback-btn"
    >
      <span class="btn-icon">
        <el-icon><ChatDotRound /></el-icon>
      </span>
      <span class="btn-label">问题反馈</span>
    </el-button>
    
    <!-- 反馈对话框 -->
    <el-dialog 
      v-model="feedbackDialogVisible" 
      title="帮助我们做得更好！"
      width="500px"
      :modal-append-to-body="false"
    >
      <div class="feedback-content">
        <!-- 二维码显示 -->
        <div class="qr-section">
          <div class="qr-code">
            <img :src="qrCodeUrl" alt="公众号二维码" v-if="hasQrCode" />
            <div v-else class="qr-placeholder">
              <el-icon><Avatar /></el-icon>
              <p>扫码关注公众号</p>
            </div>
          </div>
          <p class="qr-description">
            请扫描二维码关注我的公众号，在公众号给我留言，提交使用反馈或获取技术支持
          </p>
          <!-- 下载日志链接 -->
          <a
            href="javascript:void(0)"
            @click="downloadLogs"
            class="download-logs-link"
            :class="{ 'loading': downloadingLogs }"
          >
            {{ downloadingLogs ? '下载中...' : '下载应用日志' }}
          </a>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  ChatDotRound, Avatar, Plus, SuccessFilled, Download
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

// 获取二维码图片的 URL
const qrCodeUrl = import.meta.env.DEV ? '/qrcode.jpg' : '/static/qrcode.jpg'

const feedbackDialogVisible = ref(false)
const successDialogVisible = ref(false)
const submitting = ref(false)
const hasQrCode = ref(false)
const fileList = ref([])
const downloadingLogs = ref(false)

const formRef = ref()
const uploadRef = ref()

const feedbackForm = reactive({
  type: '',
  content: '',
  contact: ''
})

const formRules = {
  type: [
    { required: true, message: '请选择反馈类型', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入反馈内容', trigger: 'blur' },
    { min: 10, message: '反馈内容至少10个字符', trigger: 'blur' }
  ]
}

// 方法
const showFeedbackDialog = () => {
  feedbackDialogVisible.value = true
}

const handleFileChange = (file, fileList) => {
  // 检查文件大小
  if (file.raw && file.raw.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过10MB')
    return false
  }
}

const handleFileRemove = (file, fileList) => {
  // 文件移除处理
}

const submitFeedback = async () => {
  // 表单验证
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
  } catch (error) {
    ElMessage.warning('请完善反馈信息')
    return
  }
  
  submitting.value = true
  
  try {
    // 创建FormData用于文件上传
    const formData = new FormData()
    formData.append('type', feedbackForm.type)
    formData.append('content', feedbackForm.content)
    if (feedbackForm.contact) {
      formData.append('contact', feedbackForm.contact)
    }
    
    // 添加文件
    fileList.value.forEach(file => {
      if (file.raw) {
        formData.append('files', file.raw)
      }
    })
    
    // 这里应该调用API提交反馈
    // const response = await feedbackApi.submitFeedback(formData)
    
    // 模拟提交成功
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // 关闭反馈对话框，显示成功对话框
    feedbackDialogVisible.value = false
    successDialogVisible.value = true
    
    // 重置表单
    resetForm()
    
    ElMessage.success('反馈提交成功')
    
  } catch (error) {
    console.error('提交反馈失败:', error)
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  Object.assign(feedbackForm, {
    type: '',
    content: '',
    contact: ''
  })
  fileList.value = []
  formRef.value?.resetFields()
  uploadRef.value?.clearFiles()
}

// 检查二维码图片是否存在
onMounted(() => {
  checkQrCode()
})

const checkQrCode = () => {
  const img = new Image()
  img.onload = () => {
    hasQrCode.value = true
  }
  img.onerror = () => {
    hasQrCode.value = false
  }
  img.src = qrCodeUrl
}

// 下载应用日志
const downloadLogs = async () => {
  downloadingLogs.value = true
  try {
    // 获取API基础URL
    const apiBase = import.meta.env.DEV ? '/api' : window.location.origin

    // 发送下载请求
    const response = await axios({
      method: 'get',
      url: `${apiBase}/api/system/logs/download`,
      responseType: 'blob',
      headers: {
        'Content-Type': 'application/json'
      }
    })

    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url

    // 从响应头中获取文件名，或使用默认文件名
    const contentDisposition = response.headers['content-disposition'] || ''
    let fileName = 'logs.zip'
    const fileNameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
    if (fileNameMatch && fileNameMatch[1]) {
      fileName = fileNameMatch[1].replace(/['"]/g, '')
    }

    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('日志下载成功')
  } catch (error) {
    console.error('下载日志失败:', error)
    if (error.response?.status === 404) {
      ElMessage.warning('暂无日志文件')
    } else if (error.response?.status === 500) {
      ElMessage.error('服务器错误，无法下载日志')
    } else {
      ElMessage.error('下载失败，请稍后重试')
    }
  } finally {
    downloadingLogs.value = false
  }
}
</script>

<style scoped>
.feedback-button {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
}

.feedback-button .feedback-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 24px;
  border-radius: 32px;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.3);
  font-size: 15px;
  font-weight: 500;
  transition: all 0.3s;
  min-width: 140px;
}

.feedback-button .feedback-btn .btn-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.feedback-button .feedback-btn .btn-label {
  white-space: nowrap;
}

.feedback-button .feedback-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4);
}

.feedback-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.qr-section {
  text-align: center;
  padding: 20px;
  background: var(--border-lighter);
  border-radius: 8px;
}

.qr-code {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}

.qr-code img {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.qr-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  border: 2px dashed var(--border-light);
  border-radius: 8px;
  background: white;
}

.qr-placeholder .el-icon {
  font-size: 32px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.qr-placeholder p {
  margin: 0;
  font-size: 12px;
  color: var(--text-secondary);
}

.qr-description {
  margin: 0;
  font-size: 14px;
  color: var(--text-regular);
  line-height: 1.5;
}

.download-logs-link {
  display: inline-block;
  margin-top: 12px;
  font-size: 13px;
  color: #409eff;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.3s;
}

.download-logs-link:hover {
  color: #66b1ff;
  text-decoration: underline;
}

.download-logs-link.loading {
  color: #909399;
  cursor: not-allowed;
}

.download-logs-link.loading:hover {
  color: #909399;
  text-decoration: none;
}

.feedback-form {
  padding: 0 20px;
}

.upload-tip {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.success-content {
  text-align: center;
  padding: 20px;
}

.success-icon {
  font-size: 48px;
  color: var(--success-color);
  margin-bottom: 16px;
}

.success-content h3 {
  margin: 0 0 12px 0;
  color: var(--text-primary);
}

.success-content p {
  margin: 8px 0;
  color: var(--text-regular);
  line-height: 1.5;
}

.mobile .feedback-button {
  bottom: 15px;
  right: 15px;
}

.mobile .feedback-btn {
  padding: 12px 20px;
  font-size: 14px;
  min-width: 110px;
}

.mobile .feedback-btn .btn-icon {
  font-size: 20px;
}

.mobile .el-dialog {
  width: 95% !important;
  margin: 5vh auto !important;
}

.mobile .qr-code img,
.mobile .qr-placeholder {
  width: 100px;
  height: 100px;
}

.mobile .feedback-form {
  padding: 0 10px;
}

.mobile .success-content {
  padding: 15px;
}
</style>