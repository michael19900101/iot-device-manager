#!/bin/bash

echo "=== 启动物联网设备管理前端界面 ==="

# 检查Node.js环境
if ! command -v node &> /dev/null; then
    echo "错误: 未找到Node.js环境"
    echo "请先安装Node.js: https://nodejs.org/"
    exit 1
fi

# 检查npm环境
if ! command -v npm &> /dev/null; then
    echo "错误: 未找到npm环境"
    exit 1
fi

echo "1. 安装依赖..."
npm install

if [ $? -ne 0 ]; then
    echo "依赖安装失败"
    exit 1
fi

echo "2. 启动开发服务器..."
echo "前端界面将在 http://localhost:3000 启动"
echo "请确保后端服务器已启动（端口8888和8889）"
echo ""
echo "按 Ctrl+C 停止服务器"
echo ""

npm run dev






