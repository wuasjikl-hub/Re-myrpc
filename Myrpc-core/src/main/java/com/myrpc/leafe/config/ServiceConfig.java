package com.myrpc.leafe.config;

public class ServiceConfig<T> {
    private Class<T> serviceInterface;
    private T ref;
    private String Groupinfo;

    public Class<T> getInterface() {
        return serviceInterface;
    }

    public void setInterface(Class<T> anInterface) {
        this.serviceInterface = anInterface;
    }

    public T getServiceimpl() {
        return ref;
    }

    public void setRef(T serviceimpl) {
        this.ref = serviceimpl;
    }

    public void setGroupinfo(String group) {
        this.Groupinfo=group;
    }
    public String getGroupinfo() {
        return Groupinfo;
    }
}
