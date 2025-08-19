package com.michael.iot.test;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.UUID;

/**
 * 物联网设备模拟器
 */
public class IoTDeviceSimulator {
    private final String serverHost;
    private final int serverPort;
    private final String deviceId;
    private final String deviceName;
    private final String deviceType;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean running = false;
    private Thread heartbeatThread;

    public IoTDeviceSimulator(String serverHost, int serverPort, String deviceName, String deviceType) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.deviceId = UUID.randomUUID().toString().substring(0, 8);
        this.deviceName = deviceName;
        this.deviceType = deviceType;
    }
    
    /**
     * 获取设备名称
     * @return 设备名称
     */
    public String getDeviceName() {
        return deviceName;
    }
    
    /**
     * 获取设备ID
     * @return 设备ID
     */
    public String getDeviceId() {
        return deviceId;
    }
    
    /**
     * 获取设备类型
     * @return 设备类型
     */
    public String getDeviceType() {
        return deviceType;
    }

    /**
     * 连接服务器
     */
    public void connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            System.out.println("设备连接成功: " + deviceId);
            
            // 注册设备
            registerDevice();
            
            // 启动心跳线程
            startHeartbeat();
            
            // 启动数据上报线程
            startDataReporting();
            
            // 监听服务器消息
            listenToServer();
            
        } catch (Exception e) {
            System.err.println("连接服务器失败: " + e.getMessage());
        }
    }

    /**
     * 注册设备
     */
    private void registerDevice() {
        String registerMessage = String.format("REGISTER|%s|%s|%s", deviceId, deviceName, deviceType);
        out.println(registerMessage);
        
        try {
            String response = in.readLine();
            System.out.println("注册响应: " + response);
        } catch (IOException e) {
            System.err.println("读取注册响应失败: " + e.getMessage());
        }
    }

    /**
     * 启动心跳线程
     */
    private void startHeartbeat() {
        heartbeatThread = new Thread(() -> {
            while (running && !socket.isClosed()) {
                try {
                    Thread.sleep(30000); // 30秒发送一次心跳
                    if (!socket.isClosed()) {
                        out.println("HEARTBEAT|" + deviceId);
                        System.out.println("发送心跳: " + deviceId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("发送心跳失败: " + e.getMessage());
                    break;
                }
            }
        });
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    /**
     * 启动数据上报线程
     */
    private void startDataReporting() {
        Thread dataThread = new Thread(() -> {
            Random random = new Random();
            while (running && !socket.isClosed()) {
                try {
                    Thread.sleep(10 * 1000); // 10秒自动上报数据
                    if (!socket.isClosed()) {
                        // 模拟传感器数据
                        double temperature = 20 + random.nextDouble() * 10;
                        double humidity = 40 + random.nextDouble() * 30;
                        String data = String.format("{\"temperature\":%.2f,\"humidity\":%.2f}", temperature, humidity);
                        out.println("DATA|" + data);
                        System.out.println(deviceName + "(" + deviceId + ") 上报数据: " + data);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println(deviceName + "(" + deviceId + ") 上报数据失败: " + e.getMessage());
                    break;
                }
            }
        });
        dataThread.setDaemon(true);
        dataThread.start();
    }

    /**
     * 监听服务器消息
     */
    private void listenToServer() {
        running = true;
        
        // 启动用户输入监听线程
//        Thread inputThread = new Thread(() -> {
//            try {
//                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
//                String userData;
//                while (running && !socket.isClosed()) {
//                    System.out.print("请输入要发送的数据 (格式: DATA|{\"key\":\"value\"}) 或按回车跳过: ");
//                    userData = userInput.readLine();
//                    if (userData != null && !userData.trim().isEmpty()) {
//                        if (userData.startsWith("DATA|")) {
//                            out.println(userData);
//                            System.out.println("已发送数据: " + userData);
//                        } else {
//                            System.out.println("格式错误，请使用 DATA|{...} 格式");
//                        }
//                    }
//                }
//            } catch (IOException e) {
//                if (running) {
//                    System.err.println("读取用户输入失败: " + e.getMessage());
//                }
//            }
//        });
//        inputThread.setDaemon(true);
//        inputThread.start();
        
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (!"DATA_RECEIVED".equals(message)) {
                    System.out.println(deviceName + "(" + deviceId + ") 收到服务器消息: " + message);
                }
                
                if (message.equals("PING")) {
                    out.println("PONG|" + deviceId);
                }
            }
        } catch (IOException e) {
            System.err.println("读取服务器消息失败: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        running = false;
        try {
            if (heartbeatThread != null) {
                heartbeatThread.interrupt();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null) {
                socket.close();
            }
            System.out.println("设备断开连接: " + deviceId);
        } catch (IOException e) {
            System.err.println("断开连接失败: " + e.getMessage());
        }
    }
    
    /**
     * 重新连接服务器
     */
    public void reconnect() {
        // 先断开现有连接
        disconnect();
        
        // 等待一段时间确保连接完全断开
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 重新连接
        try {
            System.out.println("设备正在重新连接: " + deviceId);
            
            // 重新建立连接
            socket = new Socket(serverHost, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            System.out.println("设备连接成功: " + deviceId);
            
            // 注册设备
            registerDevice();
            
            // 设置运行状态为true，确保心跳和数据上报线程正常工作
            running = true;
            
            // 启动心跳线程
            startHeartbeat();
            
            // 启动数据上报线程
            startDataReporting();

            // 启动监听服务器消息线程
            Thread listenThread = new Thread(() -> {
                try {
                    String message;
                    while (running && (message = in.readLine()) != null) {
                        System.out.println("收到服务器消息: " + message);

                        if (message.equals("PING")) {
                            out.println("PONG|" + deviceId);
                        }
                    }
                } catch (IOException e) {
                    if (running) {
                        System.err.println("读取服务器消息失败: " + e.getMessage());
                    }
                }
            });
            listenThread.setDaemon(true);
            listenThread.start();
            
        } catch (Exception e) {
            System.err.println("重新连接失败: " + e.getMessage());
        }
    }

    /**
     * 主方法，用于测试
     */
    public static void main(String[] args) {
        String serverHost = "localhost";
        int serverPort = 8888;
        String deviceName = "测试设备";
        String deviceType = "传感器";
        
        if (args.length >= 1) {
            serverHost = args[0];
        }
        if (args.length >= 2) {
            serverPort = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            deviceName = args[2];
        }
        if (args.length >= 4) {
            deviceType = args[3];
        }

        IoTDeviceSimulator simulator = new IoTDeviceSimulator(serverHost, serverPort, deviceName, deviceType);
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(simulator::disconnect));
        
        // 连接服务器
        simulator.connect();
    }
}
