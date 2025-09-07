package com.myrpc.leafe.bootatrap.Initializer;

import com.myrpc.leafe.Handlers.PacketCodecHandler;
import com.myrpc.leafe.Handlers.Spliter;
import com.myrpc.leafe.Handlers.server.MethodCallHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyServerBootstrapInitializer {
    private static final NettyServerBootstrapInitializer INSTANCE = new NettyServerBootstrapInitializer();
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    private final ServerBootstrap serverBootstrap;
    private NettyServerBootstrapInitializer(){
         bossGroup = new NioEventLoopGroup();//监听连接
         workerGroup = new NioEventLoopGroup();//处理事件
         this.serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(bossGroup, workerGroup)//绑定线程组
                .channel(NioServerSocketChannel.class)//指定io模型为nio
                .option(ChannelOption.SO_BACKLOG,1024)//表示为ServerSocketChannel设置监听队列的大小可允许等待的连接数量
                .childOption(ChannelOption.SO_KEEPALIVE,true)//为连接后的SocketChannel启用心跳机制
                .childOption(ChannelOption.TCP_NODELAY,true)//禁用Nagle算法
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new LoggingHandler(LogLevel.DEBUG))
                                .addLast(new Spliter())
                                .addLast(PacketCodecHandler.INSTANCE)
                                .addLast(MethodCallHandler.INSTANCE);
                    }
                });
        //添加JVM关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }));
    }
    public static NettyServerBootstrapInitializer getInstance() {
        return INSTANCE;
    }
    /**
     * 获取Bootstrap实例
     * 因为Bootstrap实例是线程不安全的，所以每次调用时都返回一个克隆实例
     * 比如多个线程同时设置连接地址时，可能会导致错误
     * @return
     */
    public ServerBootstrap getBootstrap() {//因为Bootstrap实例是线程不安全的，所以每次调用时都返回一个克隆实例
        return serverBootstrap.clone(); // 返回克隆实例
    }
}
