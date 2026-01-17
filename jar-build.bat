@echo off
REM Jar包 构建脚本 (Windows版本)

echo 开始构建 前端包...
cd frontend
call npm install
call npm run build-and-deploy
cd ..

echo 开始构建 后端包...
cd backend
call mvn clean package
cd ..

pause