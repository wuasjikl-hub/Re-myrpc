package com.myrpc.leafe.Handlers;

import com.myrpc.leafe.Handlers.server.MessageRequestHandler;
import com.myrpc.leafe.MyRpcBootstrap;
import com.myrpc.leafe.common.Constant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NettyBootstrapInitializer {
    private static final NettyBootstrapInitializer INSTANCE = new NettyBootstrapInitializer();
    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private NettyBootstrapInitializer(){
        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Constant.CLIENT_CONNECT_TIMEOUT)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        // 添加编解码器和处理器
                        ch.pipeline().addLast(MessageRequestHandler.INSTANCE);
//                        ch.pipeline().addLast(new RpcEncoder(RpcRequest.class));
//                        ch.pipeline().addLast(new RpcDecoder(RpcResponse.class));
//                        ch.pipeline().addLast(new RpcClientHandler());
                    }
                });
        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            group.shutdownGracefully();
            List<Channel> channels = new ArrayList<>(MyRpcBootstrap.CHANNEL_CACHE.values());
            // 然后立即清空缓存，防止新的操作如增加新的缓存
            MyRpcBootstrap.CHANNEL_CACHE.clear();
            channels.forEach(channel -> {
                if(channel != null && channel.isActive()){
                    channel.close();
                }
            });
        }));
    }
    public static NettyBootstrapInitializer getInstance() {
        return INSTANCE;
    }
    /**
     * 获取Bootstrap实例
     * 因为Bootstrap实例是线程不安全的，所以每次调用时都返回一个克隆实例
     * 比如多个线程同时设置连接地址时，可能会导致错误
     * @return
     */
    public Bootstrap getBootstrap() {//因为Bootstrap实例是线程不安全的，所以每次调用时都返回一个克隆实例
        return bootstrap.clone(); // 返回克隆实例
    }
    public EventLoopGroup getEventLoopGroup() {
        return group;
    }
}
