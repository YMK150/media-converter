<template>
  <div class="file-browser">
    <el-card class="media-card">
      <template #header>
        <div class="card-header">
          <span>文件浏览器</span>
          <div class="header-actions">
            <el-button
                type="primary"
                size="small"
                @click="showDirectoryDialog"
                :icon="Folder"
            >
              选择目录
            </el-button>


          </div>
        </div>
      </template>
      
      <!-- 面包屑导航 -->
      <el-breadcrumb separator="/" class="breadcrumb">
        <el-breadcrumb-item 
          v-for="(segment, index) in pathSegments" 
          :key="index"
          @click="navigateToPath(getPathToSegment(index))"
          class="breadcrumb-item"
        >
          {{ segment }}
        </el-breadcrumb-item>
      </el-breadcrumb>
      
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button-group>
          <el-button 
            size="small" 
            @click="navigateToParent"
            :icon="FolderOpened"
            :disabled="!hasParentDirectory"
          >
            返回上级
          </el-button>
          <el-button 
            size="small" 
            @click="selectAllFiles"
            :icon="Select"
          >
            全选
          </el-button>
          <el-button 
            size="small" 
            @click="clearSelection"
            :icon="CloseBold"
          >
            清空选择
          </el-button>
          <el-button 
            size="small" 
            @click="refreshFileList"
            :icon="Refresh"
          >
            刷新
          </el-button>
        </el-button-group>
        
        <div class="view-toggle">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="list">
              <el-icon><List /></el-icon>
              列表
            </el-radio-button>
            <el-radio-button label="grid">
              <el-icon><Grid /></el-icon>
              网格
            </el-radio-button>
          </el-radio-group>
        </div>
      </div>
      
      <!-- 文件列表 -->
      <div class="file-list custom-scrollbar" v-loading="loading">
        <div v-if="files.length === 0 && !loading" class="empty-state">
          <el-icon class="empty-icon"><FolderOpened /></el-icon>
          <div class="empty-text">当前目录为空</div>
        </div>
        
        <!-- 列表视图 -->
        <div v-if="viewMode === 'list'" class="list-view">
          <div 
            v-for="file in files" 
            :key="file.path"
            class="file-item"
            :class="{ 
              selected: selectedFiles.some(f => f.path === file.path),
              'directory-item': file.isDirectory,
              'media-file-item': !file.isDirectory && file.isMediaFile,
              'regular-file-item': !file.isDirectory && !file.isMediaFile
            }"
            @click="handleFileClick(file)"
          >
            <el-icon class="file-icon" :class="{ 'directory-icon': file.isDirectory }">
              <Folder v-if="file.isDirectory" />
              <VideoPlay v-else-if="file.mediaType === 'video'" />
              <Headset v-else-if="file.mediaType === 'audio'" />
              <Document v-else />
            </el-icon>
            
            <div class="file-info">
              <div class="file-name" :class="{ 'directory-name': file.isDirectory }">
                {{ file.name }}
              </div>
              <div class="file-meta" v-if="!file.isDirectory">
                {{ formatFileSize(file.size) }} • 
                {{ formatDateTime(file.lastModified) }}
              </div>
              <div class="directory-info" v-else>
                单击打开目录
              </div>
            </div>
            
            <div class="file-actions">
              <el-tag 
                v-if="file.isDirectory"
                size="small"
                type="info"
                effect="plain"
              >
                目录
              </el-tag>
              <el-tag 
                v-else-if="file.isMediaFile && file.mediaType"
                size="small"
                :type="file.mediaType === 'video' ? 'success' : 'warning'"
                effect="plain"
              >
                {{ file.mediaType === 'video' ? '视频' : '音频' }}
              </el-tag>
              <el-tag 
                v-else-if="!file.isDirectory"
                size="small"
                type="info"
                effect="plain"
              >
                文件
              </el-tag>
              <el-checkbox 
                v-if="!file.isDirectory && file.isMediaFile"
                :model-value="selectedFiles.some(f => f.path === file.path)"
                @change="(checked) => toggleFileSelection(file, checked)"
                @click.stop
                class="file-checkbox"
              />
            </div>
          </div>
        </div>
        
        <!-- 网格视图 -->
        <div v-else class="grid-view">
          <div 
            v-for="file in files" 
            :key="file.path"
            class="grid-item"
            :class="{ 
              selected: selectedFiles.some(f => f.path === file.path),
              'directory-grid': file.isDirectory
            }"
            @click="handleFileClick(file)"
          >
            <el-icon class="grid-icon" :class="{ 'directory-grid-icon': file.isDirectory }">
              <Folder v-if="file.isDirectory" />
              <VideoPlay v-else-if="file.mediaType === 'video'" />
              <Headset v-else-if="file.mediaType === 'audio'" />
              <Document v-else />
            </el-icon>
            
            <div class="grid-name" :class="{ 'directory-grid-name': file.isDirectory }">{{ file.name }}</div>
            <div class="directory-hint" v-if="file.isDirectory">单击进入</div>
            
            <el-checkbox 
              v-if="!file.isDirectory && file.isMediaFile"
              class="grid-checkbox"
              :model-value="selectedFiles.some(f => f.path === file.path)"
              @change="(checked) => toggleFileSelection(file, checked)"
              @click.stop
            />
          </div>
        </div>
      </div>

            <!-- 目录统计信息 -->
      <div class="directory-stats" v-if="files.length > 0">
        <el-tag size="small" type="info" effect="plain">
          总计: {{ files.length -1}} 项
        </el-tag>
        <el-tag size="small" type="success" effect="plain" v-if="directoryCount > 0">
          目录: {{ directoryCount -1}}
        </el-tag>
        <el-tag size="small" type="warning" effect="plain" v-if="mediaFileCount > 0">
          媒体文件: {{ mediaFileCount }}
        </el-tag>
      </div>
    </el-card>
    

    
    <!-- 目录选择对话框 -->
    <el-dialog 
      v-model="directoryDialogVisible" 
      title="选择目录"
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
    
    <!-- 创建目录对话框 -->
    <el-dialog 
      v-model="createDirDialogVisible" 
      title="新建目录"
      width="30%"
    >
      <el-form :model="newDirForm" label-width="80px">
        <el-form-item label="目录名">
          <el-input 
            v-model="newDirForm.name" 
            placeholder="请输入目录名称"
            @keyup.enter="confirmCreateDirectory"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="createDirDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreateDirectory">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { 
  Folder, FolderOpened, VideoPlay, Headset, Document,
  Select, Refresh, List, Grid
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fileBrowserApi } from '@/utils/api'
import { useFileStore } from '@/stores/file'
import { useConversionStore } from '@/stores/conversion'

const fileStore = useFileStore()
const conversionStore = useConversionStore()

// 响应式数据
const loading = ref(false)
const viewMode = ref('list')
const currentPath = ref('')
const files = ref([])
const selectedFiles = ref([])
const directoryDialogVisible = ref(false)
const createDirDialogVisible = ref(false)
const directoryTree = ref([])
const selectedDirectory = ref('')

// 目录选择对话框相关
const dialogLoading = ref(false)
const dialogViewMode = ref('list')
const dialogCurrentPath = ref('')
const dialogFiles = ref([])
const selectedDialogDirectory = ref(null)

const newDirForm = reactive({
  name: ''
})

const treeProps = {
  label: 'label',
  children: 'children',
  isLeaf: 'isLeaf'
}

// 计算属性
const pathSegments = computed(() => {
  if (!currentPath.value) return ['根目录']
  
  const path = currentPath.value
  
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

const hasParentDirectory = computed(() => {
  const segments = pathSegments.value
  const path = currentPath.value
  
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

const directoryCount = computed(() => {
  return files.value.filter(file => file.isDirectory).length
})

const mediaFileCount = computed(() => {
  return files.value.filter(file => !file.isDirectory && file.isMediaFile).length
})

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

// 方法
const loadFileList = async (path) => {
  if (!path && path !== '') {
    console.error('路径参数为空')
    return
  }
  
  loading.value = true
  try {
    const response = await fileBrowserApi.listFiles(path)
    
    if (response && response.files) {
      currentPath.value = response.currentPath || path
      files.value = response.files || []
      
      // 更新fileStore中的当前目录
      fileStore.setCurrentDirectory(currentPath.value)
      
      // 清空选择状态
      selectedFiles.value = []
    } else {
      ElMessage.error('加载文件列表失败: 响应格式错误')
      files.value = []
    }
  } catch (error) {
    console.error('加载文件列表失败:', error)
    // 错误信息已由axios拦截器统一处理
    files.value = []
  } finally {
    loading.value = false
  }
}

const loadSystemRoots = async () => {
  try {
    const roots = await fileBrowserApi.getSystemRoots()
    directoryTree.value = roots.map(root => ({
      label: root,
      path: root,
      leaf: false
    }))
  } catch (error) {
    console.error('加载系统根目录失败:', error)
  }
}

const loadNode = async (node, resolve) => {
  try {
    if (node.level === 0) {
      // 根节点，加载系统根目录
      const roots = await fileBrowserApi.getSystemRoots()
      
      const rootNodes = roots.map(root => ({
        label: root,
        path: root,
        isLeaf: false
      }))

      resolve(rootNodes)
    } else {
      // 子节点，加载目录内容
      const response = await fileBrowserApi.listFiles(node.data.path)
      
      if (response && response.files) {
          const allDirectories = response.files.filter(file => file.isDirectory && file.name !== "..")
        
        const directories = allDirectories.map(dir => ({
          label: dir.name,
          path: dir.path,
          isLeaf: false // 所有目录都设置为非叶子节点，让loadNode处理
        }))
        
        resolve(directories)
      } else {
        resolve([])
      }
    }
  } catch (error) {
    console.error('loadNode 失败:', error)
    resolve([])
  }
}

const showDirectoryDialog = async () => {
  directoryDialogVisible.value = true
  selectedDirectory.value = ''
  selectedDialogDirectory.value = null
  
  // 初始化对话框为当前路径
  try {
    const roots = await fileBrowserApi.getSystemRoots()
    if (roots && roots.length > 0) {
      let targetPath = currentPath.value || roots[0]
      
      await loadDialogFileList(targetPath)
    }
  } catch (error) {
    console.error('初始化目录对话框失败:', error)
    ElMessage.error('初始化目录对话框失败')
  }
}

const showCreateDirectoryDialog = () => {
  if (!currentPath.value) {
    ElMessage.warning('请先选择一个目录')
    return
  }
  newDirForm.name = ''
  createDirDialogVisible.value = true
}



const confirmCreateDirectory = async () => {
  if (!newDirForm.name.trim()) {
    ElMessage.warning('请输入目录名称')
    return
  }
  
  try {
    await fileBrowserApi.createDirectory(currentPath.value, newDirForm.name.trim())
    ElMessage.success('目录创建成功')
    createDirDialogVisible.value = false
    refreshFileList()
  } catch (error) {
    console.error('创建目录失败:', error)
    // 错误信息已由axios拦截器统一处理
  }
}

const handleDirectorySelect = (data) => {
  selectedDirectory.value = data.path
}

const confirmDirectorySelect = () => {
  if (selectedDialogDirectory.value) {
    selectedDirectory.value = selectedDialogDirectory.value.path
    loadFileList(selectedDialogDirectory.value.path)
    directoryDialogVisible.value = false
  }
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

const handleFileClick = (file) => {
  if (file.isDirectory) {
    //ElMessage.success(`进入目录: ${file.name}`)
    navigateToPath(file.path)
    return
  }
  
  if (file.isMediaFile) {
    toggleFileSelection(file)
  }
}

const handleFileDoubleClick = (file) => {
  if (file.isDirectory) {
    //ElMessage.success(`进入目录: ${file.name}`)
    navigateToPath(file.path)
  }
}

const toggleFileSelection = (file, checked) => {
  if (!file.isMediaFile) return
  
  const index = selectedFiles.value.findIndex(f => f.path === file.path)
  
  if (checked === undefined) {
    // Toggle
    if (index === -1) {
      selectedFiles.value.push(file)
    } else {
      selectedFiles.value.splice(index, 1)
    }
  } else {
    // Set specific value
    if (checked && index === -1) {
      selectedFiles.value.push(file)
    } else if (!checked && index !== -1) {
      selectedFiles.value.splice(index, 1)
    }
  }
  
  // 自动同步到conversionStore
  updateConversionStore()
}

const updateConversionStore = () => {
  const filesToAdd = selectedFiles.value.map(file => file.path)
  conversionStore.setSourceFiles(filesToAdd)
}

const selectAllFiles = () => {
  const mediaFiles = files.value.filter(file => !file.isDirectory && file.isMediaFile)
  selectedFiles.value = [...mediaFiles]
  updateConversionStore()
}

const clearSelection = () => {
  selectedFiles.value = []
  updateConversionStore()
}

const refreshFileList = () => {
  loadFileList(currentPath.value)
}

const navigateToPath = (path) => {
  loadFileList(path)
}

const navigateToParent = () => {
  if (!hasParentDirectory.value) return
  
  const path = currentPath.value
  
  // 如果是根目录的子目录（如 /bin, /usr, /lib 等），直接返回根目录
  if (path && path.startsWith('/') && !path.match(/^[A-Za-z]:[\\\/]?$/)) {
    // 检查是否只有一段路径（如 /bin, /lib 等）
    const parts = path.split('/').filter(Boolean)
    if (parts.length === 1) {
      navigateToPath('/')
      return
    }
    
    // 多段路径，返回到上一级
    const parentParts = parts.slice(0, -1)
    const parentPath = '/' + parentParts.join('/')
    navigateToPath(parentPath)
    return
  }
  
  // Windows系统处理
  if (path && (path.includes('\\') || path.includes(':'))) {
    const parts = path.split(/[\\/]/).filter(Boolean)
    if (parts.length > 1) {
      const parentParts = parts.slice(0, -1)
      const parentPath = parentParts.join('/') || (path.match(/^[A-Za-z]:/) ? path.match(/^[A-Za-z]:/)[0] + '\\' : '')
      navigateToPath(parentPath)
    } else {
      // 单段路径，可能是驱动器根目录的子目录
      const match = path.match(/^[A-Za-z]:/)
      if (match) {
        navigateToPath(match[0] + '\\')
      }
    }
  }
}

const getPathToSegment = (index) => {
  const segments = pathSegments.value
  const path = currentPath.value
  
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



const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 键盘事件处理
const handleKeyNavigation = (event) => {
  const key = event.key
  if (key === 'Enter') {
    // Enter键进入选中的第一个目录
    const selectedDirs = files.value.filter(file => file.isDirectory)
    if (selectedDirs.length > 0) {
      navigateToPath(selectedDirs[0].path)
    }
  } else if (key === 'Backspace') {
    // Backspace键返回上级目录（适用于所有系统）
    if (hasParentDirectory.value) {
      navigateToParent()
    }
  } else if (key === 'F5') {
    // F5刷新
    event.preventDefault()
    refreshFileList()
  } else if (event.altKey && key === 'ArrowUp') {
    // Alt+Up 返回上级目录（Linux常用快捷键）
    event.preventDefault()
    if (hasParentDirectory.value) {
      navigateToParent()
    }
  }
}

// 生命周期
onMounted(async () => {
  try {
    // 先获取系统根目录，然后加载第一个根目录
    const roots = await fileBrowserApi.getSystemRoots()
    
    if (roots && roots.length > 0) {
      // 优先选择用户主目录（如果存在）
      let targetPath = roots[0]
      const isWindows = navigator.platform.toLowerCase().includes('win')
      
      if (!isWindows) {
        // Linux/macOS: 优先选择用户主目录或根目录
        const userHome = roots.find(root => root.includes('Users') || root.includes('home') || root.includes('user'))
        if (userHome) {
          targetPath = userHome
        } else if (roots.some(root => root === '/')) {
          targetPath = '/'
        }
      } else {
        // Windows: 优先选择C盘用户目录
        const cDrive = roots.find(root => root.startsWith('C:'))
        if (cDrive) {
          targetPath = cDrive
        }
      }
      
      await loadFileList(targetPath)
    } else {
      ElMessage.warning('未找到可访问的目录')
    }
  } catch (error) {
    console.error('初始化文件浏览器失败:', error)
    ElMessage.error('初始化文件浏览器失败')
  }
  
  // 添加键盘事件监听
  window.addEventListener('keydown', handleKeyNavigation)
  
  // 监听conversionStore的变化，同步本地选择状态
  watch(
    () => conversionStore.sourceFiles,
    (newSourceFiles) => {
      // 如果store被清空但本地还有选择，清空本地选择
      if (newSourceFiles.length === 0 && selectedFiles.value.length > 0) {
        selectedFiles.value = []
      }
      // 如果store中有文件但本地为空，从store恢复选择状态
      else if (newSourceFiles.length > 0 && selectedFiles.value.length === 0) {
        // 这里可以添加恢复逻辑，但为了简化，我们清空选择让用户重新选择
        selectedFiles.value = []
      }
    },
    { deep: true }
  )
})
</script>

<style scoped>
.file-browser {
  height: 100%;
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

.breadcrumb {
  margin: 16px 0;
  padding: 12px;
  background: var(--border-lighter);
  border-radius: 4px;
}

.breadcrumb-item {
  cursor: pointer;
}

.breadcrumb-item:hover {
  color: var(--primary-color);
}

.directory-stats {
  margin: 8px 0;
  padding: 8px 12px;
  background: var(--background-base);
  border-radius: 4px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 8px 0;
}



.view-toggle {
  margin-left: auto;
}

.file-list {
  max-height: 500px;
  overflow: auto;
  overflow-x: hidden;
  border: 1px solid var(--border-light);
  border-radius: 4px;
}

.list-view {
  /* 列表视图样式已在main.css中定义 */
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

.grid-checkbox {
  position: absolute;
  top: 8px;
  right: 8px;
}

.directory-selector {
  max-height: 400px;
  overflow-y: auto;
}

.mobile .card-header {
  flex-direction: column;
  gap: 12px;
}

.mobile .header-actions {
  width: 100%;
  justify-content: space-between;
}

.mobile .toolbar {
  flex-direction: column;
  gap: 12px;
}

.mobile .view-toggle {
  margin-left: 0;
}

.mobile .grid-view {
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 8px;
  padding: 8px;
}

.mobile .grid-item {
  padding: 8px;
}

.mobile .grid-icon {
  font-size: 24px;
}

.mobile .grid-name {
  font-size: 11px;
}

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

/* 树形选择器样式 */
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
}

.tree-node-label {
  font-size: 14px;
  color: var(--text-regular);
}

.tree-node:hover .tree-node-label {
  color: var(--primary-color);
}

/* 媒体文件样式 */
.media-file-item {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-left: 3px solid #0ea5e9;
}

.media-file-item:hover {
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
  border-left-color: #0284c7;
}

/* 普通文件样式 */
.regular-file-item {
  background: var(--background-base);
  opacity: 0.7;
}

.regular-file-item:hover {
  opacity: 1;
  background: var(--border-lighter);
}

/* 改进的选择状态样式 */
.file-item.selected {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  border-left: 4px solid var(--primary-color);
  border-radius: 0 8px 8px 0;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
  position: relative;
}

.file-item.selected::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, var(--primary-color) 0%, #409eff 100%);
}

.directory-item.selected {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  border-left: 4px solid #2563eb;
  box-shadow: 0 2px 12px rgba(37, 99, 235, 0.2);
}

.media-file-item.selected {
  background: linear-gradient(135deg, #dbeafe 0%, #93c5fd 100%);
  border-left: 4px solid #0369a1;
  box-shadow: 0 2px 12px rgba(3, 105, 161, 0.2);
}

/* 增强网格视图的选择效果 */
.grid-item.selected {
  background: linear-gradient(135deg, #dbeafe 0%, #93c5fd 100%);
  border: 3px solid var(--primary-color);
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.3);
  transform: translateY(-4px) scale(1.02);
}

.grid-item.selected .grid-icon {
  color: var(--primary-color) !important;
  transform: scale(1.1);
}

/* 文件操作区域样式 */
.file-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.file-checkbox {
  margin-left: 4px;
  transform: scale(1.2);
}

.file-checkbox:hover {
  transform: scale(1.3);
}

/* 增强复选框的视觉反馈 */
.file-checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.file-checkbox :deep(.el-checkbox__inner:hover) {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

/* 让可选择的文件项更明显 */
.file-item.selectable {
  position: relative;
}

.file-item.selectable:hover .file-checkbox {
  opacity: 1;
  transform: scale(1.3);
}

/* 文件名在选择状态下的样式 */
.file-item.selected .file-name {
  color: var(--primary-color);
  font-weight: 600;
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

.file-meta {
  font-size: 12px;
  color: var(--text-secondary);
}

.directory-info {
  font-size: 12px;
  color: var(--text-secondary);
  font-style: italic;
}

/* 转换队列操作区域样式 */
.conversion-action-area {
  margin-top: 20px;
  animation: slideInUp 0.3s ease-out;
}

/* PC模式：显示在组件内部，为转换设置上方做准备 */
.conversion-action-area:not(.mobile-mode) {
  margin-bottom: 20px;
  position: relative;
  width: 100%;
}

/* 移动模式：保持在底部 */
.conversion-action-area.mobile-mode {
  margin-top: 20px;
  position: relative;
  width: 100%;
}

.conversion-action-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border: 2px solid var(--primary-color);
  border-radius: 12px;
  overflow: hidden;
}

.conversion-action-card:hover {
  box-shadow: 0 8px 25px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.conversion-action-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 24px;
  flex-wrap: wrap;
}

.selected-files-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 200px;
}

.files-icon {
  font-size: 48px;
  color: var(--primary-color);
  opacity: 0.8;
}

.files-text h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.files-text p {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.add-to-queue-btn {
  background: linear-gradient(135deg, var(--primary-color) 0%, #409eff 100%);
  border: none;
  font-size: 16px;
  font-weight: 600;
  padding: 12px 24px;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.3);
  transition: all 0.3s ease;
}

.add-to-queue-btn:hover {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4);
}

.add-to-queue-btn:active {
  transform: translateY(0);
}

.clear-selection-btn {
  background: transparent;
  border: 2px solid var(--border-color);
  color: var(--text-secondary);
  font-size: 14px;
  padding: 10px 20px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.clear-selection-btn:hover {
  border-color: var(--danger-color);
  color: var(--danger-color);
  background: rgba(245, 108, 108, 0.05);
}

.btn-icon {
  margin-right: 8px;
  font-size: 18px;
}

/* 动画效果 */
@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .conversion-action-content {
    flex-direction: column;
    align-items: stretch;
    text-align: center;
    padding: 16px 20px;
  }
  
  .selected-files-info {
    justify-content: center;
    text-align: center;
    flex-direction: column;
    gap: 12px;
    margin-bottom: 16px;
  }
  
  .files-icon {
    font-size: 40px;
  }
  
  .files-text h3 {
    font-size: 18px;
  }
  
  .files-text p {
    font-size: 14px;
  }
  
  .add-to-queue-btn,
  .clear-selection-btn {
    width: 100%;
    margin-top: 8px;
    padding: 14px 20px;
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .conversion-action-content {
    padding: 14px 16px;
  }
  
  .files-text h3 {
    font-size: 16px;
  }
  
  .files-text p {
    font-size: 13px;
  }
  
  .add-to-queue-btn,
  .clear-selection-btn {
    font-size: 15px;
    padding: 12px 16px;
  }
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
</style>