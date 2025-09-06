package com.myrpc.leafe.config;

import com.myrpc.leafe.Handlers.client.RPCConsumerInvocationHandler;
import com.myrpc.leafe.Registry.Registry;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;

@Slf4j
public class ReferenceConfig<T> {
    private Class<T> anInterface;//接口

    private Registry anRegistry;//注册中心
    public T get() {
        //这里用动态代理生成代理对象
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class[] classes = new Class[]{anInterface};
        RPCConsumerInvocationHandler rpcConsumerInvocationHandler = new RPCConsumerInvocationHandler(anInterface, anRegistry);
        Object object=Proxy.newProxyInstance(classLoader, classes, rpcConsumerInvocationHandler);
        return (T)object;
    }

    public void setInterface(Class<T> greetingServiceClass) {
        this.anInterface = greetingServiceClass;
    }
    public Class<T> getInterface() {
        return anInterface;
    }

    public Registry getAnRegistry() {
        return anRegistry;
    }

    public void setAnRegistry(Registry anRegistry) {
        this.anRegistry = anRegistry;
    }
}
