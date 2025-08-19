package com.michael.iot.test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * IoT设备批量模拟器
 * 可以同时启动多个IoT设备模拟器客户端
 * 通过Socket服务器(端口4567)监听用户输入
 */
public class IoTDeviceBatchSimulator {
    
    private final String serverHost;
    private final int serverPort;
    private final int deviceCount;
    private final List<IoTDeviceSimulator> devices;
    private final ExecutorService executorService;
    private final CountDownLatch startLatch;
    private final CountDownLatch stopLatch;
    private ServerSocket commandServer;
    private boolean commandServerRunning = false;
    
    /**
     * 构造函数
     * @param serverHost 服务器地址
     * @param serverPort 服务器端口
     * @param deviceCount 设备数量
     */
    public IoTDeviceBatchSimulator(String serverHost, int serverPort, int deviceCount) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.deviceCount = deviceCount;
        this.devices = new ArrayList<>();
        this.executorService = Executors.newFixedThreadPool(deviceCount + 5); // 增加线程池大小以支持命令服务器
        this.startLatch = new CountDownLatch(deviceCount);
        this.stopLatch = new CountDownLatch(deviceCount);
    }
    
    /**
     * 启动命令服务器
     */
    private void startCommandServer() {
        try {
            commandServer = new ServerSocket(4567);
            commandServerRunning = true;
            System.out.println("✅ 命令服务器已启动，监听端口: 4567");
            System.out.println("   您可以使用以下方式发送命令:");
            System.out.println("   1. telnet localhost 4567");
            System.out.println("   2. nc localhost 4567");
            System.out.println("   3. 其他Socket客户端工具");
            System.out.println();
            
            // 启动命令处理线程
            executorService.submit(() -> {
                while (commandServerRunning && !commandServer.isClosed()) {
                    try {
                        Socket clientSocket = commandServer.accept();
                        System.out.println("📡 客户端已连接: " + clientSocket.getInetAddress().getHostAddress());
                        
                        // 为每个客户端创建独立的处理线程
                        executorService.submit(() -> handleClientCommand(clientSocket));
                    } catch (IOException e) {
                        if (commandServerRunning) {
                            System.err.println("接受客户端连接失败: " + e.getMessage());
                        }
                    }
                }
            });
            
        } catch (IOException e) {
            System.err.println("启动命令服务器失败: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * 处理客户端命令
     */
    private void handleClientCommand(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true)) {
            
            writer.println("=== IoT设备批量模拟器命令服务器 ===");
            writer.println("可用命令:");
            writer.println("  status - 显示设备状态");
            writer.println("  list - 显示所有设备列表");
            writer.println("  stop - 停止所有设备");
            writer.println("  exit - 退出程序");
            writer.println("  disconnect|deviceId - 停止指定设备 (例如: disconnect|29e5fc6c)");
            writer.println("  connect|deviceId - 重新连接指定设备 (例如: connect|29e5fc6c)");
            writer.println("  help - 显示帮助信息");
            writer.println("请输入命令: ");
            
            String input;
            while ((input = reader.readLine()) != null) {
                input = normalizeCommandInput(input);
                if (input.isEmpty()) {
                    writer.println("请输入命令: ");
                    continue;
                }
                
                System.out.println("收到命令: " + input + " (来自: " + clientSocket.getInetAddress().getHostAddress() + ")");
                
                // 处理命令
                String response = processCommand(input);
                System.out.println("命令响应: " + response);
                writer.println(response);
                
                // 如果是退出命令，关闭客户端连接
                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("stop")) {
                    break;
                }
                
                writer.println("请输入命令: ");
            }
            
        } catch (IOException e) {
            System.err.println("处理客户端命令失败: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("客户端连接已关闭: " + clientSocket.getInetAddress().getHostAddress());
            } catch (IOException e) {
                System.err.println("关闭客户端连接失败: " + e.getMessage());
            }
        }
    }

    /**
     * 规范化命令输入：
     * - 去除首尾空白
     * - 将全角竖线（｜）替换为半角竖线（|）
     * - 去除竖线两侧多余空白
     */
    private String normalizeCommandInput(String raw) {
        if (raw == null) return "";
        String s = raw.trim()
                .replace('\uFF5C', '|'); // 全角竖线 → 半角
        // 去除竖线两侧空白，例如 "disconnect | 29e5" → "disconnect|29e5"
        s = s.replaceAll("\\s*\\|\\s*", "|");
        return s;
    }
    
    /**
     * 处理命令并返回响应
     */
    private String processCommand(String input) {
        System.out.println("正在处理命令: '" + input + "'");
        StringBuilder response = new StringBuilder();
        
        // 检查是否是断开指定设备的命令
        if (input.startsWith("disconnect|")) {
            String deviceId = input.substring(11).trim(); // 去掉"disconnect|"前缀
            System.out.println("断开命令，设备ID: '" + deviceId + "'");
            if (!deviceId.isEmpty()) {
                boolean success = stopDeviceById(deviceId);
                response.append(success ? "成功: 设备已停止" : "错误: 停止设备失败");
            } else {
                response.append("错误: 请指定设备ID，格式: disconnect|deviceId");
            }
        } else if (input.startsWith("connect|")) {
            // 检查是否是重新连接指定设备的命令
            String deviceId = input.substring(8).trim(); // 去掉"connect|"前缀
            System.out.println("连接命令，设备ID: '" + deviceId + "'");
            if (!deviceId.isEmpty()) {
                boolean success = reconnectDeviceById(deviceId);
                response.append(success ? "成功: 设备已重新连接" : "错误: 重新连接设备失败");
            } else {
                response.append("错误: 请指定设备ID，格式: connect|deviceId");
            }
        } else {
            // 处理其他命令
            String command = input.toLowerCase();
            System.out.println("正在处理基本命令: '" + command + "'");
            switch (command) {
                case "status":
                    response.append(getStatusResponse());
                    break;
                case "list":
                    response.append(getDeviceListResponse());
                    break;
                case "stop":
                    response.append("正在停止所有设备...");
                    stopAllDevices();
                    break;
                case "exit":
                    response.append("正在退出程序...");
                    stopAllDevices();
                    break;
                case "help":
                    response.append(getHelpResponse());
                    break;
                default:
                    response.append("未知命令: " + input);
                    response.append("\n可用命令: status, list, stop, exit, disconnect|deviceId, connect|deviceId, help");
                    break;
            }
        }
        
        System.out.println("命令响应: " + response.toString());
        return response.toString();
    }
    
    /**
     * 获取状态响应
     */
    private String getStatusResponse() {
        StringBuilder response = new StringBuilder();
        response.append("=== 设备状态 ===\n");
        response.append("总设备数: ").append(deviceCount).append("\n");
        response.append("已启动设备: ").append(deviceCount - startLatch.getCount()).append("\n");
        response.append("运行中设备: ").append(deviceCount - stopLatch.getCount()).append("\n");
        return response.toString();
    }
    
    /**
     * 获取设备列表响应
     */
    private String getDeviceListResponse() {
        StringBuilder response = new StringBuilder();
        response.append("=== 所有设备列表 ===\n");
        for (int i = 0; i < devices.size(); i++) {
            IoTDeviceSimulator device = devices.get(i);
            response.append(String.format("%d. %s (ID: %s)%n", i + 1, device.getDeviceName(), device.getDeviceId()));
        }
        return response.toString();
    }
    
    /**
     * 获取帮助信息响应
     */
    private String getHelpResponse() {
        StringBuilder response = new StringBuilder();
        response.append("=== IoT设备批量模拟器帮助 ===\n");
        response.append("可用命令:\n");
        response.append("  status - 显示设备状态\n");
        response.append("  list - 显示所有设备列表\n");
        response.append("  stop - 停止所有设备\n");
        response.append("  exit - 退出程序\n");
        response.append("  disconnect|deviceId - 停止指定设备 (例如: disconnect|29e5fc6c)\n");
        response.append("  connect|deviceId - 重新连接指定设备 (例如: connect|29e5fc6c)\n");
        response.append("  help - 显示帮助信息\n");
        return response.toString();
    }
    
    /**
     * 启动所有设备
     */
    public void startAllDevices() {
        System.out.println("=== 正在启动 " + deviceCount + " 个IoT设备模拟器 ===");
        System.out.println("服务器地址: " + serverHost + ":" + serverPort);
        System.out.println("设备类型: 传感器");
        System.out.println();
        
        // 启动命令服务器
        startCommandServer();
        
        // 创建并启动所有设备
        for (int i = 1; i <= deviceCount; i++) {
            String deviceName = String.format("传感器-%03d", i);
            IoTDeviceSimulator device = new IoTDeviceSimulator(serverHost, serverPort, deviceName, "传感器");
            devices.add(device);
            
            final int deviceIndex = i;
            executorService.submit(() -> {
                try {
                    System.out.println("正在启动设备 " + deviceIndex + "/" + deviceCount + ": " + deviceName);
                    device.connect();
                    startLatch.countDown();
                    stopLatch.countDown();
                } catch (Exception e) {
                    System.err.println("设备 " + deviceName + " 启动失败: " + e.getMessage());
                    startLatch.countDown();
                    stopLatch.countDown();
                }
            });
        }
        
        // 等待所有设备启动完成
        try {
            boolean allStarted = startLatch.await(30, TimeUnit.SECONDS);
            if (allStarted) {
                System.out.println();
                System.out.println("✅ 所有 " + deviceCount + " 个设备启动成功!");
                System.out.println("设备正在运行...");
                System.out.println();
                System.out.println("=== 操作指南 ===");
                System.out.println("命令服务器地址: localhost:4567");
                System.out.println("可用命令: status, list, stop, exit, disconnect|deviceName, connect|deviceName, help");
                System.out.println();
                System.out.println("程序将继续运行，等待命令...");
                System.out.println("按 Ctrl+C 退出");
                System.out.println();
            } else {
                System.out.println("⚠️  部分设备启动超时");
            }
        } catch (InterruptedException e) {
            System.err.println("等待设备启动被中断: " + e.getMessage());
        }
    }
    
    /**
     * 停止所有设备
     */
    public void stopAllDevices() {
        System.out.println("正在停止所有设备...");
        
        // 停止命令服务器
        commandServerRunning = false;
        if (commandServer != null && !commandServer.isClosed()) {
            try {
                commandServer.close();
                System.out.println("命令服务器已关闭");
            } catch (IOException e) {
                System.err.println("关闭命令服务器失败: " + e.getMessage());
            }
        }
        
        // 停止所有设备
        for (IoTDeviceSimulator device : devices) {
            try {
                device.disconnect();
            } catch (Exception e) {
                System.err.println("停止设备失败: " + e.getMessage());
            }
        }
        
        // 关闭线程池
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        
        System.out.println("✅ 所有设备已停止");
    }
    
    /**
     * 获取设备状态
     */
    public void showStatus() {
        System.out.println("=== 设备状态 ===");
        System.out.println("总设备数: " + deviceCount);
        System.out.println("已启动设备: " + (deviceCount - startLatch.getCount()));
        System.out.println("运行中设备: " + (deviceCount - stopLatch.getCount()));
        System.out.println();
    }
    
    /**
     * 等待所有设备运行完成
     */
    public void waitForCompletion() {
        try {
            stopLatch.await();
            System.out.println("所有设备已完成");
        } catch (InterruptedException e) {
            System.err.println("等待设备完成被中断: " + e.getMessage());
        }
    }
    
    /**
     * 根据设备ID停止指定设备
     * @param deviceId 设备ID
     * @return 是否成功停止设备
     */
    public boolean stopDeviceById(String deviceId) {
        System.out.println("正在查找要停止的设备，ID: '" + deviceId + "'");
        System.out.println("可用设备ID: " + getAllDeviceIds());
        
        for (IoTDeviceSimulator device : devices) {
            System.out.println("检查设备ID: '" + device.getDeviceId() + "' 与 '" + deviceId + "'");
            if (device.getDeviceId().equals(deviceId)) {
                try {
                    System.out.println("正在停止设备: " + device.getDeviceName() + " (ID: " + deviceId + ")");
                    device.disconnect();
                    System.out.println("✅ 设备 " + device.getDeviceName() + " (ID: " + deviceId + ") 已停止");
                    return true;
                } catch (Exception e) {
                    System.err.println("停止设备失败 " + device.getDeviceName() + " (ID: " + deviceId + "): " + e.getMessage());
                    return false;
                }
            }
        }
        System.err.println("❌ 未找到ID为 " + deviceId + " 的设备");
        return false;
    }
    
    /**
     * 根据设备ID重新连接指定设备
     * @param deviceId 设备ID
     * @return 是否成功重新连接设备
     */
    public boolean reconnectDeviceById(String deviceId) {
        System.out.println("正在查找要重新连接的设备，ID: '" + deviceId + "'");
        System.out.println("可用设备ID: " + getAllDeviceIds());
        
        for (IoTDeviceSimulator device : devices) {
            System.out.println("检查设备ID: '" + device.getDeviceId() + "' 与 '" + deviceId + "'");
            if (device.getDeviceId().equals(deviceId)) {
                try {
                    System.out.println("正在重新连接设备: " + device.getDeviceName() + " (ID: " + deviceId + ")");
                    device.reconnect();
                    System.out.println("✅ 设备 " + device.getDeviceName() + " (ID: " + deviceId + ") 已重新连接");
                    return true;
                } catch (Exception e) {
                    System.err.println("重新连接设备失败 " + device.getDeviceName() + " (ID: " + deviceId + "): " + e.getMessage());
                    return false;
                }
            }
        }
        System.err.println("❌ 未找到ID为 " + deviceId + " 的设备");
        return false;
    }
    
    /**
     * 获取所有设备名称列表
     * @return 设备名称列表
     */
    public List<String> getAllDeviceNames() {
        List<String> deviceNames = new ArrayList<>();
        for (IoTDeviceSimulator device : devices) {
            deviceNames.add(device.getDeviceName());
        }
        return deviceNames;
    }
    
    /**
     * 获取所有设备ID列表
     * @return 设备ID列表
     */
    public List<String> getAllDeviceIds() {
        List<String> deviceIds = new ArrayList<>();
        for (IoTDeviceSimulator device : devices) {
            deviceIds.add(device.getDeviceId());
        }
        return deviceIds;
    }
    
    /**
     * 显示所有设备列表
     */
    public void showAllDevices() {
        System.out.println("=== 所有设备列表 ===");
        List<String> deviceNames = getAllDeviceNames();
        for (int i = 0; i < deviceNames.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, deviceNames.get(i));
        }
        System.out.println();
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        String serverHost = "localhost";
        int serverPort = 8888;
        int deviceCount = 1000;
        
        // 解析命令行参数
        if (args.length >= 1) {
            serverHost = args[0];
        }
        if (args.length >= 2) {
            serverPort = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            deviceCount = Integer.parseInt(args[2]);
        }
        
        // 验证参数
        if (deviceCount <= 0 || deviceCount > 1000) {
            System.err.println("设备数量必须在1-1000之间");
            System.exit(1);
        }
        
        IoTDeviceBatchSimulator batchSimulator = new IoTDeviceBatchSimulator(serverHost, serverPort, deviceCount);
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(batchSimulator::stopAllDevices));
        
        // 启动所有设备
        batchSimulator.startAllDevices();
        
        // 保持程序运行，等待命令
        try {
            // 主线程等待，直到程序被中断
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            System.out.println("程序被中断，正在退出...");
        }
    }
}
