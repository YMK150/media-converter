import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fileBrowserApi } from '@/utils/api'

export const useFileStore = defineStore('file', () => {
  // 状态
  const currentDirectory = ref('')
  const selectedFiles = ref([])
  const supportedFormats = ref({
    audio: [],
    video: []
  })
  
  // 方法
  const setCurrentDirectory = (path) => {
    currentDirectory.value = path
  }
  
  const setSelectedFiles = (files) => {
    selectedFiles.value = files
  }
  
  const addSelectedFile = (file) => {
    const exists = selectedFiles.value.some(f => f.path === file.path)
    if (!exists) {
      selectedFiles.value.push(file)
    }
  }
  
  const removeSelectedFile = (filePath) => {
    const index = selectedFiles.value.findIndex(f => f.path === filePath)
    if (index > -1) {
      selectedFiles.value.splice(index, 1)
    }
  }
  
  const clearSelectedFiles = () => {
    selectedFiles.value = []
  }
  
  const loadSupportedFormats = async () => {
    try {
      const response = await fileBrowserApi.getSupportedFormats()
      if (response.audioFormats && response.videoFormats) {
        supportedFormats.value = {
          audio: response.audioFormats,
          video: response.videoFormats
        }
      }
    } catch (error) {
      console.error('加载支持格式失败:', error)
    }
  }
  
  const getTargetFormats = async (sourceFormat) => {
    try {
      const response = await fileBrowserApi.getSupportedFormats(sourceFormat)
      return response.targetFormats || []
    } catch (error) {
      console.error('获取目标格式失败:', error)
      return []
    }
  }
  
  return {
    // 状态
    currentDirectory,
    selectedFiles,
    supportedFormats,
    
    // 方法
    setCurrentDirectory,
    setSelectedFiles,
    addSelectedFile,
    removeSelectedFile,
    clearSelectedFiles,
    loadSupportedFormats,
    getTargetFormats
  }
})