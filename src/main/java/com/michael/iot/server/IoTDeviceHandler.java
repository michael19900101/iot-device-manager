package com.michael.iot.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;

/**
 * 物联网设备处理器
 */
public class IoTDeviceHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(IoTDeviceHandler.class);
    
    private final DeviceManager deviceManager;
    private String deviceId;

    public IoTDeviceHandler(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        String ipAddress = address.getAddress().getHostAddress();
        int port = address.getPort();
        
        logger.info("新设备连接: {}:{}", ipAddress, port);
        
        // 发送欢迎消息
        ctx.writeAndFlush("欢迎连接到物联网服务器！请发送设备注册信息。\n");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String message = (String) msg;
        logger.info("收到消息: {}", message);

        // 解析消息
        String[] parts = message.trim().split("\\|");
        if (parts.length < 2) {
            ctx.writeAndFlush("消息格式错误，请使用: 命令|参数1|参数2...\n");
            return;
        }

        String command = parts[0];
        switch (command) {
            case "REGISTER":
                handleRegister(ctx, parts);
                break;
            case "HEARTBEAT":
                handleHeartbeat(ctx, parts);
                break;
            case "DATA":
                handleData(ctx, parts);
                break;
            case "PONG":
                handlePong(ctx, parts);
                break;
            default:
                ctx.writeAndFlush("未知命令: " + command + "\n");
        }
    }

    /**
     * 处理设备注册
     */
    private void handleRegister(ChannelHandlerContext ctx, String[] parts) {
        if (parts.length < 4) {
            ctx.writeAndFlush("注册格式错误: REGISTER|设备ID|设备名称|设备类型\n");
            return;
        }

        String deviceId = parts[1];
        String deviceName = parts[2];
        String deviceType = parts[3];

        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        String ipAddress = address.getAddress().getHostAddress();
        int port = address.getPort();

        // 注册设备
        deviceManager.deviceOnline(deviceId, deviceName, deviceType, ipAddress, port, ctx.channel());
        this.deviceId = deviceId;

        ctx.writeAndFlush("注册成功！设备ID: " + deviceId + "\n");
        logger.info("设备注册成功: {}", deviceId);
    }

    /**
     * 处理心跳
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, String[] parts) {
        if (deviceId == null) {
            ctx.writeAndFlush("请先注册设备\n");
            return;
        }

        deviceManager.updateHeartbeat(deviceId);
        ctx.writeAndFlush("HEARTBEAT_OK\n");
        logger.debug("收到设备心跳: {}", deviceId);
    }

    /**
     * 处理数据上报
     */
    private void handleData(ChannelHandlerContext ctx, String[] parts) {
        if (deviceId == null) {
            ctx.writeAndFlush("请先注册设备\n");
            return;
        }

        if (parts.length < 2) {
            ctx.writeAndFlush("数据格式错误: DATA|数据内容\n");
            return;
        }

        String data = parts[1];
        logger.info("收到设备数据: {} -> {}", deviceId, data);
        
        // 保存设备数据
        DeviceInfo deviceInfo = deviceManager.getDevice(deviceId);
        if (deviceInfo != null) {
            deviceInfo.setLastData(data);
            deviceInfo.setLastDataTime(LocalDateTime.now());
            
            // 通知数据更新
            deviceManager.notifyDataUpdate(deviceInfo, data);
        }
        
        // 这里可以添加数据处理逻辑
        ctx.writeAndFlush("DATA_RECEIVED\n");
    }

    /**
     * 处理PONG响应
     */
    private void handlePong(ChannelHandlerContext ctx, String[] parts) {
        if (deviceId == null) {
            ctx.writeAndFlush("请先注册设备\n");
            return;
        }

        if (parts.length < 2) {
            ctx.writeAndFlush("PONG格式错误: PONG|设备ID\n");
            return;
        }

        String pongDeviceId = parts[1];
        if (!deviceId.equals(pongDeviceId)) {
            ctx.writeAndFlush("设备ID不匹配\n");
            return;
        }

        logger.debug("收到设备PONG响应: {}", deviceId);
        // 更新心跳时间
        deviceManager.updateHeartbeat(deviceId);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (deviceId != null) {
            deviceManager.deviceOffline(deviceId);
            logger.info("设备断开连接: {}", deviceId);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                logger.warn("设备心跳超时: {}", deviceId);
                ctx.close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                // 发送心跳检测
                ctx.writeAndFlush("PING\n");
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("设备连接异常: {}", cause.getMessage(), cause);
        if (deviceId != null) {
            deviceManager.deviceOffline(deviceId);
        }
        ctx.close();
    }
}
