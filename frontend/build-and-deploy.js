const fs = require('fs');
const path = require('path');

// 配置路径
const frontendDist = path.join(__dirname, 'dist');
const backendResources = path.join(__dirname, '../backend/src/main/resources/static');

console.log('开始构建和部署前端资源...');

// 清理目标目录
if (fs.existsSync(backendResources)) {
    fs.rmSync(backendResources, { recursive: true, force: true });
    console.log('已清理目标目录:', backendResources);
}

// 确保目标目录存在
fs.mkdirSync(backendResources, { recursive: true });

// 检查前端dist目录是否存在
if (!fs.existsSync(frontendDist)) {
    console.error('前端dist目录不存在，请先执行构建命令: npm run build');
    process.exit(1);
}

// 复制文件函数
function copyDir(src, dest) {
    const entries = fs.readdirSync(src, { withFileTypes: true });
    
    for (const entry of entries) {
        const srcPath = path.join(src, entry.name);
        const destPath = path.join(dest, entry.name);
        
        if (entry.isDirectory()) {
            fs.mkdirSync(destPath, { recursive: true });
            copyDir(srcPath, destPath);
        } else {
            fs.copyFileSync(srcPath, destPath);
        }
    }
}

// 复制dist目录内容到static目录
copyDir(frontendDist, backendResources);

console.log('✅ 前端资源已成功部署到后端static目录');
console.log('源目录:', frontendDist);
console.log('目标目录:', backendResources);

// 显示复制的文件统计
function countFiles(dir) {
    let count = 0;
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    
    for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            count += countFiles(fullPath);
        } else {
            count++;
        }
    }
    return count;
}

const fileCount = countFiles(backendResources);
console.log(`总共复制了 ${fileCount} 个文件`);