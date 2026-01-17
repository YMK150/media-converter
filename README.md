# 飞牛格式转换器 (Media Converter)

一个在飞牛NAS运行，基于Java Spring Boot + Vue3 + FFMPEG的本地音视频格式转换器应用，提供高效的文件格式转换服务。

## 📋 项目概述

格式转换器是一个功能完整的本地音视频转换工具，支持广泛的格式转换，配备直观的文件浏览器、转换配置和任务管理界面。

## ✨ 功能特性

### 🎬 核心转换功能
- **多格式支持**：支持主流音视频格式的相互转换
- **批量处理**：支持多文件同时启动转换，提高工作效率
- **自定义参数**：可调节分辨率、比特率、帧率等转换参数

### 🖥️ 用户界面
- **文件浏览器**：内置本地文件系统浏览器，支持文件预览和选择
- **任务管理**：完整的任务队列管理，支持暂停、取消、重试操作
- **响应式设计**：完美适配桌面端和移动端设备
- **现代UI**：基于Element Plus的美观界面设计

### 🔧 系统特性
- **本地部署**：完全在本地运行，不向云端传输任何数据，保证文件安全和隐私
- **性能优化**：智能资源管理，高效利用系统资源

## 🛠️ 技术架构

### 后端技术栈
- **Java 17** - 核心开发语言
- **Spring Boot 3.2** - 应用框架
- **SQLite** - 轻量级数据库
- **WebSocket** - 实时通信协议
- **FFMPEG** - 音视频处理引擎
- **Maven** - 项目构建工具

### 前端技术栈
- **Vue 3** - 前端框架
- **Element Plus** - UI组件库
- **Pinia** - 状态管理
- **Axios** - HTTP客户端
- **Vite** - 构建工具
- **Vue Router** - 路由管理

## 🚀 快速开始

### 系统要求
- **Java**: JDK 17+
- **Node.js**: 16.0+
- **Maven**: 3.6+
- **FFMPEG**: 6.0+

### 安装与启动

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd media-converter
   ```

2. **启动后端服务**
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```
   后端服务将在 `http://localhost:8080` 启动

3. **启动前端服务**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   前端服务将在 `http://localhost:3000` 启动

4. **访问应用**
   打开浏览器访问 `http://localhost:3000`

## 📖 使用指南

### 基本操作流程

1. **文件选择**
   - 使用左侧文件浏览器导航到目标目录
   - 选择要转换的音视频文件（支持多选）
   - 文件将自动添加到转换列表

2. **转换设置**
   - 选择目标格式（视频：MP4、AVI、MOV等；音频：MP3、WAV、AAC等）
   - 设置输出目录（默认为系统指定目录）
   - 配置高级参数（分辨率、比特率、帧率）

3. **开始转换**
   - 点击"开始转换"按钮
   - 系统将创建转换任务并加入队列
   - 可在任务列表中查看实时进度

4. **查看结果**
   - 转换完成后，任务状态将更新为"已完成"
   - 可直接打开输出文件所在目录
   - 支持任务重试和删除操作

### 高级功能

#### 视频转换设置
- **分辨率预设**: 360p, 480p, 720p, 1080p, 2K, 4K
- **自定义分辨率**: 支持自定义宽度和高度
- **视频比特率**: 500kbps - 15Mbps可选
- **帧率设置**: 15fps - 60fps可选

#### 音频转换设置
- **音频比特率**: 64kbps - 320kbps可选
- **格式质量**: 支持有损和无损压缩格式


## 📦 支持格式

### 视频格式

 | 文件后缀 | 格式 |
 |------|----|
 | MP4  |   通用视频格式 |
 | AVI  |   通用视频格式 |
 | MP4  |  通用视频格式 |
 | AVI  |  高质量视频 |
 | MOV  |  Apple格式|
 | WMV  |  Windows格 |
 | MKV  |  多轨道视频 |
 | FLV  |  Flash视频|


### 音频格式

| 文件后缀 | 格式 |
 |------|-|
| MP3  | 通用音频 |
| WAV  | 无损音频  |
| AAC  | 高质量音频 |
| FLAC | 无损压缩  |
| M4A  | Apple音频 |
| WMA  | Windows音频 |

## 😄 构建jar包或飞牛包

### 构建jar包
```bash
# 使用提供的构建脚本
./jar-build.bat
```

### 构建飞牛包
```bash
# 使用提供的构建脚本
./fn-build.bat
```

## 🏗️ 项目结构

```
media-converter/
├── backend/                      # 后端服务
│   ├── src/main/java/
│   │   └── com/mediaconverter/
│   │       ├── controller/        # REST控制器
│   │       ├── service/          # 业务逻辑层
│   │       ├── repository/       # 数据访问层
│   │       ├── entity/           # 数据实体
│   │       ├── dto/              # 数据传输对象
│   │       ├── config/           # 配置类
│   │       └── websocket/        # WebSocket处理
│   ├── src/main/resources/
│   │   ├── application.yml       # 应用配置
│   │   └── static/              # 静态资源
│   └── pom.xml                  # Maven配置
├── frontend/                     # 前端应用
│   ├── src/
│   │   ├── components/           # Vue组件
│   │   ├── views/               # 页面视图
│   │   ├── stores/              # Pinia状态管理
│   │   ├── utils/               # 工具函数
│   │   ├── router/              # 路由配置
│   │   ├── api/                 # API接口
│   │   └── assets/              # 静态资源
│   ├── public/                  # 公共资源
│   ├── index.html               # 入口HTML
│   ├── package.json             # NPM配置
│   └── vite.config.js           # Vite配置
├── fn-media-converter/          # 飞牛配置
├── docker-compose.yaml          # Docker编排文件
├── Dockerfile                  # Docker镜像构建
└── README.md                   # 项目文档
```
## 📞 技术支持

- **问题反馈**: 请扫码关注我的公众号，直接给我留言

![](\frontend\public\qrcode.jpg)

**注意**: 本项目仅用于合法的格式文件转换，请遵守当地法律法规使用，非法用途造成的后果与作者无关。