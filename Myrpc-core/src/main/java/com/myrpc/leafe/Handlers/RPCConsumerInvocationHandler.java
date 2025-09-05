package com.myrpc.leafe.Handlers;

import com.myrpc.leafe.Handlers.client.Initializer.NettyBootstrapInitializer;
import com.myrpc.leafe.MyRpcBootstrap;
import com.myrpc.leafe.Registry.registry;
import com.myrpc.leafe.common.Constant;
import com.myrpc.leafe.exceptions.LinktoProviderexception;
import com.myrpc.leafe.packet.client.rpcRequestPacket;
import com.myrpc.leafe.packet.client.rpcRequestPayload;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RPCConsumerInvocationHandler implements InvocationHandler {
    private final Class<?> anInterface;//接口
    private final registry anRegistry;//注册中心

    public RPCConsumerInvocationHandler(Class<?> anInterface, registry anRegistry) {
        this.anInterface = anInterface;
        this.anRegistry = anRegistry;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //1.发现服务
        List<InetSocketAddress> addresses = anRegistry.discovery(anInterface.getName());
        log.info("可用服务提供者地址：{}",addresses);
        //2.从缓存中获取或创建channel
        Channel channel = getOrCreateChannel(addresses.get(0));

        //3.创建rpc请求
        //先封装负载
        rpcRequestPayload requestPayload = rpcRequestPayload.builder()
                .interfaceName(anInterface.getName())
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .parameters(args)
                .returnType(method.getReturnType())
                .build();
        rpcRequestPacket requestPacket = new rpcRequestPacket((byte) 1,  // requestType
                (byte) 1,  // compressType
                (byte) 1,  // serializeType
                1L,     // requestId
                requestPayload);

        //4.发送请求
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        //添加监听器，当请求发送成功时，将结果保存到CompletableFuture中
        MyRpcBootstrap.PENDING_REQUESTS.put(requestPacket.getRequestId(),completableFuture);
        ChannelFuture channelFuture = channel.writeAndFlush(requestPacket).addListener((ChannelFutureListener) promise->{
            if(!promise.isSuccess()){
                log.error("请求服务失败：{}",promise.cause());
                completableFuture.completeExceptionally(promise.cause());
            }
        });
        //5.这里会阻塞，等待客户端调用completed方法返回结果
        return completableFuture.get(10, TimeUnit.SECONDS);
        //return null;
    }
    //获取或创建连接
    private Channel getOrCreateChannel(InetSocketAddress address){
        Channel channel = MyRpcBootstrap.CHANNEL_CACHE.get(address);
        if (channel != null && channel.isActive()) {
            return channel;
        }
        if(channel!=null&&!channel.isActive()){
            MyRpcBootstrap.CHANNEL_CACHE.remove(address);
        }
        //  如果在缓存中找不到或缓存中的连接不活跃，则重新创建连接
        try {
            //让await等待连接成功后返回
            //await和sync的区别是sync会阻塞当前线程，await不会
            //sync会直接抛出异常，await不会,需要自己检查是否成功
            ChannelFuture channelFuture = NettyBootstrapInitializer.getInstance().getBootstrap()
                    .connect(address.getHostName(), Constant.PORT).await();
            if(channelFuture.isSuccess()){
                Channel newChannel = channelFuture.channel();
                //添加监听器，当连接关闭时，从缓存中移除该连接
                newChannel.closeFuture().addListener((ChannelFutureListener) future -> {
                    MyRpcBootstrap.CHANNEL_CACHE.remove(address, newChannel);
                });
                MyRpcBootstrap.CHANNEL_CACHE.put(address, newChannel);
                log.debug("已经和【{}】建立连接: " + address);
                return newChannel;
            }else{
                throw new LinktoProviderexception("连接服务提供者失败: " + address);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LinktoProviderexception("连接服务提供者失败: " + address, e);
        }
    }
}
