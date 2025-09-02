package com.myrpc.leafe;

import java.lang.reflect.Proxy;

public class ReferenceConfig<T> {
    private Class<T> anInterface;

    public T get() {
        //这里用动态代理生成代理对象
        Object object = Proxy.newProxyInstance(anInterface.getClassLoader(), new Class[]{anInterface}, (proxy, method, args) -> {
            System.out.println("调用代理方法");
            return null;
        });
        return (T) object;
    }

    public void setInterface(Class<T> greetingServiceClass) {
        this.anInterface = greetingServiceClass;
    }
    public Class<T> getInterface() {
        return anInterface;
    }
}
