package com.myrpc.leafe.Handlers.server;

import com.myrpc.leafe.bootatrap.Initializer.ShutdownHolder;
import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.config.ServiceConfig;
import com.myrpc.leafe.enumeration.StatusCode;
import com.myrpc.leafe.packet.client.rpcRequestPacket;
import com.myrpc.leafe.packet.client.rpcRequestPayload;
import com.myrpc.leafe.packet.server.rpcResponsePacket;
import com.myrpc.leafe.protection.RateLimiter;
import com.myrpc.leafe.protection.impl.TokenBarrelRateLimiter;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.Map;

@Slf4j
@ChannelHandler.Sharable
public class MethodCallHandler extends SimpleChannelInboundHandler<rpcRequestPacket> {
    public static final MethodCallHandler INSTANCE = new MethodCallHandler();
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, rpcRequestPacket requestPacket) throws Exception {
        SocketAddress socketAddress = channelHandlerContext.channel().remoteAddress();
        Map<SocketAddress, RateLimiter> rateLimiterMap = MyRpcBootstrap.getInstance().getConfigration().getSERVICE_TO_RATELIMITER();
        RateLimiter rateLimiter = rateLimiterMap.computeIfAbsent
                (socketAddress, k -> new TokenBarrelRateLimiter());
        StatusCode statusCode = StatusCode.SUCCESS;
        //2.查看挡板状态
        if(ShutdownHolder.isShutdown.get()){
            statusCode=StatusCode.BECOLSING;
            log.error("服务正在被关闭{}",socketAddress);
            rpcResponsePacket responsePacket = createResponsePacket(requestPacket, statusCode, null);
            channelHandlerContext.writeAndFlush(responsePacket);
            return;
        }
        //计数器加1
        ShutdownHolder.longAdder.increment();
        //3.封装响应
        //判断限流
        if(!rateLimiter.tryAcquire()){
            statusCode = StatusCode.RATE_LIMIT;
            log.error("Rate limit exceeded for address: {}", socketAddress);
            rpcResponsePacket responsePacket = createResponsePacket(requestPacket, statusCode, null);
            channelHandlerContext.writeAndFlush(responsePacket);
            return;
        }
        //1.拿到负载
        rpcRequestPayload payload = requestPacket.getPayload();
        //2.根据接口名，方法名，参数类型，参数值，返回值类型，调用对应的方法
        Object result=null;
        try {
            result= MethodCaller(payload);
        }catch (Exception e){
            log.error("调用方法异常：{}",e.getMessage());
            statusCode = StatusCode.BECOLSING;
        }
        rpcResponsePacket responsePacket = createResponsePacket(requestPacket, statusCode, result);

        //4.发送响应
        channelHandlerContext.writeAndFlush(responsePacket);
        //5.计数器减1
        ShutdownHolder.longAdder.decrement();
    }
    private rpcResponsePacket createResponsePacket(rpcRequestPacket requestPacket,
                                                   StatusCode statusCode, Object result) {
        return new rpcResponsePacket(
                requestPacket.getCompressType(),
                requestPacket.getSerializeType(),
                requestPacket.getRequestId(),
                statusCode.getCode(),
                result
        );
    }
    private Object MethodCaller(rpcRequestPayload payload) {
        String methodName = payload.getMethodName();
        Class<?>[] parameterTypes = payload.getParameterTypes();
        Object[] parameters = payload.getParameters();
        Class<?> returnType = payload.getReturnType();
        String interfaceName = payload.getInterfaceName();
        //寻找对应的方法
        ServiceConfig<?> serviceConfig = MyRpcBootstrap.getInstance().getConfigration().getSERVER_MAP().get(interfaceName);
        //服务的实现类
        Object serviceimpl = serviceConfig.getServiceimpl();
        //通过反射调用对应的方法
        Object res=null;
        try {
            Class<?> aClass = serviceimpl.getClass();
            Method method = aClass.getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            log.info("调用方法：{}",methodName);
            res = method.invoke(serviceimpl, parameters);
        } catch (Exception e) {
            throw new RuntimeException(methodName, e);
        }
        return res;
    }
}
