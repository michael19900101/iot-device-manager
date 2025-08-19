# 批量IoT设备模拟器使用指南

## 📋 概述

批量IoT设备模拟器是一个Java程序，可以同时启动多个IoT设备模拟器客户端，用于测试物联网系统的并发性能和负载能力。

## 🚀 功能特性

- **批量启动**: 同时启动1-1000个IoT设备
- **并发管理**: 使用线程池管理所有设备连接
- **实时监控**: 实时显示设备启动状态和运行状态
- **交互控制**: 支持运行时查看状态、停止设备等操作
- **单独控制**: 支持单独停止指定设备（disconnect|设备ID）
- **重新连接**: 支持单独重新连接指定设备（connect|设备ID）
- **设备列表**: 支持查看所有设备列表
- **优雅退出**: 支持优雅停止所有设备并清理资源
- **输入隔离**: 批量模式下的设备不会占用System.in，确保主程序能正常接收用户输入

## 📁 文件结构

```
src/main/java/com/michael/iot/test/
├── IoTDeviceSimulator.java          # 单个设备模拟器
└── IoTDeviceBatchSimulator.java     # 批量设备模拟器
```

## 🌍 国际化支持

### 字符编码
- **UTF-8支持**: 所有Socket通信都使用UTF-8字符编码
- **英文界面**: 命令服务器界面使用英文，避免控制台中文输入问题
- **设备命名**: 设备名称使用中文格式（如: 传感器-001, 传感器-002）

### 兼容性
- **跨平台**: 支持Windows、macOS、Linux系统
- **终端兼容**: 兼容各种终端和SSH客户端
- **网络工具**: 支持telnet、netcat等标准网络工具

## 🛠️ 使用方法

### 方法一：使用启动脚本（推荐）

```bash
# 编译项目
cd backend && mvn compile

# 启动100个设备（默认）
cd backend && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     com.michael.iot.test.IoTDeviceBatchSimulator localhost 8888 100

# 启动50个设备
cd backend && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     com.michael.iot.test.IoTDeviceBatchSimulator localhost 8888 50

# 连接到指定服务器
cd backend && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     com.michael.iot.test.IoTDeviceBatchSimulator 192.168.1.100 8888 100
```

### 方法二：直接运行Java程序

```bash
# 编译项目
cd backend && mvn compile

# 启动100个设备
cd backend && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     com.michael.iot.test.IoTDeviceBatchSimulator \
     localhost 8888 100
```

### 方法三：使用Socket命令服务器

程序启动后会在端口4567启动一个命令服务器，您可以通过Socket连接发送命令：

```bash
# 启动批量设备模拟器
cd backend && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     com.michael.iot.test.IoTDeviceBatchSimulator localhost 8888 100
```

```bash
# 在另一个终端连接命令服务器
telnet localhost 4567
# 或者
nc localhost 4567
```

## 📊 运行示例

### 启动过程
```
=== 启动 100 个IoT设备模拟器 ===
服务器地址: localhost:8888
设备类型: 传感器

启动设备 1/100: 传感器-001
启动设备 2/100: 传感器-002
启动设备 3/100: 传感器-003
...
启动设备 100/100: 传感器-100

✅ 所有 100 个设备已启动完成！
设备正在运行中...

=== 操作说明 ===
命令服务器地址: localhost:4567
可用命令: status, list, stop, exit, disconnect|设备ID, connect|设备ID

程序将持续运行，等待命令输入...
按 Ctrl+C 退出程序
```

### 运行时操作
```
status              # 查看设备状态
list                # 查看所有设备列表
stop                # 停止所有设备
disconnect|deviceId  # 停止指定设备（如: disconnect|29e5fc6c）
connect|deviceId     # 重新连接指定设备（如: connect|29e5fc6c）
help                 # 显示帮助信息
exit                 # 退出程序
```

#### 操作示例
```
status
=== Device Status ===
Total devices: 100
Started devices: 100
Running devices: 95

list
=== All Device List ===
1. sensor-001
2. sensor-002
3. sensor-003
...

disconnect|29e5fc6c
正在查找要停止的设备，ID: '29e5fc6c'
正在停止设备: 传感器-003 (ID: 29e5fc6c)
✅ 设备 传感器-003 (ID: 29e5fc6c) 已停止

connect|29e5fc6c
正在查找要重新连接的设备，ID: '29e5fc6c'
正在重新连接设备: 传感器-003 (ID: 29e5fc6c)
设备正在重新连接: 29e5fc6c
设备连接成功: 29e5fc6c
✅ 设备 传感器-003 (ID: 29e5fc6c) 已重新连接

help
=== IoT Device Batch Simulator Help ===
Available Commands:
  status - Show device status
  list - Show all device list
  stop - Stop all devices
  exit - Exit program
  disconnect|deviceId - Stop specific device (e.g: disconnect|29e5fc6c)
  connect|deviceId - Reconnect specific device (e.g: connect|29e5fc6c)
  help - Show this help message

stop
Stopping all devices...
✅ All devices stopped
```

## ⚙️ 配置参数

### 命令行参数

| 参数 | 说明 | 默认值 | 范围 |
|------|------|--------|------|
| `serverHost` | 服务器地址 | localhost | 任意有效IP或域名 |
| `serverPort` | 服务器端口 | 8888 | 1-65535 |
| `deviceCount` | 设备数量 | 100 | 1-1000 |

### 性能配置

- **线程池大小**: 等于设备数量
- **启动超时**: 30秒
- **停止超时**: 10秒
- **设备命名**: 传感器-001, 传感器-002, ...

## 🔧 技术实现

### 核心组件

1. **IoTDeviceBatchSimulator**: 主控制类
   - 管理设备列表
   - 控制线程池
   - 处理用户交互
   - 支持单独设备控制
   - 提供设备查询功能

2. **ExecutorService**: 线程池
   - 并发启动所有设备
   - 管理设备生命周期

3. **CountDownLatch**: 同步机制
   - `startLatch`: 等待所有设备启动完成
   - `stopLatch`: 等待所有设备运行完成

### 设备管理流程

```
1. 创建设备列表
   ↓
2. 启动命令服务器（端口4567）
   ↓
3. 提交到线程池
   ↓
4. 并发启动设备
   ↓
5. 等待启动完成
   ↓
6. 进入交互模式
   ↓
7. 处理Socket命令
   ↓
8. 优雅停止所有设备
```

### 命令服务器机制

系统通过Socket命令服务器（端口4567）实现远程控制功能：

1. **命令服务器**: 启动时自动在端口4567启动Socket服务器
2. **多客户端支持**: 支持多个客户端同时连接发送命令
3. **UTF-8编码**: 所有通信使用UTF-8字符编码
4. **命令解析**: 支持规范化命令输入，自动处理全角字符
5. **设备控制**: 支持通过设备ID精确控制单个设备
6. **状态查询**: 实时查询设备状态和列表信息
7. **优雅退出**: 支持优雅停止所有设备和清理资源

## 📈 性能测试

### 测试场景

1. **并发连接测试**: 测试服务器能同时处理多少设备连接
2. **数据吞吐量测试**: 测试大量设备同时发送数据的性能
3. **连接稳定性测试**: 测试长时间运行的稳定性

### 测试命令

```bash
# 小规模测试（10个设备）
cd backend && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     com.michael.iot.test.IoTDeviceBatchSimulator localhost 8888 10

# 中等规模测试（100个设备）
cd backend && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     com.michael.iot.test.IoTDeviceBatchSimulator localhost 8888 100

# 大规模测试（500个设备）
cd backend && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     com.michael.iot.test.IoTDeviceBatchSimulator localhost 8888 500

# 极限测试（1000个设备）
cd backend && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     com.michael.iot.test.IoTDeviceBatchSimulator localhost 8888 1000
```

### 单独设备控制测试

```bash
# 启动测试
cd backend && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     com.michael.iot.test.IoTDeviceBatchSimulator localhost 8888 5
```

```bash
# 测试步骤:
# 1. 启动5个设备
# 2. 连接命令服务器: telnet localhost 4567
# 3. 输入 'list' 查看设备列表
# 4. 输入 'disconnect|设备ID' 停止指定设备
# 5. 输入 'status' 查看状态变化
# 6. 输入 'exit' 退出程序
```

### 设备重新连接测试

```bash
# 启动测试
cd backend && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     com.michael.iot.test.IoTDeviceBatchSimulator localhost 8888 3

# 测试步骤:
# 1. 启动3个设备
# 2. 连接命令服务器: telnet localhost 4567
# 3. 输入 'list' 查看设备列表
# 4. 输入 'status' 查看设备状态
# 5. 输入 'disconnect|设备ID' 停止指定设备
# 6. 输入 'status' 查看设备状态变化
# 7. 输入 'connect|设备ID' 重新连接指定设备
# 8. 输入 'status' 查看设备状态恢复
# 9. 输入 'exit' 退出程序

# 预期结果:
# - 可以成功停止指定设备
# - 可以成功重新连接指定设备
# - 设备状态正确反映连接状态
```



### 心跳功能测试

```bash
# 启动测试
cd backend && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     com.michael.iot.test.IoTDeviceBatchSimulator localhost 8888 2

# 测试步骤:
# 1. 启动2个设备
# 2. 观察控制台输出，应该能看到心跳和数据上报信息
# 3. 连接命令服务器: telnet localhost 4567
# 4. 输入 'disconnect|设备ID' 停止指定设备
# 5. 输入 'connect|设备ID' 重新连接指定设备
# 6. 观察重新连接后是否继续发送心跳
# 7. 等待30-60秒，观察心跳是否正常发送
# 8. 输入 'exit' 退出程序

# 预期结果:
# - 设备启动后能看到心跳发送信息（30秒间隔）
# - 设备能正常上报数据（10秒间隔）
# - 重新连接后心跳功能正常
# - 设备不会被服务器判定为离线
```

### Socket命令服务器测试

```bash
# 启动测试
cd backend && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" \
     com.michael.iot.test.IoTDeviceBatchSimulator localhost 8888 3

# 测试步骤:
# 1. 启动3个设备
# 2. 等待程序启动完成
# 3. 打开新的终端窗口
# 4. 连接命令服务器: telnet localhost 4567
# 5. 测试命令:
#    - status (查看设备状态)
#    - list (查看设备列表)
#    - disconnect|设备ID (停止指定设备)
#    - connect|设备ID (重新连接指定设备)
#    - help (显示帮助信息)
#    - exit (退出命令服务器)
# 6. 观察主程序控制台输出
# 7. 按 Ctrl+C 退出主程序

# 预期结果:
# - 命令服务器正常启动（端口4567）
# - 可以通过Socket连接发送命令
# - 命令执行结果正确返回
# - 支持多客户端同时连接
# - 支持UTF-8字符编码
```



## 🚨 注意事项

### 系统资源

- **内存使用**: 每个设备约占用2-5MB内存
- **网络连接**: 每个设备占用一个TCP连接
- **CPU使用**: 主要消耗在心跳包和数据处理

### 限制条件

- **最大设备数**: 1000个（可调整）
- **启动超时**: 30秒
- **服务器负载**: 需要确保服务器能处理大量并发连接

### 故障处理

1. **启动失败**: 检查服务器是否运行，网络是否正常
2. **连接断开**: 设备会自动重连（如果支持）
3. **内存不足**: 减少设备数量或增加系统内存

## 🔍 故障排除

### 常见问题

1. **编译失败**
   ```bash
   # 清理并重新编译
   cd backend && mvn clean compile
   ```

2. **连接失败**
   ```bash
   # 检查服务器状态
   lsof -i :8888
   
   # 启动服务器
   cd backend && mvn compile && java -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" com.michael.iot.server.IoTApplication 8888 8889
   ```

3. **设备启动超时**
   - 检查网络连接
   - 减少设备数量
   - 增加启动超时时间

### 日志分析

- **启动日志**: 显示每个设备的启动状态
- **错误日志**: 显示连接失败和异常信息
- **状态日志**: 显示设备运行状态

### 日志文件位置
- **服务器日志**: `backend/logs/iot-device-manager.log`
- **实时查看**: `tail -f backend/logs/iot-device-manager.log`

## 📝 更新日志

### v1.0.0
- 初始版本
- 支持批量启动IoT设备
- 支持实时状态监控
- 支持交互式控制

## 🤝 贡献

欢迎提交Issue和Pull Request来改进这个项目！

---

**总结**: 批量IoT设备模拟器提供了强大的并发测试能力，可以轻松模拟大量IoT设备同时连接和通信的场景，是物联网系统性能测试和负载测试的理想工具。
