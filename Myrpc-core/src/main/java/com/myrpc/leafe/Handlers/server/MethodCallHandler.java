package com.myrpc.leafe.Handlers.server;

import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.config.ServiceConfig;
import com.myrpc.leafe.enumeration.StatusCode;
import com.myrpc.leafe.packet.client.rpcRequestPacket;
import com.myrpc.leafe.packet.client.rpcRequestPayload;
import com.myrpc.leafe.packet.server.rpcResponsePacket;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

@Slf4j
@ChannelHandler.Sharable
public class MethodCallHandler extends SimpleChannelInboundHandler<rpcRequestPacket> {
    public static final MethodCallHandler INSTANCE = new MethodCallHandler();
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, rpcRequestPacket requestPacket) throws Exception {
        rpcRequestPayload payload = requestPacket.getPayload();
        //2.根据接口名，方法名，参数类型，参数值，返回值类型，调用对应的方法
        Object result = MethodCaller(payload);
        //3.封装响应
        rpcResponsePacket responsePacket = new rpcResponsePacket(requestPacket.getCompressType(),
                requestPacket.getSerializeType(),requestPacket.getRequestId(),result,StatusCode.SUCCESS.getCode());
        //4.发送响应
        channelHandlerContext.writeAndFlush(responsePacket);
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
            e.printStackTrace();
        }
        return res;
    }
}
