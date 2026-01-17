<template>
  <div class="conversion-settings">
    <el-card class="media-card">
      <template #header>
        <div class="card-header">
          <span>转换设置</span>
          <el-button 
            v-if="conversionStore.sourceFiles.length > 0"
            type="warning" 
            size="small" 
            @click="clearSourceFiles"
            :icon="Close"
          >
            清空文件 ({{ conversionStore.sourceFiles.length }})
          </el-button>
        </div>
      </template>
      
      <div v-if="conversionStore.sourceFiles.length === 0" class="empty-state">
        <el-icon class="empty-icon"><Document /></el-icon>
        <div class="empty-text">请先在文件浏览器中选择要转换的文件</div>
      </div>
      
      <el-form 
        v-else
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        size="default"
      >
        <!-- 已选择文件列表 -->
        <el-form-item label="已选择文件">
          <div class="selected-files-container">
            <div class="selected-files-list">
              <div 
                v-for="(file, index) in conversionStore.sourceFiles"
                :key="index"
                class="selected-file-item"
                @click="showFullFilePath(file)"
              >
                <div class="file-item-path">
                  {{ file }}
                </div>
                <div class="file-item-actions" @click.stop>
                  <el-button 
                    size="small"
                    type="danger"
                    plain
                    circle
                    @click="removeSourceFile(index)"
                    :icon="Close"
                    class="remove-file-btn"
                  />
                </div>
              </div>
            </div>
          </div>
        </el-form-item>
        
        <!-- 目标格式选择 -->
        <el-form-item label="目标格式" prop="targetFormat">
          <el-select 
            v-model="form.targetFormat"
            placeholder="请选择目标格式"
            style="width: 100%"
            @change="onFormatChange"
            clearable
          >
            <el-option-group
              v-for="group in formatGroups"
              :key="group.label"
              :label="group.label"
            >
              <el-option
                v-for="format in group.options"
                :key="format.value"
                :label="format.label"
                :value="format.value"
              >
                <div class="format-option">
                  <el-icon><VideoPlay v-if="group.type === 'video'" /><Headset v-else /></el-icon>
                  <span>{{ format.label }}</span>
                  <span class="format-desc">{{ format.desc }}</span>
                </div>
              </el-option>
            </el-option-group>
          </el-select>
        </el-form-item>
        
        <!-- 目标目录 -->
        <el-form-item label="目标目录" prop="targetDirectory">
          <div class="directory-selector">
            <el-input 
              v-model="form.targetDirectory" 
              placeholder="请选择目标目录"
              readonly
              class="directory-input"
              @click="showDirectoryDialog"
            >
              <template #prefix>
                <el-icon class="directory-icon">
                  <Folder />
                </el-icon>
              </template>
            </el-input>
            
            <div class="directory-buttons">
              <el-tooltip content="浏览选择目录" placement="top">
                <el-button 
                  @click="showDirectoryDialog" 
                  :icon="Folder"
                  class="browse-btn"
                  type="primary"
                  size="default"
                >
                  浏览
                </el-button>
              </el-tooltip>
              
              <el-tooltip content="使用文件浏览器当前目录" placement="top">
                <el-button 
                  @click="syncWithFileBrowser" 
                  :icon="FolderOpened"
                  type="success"
                  size="default"
                  class="sync-btn"
                >
                  当前目录
                </el-button>
              </el-tooltip>
            </div>
          </div>
        </el-form-item>
        
        <!-- 高级设置 -->
        <el-collapse v-model="activeAdvanced">
          <el-collapse-item title="高级设置" name="advanced">
            <!-- 视频设置 -->
            <el-form-item 
              v-if="isVideoFormat(form.targetFormat)"
              label="视频分辨率"
            >
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-input-number
                    v-model="form.videoWidth"
                    :min="160"
                    :max="3840"
                    :step="16"
                    placeholder="宽度"
                    style="width: 100%"
                  />
                </el-col>
                <el-col :span="12">
                  <el-input-number
                    v-model="form.videoHeight"
                    :min="120"
                    :max="2160"
                    :step="9"
                    placeholder="高度"
                    style="width: 100%"
                  />
                </el-col>
              </el-row>
              <div class="resolution-presets">
                <el-button
                  v-for="preset in resolutionPresets"
                  :key="preset.label"
                  size="small"
                  @click="setResolution(preset.width, preset.height)"
                >
                  {{ preset.label }}
                </el-button>
              </div>
            </el-form-item>
            
            <el-form-item 
              v-if="isVideoFormat(form.targetFormat)"
              label="视频比特率"
            >
              <el-select 
                v-model="form.videoBitrate"
                placeholder="选择比特率"
                style="width: 100%"
              >
                <el-option
                  v-for="bitrate in videoBitrateOptions"
                  :key="bitrate.value"
                  :label="bitrate.label"
                  :value="bitrate.value"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="音频比特率">
              <el-select 
                v-model="form.audioBitrate"
                placeholder="选择比特率"
                style="width: 100%"
              >
                <el-option
                  v-for="bitrate in audioBitrateOptions"
                  :key="bitrate.value"
                  :label="bitrate.label"
                  :value="bitrate.value"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item 
              v-if="isVideoFormat(form.targetFormat)"
              label="帧率"
            >
              <el-select 
                v-model="form.frameRate"
                placeholder="选择帧率"
                style="width: 100%"
              >
                <el-option
                  v-for="fps in frameRateOptions"
                  :key="fps.value"
                  :label="fps.label"
                  :value="fps.value"
                />
              </el-select>
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
        
        <!-- 操作按钮 -->
        <div class="conversion-button-container">
          <el-button 
            type="primary" 
            size="large"
            @click="startConversion"
            :loading="conversionStore.isLoading"
            class="start-conversion-button"
          >
            <el-icon><CaretRight /></el-icon>
            开始转换 ({{ conversionStore.sourceFiles.length }} 个文件)
          </el-button>
        </div>
      </el-form>
    </el-card>
    
    <!-- 目录选择对话框 -->
    <el-dialog 
      v-model="directoryDialogVisible" 
      title="选择目标目录"
      width="80%"
      :modal-append-to-body="false"
      class="directory-dialog"
    >
      <div class="directory-browser">
        <!-- 面包屑导航 -->
        <el-breadcrumb separator="/" class="directory-breadcrumb">
          <el-breadcrumb-item 
            v-for="(segment, index) in dialogPathSegments" 
            :key="index"
            @click="navigateDialogToPath(getDialogPathToSegment(index))"
            class="breadcrumb-item"
          >
            {{ segment }}
          </el-breadcrumb-item>
        </el-breadcrumb>
        
        <!-- 工具栏 -->
        <div class="directory-toolbar">
          <el-button 
            size="small" 
            @click="navigateDialogToParent"
            :icon="FolderOpened"
            :disabled="!dialogHasParentDirectory"
          >
            返回上级
          </el-button>
          <el-button 
            size="small" 
            @click="refreshDialogFileList"
            :icon="Refresh"
          >
            刷新
          </el-button>
        </div>
        
        <!-- 目录列表 -->
        <div class="directory-list custom-scrollbar" v-loading="dialogLoading">
          <div v-if="dialogFiles.length === 0 && !dialogLoading" class="empty-state">
            <el-icon class="empty-icon"><FolderOpened /></el-icon>
            <div class="empty-text">当前目录为空</div>
          </div>
          
          <!-- 列表视图 -->
          <div v-if="dialogViewMode === 'list'" class="list-view">
            <div 
              v-for="file in dialogFiles" 
              :key="file.path"
              class="file-item directory-item"
              :class="{ 
                selected: selectedDialogDirectory && selectedDialogDirectory.path === file.path
              }"
              @click="handleDialogFileClick(file)"

            >
              <el-icon class="file-icon directory-icon">
                <Folder />
              </el-icon>
              
              <div class="file-info">
                <div class="file-name directory-name">
                  {{ file.name }}
                </div>
                <div class="directory-info">
                  单击进入目录
                </div>
              </div>
            </div>
          </div>
          
          <!-- 网格视图 -->
          <div v-else class="grid-view">
            <div 
              v-for="file in dialogFiles" 
              :key="file.path"
              class="grid-item directory-grid"
              :class="{ 
                selected: selectedDialogDirectory && selectedDialogDirectory.path === file.path
              }"
              @click="handleDialogFileClick(file)"

            >
              <el-icon class="grid-icon directory-grid-icon">
                <Folder />
              </el-icon>
              <div class="grid-name directory-grid-name">{{ file.name }}</div>
              <div class="directory-hint">单击进入</div>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="directoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmDirectorySelect" :disabled="!selectedDialogDirectory">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 创建目录对话框已移除 -->
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, h } from 'vue'
import { 
  Close, Document, VideoPlay, Headset, Folder,
  CaretRight, Refresh, List, Grid, FolderOpened,
  LocationInformation
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useConversionStore } from '@/stores/conversion'
import { useFileStore } from '@/stores/file'
import { fileBrowserApi } from '@/utils/api'

const conversionStore = useConversionStore()
let fileStore = null

// 延迟初始化 FileStore 以避免循环依赖
const getFileStore = () => {
  if (!fileStore) {
    fileStore = useFileStore()
  }
  return fileStore
}

const formRef = ref()
const directoryDialogVisible = ref(false)
const activeAdvanced = ref([])

// 目录选择对话框相关
const dialogLoading = ref(false)
const dialogViewMode = ref('list')
const dialogCurrentPath = ref('')
const dialogFiles = ref([])
const selectedDialogDirectory = ref(null)

// 标记用户是否已手动设置目标目录
const userModifiedTargetDirectory = ref(false)

// 组件是否已挂载完成
const isComponentMounted = ref(false)

// 是否正在通过对话框改变目录
const isDialogChangingDirectory = ref(false)

const form = reactive({
  targetFormat: '',
  targetDirectory: '',
  videoWidth: null,
  videoHeight: null,
  audioBitrate: 128,
  videoBitrate: 1000,
  frameRate: 25
})

// 表单验证规则
const rules = {
  targetFormat: [
    { required: true, message: '请选择目标格式', trigger: ['change', 'blur'] }
  ],
  targetDirectory: [
    { required: true, message: '请选择目标目录', trigger: ['change', 'blur'] }
  ]
}

// 格式选项
const formatGroups = computed(() => [
  {
    label: '视频格式',
    type: 'video',
    options: [
      { label: 'MP4', value: 'mp4', desc: '通用视频格式' },
      { label: 'AVI', value: 'avi', desc: '高质量视频' },
      { label: 'MOV', value: 'mov', desc: 'Apple格式' },
      { label: 'WMV', value: 'wmv', desc: 'Windows格式' },
      { label: 'MKV', value: 'mkv', desc: '多轨道视频' },
      { label: 'FLV', value: 'flv', desc: 'Flash视频' }
    ]
  },
  {
    label: '音频格式',
    type: 'audio',
    options: [
      { label: 'MP3', value: 'mp3', desc: '通用音频' },
      { label: 'WAV', value: 'wav', desc: '无损音频' },
      { label: 'AAC', value: 'aac', desc: '高质量音频' },
      { label: 'FLAC', value: 'flac', desc: '无损压缩' },
      { label: 'M4A', value: 'm4a', desc: 'Apple音频' },
      { label: 'WMA', value: 'wma', desc: 'Windows音频' }
    ]
  }
])

// 分辨率预设
const resolutionPresets = [
  { label: '4K', width: 3840, height: 2160 },
  { label: '2K', width: 2560, height: 1440 },
  { label: '1080p', width: 1920, height: 1080 },
  { label: '720p', width: 1280, height: 720 },
  { label: '480p', width: 854, height: 480 },
  { label: '360p', width: 640, height: 360 }
]

// 视频比特率选项
const videoBitrateOptions = [
  { label: '500 kbps', value: 500 },
  { label: '1000 kbps (1 Mbps)', value: 1000 },
  { label: '2000 kbps (2 Mbps)', value: 2000 },
  { label: '5000 kbps (5 Mbps)', value: 5000 },
  { label: '8000 kbps (8 Mbps)', value: 8000 },
  { label: '10000 kbps (10 Mbps)', value: 10000 },
  { label: '15000 kbps (15 Mbps)', value: 15000 }
]

// 音频比特率选项
const audioBitrateOptions = [
  { label: '64 kbps', value: 64 },
  { label: '96 kbps', value: 96 },
  { label: '128 kbps (标准)', value: 128 },
  { label: '160 kbps', value: 160 },
  { label: '192 kbps (高质量)', value: 192 },
  { label: '256 kbps', value: 256 },
  { label: '320 kbps (最高质量)', value: 320 }
]

// 帧率选项
const frameRateOptions = [
  { label: '15 fps', value: 15 },
  { label: '24 fps (电影)', value: 24 },
  { label: '25 fps (PAL)', value: 25 },
  { label: '30 fps (NTSC)', value: 30 },
  { label: '50 fps', value: 50 },
  { label: '60 fps (流畅)', value: 60 }
]

// 方法
const getFileName = (filePath) => {
  return filePath.split(/[\\/]/).pop()
}

const isVideoFile = (filePath) => {
  const videoExts = ['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv', 'webm', '3gp', 'm4v']
  const ext = filePath.split('.').pop().toLowerCase()
  return videoExts.includes(ext)
}

const isVideoFormat = (format) => {
  const videoFormats = ['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv', 'webm', '3gp', 'm4v']
  return videoFormats.includes(format?.toLowerCase())
}

const removeSourceFile = (index) => {
  conversionStore.sourceFiles.splice(index, 1)
}

const clearSourceFiles = () => {
  conversionStore.setSourceFiles([])
}

const showFullFilePath = (filePath) => {
  ElMessageBox.alert(filePath, '完整文件路径', {
    confirmButtonText: '确定',
    type: 'info'
  })
}

const onFormatChange = () => {
  console.log('格式变化:', form.targetFormat)
  
  // 如果切换到音频格式，清除视频相关设置
  if (!isVideoFormat(form.targetFormat)) {
    form.videoWidth = null
    form.videoHeight = null
    form.videoBitrate = null
    form.frameRate = null
  }
}

const setResolution = (width, height) => {
  form.videoWidth = width
  form.videoHeight = height
}

// 目录选择对话框相关方法
const loadDialogFileList = async (path) => {
  if (!path && path !== '') {
    console.error('对话框路径参数为空')
    return
  }
  
  dialogLoading.value = true
  try {
    const response = await fileBrowserApi.listFiles(path)
    
    if (response && response.files) {
      dialogCurrentPath.value = response.currentPath || path
      // 只显示目录
      dialogFiles.value = response.files.filter(file => file.isDirectory && file.name !== "..")
    } else {
      ElMessage.error('加载目录列表失败: 响应格式错误')
      dialogFiles.value = []
    }
  } catch (error) {
    console.error('加载目录列表失败:', error)
    // 错误信息已由axios拦截器统一处理
    dialogFiles.value = []
  } finally {
    dialogLoading.value = false
  }
}

const handleDialogFileClick = (file) => {
  selectedDialogDirectory.value = file
  navigateDialogToPath(file.path)
}

const refreshDialogFileList = () => {
  loadDialogFileList(dialogCurrentPath.value)
}

const navigateDialogToPath = (path) => {
  loadDialogFileList(path)
}

const navigateDialogToParent = () => {
  if (!dialogHasParentDirectory.value) return
  
  const path = dialogCurrentPath.value
  
  // 如果是根目录的子目录（如 /bin, /usr, /lib 等），直接返回根目录
  if (path && path.startsWith('/') && !path.match(/^[A-Za-z]:[\\\/]?$/)) {
    // 检查是否只有一段路径（如 /bin, /lib 等）
    const parts = path.split('/').filter(Boolean)
    if (parts.length === 1) {
      navigateDialogToPath('/')
      return
    }
    
    // 多段路径，返回到上一级
    const parentParts = parts.slice(0, -1)
    const parentPath = '/' + parentParts.join('/')
    navigateDialogToPath(parentPath)
    return
  }
  
  // Windows系统处理
  if (path && (path.includes('\\') || path.includes(':'))) {
    const parts = path.split(/[\\/]/).filter(Boolean)
    if (parts.length > 1) {
      const parentParts = parts.slice(0, -1)
      const parentPath = parentParts.join('/') || (path.match(/^[A-Za-z]:/) ? path.match(/^[A-Za-z]:/)[0] + '\\' : '')
      navigateDialogToPath(parentPath)
    } else {
      // 单段路径，可能是驱动器根目录的子目录
      const match = path.match(/^[A-Za-z]:/)
      if (match) {
        navigateDialogToPath(match[0] + '\\')
      }
    }
  }
}

const getDialogPathToSegment = (index) => {
  const segments = dialogPathSegments.value
  const path = dialogCurrentPath.value
  
  // 如果是Windows系统
  const isWindows = navigator.platform.toLowerCase().includes('win') || 
                   (path && (path.includes('\\') || path.includes(':')))
  
  if (segments[0] === '根目录') return ''
  
  // 获取路径段
  const pathParts = segments.slice(0, index + 1)
  
  // 处理Linux/macOS路径
  if (!isWindows) {
    if (pathParts[0] === '/') {
      // 如果第一个段是根目录，构建绝对路径
      const joinedPath = pathParts.slice(1).join('/') // 跳过根目录符号
      return '/' + joinedPath
    } else if (pathParts[0] === '根目录') {
      // 处理显示为"根目录"的情况
      if (index === 0) return ''
      const remainingParts = pathParts.slice(1).join('/')
      return remainingParts ? '/' + remainingParts : '/'
    } else {
      // 构建相对路径
      return pathParts.join('/')
    }
  }
  
  // Windows路径
  return pathParts.join('/')
}

const confirmDirectorySelect = () => {
  if (selectedDialogDirectory.value) {
    // 标记正在通过对话框改变目录
    isDialogChangingDirectory.value = true
    
    form.targetDirectory = selectedDialogDirectory.value.path
    
    // 标记用户已手动选择目录，停止自动同步
    userModifiedTargetDirectory.value = true
    
    directoryDialogVisible.value = false
  }
}

// 目录选择对话框相关计算属性
const dialogPathSegments = computed(() => {
  if (!dialogCurrentPath.value) return ['根目录']
  
  const path = dialogCurrentPath.value
  
  // 检测操作系统类型
  const isWindows = navigator.platform.toLowerCase().includes('win') || 
                   path.includes('\\') || 
                   path.includes(':')
  
  if (isWindows) {
    // Windows路径处理
    const parts = path.split(/[\\/]/).filter(Boolean)
    return parts.length > 0 ? parts : ['根目录']
  } else {
    // Linux/macOS路径处理
    const parts = path.split('/').filter(Boolean)
    
    // 如果路径是空的或者是根目录 /
    if (parts.length === 0) {
      return ['/']
    }
    
    // 如果路径以 / 开头，保留根目录符号
    if (path.startsWith('/')) {
      return ['/', ...parts]
    }
    
    return parts
  }
})

const dialogHasParentDirectory = computed(() => {
  const segments = dialogPathSegments.value
  const path = dialogCurrentPath.value
  
  // 如果路径为空，没有上级目录
  if (!path) return false
  
  // Linux根目录没有上级目录
  if (path === '/') return false
  
  // Windows驱动器根目录没有上级目录
  if (path.match(/^[A-Za-z]:[\\\/]?$/)) return false
  
  // 如果只有一个段且是根目录符号，没有上级目录
  if (segments.length === 1 && (segments[0] === '根目录' || segments[0] === '/')) {
    return false
  }
  
  // 任何超过一个段的路径，或者单个段但不是根目录的情况都有上级目录
  return true
})

const showDirectoryDialog = async () => {
  directoryDialogVisible.value = true
  selectedDialogDirectory.value = null
  
  // 初始化对话框
  try {
    const roots = await fileBrowserApi.getSystemRoots()
    if (roots && roots.length > 0) {
      let targetPath = form.targetDirectory || roots[0]
      
      // 如果当前路径为空，选择合适的默认路径
      if (!form.targetDirectory) {
        const isWindows = navigator.platform.toLowerCase().includes('win')
        if (!isWindows) {
          const userHome = roots.find(root => root.includes('Users') || root.includes('home') || root.includes('user'))
          if (userHome) {
            targetPath = userHome
          } else if (roots.some(root => root === '/')) {
            targetPath = '/'
          }
        } else {
          const cDrive = roots.find(root => root.startsWith('C:'))
          if (cDrive) {
            targetPath = cDrive
          }
        }
      }
      
      await loadDialogFileList(targetPath)
    }
  } catch (error) {
    console.error('初始化目录对话框失败:', error)
    ElMessage.error('初始化目录对话框失败')
  }
}

const syncWithFileBrowser = () => {
  const fileStoreInstance = getFileStore()
  const currentDir = fileStoreInstance?.currentDirectory
  if (currentDir) {
    form.targetDirectory = currentDir
    userModifiedTargetDirectory.value = false // 重置标记，重新启用自动同步
    ElMessage.success('已同步到文件浏览器当前目录')
  } else {
    ElMessage.warning('文件浏览器当前目录不可用')
  }
}



const startConversion = async () => {
  // 表单验证
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
  } catch (error) {
    console.log('表单验证失败:', error)
    ElMessage.warning('请完善转换设置')
    return
  }
  
  // 先设置转换参数到store
  conversionStore.setConversionSettings({
    targetFormat: form.targetFormat,
    targetDirectory: form.targetDirectory,
    videoWidth: form.videoWidth,
    videoHeight: form.videoHeight,
    audioBitrate: form.audioBitrate,
    videoBitrate: form.videoBitrate,
    frameRate: form.frameRate
  })
  
  // 使用store中的数据进行验证（确保数据同步）
  const errors = conversionStore.validateSettings()
  if (errors.length > 0) {
    console.log('Store验证失败:', errors)
    console.log('当前form值:', form)
    console.log('当前store值:', conversionStore.conversionSettings)
    ElMessage.error(errors[0])
    return
  }
  
    // 显示确认对话框
  try {
    await ElMessageBox.confirm(
      h('div', { style: 'line-height: 1.8; color: #303133;' }, [
        h('p', { style: 'font-size: 14px; margin: 6px 0; color: #909399; padding-left: 0;' }, `文件数量：${conversionStore.sourceFiles.length} 个`),
        h('p', { style: 'font-size: 14px; margin: 6px 0; color: #909399; padding-left: 0;' }, `目标格式：${form.targetFormat.toUpperCase()}`),
        h('p', { style: 'font-size: 14px; margin: 6px 0; color: #909399; padding-left: 0;' }, `目标目录：${form.targetDirectory}`),
        h('p', { style: 'font-size: 15px; font-weight: 500; margin: 12px 0 8px 0; color: #606266;' }, `转换开始后，文件将被添加到转换队列中按顺序处理。`),
        h('div', { style: 'margin-top: 16px; padding: 16px; background-color: #fef0f0; border: 1px solid #fbc4c4; border-radius: 6px;' }, [
          h('p', { style: 'font-size: 14px; font-weight: 600; color: #f56c6c; margin: 0 0 8px 0;' }, '⚠️ 性能风险警告'),
          h('p', { style: 'font-size: 13px; font-weight: 400; color: #f56c6c; line-height: 1.6; margin: 0;' }, '音视频转换属于高 CPU 占用操作，可能会占用大量系统资源，导致电脑运行缓慢。请确保关闭其他重要程序，避免在任务执行期间进行其他高负载操作。长时间运行可能导致系统发热，请保证电脑通风散热良好。')
        ])
      ]),
      '确定要开始转换吗？',
      {
        confirmButtonText: '确认转换',
        cancelButtonText: '取消',
        type: 'warning',
        draggable: true,
        customStyle: {
          maxWidth: '500px'
        }
      }
    )
    
    // 用户确认后执行转换
    await executeConversion()
    
  } catch (cancel) {
    // 用户取消操作
    console.log('用户取消了转换操作')
  }
}

// 执行转换的独立方法
const executeConversion = async () => {
  try {
    // 创建转换任务
    const taskId = await conversionStore.createConversionTask(
      conversionStore.sourceFiles,
      conversionStore.conversionSettings
    )
    
    // 任务创建成功（返回任务ID）
    if (taskId) {
      ElMessage.success(`已添加 ${conversionStore.sourceFiles.length} 个文件到转换队列`)
      
      // 清空表单
      conversionStore.setSourceFiles([])
      Object.assign(form, {
        targetFormat: '',
        targetDirectory: '',
        videoWidth: null,
        videoHeight: null,
        audioBitrate: 128,
        videoBitrate: 1000,
        frameRate: 25
      })
      
      // 等待一小段时间让后端保存任务，然后刷新任务列表
      // WebSocket 也会推送更新，但为了确保立即显示完整信息，这里也刷新一下
      // 延迟时间稍长一些，确保后端已完全保存任务
      setTimeout(async () => {
        await conversionStore.loadTasks(0, 20)
        await conversionStore.loadQueueStats()
      }, 800)
    }
  } catch (error) {
    console.error('创建转换任务失败:', error)
    // 错误信息已由axios拦截器统一处理
  }
}

// 监听表单变化
watch(() => form.targetFormat, (newValue) => {
  console.log('表单格式变化:', newValue)
})

watch(() => form.targetDirectory, (newValue, oldValue) => {
  console.log('表单目录变化:', newValue, 'oldValue:', oldValue)
  
  // 只有在组件挂载完成后，并且用户操作导致的目录变化才标记为手动设置
  if (newValue && oldValue && isComponentMounted.value) {
    // 检查是否是通过对话框设置目录导致的 change
    // 如果不是通过对话框设置，可能是初始化导致的，不标记为用户手动设置
    if (isDialogChangingDirectory.value) {
      userModifiedTargetDirectory.value = true
      isDialogChangingDirectory.value = false
    }
  }
})

// 监听文件浏览器当前路径变化，同步更新目标目录
watch(
  () => getFileStore()?.currentDirectory,
  (newPath) => {
    console.log('文件浏览器路径变化:', newPath, 'userModified:', userModifiedTargetDirectory.value)
    
    // 只有在用户没有手动设置过目标目录时才自动同步
    if (newPath && !userModifiedTargetDirectory.value) {
      console.log('自动同步目标目录:', newPath)
      form.targetDirectory = newPath
    }
  },
  { immediate: true }
)

// 生命周期
onMounted(() => {
  // 初始设置目标目录为文件浏览器的当前目录（仅获取一次，避免循环依赖）
  const fileStoreInstance = getFileStore()
  const initialDirectory = fileStoreInstance?.currentDirectory
  if (initialDirectory) {
    form.targetDirectory = initialDirectory
    console.log('设置目标目录为文件浏览器当前目录:', initialDirectory)
    // 不要标记为用户修改，让自动同步继续生效
    userModifiedTargetDirectory.value = false
  } else {
    // 如果文件浏览器还没有加载，设置默认目录
    form.targetDirectory = getDefaultOutputDirectory()
    console.log('设置目标目录为默认目录:', getDefaultOutputDirectory())
    // 设置默认目录也不标记为用户修改，允许后续自动同步
    userModifiedTargetDirectory.value = false
  }
  console.log('组件挂载，初始表单值:', form)
  
  // 组件挂载完成
  isComponentMounted.value = true
})



const getDefaultOutputDirectory = () => {
  // 简单的默认目录设置
  const os = navigator.userAgent.toLowerCase()
  if (os.includes('win')) {
    return 'C:\\Users\\Public\\Videos\\Converted'
  } else if (os.includes('mac')) {
    return '/Users/Shared/Movies/Converted'
  } else {
    return '/home/Shared/Videos/Converted'
  }
}
</script>

<style scoped>
.conversion-settings {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.selected-files-container {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: white;
  overflow: hidden;
}

.selected-files-list {
  max-height: 240px;
  overflow-y: auto;
}

.selected-file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  border-bottom: 1px solid #f0f2f5;
  transition: all 0.2s ease;
  cursor: pointer;
  min-height: 32px;
}

.selected-file-item:hover {
  background: #f8f9fa;
}

.selected-file-item:hover .file-item-path {
  background: #e9ecef;
  border-color: #d1d5db;
}

.selected-file-item:last-child {
  border-bottom: none;
}

.file-item-path {
  flex: 1;
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  background: #f8f9fa;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  margin-right: 8px;
  transition: all 0.2s ease;
  line-height: 1.2;
}

.file-item-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.remove-file-btn {
  opacity: 0.6;
  transition: all 0.2s ease;
  padding: 4px !important;
  min-height: 24px !important;
  width: 24px !important;
}

.remove-file-btn:hover {
  opacity: 1;
  transform: scale(1.05);
}

/* 目录选择器样式 */
.directory-selector {
  width: 100%;
}

.directory-input {
  width: 100%;
  margin-bottom: 8px;
}

.directory-input .el-input__inner {
  cursor: pointer;
  padding-left: 40px;
}

.directory-input .el-input__inner:hover {
  background-color: var(--background-lighter);
  border-color: var(--primary-color);
}

.directory-icon {
  color: var(--primary-color);
  font-size: 16px;
  margin-left: 4px;
}

.directory-buttons {
  display: flex;
  gap: 8px;
  width: auto;
  align-items: stretch;
  justify-content: flex-end;
  margin-left: auto;
  max-width: 200px;
}

.browse-btn {
  flex: 0 1 auto;
  min-width: 80px;
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border: 1px solid #3a8ee6;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(64, 158, 255, 0.2);
  height: 40px;
  padding: 0 12px;
  font-size: 13px;
}

.browse-btn:hover {
  background: linear-gradient(135deg, #3a8ee6 0%, #337ecc 100%);
  border-color: #337ecc;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.3);
}

.browse-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.browse-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.6s;
}

.browse-btn:hover::before {
  left: 100%;
}

.sync-btn {
  flex: 0 1 auto;
  min-width: 80px;
  background: linear-gradient(135deg, #67c23a 0%, #529b2e 100%);
  border: 1px solid #529b2e;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(103, 194, 58, 0.2);
  height: 40px;
  padding: 0 12px;
  font-size: 13px;
}

.sync-btn:hover {
  background: linear-gradient(135deg, #529b2e 0%, #3d7e1f 100%);
  border-color: #3d7e1f;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(103, 194, 58, 0.3);
}

.sync-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.2);
}

.sync-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.6s;
}

.sync-btn:hover::before {
  left: 100%;
}

.resolution-presets {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.format-option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.format-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin-left: auto;
}

.mobile .card-header {
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
}

.mobile .selected-files-container {
  border-radius: 6px;
}

.mobile .selected-files-list {
  max-height: 180px;
}

.mobile .selected-file-item {
  padding: 5px 8px;
  min-height: 28px;
}

.mobile .file-item-path {
  font-size: 11px;
  padding: 3px 6px;
  margin-right: 6px;
  line-height: 1.1;
}

.mobile .remove-file-btn {
  padding: 3px !important;
  min-height: 20px !important;
  width: 20px !important;
}



.mobile .resolution-presets {
  gap: 4px;
}

.mobile .resolution-presets .el-button {
  font-size: 12px;
  padding: 4px 8px;
}

/* 目录选择对话框样式 */
.directory-dialog .el-dialog__body {
  padding: 0;
}

.directory-browser {
  display: flex;
  flex-direction: column;
  height: 500px;
  max-height: 70vh;
}

.directory-breadcrumb {
  margin: 16px;
  padding: 12px;
  background: var(--border-lighter);
  border-radius: 4px;
}

.directory-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 16px 16px 16px;
  padding: 8px 0;
}

.directory-list {
  flex: 1;
  margin: 0 16px 16px 16px;
  overflow: auto;
  overflow-x: hidden;
  border: 1px solid var(--border-light);
  border-radius: 4px;
}

/* 移动端适配 */
.mobile .directory-browser {
  height: 400px;
}

.mobile .directory-dialog {
  width: 95% !important;
  margin: 2.5vh auto;
}

.mobile .directory-breadcrumb,
.mobile .directory-toolbar,
.mobile .directory-list {
  margin-left: 8px;
  margin-right: 8px;
}

.mobile .directory-breadcrumb {
  padding: 8px;
  font-size: 12px;
}

.mobile .directory-toolbar .el-button {
  font-size: 12px;
  padding: 4px 8px;
}

.mobile .directory-list .grid-view {
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 8px;
  padding: 8px;
}

.mobile .directory-list .grid-item {
  padding: 8px;
}

.mobile .directory-list .grid-icon {
  font-size: 24px;
}

.mobile .directory-list .grid-name {
  font-size: 11px;
}





/* 移动端适配 */
.mobile .directory-buttons {
  gap: 6px;
  flex-wrap: nowrap;
  justify-content: space-between;
  max-width: 100%;
  margin-left: 0;
}

.mobile .browse-btn,
.mobile .sync-btn {
  height: 44px;
  font-size: 13px;
  min-width: 70px;
  padding: 0 8px;
  flex: 1;
}

.mobile .directory-icon {
  font-size: 14px;
}

.mobile .directory-input .el-input__inner {
  padding-left: 36px;
}

/* 中等屏幕适配 */
@media (max-width: 768px) {
  .directory-buttons {
    max-width: 180px;
  }
}

/* 小屏幕适配 - 保持按钮在一行 */
@media (max-width: 480px) {
  .directory-buttons {
    gap: 4px;
    max-width: 100%;
  }
  
  .browse-btn,
  .sync-btn {
    min-width: 65px;
    padding: 0 6px;
    font-size: 12px;
  }
}

/* 更小屏幕适配 */
@media (max-width: 320px) {
  .directory-buttons {
    gap: 3px;
  }
  
  .browse-btn,
  .sync-btn {
    min-width: 60px;
    padding: 0 4px;
    font-size: 11px;
  }
}



/* 新建目录按钮样式已移除 */

/* 目录样式 */
.directory-item {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-left: 4px solid #409eff;
  cursor: pointer !important;
}

.directory-item:hover {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  border-left-color: #1976d2;
}

.directory-item.selected {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  border-left: 4px solid #2563eb;
  box-shadow: 0 2px 12px rgba(37, 99, 235, 0.2);
}

.directory-icon {
  color: #409eff !important;
}

.directory-name {
  font-weight: 600;
  color: #1976d2;
  cursor: pointer;
}

.directory-info {
  font-size: 12px;
  color: #666;
  font-style: italic;
}

/* 网格视图目录样式 */
.directory-grid {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border: 2px solid #409eff;
}

.directory-grid:hover {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  border-color: #1976d2;
  transform: translateY(-4px) scale(1.02);
}

.directory-grid.selected {
  background: linear-gradient(135deg, #dbeafe 0%, #93c5fd 100%);
  border: 3px solid var(--primary-color);
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.3);
  transform: translateY(-4px) scale(1.02);
}

.directory-grid-icon {
  color: #409eff !important;
  font-size: 36px !important;
}

.directory-grid-name {
  font-weight: 600;
  color: #1976d2;
  font-size: 13px;
}

.directory-hint {
  font-size: 10px;
  color: #666;
  margin-top: 4px;
  text-align: center;
}

/* 文件项布局优化 */
.file-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-lighter);
  transition: all 0.2s ease;
  cursor: pointer;
  gap: 12px;
}

.file-item:hover {
  background-color: var(--background-base);
}

.file-icon {
  font-size: 20px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
  word-break: break-all;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: var(--text-secondary);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  font-size: 14px;
}

.breadcrumb-item {
  cursor: pointer;
}

.breadcrumb-item:hover {
  color: var(--primary-color);
}

.grid-view {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
  padding: 16px;
}

.grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.grid-item:hover {
  border-color: var(--primary-color);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.grid-item.selected {
  background-color: #ecf5ff;
  border-color: var(--primary-color);
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.3);
  transform: translateY(-4px) scale(1.02);
}

.grid-icon {
  font-size: 32px;
  margin-bottom: 8px;
  color: var(--text-regular);
}

.grid-name {
  font-size: 12px;
  text-align: center;
  word-break: break-all;
  line-height: 1.4;
}

/* 开始转换按钮样式 - 确保始终居中 */
.conversion-button-container {
  margin-top: 20px;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  padding: 0 20px;
}

.start-conversion-button {
  width: 100% !important;
  max-width: 400px;
  margin: 0 auto !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  height: 48px;
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%) !important;
  border: 1px solid #337ecc !important;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  margin-left: auto !important;
  margin-right: auto !important;
}

.start-conversion-button:hover {
  background: linear-gradient(135deg, #337ecc 0%, #2b6cb0 100%) !important;
  border-color: #2b6cb0 !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.4) !important;
}

.start-conversion-button:active {
  transform: translateY(0) !important;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2) !important;
}

.start-conversion-button .el-icon {
  font-size: 18px;
}

/* 滚动条样式 */
.selected-files-list::-webkit-scrollbar {
  width: 6px;
}

.selected-files-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.selected-files-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.selected-files-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 移动端适配 */
.mobile .start-conversion-button {
  max-width: 100%;
  font-size: 14px;
  height: 44px;
}

.mobile .start-conversion-button .el-icon {
  font-size: 16px;
}

/* 警告文字样式 */
.confirm-dialog-content {
  line-height: 1.8;
  color: #303133;
}

.dialog-info {
  font-size: 15px;
  font-weight: 500;
  margin: 12px 0 8px 0;
  color: #606266;
}

.dialog-detail {
  font-size: 14px;
  margin: 6px 0;
  color: #909399;
  padding-left: 0;
}

.warning-box {
  margin-top: 16px;
  padding: 16px;
  background-color: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 6px;
}

.warning-title {
  font-size: 14px;
  font-weight: 600;
  color: #f56c6c;
  margin: 0 0 8px 0;
}

.warning-text {
  font-size: 13px;
  font-weight: 400;
  color: #f56c6c;
  line-height: 1.6;
  margin: 0;
}
</style>