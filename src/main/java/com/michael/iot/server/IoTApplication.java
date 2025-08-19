package com.michael.iot.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

/**
 * 物联网应用启动类
 */
public class IoTApplication {
    private static final Logger logger = LoggerFactory.getLogger(IoTApplication.class);
    
    private final IoTNettyServer tcpServer;
    private final WebSocketServer webSocketServer;
    private final DeviceManager deviceManager;
    private final ScheduledExecutorService scheduler;

    public IoTApplication(int tcpPort, int wsPort) {
        this.deviceManager = new DeviceManager();
        this.tcpServer = new IoTNettyServer(tcpPort, deviceManager);
        this.webSocketServer = new WebSocketServer(wsPort, deviceManager);
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * 启动应用
     */
    public void start() {
        try {
            logger.info("启动物联网应用...");
            
            // 启动WebSocket服务器（在后台线程中运行）
            Thread wsThread = new Thread(() -> {
                try {
                    webSocketServer.start();
                } catch (Exception e) {
                    logger.error("WebSocket服务器启动失败", e);
                }
            });
            wsThread.setDaemon(true);
            wsThread.start();
            
            // 等待WebSocket服务器启动
            Thread.sleep(1000);
            

            
            // 启动TCP服务器（主线程）
            tcpServer.start();
            
        } catch (Exception e) {
            logger.error("应用启动失败", e);
        }
    }



    /**
     * 停止应用
     */
    public void stop() {
        logger.info("停止物联网应用...");
        tcpServer.shutdown();
        webSocketServer.shutdown();
        scheduler.shutdown();
    }

    /**
     * 获取设备管理器
     */
    public DeviceManager getDeviceManager() {
        return deviceManager;
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        int tcpPort = 8888;
        int wsPort = 8889;
        
        if (args.length >= 1) {
            tcpPort = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            wsPort = Integer.parseInt(args[1]);
        }

        IoTApplication app = new IoTApplication(tcpPort, wsPort);
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
        
        // 启动应用
        app.start();
    }
}
