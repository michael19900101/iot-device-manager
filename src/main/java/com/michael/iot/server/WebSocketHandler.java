package com.michael.iot.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * WebSocket处理器
 */
public class WebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    
    private final WebSocketServer webSocketServer;
    private String clientId;

    public WebSocketHandler(WebSocketServer webSocketServer) {
        this.webSocketServer = webSocketServer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        clientId = UUID.randomUUID().toString();
        webSocketServer.addClient(clientId, ctx.channel());
        logger.info("WebSocket客户端连接: {}", clientId);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            String message = ((TextWebSocketFrame) frame).text();
            logger.info("收到WebSocket消息: {}", message);
            
            // 这里可以处理前端发送的消息
            // 例如：请求设备列表、发送命令等
            handleWebSocketMessage(ctx, message);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (clientId != null) {
            webSocketServer.removeClient(clientId);
        }
        logger.info("WebSocket客户端断开: {}", clientId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("WebSocket连接异常", cause);
        ctx.close();
    }

    /**
     * 处理WebSocket消息
     */
    private void handleWebSocketMessage(ChannelHandlerContext ctx, String message) {
        try {
            // 尝试解析JSON消息
            if (message.startsWith("{")) {
                // 简单的JSON解析
                if (message.contains("\"type\":\"REQUEST_DEVICE_LIST\"")) {
                    // 请求设备列表
                    webSocketServer.broadcastDeviceListUpdate();
                    logger.info("收到设备列表请求，已广播设备列表");
                }
            } else {
                logger.info("处理WebSocket消息: {}", message);
                // 简单回复
                ctx.channel().writeAndFlush(new TextWebSocketFrame("消息已收到: " + message));
            }
        } catch (Exception e) {
            logger.error("处理WebSocket消息失败", e);
        }
    }
}
