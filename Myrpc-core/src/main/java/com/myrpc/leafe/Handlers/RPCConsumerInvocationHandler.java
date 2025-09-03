package com.myrpc.leafe.Handlers;

import com.myrpc.leafe.Registry.registry;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.List;
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
        log.info("调用方法：{}",method.getName());
        log.info("参数：{}",args);
        //1.调用zookeeper服务
        //1.1 获取服务提供者的地址(host)
        List<InetSocketAddress> addresses = anRegistry.discovery(anInterface.getName());
        log.info("可用服务提供者地址：{}",addresses);
        //2.用netty 实现远程调用
        return null;
    }
}
