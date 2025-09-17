package com.myrpc.leafe.Handlers.client;

import com.myrpc.leafe.Registry.Registry;
import com.myrpc.leafe.Serialize.SerializerFactory;
import com.myrpc.leafe.bootatrap.Initializer.NettyBootstrapInitializer;
import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.bootatrap.annotaion.RetryAnno;
import com.myrpc.leafe.compress.CompressFactory;
import com.myrpc.leafe.exceptions.CircuitBreakerException;
import com.myrpc.leafe.exceptions.LinktoProviderexception;
import com.myrpc.leafe.packet.client.rpcRequestPacket;
import com.myrpc.leafe.packet.client.rpcRequestPayload;
import com.myrpc.leafe.protection.CircuitBreaker;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class RPCConsumerInvocationHandler implements InvocationHandler {
    private final Class<?> anInterface;//接口
    private final Registry anRegistry;//注册中心
    private String groupinfo;

    public RPCConsumerInvocationHandler(Class<?> anInterface, Registry anRegistry,String groupinfo) {
        this.anInterface = anInterface;
        this.anRegistry = anRegistry;
        this.groupinfo=groupinfo;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //查看接口方法中是否要求重试
        RetryConfig retryConfig = getRetryConfig( method);
        return objWithretry(retryConfig,()->invokeremotemethod(method, args));
    }

    private Object invokeremotemethod(Method method, Object[] args) {
        //1.创建rpc请求
        //先封装负载
        rpcRequestPacket requestPacket = createrpcRequestPacket(method, args);
        MyRpcBootstrap.getInstance().getConfigration().getREQUEST_THREAD_LOCAL().set(requestPacket);
        //2.发现服务
        InetSocketAddress serviceAddress = MyRpcBootstrap.getInstance().getConfigration()
                .getLoadBalancer(anInterface.getName()).selectServiceAddress(anInterface.getName(),groupinfo);
        log.info("通过负载均衡获取的服务提供者地址：{}", serviceAddress);
        //3.从缓存中获取或创建channel
        Channel channel = getOrCreateChannel(serviceAddress);
        //4.获取熔断器
        //拿到熔断器
        Map<InetSocketAddress, CircuitBreaker> serviceToCircuitbreaker = MyRpcBootstrap.getInstance()
                .getConfigration().getSERVICE_TO_CIRCUITBREAKER();
        CircuitBreaker circuitBreaker = serviceToCircuitbreaker.computeIfAbsent(serviceAddress, k -> new CircuitBreaker());
        // 5. 通过熔断器执行调用
        try {
            return circuitBreaker.execute(() -> {
                CompletableFuture<Object> resultFuture = new CompletableFuture<>();
                try {
                    // 保存请求ID与Future的映射
                    MyRpcBootstrap.getInstance().getConfigration().getPNDING_REQUESTS()
                            .put(requestPacket.getRequestId(), resultFuture);
                    // 发送请求
                    channel.writeAndFlush(requestPacket).addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess()) {
                            log.error("请求发送失败", future.cause());
                            resultFuture.completeExceptionally(future.cause());
                            // 清理失败的请求
                            MyRpcBootstrap.getInstance().getConfigration().getPNDING_REQUESTS()
                                    .remove(requestPacket.getRequestId());
                        }
                    });
                    ScheduledExecutorService timeoutScheduler = MyRpcBootstrap.getInstance()
                            .getConfigration().getTimeoutScheduler();
                    ScheduledFuture<?> scheduledFuture = timeoutScheduler.schedule(() -> {
                        if (!resultFuture.isDone()) {
                            resultFuture.completeExceptionally(new TimeoutException("请求超时"));
                        }
                    }, 10, TimeUnit.SECONDS);
                    resultFuture.whenComplete((result, ex) -> scheduledFuture.cancel(false));

                    return resultFuture.get();
                } catch (Exception e) {
                    // 清理资源
                    MyRpcBootstrap.getInstance().getConfigration().getPNDING_REQUESTS()
                            .remove(requestPacket.getRequestId());
                    throw new LinktoProviderexception( "发送请求失败",e);
                }
            }, channel);
        } catch (Exception e) {
            throw new CircuitBreakerException("熔断器执行时异常",e);
        }finally {
            MyRpcBootstrap.getInstance().getConfigration().getREQUEST_THREAD_LOCAL().remove();
        }
    }

    private Object objWithretry(RetryConfig retryConfig, Callable<Object> remotemethodcall) {
        int retryCount = retryConfig.getRetryCount();
        long retryDelay = retryConfig.getRetryDelay();
        long maxRetryDelay = retryConfig.getMaxRetryDelay();
        while(true){
            try{
                return remotemethodcall.call();
            }catch (Exception e){
                if(retryCount <= 0){
                    log.error("重试次数已用完，请求服务{}失败", anInterface.getName(),e);
                    throw new LinktoProviderexception("请求服务失败");
                }
                retryCount--;
                log.error("请求服务失败，剩余重试次数: {}", retryCount);
                if (retryDelay>0){
                    try{
                        log.error("等待重试，当前延迟: {} ms", retryDelay);
                        Thread.sleep(Math.min(retryDelay, maxRetryDelay));
                        retryDelay *= 2;
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断",ex);
                    }
                }
            }
        }
    }

    private RetryConfig getRetryConfig(Method method) {
        RetryAnno annotation = method.getAnnotation(RetryAnno.class);
        if (annotation != null) {
            return new RetryConfig(annotation.retryCount(), annotation.retryDelay(), annotation.maxRetryDelay());
        }
        return new RetryConfig(0, 0, 0); // 默认不重试
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
    //重试配置类
    private static class RetryConfig {
        private final int retryCount;
        private final long retryDelay;
        private final long maxRetryDelay;

        public RetryConfig(int retryCount, long retryDelay, long maxRetryDelay) {
            this.retryCount = retryCount;
            this.retryDelay = retryDelay;
            this.maxRetryDelay = maxRetryDelay;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public long getRetryDelay() {
            return retryDelay;
        }

        public long getMaxRetryDelay() {
            return maxRetryDelay;
        }

    }
}
