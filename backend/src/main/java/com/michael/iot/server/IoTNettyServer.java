package com.michael.iot.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 基于Netty的物联网TCP服务器
 */
public class IoTNettyServer {
    private static final Logger logger = LoggerFactory.getLogger(IoTNettyServer.class);
    
    private final int port;
    private final DeviceManager deviceManager;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public IoTNettyServer(int port, DeviceManager deviceManager) {
        this.port = port;
        this.deviceManager = deviceManager;
    }

    /**
     * 启动服务器
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
                            
                            // 空闲状态检测
                            pipeline.addLast(new IdleStateHandler(60, 30, 0, TimeUnit.SECONDS));
                            
                            // 编解码器
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            
                            // 业务处理器
                            pipeline.addLast(new IoTDeviceHandler(deviceManager));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // 绑定端口并启动服务器
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            
            logger.info("物联网TCP服务器启动成功，监听端口: {}", port);
            
            // 等待服务器关闭
            serverChannel.closeFuture().sync();
        } finally {
            shutdown();
        }
    }

    /**
     * 关闭服务器
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
        logger.info("物联网TCP服务器已关闭");
    }

    /**
     * 获取设备管理器
     */
    public DeviceManager getDeviceManager() {
        return deviceManager;
    }

    /**
     * 主方法，用于测试
     */
    public static void main(String[] args) throws Exception {
        int port = 8888;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        DeviceManager deviceManager = new DeviceManager();
        IoTNettyServer server = new IoTNettyServer(port, deviceManager);
        server.start();
    }
}
