# IoT设备管理系统

这是一个完整的物联网设备管理系统，基于Netty构建高性能服务器，提供设备连接管理、实时监控、状态跟踪等功能。系统包含后端服务器和Vue3前端界面。


## 🚀 项目状态

✅ **物联网服务器**: 运行正常 (TCP:8888, WebSocket:8889)  
✅ **前端界面**: Vue3 + Element Plus  
✅ **设备模拟器**: 支持多设备并发测试  
  
✅ **日志系统**: 完整的日志记录和监控  

## 📁 项目结构

```
iot-device-manager/
├── backend/                   # 后端Java项目
│   ├── src/main/java/com/michael/iot/
│   │   ├── server/                 # 物联网设备管理系统核心模块
│   │   │   ├── DeviceInfo.java     # 设备信息实体类
│   │   │   ├── DeviceStatus.java   # 设备状态枚举
│   │   │   ├── DeviceManager.java  # 设备管理器
│   │   │   ├── IoTNettyServer.java # 物联网TCP服务器
│   │   │   ├── IoTDeviceHandler.java # 设备处理器
│   │   │   ├── WebSocketServer.java # WebSocket服务器
│   │   │   ├── WebSocketHandler.java # WebSocket处理器
│   │   │   ├── DeviceStatusListener.java # 设备状态监听器接口
│   │   │   ├── DeviceDataListener.java # 设备数据监听器接口
│   │   │   └── IoTApplication.java # 应用启动类
│   │   └── test/                   # 设备模拟器测试模块
│   │       ├── IoTDeviceSimulator.java # 单个设备模拟器
│   │       └── IoTDeviceBatchSimulator.java # 批量设备模拟器
│   ├── logs/                      # 日志文件
│   └── pom.xml                    # Maven配置
├── frontend/                   # Vue3前端界面
│   ├── src/
│   │   ├── App.vue            # 主应用组件
│   │   └── main.js            # 应用入口
│   ├── package.json           # 前端依赖
│   └── vite.config.js         # Vite配置
```

## 🏗️ 后端代码架构

### 核心模块结构

```
com.michael.iot.server/
├── 实体类 (Entity)
│   ├── DeviceInfo.java          # 设备信息实体
│   └── DeviceStatus.java        # 设备状态枚举
├── 管理器 (Manager)
│   └── DeviceManager.java       # 设备管理器
├── 服务器 (Server)
│   ├── IoTNettyServer.java      # TCP服务器
│   └── WebSocketServer.java     # WebSocket服务器
├── 处理器 (Handler)
│   ├── IoTDeviceHandler.java    # 设备消息处理器
│   └── WebSocketHandler.java    # WebSocket消息处理器
├── 监听器 (Listener)
│   ├── DeviceStatusListener.java # 设备状态监听器
│   └── DeviceDataListener.java   # 设备数据监听器
└── 应用类 (Application)
    └── IoTApplication.java      # 应用启动类
```

### 模块职责说明

#### 🔧 核心组件
- **DeviceInfo**: 设备信息实体类，包含设备ID、名称、类型、状态等属性
- **DeviceStatus**: 设备状态枚举，定义ONLINE、OFFLINE等状态
- **DeviceManager**: 设备管理器，负责设备注册、状态管理、心跳检测等核心业务逻辑

#### 🌐 网络通信
- **IoTNettyServer**: 基于Netty的TCP服务器，处理物联网设备连接
- **IoTDeviceHandler**: 设备消息处理器，解析和处理设备发送的注册、心跳、数据消息
- **WebSocketServer**: WebSocket服务器，向前端推送实时数据
- **WebSocketHandler**: WebSocket消息处理器，处理前端请求

#### 📡 事件监听
- **DeviceStatusListener**: 设备状态变更监听器接口
- **DeviceDataListener**: 设备数据更新监听器接口

#### 🚀 应用启动
- **IoTApplication**: 应用启动类，协调各组件启动和停止

#### 🧪 测试工具
- **IoTDeviceSimulator**: 单个设备模拟器，用于测试单个设备连接
- **IoTDeviceBatchSimulator**: 批量设备模拟器，用于压力测试和并发测试

## 🌟 物联网设备管理系统

### 功能特性

- **🔄 设备连接管理**: 支持物联网设备通过TCP连接，自动管理连接生命周期
- **📊 实时状态监控**: 实时显示设备在线/离线状态，支持状态筛选
- **🔍 设备筛选**: 支持按状态、类型、名称等多维度筛选设备
- **🔔 实时通知**: 设备上线下线实时通知，WebSocket推送
- **💓 心跳检测**: 自动检测设备心跳，超时自动下线（60秒超时）
- **📈 数据上报**: 支持设备数据上报和接收，实时数据展示
- **📱 响应式界面**: 基于Vue3和Element Plus的现代化界面
- **📝 完整日志**: 详细的设备操作日志记录

### 🚀 快速开始

#### 1. 环境准备

##### 后端环境
- Java 11+
- Maven 3.6+

##### 前端环境
- Node.js 16+
- npm 8+

#### 2. 启动后端服务器

```bash
# 编译项目
cd backend 

mvn clean compile

# 启动物联网服务器（TCP端口8888，WebSocket端口8889）
mvn compile && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" com.michael.iot.server.IoTApplication 8888 8889
```
```

**启动成功标志**:
- TCP服务器监听端口8888
- WebSocket服务器监听端口8889
- 日志显示"物联网TCP服务器启动成功"和"WebSocket服务器启动成功"

#### 3. 启动前端界面

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端界面将在 http://localhost:3000 启动。

#### 3. 测试设备连接

使用设备模拟器测试：

```bash
# 编译并运行单个设备模拟器
cd backend && mvn compile && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" com.michael.iot.test.IoTDeviceSimulator localhost 8888 "温度传感器" "传感器"
```


```

### 📡 设备通信协议

#### 设备注册
```
REGISTER|设备ID|设备名称|设备类型
```
示例: `REGISTER|fb0c4407|温度传感器1|传感器1`

#### 心跳消息
```
HEARTBEAT|设备ID
```
示例: `HEARTBEAT|fb0c4407`

#### 数据上报
```
DATA|数据内容
```
示例: `DATA|{"temperature":28.64,"humidity":48.41}`

### 📄 分页功能

设备列表支持分页显示，提供更好的用户体验：

#### 分页特性
- **默认设置**: 每页显示10条记录
- **页面大小**: 支持10、20、50、100条记录/页
- **智能导航**: 支持上一页、下一页、跳转到指定页面
- **总数显示**: 显示筛选后的设备总数
- **自动重置**: 筛选或重置时自动回到第一页

#### 分页组件功能
- **总数显示**: 显示当前筛选条件下的设备总数
- **页面大小选择器**: 可选择每页显示的记录数
- **分页导航**: 上一页、下一页按钮
- **页码显示**: 显示当前页码和总页数
- **跳转功能**: 可直接跳转到指定页面

### 🎨 前端功能

- **📊 设备统计**: 显示设备总数、在线数量、离线数量
- **🔍 设备筛选**: 支持按状态、类型、名称筛选
- **📄 分页显示**: 设备列表分页显示，支持10/20/50/100条记录每页
- **⚡ 实时更新**: WebSocket实时接收设备状态变更
- **📋 设备详情**: 查看设备详细信息（ID、名称、类型、IP、连接时间等）
- **🔔 状态通知**: 设备上线下线实时通知
- **📈 数据展示**: 实时显示设备上报的传感器数据

### 🛠️ 技术栈

#### 后端
- **Netty 4.1.94**: 高性能网络框架
- **Java 11**: 主要开发语言
- **Maven**: 项目构建工具
- **SLF4J + Logback**: 日志框架
- **WebSocket**: 实时通信协议

#### 前端
- **Vue 3**: 现代化前端框架
- **Element Plus**: 企业级UI组件库
- **WebSocket**: 实时通信
- **Vite**: 快速构建工具



## ⚙️ 配置说明

### 端口配置
- TCP服务器默认端口: 8888
- WebSocket服务器默认端口: 8889
- 前端开发服务器端口: 3000

### 心跳配置
- 心跳超时时间: 60秒
- 心跳发送间隔: 30秒
- 数据上报间隔: 60秒

### 日志配置
- 日志文件: `backend/logs/iot-device-manager.log`
- 日志级别: INFO
- 日志格式: 时间戳 + 线程 + 级别 + 类名 + 消息



## 🔧 开发说明

### 添加新设备类型
1. 在 `DeviceInfo.java` 中添加设备类型
2. 在前端 `App.vue` 中更新设备类型选项
3. 在设备模拟器中添加相应的模拟逻辑

### 扩展功能
- 设备命令下发
- 设备数据存储（数据库集成）
- 设备分组管理
- 告警规则配置
- 历史数据查询
- 设备固件升级
- 用户权限管理

## 🚨 故障排除

### 常见问题

1. **端口被占用**
   ```bash
   # 查看端口占用
   lsof -i :8888 -i :8889
   
   # 修改启动脚本中的端口号
   # 或者停止占用端口的进程
   ```

2. **WebSocket连接失败**
   - 检查WebSocket服务器是否启动
   - 检查防火墙设置
   - 确认端口8889是否开放

3. **设备连接失败**
   - 检查TCP服务器是否启动
   - 确认端口8888是否开放
   - 检查网络连接

4. **前端无法访问**
   - 确认前端服务器是否启动
   - 检查端口3000是否被占用
   - 查看浏览器控制台错误信息

5. **依赖项缺失**
   ```bash
# 重新编译并下载依赖
cd backend && mvn clean compile
   
   # 检查类路径设置
cd backend && mvn dependency:build-classpath
   ```

### 日志查看
```bash
# 查看服务器日志
tail -f backend/logs/iot-device-manager.log



# 查看前端日志
cd frontend && npm run dev
```

### 系统监控
```bash
# 查看进程状态
ps aux | grep java

# 查看端口监听
netstat -an | grep 8888
netstat -an | grep 8889

# 查看内存使用
jstat -gc <pid>
```

## 📈 项目亮点

- **高性能**: 基于Netty的高并发处理能力
- **实时性**: WebSocket实时通信，毫秒级响应
- **可扩展**: 模块化设计，易于扩展新功能
- **易用性**: 完整的启动脚本和测试工具
- **监控性**: 详细的日志记录和性能监控
- **现代化**: Vue3 + Element Plus的现代化界面

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 联系方式

如有问题或建议，请通过以下方式联系：
- 提交 Issue
- 发送邮件
- 项目讨论区 