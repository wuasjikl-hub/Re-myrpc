package com.myrpc.leafe.Handlers.client;

import com.myrpc.leafe.Registry.Registry;
import com.myrpc.leafe.Serialize.SerializerFactory;
import com.myrpc.leafe.bootatrap.Initializer.NettyBootstrapInitializer;
import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.bootatrap.annotaion.RetryAnno;
import com.myrpc.leafe.compress.CompressFactory;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RPCConsumerInvocationHandler implements InvocationHandler {
    private final Class<?> anInterface;//接口
    private final Registry anRegistry;//注册中心

    public RPCConsumerInvocationHandler(Class<?> anInterface, Registry anRegistry) {
        this.anInterface = anInterface;
        this.anRegistry = anRegistry;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //查看接口方法中是否要求重试
        int retryCount = 0;//不重试
        long retryDelay = 0;
        long maxRetryDelay = 0;
        RetryAnno annotation = method.getAnnotation(RetryAnno.class);
        if(annotation != null){
            retryCount = annotation.retryCount();
            retryDelay = annotation.retryDelay();
            maxRetryDelay = annotation.maxRetryDelay();
        }
        InetSocketAddress serviceAddress=null;
        Channel channel=null;
        while(true) {
            try {
                //1.创建rpc请求
                //先封装负载
                rpcRequestPacket requestPacket = createrpcRequestPacket(method, args);
                MyRpcBootstrap.getInstance().getConfigration().getREQUEST_THREAD_LOCAL().set(requestPacket);
                //2.发现服务
                serviceAddress = MyRpcBootstrap.getInstance().getConfigration().getLoadBalancer(anInterface.getName()).selectServiceAddress(anInterface.getName());
                //InetSocketAddress serviceAddress = MyRpcBootstrap.minimumResponseTimeLoadBalancer.selectServiceAddress(anInterface.getName());
                //InetSocketAddress serviceAddress = MyRpcBootstrap.consistentHashLoadBalancer.selectServiceAddress(anInterface.getName());

                log.info("通过负载均衡获取的服务提供者地址：{}", serviceAddress);
                //3.从缓存中获取或创建channel
                channel = getOrCreateChannel(serviceAddress);

                //4.发送请求
                CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                //添加监听器，当请求发送成功时，将结果保存到CompletableFuture中
                MyRpcBootstrap.getInstance().getConfigration().getPNDING_REQUESTS().put(requestPacket.getRequestId(), completableFuture);
                ChannelFuture channelFuture = channel.writeAndFlush(requestPacket).addListener((ChannelFutureListener) promise -> {
                    if (!promise.isSuccess()) {
                        log.error("请求服务失败：{}", promise.cause());
                        completableFuture.completeExceptionally(promise.cause());
                    }
                });
                //清理ThreadLocal
                MyRpcBootstrap.getInstance().getConfigration().getREQUEST_THREAD_LOCAL().remove();
                //5.这里会阻塞，等待客户端调用completed方法返回结果
                return completableFuture.get(10, TimeUnit.SECONDS);
                //return null;
            } catch (Exception e) {
                retryCount--;
                if(retryCount <= 0){
                    log.error("重试次数已用完，请求服务{}失败", anInterface.getName(),e);
                    channel.close();
                    break;
                }
                //使用指数退避
                try {
                    Thread.sleep(Math.min(retryDelay, maxRetryDelay));
                    retryDelay *= 2;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new LinktoProviderexception("请求服务失败");
    }
    private rpcRequestPacket createrpcRequestPacket(Method method, Object[] args){
        rpcRequestPayload requestPayload = rpcRequestPayload.builder()
                .interfaceName(anInterface.getName())
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .parameters(args)
                .returnType(method.getReturnType())
                .build();
        rpcRequestPacket requestPacket = new rpcRequestPacket(
                CompressFactory.getCompressorByName(MyRpcBootstrap.getInstance().getConfigration().getCompressType()).getCode(),  // compressType
                SerializerFactory.getSerializerByName(MyRpcBootstrap.getInstance().getConfigration().getSerializeType()).getCode(),  // serializeType
                MyRpcBootstrap.getInstance().getConfigration().idGenerator.getId(),    // requestId
                requestPayload);
        return requestPacket;
    }
    //获取或创建连接
    private Channel getOrCreateChannel(InetSocketAddress address){
        Channel channel = MyRpcBootstrap.getInstance().getConfigration().getCHANNEL_CACHE().get(address);
        if (channel != null && channel.isActive()) {
            return channel;
        }
        if(channel!=null&&!channel.isActive()){
            MyRpcBootstrap.getInstance().getConfigration().getCHANNEL_CACHE().remove(address);
        }
        //  如果在缓存中找不到或缓存中的连接不活跃，则重新创建连接
        try {
            //让await等待连接成功后返回
            //await和sync的区别是sync会阻塞当前线程，await不会
            //sync会直接抛出异常，await不会,需要自己检查是否成功
            ChannelFuture channelFuture = NettyBootstrapInitializer.getInstance().getBootstrap()
                    .connect(address.getHostName(), address.getPort()).await();
            if(channelFuture.isSuccess()){
                Channel newChannel = channelFuture.channel();
                //添加监听器，当连接关闭时，从缓存中移除该连接
                newChannel.closeFuture().addListener((ChannelFutureListener) future -> {
                    MyRpcBootstrap.getInstance().getConfigration().getCHANNEL_CACHE().remove(address, newChannel);
                });
                MyRpcBootstrap.getInstance().getConfigration().getCHANNEL_CACHE().put(address, newChannel);
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
