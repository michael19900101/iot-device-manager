package com.michael.iot.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务器，用于向前端推送设备状态变更
 */
public class WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);
    
    private final int port;
    private final DeviceManager deviceManager;

    private final Map<String, Channel> webSocketClients = new ConcurrentHashMap<>();
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public WebSocketServer(int port, DeviceManager deviceManager) {
        this.port = port;
        this.deviceManager = deviceManager;
        
        // 添加设备状态监听器
        deviceManager.addStatusListener(new DeviceStatusListener() {
            @Override
            public void onStatusChange(DeviceInfo device, DeviceStatus oldStatus, DeviceStatus newStatus) {
                broadcastDeviceStatusChange(device, oldStatus, newStatus);
                // 同时广播设备列表更新
                broadcastDeviceListUpdate();
            }
        });
        
        // 添加设备数据监听器
        deviceManager.addDataListener(new DeviceDataListener() {
            @Override
            public void onDataUpdate(DeviceInfo device, String data) {
                broadcastDeviceDataUpdate(device, data);
            }
        });
    }

    /**
     * 启动WebSocket服务器
     */
    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // HTTP编解码器
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new ChunkedWriteHandler());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            
                            // WebSocket处理器
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true));
                            pipeline.addLast(new WebSocketHandler(WebSocketServer.this));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            
            logger.info("WebSocket服务器启动成功，监听端口: {}", port);
            
            serverChannel.closeFuture().sync();
        } finally {
            shutdown();
        }
    }

    /**
     * 关闭WebSocket服务器
     */
    public void shutdown() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        logger.info("WebSocket服务器已关闭");
    }

    /**
     * 添加WebSocket客户端
     */
    public void addClient(String clientId, Channel channel) {
        webSocketClients.put(clientId, channel);
        logger.info("WebSocket客户端连接: {}", clientId);
    }

    /**
     * 移除WebSocket客户端
     */
    public void removeClient(String clientId) {
        webSocketClients.remove(clientId);
        logger.info("WebSocket客户端断开: {}", clientId);
    }

    /**
     * 广播设备状态变更
     */
    private void broadcastDeviceStatusChange(DeviceInfo device, DeviceStatus oldStatus, DeviceStatus newStatus) {
        try {
            // 构建简单的JSON消息
            String jsonMessage = String.format(
                "{\"type\":\"DEVICE_STATUS_CHANGE\",\"deviceId\":\"%s\",\"deviceName\":\"%s\",\"deviceType\":\"%s\",\"oldStatus\":\"%s\",\"newStatus\":\"%s\",\"timestamp\":%d}",
                device.getDeviceId(),
                device.getDeviceName(),
                device.getDeviceType(),
                oldStatus.name(),
                newStatus.name(),
                System.currentTimeMillis()
            );
            
            TextWebSocketFrame frame = new TextWebSocketFrame(jsonMessage);

            // 广播给所有WebSocket客户端
            for (Channel clientChannel : webSocketClients.values()) {
                if (clientChannel.isActive()) {
                    clientChannel.writeAndFlush(frame.retain());
                }
            }

            logger.info("广播设备状态变更: {} -> {}", device.getDeviceId(), newStatus);
        } catch (Exception e) {
            logger.error("广播设备状态变更失败", e);
        }
    }

    /**
     * 广播设备数据更新
     */
    private void broadcastDeviceDataUpdate(DeviceInfo device, String data) {
        try {
            // 构建设备数据更新JSON消息
            String jsonMessage = String.format(
                "{\"type\":\"DEVICE_DATA_UPDATE\",\"deviceId\":\"%s\",\"deviceName\":\"%s\",\"data\":\"%s\",\"timestamp\":%d}",
                device.getDeviceId(),
                device.getDeviceName(),
                data.replace("\"", "\\\""),
                System.currentTimeMillis()
            );
            
            TextWebSocketFrame frame = new TextWebSocketFrame(jsonMessage);

            for (Channel clientChannel : webSocketClients.values()) {
                if (clientChannel.isActive()) {
                    clientChannel.writeAndFlush(frame.retain());
                }
            }

            logger.info("广播设备数据更新: {} -> {}", device.getDeviceId(), data);
        } catch (Exception e) {
            logger.error("广播设备数据更新失败", e);
        }
    }



    /**
     * 获取当前连接的客户端数量
     * @return 客户端数量
     */
    public int getClientCount() {
        return webSocketClients.size();
    }

    /**
     * 广播设备列表更新
     */
    public void broadcastDeviceListUpdate() {
        try {
            // 构建简单的设备列表JSON消息
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"type\":\"DEVICE_LIST_UPDATE\",\"devices\":[");
            
            java.util.List<DeviceInfo> devices = deviceManager.getAllDevices();
            for (int i = 0; i < devices.size(); i++) {
                DeviceInfo device = devices.get(i);
                if (i > 0) jsonBuilder.append(",");
                jsonBuilder.append(String.format(
                    "{\"deviceId\":\"%s\",\"deviceName\":\"%s\",\"deviceType\":\"%s\",\"status\":\"%s\",\"ipAddress\":\"%s\",\"port\":%d,\"connectTime\":\"%s\",\"lastHeartbeat\":\"%s\",\"disconnectTime\":\"%s\",\"lastData\":\"%s\",\"lastDataTime\":\"%s\"}",
                    device.getDeviceId(),
                    device.getDeviceName(),
                    device.getDeviceType(),
                    device.getStatus().name(),
                    device.getIpAddress() != null ? device.getIpAddress() : "",
                    device.getPort(),
                    device.getConnectTime() != null ? device.getConnectTime().toString() : "",
                    device.getLastHeartbeat() != null ? device.getLastHeartbeat().toString() : "",
                    device.getDisconnectTime() != null ? device.getDisconnectTime().toString() : "",
                    device.getLastData() != null ? device.getLastData().replace("\"", "\\\"") : "",
                    device.getLastDataTime() != null ? device.getLastDataTime().toString() : ""
                ));
            }
            
            jsonBuilder.append("],\"timestamp\":").append(System.currentTimeMillis()).append("}");
            
            TextWebSocketFrame frame = new TextWebSocketFrame(jsonBuilder.toString());

            for (Channel clientChannel : webSocketClients.values()) {
                if (clientChannel.isActive()) {
                    clientChannel.writeAndFlush(frame.retain());
                }
            }
        } catch (Exception e) {
            logger.error("广播设备列表更新失败", e);
        }
    }

    /**
     * 设备状态变更消息
     */
    public static class DeviceStatusMessage {
        private String type;
        private String deviceId;
        private String deviceName;
        private String deviceType;
        private String oldStatus;
        private String newStatus;
        private long timestamp;

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
        public String getOldStatus() { return oldStatus; }
        public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }
        public String getNewStatus() { return newStatus; }
        public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    /**
     * 设备列表更新消息
     */
    public static class DeviceListMessage {
        private String type;
        private java.util.List<DeviceInfo> devices;
        private long timestamp;

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public java.util.List<DeviceInfo> getDevices() { return devices; }
        public void setDevices(java.util.List<DeviceInfo> devices) { this.devices = devices; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
