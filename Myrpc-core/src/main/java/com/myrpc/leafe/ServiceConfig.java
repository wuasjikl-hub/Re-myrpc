package com.myrpc.leafe;

public class ServiceConfig<T> {
    private Class<T> serviceInterface;
    private T serviceimpl;

    public Class<T> getInterface() {
        return serviceInterface;
    }

    public void setInterface(Class<T> anInterface) {
        this.serviceInterface = anInterface;
    }

    public T getServiceimpl() {
        return serviceimpl;
    }

    public void setRef(T serviceimpl) {
        this.serviceimpl = serviceimpl;
    }
}
