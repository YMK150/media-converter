<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <div class="user-menu-trigger">
      <el-icon :size="20"><User /></el-icon>
      <span class="username">{{ username }}</span>
      <el-icon :size="16"><ArrowDown /></el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item disabled>
          <el-icon><User /></el-icon>
          <span>{{ username }}</span>
        </el-dropdown-item>
        <el-dropdown-item divided command="changePassword">
          <el-icon><Lock /></el-icon>
          <span>修改密码</span>
        </el-dropdown-item>
        <el-dropdown-item command="logout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出登录</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>

  <!-- 修改密码对话框 -->
  <el-dialog
    v-model="dialogVisible"
    title="修改密码"
    width="400px"
    :close-on-click-modal="false"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
    >
      <el-form-item label="当前密码" prop="currentPassword">
        <el-input
          v-model="form.currentPassword"
          type="password"
          placeholder="请输入当前密码"
          show-password
        />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          placeholder="请输入新密码"
          show-password
        />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="请再次输入新密码"
          show-password
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleChangePassword">
          确认修改
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, ArrowDown, Lock, SwitchButton } from '@element-plus/icons-vue'
import { changePassword as changePasswordApi } from '../utils/authApi'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const dialogVisible = ref(false)
const loading = ref(false)
const formRef = ref(null)

const username = computed(() => authStore.user?.username || '用户')

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度在 6 到 50 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'changePassword') {
    dialogVisible.value = true
  }
}

const handleChangePassword = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await changePasswordApi(form.currentPassword, form.newPassword)
      ElMessage.success('密码修改成功，请重新登录')
      dialogVisible.value = false
      // 重置表单
      form.currentPassword = ''
      form.newPassword = ''
      form.confirmPassword = ''
      // 退出登录
      handleLogout()
    } catch (error) {
      console.error('修改密码失败:', error)
      const message = error.response?.data?.message || error.message || '修改密码失败'
      ElMessage.error(message)
    } finally {
      loading.value = false
    }
  })
}

const handleLogout = () => {
  authStore.logout()
  ElMessage.info('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.user-menu-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.3s;
  background: #f5f7fa;
}

.user-menu-trigger:hover {
  background: #e4e7ed;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-dropdown-menu__item.is-disabled) {
  cursor: default;
  color: #333;
  font-weight: 500;
}
</style>
