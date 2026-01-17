<template>
  <div class="setup-container">
    <div class="setup-card">
      <div class="setup-header">
        <div class="icon-wrapper">
          <el-icon :size="48"><UserFilled /></el-icon>
        </div>
        <h1>欢迎使用格式转换器</h1>
        <p>请创建管理员账户以开始使用</p>
      </div>

      <el-steps :active="currentStep" finish-status="success" align-center class="steps">
        <el-step title="创建账户" />
        <el-step title="设置密码" />
        <el-step title="完成" />
      </el-steps>

      <div class="setup-content">
        <!-- 步骤1：创建账户 -->
        <div v-if="currentStep === 0" class="step-content">
          <el-form
            ref="accountFormRef"
            :model="accountForm"
            :rules="accountRules"
            label-position="top"
            @submit.prevent="nextStep"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="accountForm.username"
                placeholder="请输入用户名"
                size="large"
                clearable
                @keyup.enter="nextStep"
              >
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
              <div class="form-tip">用户名长度在3-20个字符之间</div>
            </el-form-item>
          </el-form>

          <el-button
            type="primary"
            size="large"
            style="width: 100%; margin-top: 20px"
            @click="nextStep"
          >
            下一步
          </el-button>
        </div>

        <!-- 步骤2：设置密码 -->
        <div v-if="currentStep === 1" class="step-content">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-position="top"
            @submit.prevent="handleRegister"
          >
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="passwordForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                show-password
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
              <div class="form-tip">密码长度在6-50个字符之间</div>
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                size="large"
                show-password
                @keyup.enter="handleRegister"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-form>

          <div class="button-group">
            <el-button
              size="large"
              style="flex: 1"
              @click="prevStep"
            >
              上一步
            </el-button>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              style="flex: 1"
              @click="handleRegister"
            >
              创建账户
            </el-button>
          </div>
        </div>

        <!-- 步骤3：完成 -->
        <div v-if="currentStep === 2" class="step-content success">
          <el-result
            icon="success"
            title="账户创建成功"
            sub-title="现在可以开始使用格式转换器了"
          >
            <template #extra>
              <el-button
                type="primary"
                size="large"
                @click="goToLogin"
              >
                前往登录
              </el-button>
            </template>
          </el-result>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, UserFilled } from '@element-plus/icons-vue'
import { register as registerApi, checkSetup } from '../utils/authApi'

const router = useRouter()

const currentStep = ref(0)
const loading = ref(false)

const accountFormRef = ref(null)
const passwordFormRef = ref(null)

const accountForm = reactive({
  username: ''
})

const passwordForm = reactive({
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const accountRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ]
}

const passwordRules = {
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度在 6 到 50 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const nextStep = async () => {
  if (currentStep.value === 0) {
    if (!accountFormRef.value) return
    try {
      await accountFormRef.value.validate()
      currentStep.value++
    } catch (error) {
      console.log('表单验证失败')
      ElMessage.warning('请先填写正确的用户名')
    }
  }
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const handleRegister = async () => {
  if (!passwordFormRef.value) return

  try {
    await passwordFormRef.value.validate()
  } catch (error) {
    console.log('密码表单验证失败')
    ElMessage.warning('请先填写正确的密码信息')
    return
  }

  loading.value = true
  try {
    const response = await registerApi(accountForm.username, passwordForm.password)

    if (response.success) {
      ElMessage.success('账户创建成功')
      currentStep.value = 2
    }
  } catch (error) {
    console.error('注册失败:', error)
    const message = error.response?.data?.message || error.message || '注册失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.setup-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.setup-card {
  width: 100%;
  max-width: 480px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 40px;
}

.setup-header {
  text-align: center;
  margin-bottom: 32px;
}

.icon-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  margin-bottom: 20px;
  color: white;
}

.setup-header h1 {
  font-size: 28px;
  color: #333;
  margin-bottom: 8px;
  font-weight: 600;
}

.setup-header p {
  font-size: 14px;
  color: #999;
}

.steps {
  margin-bottom: 40px;
}

.step-content {
  min-height: 200px;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.button-group {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-button) {
  border-radius: 8px;
  font-weight: 500;
}

.success {
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .setup-card {
    padding: 30px 20px;
  }

  .setup-header h1 {
    font-size: 24px;
  }
}
</style>
