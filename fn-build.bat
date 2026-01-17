@echo off
REM 飞牛构建脚本 (Windows版本)

echo 开始构建 前端包...
cd frontend
call npm install
call npm run build-and-deploy
cd ..

echo 开始构建 后端包...
cd backend
call mvn clean package
cd ..

echo 开始构建 飞牛包...
copy backend\target\media-converter-1.0.0.jar fn-media-converter\app\server
cd fn-media-converter
wsl fnpack build

pause