package com.myrpc.leafe;

import com.myrpc.leafe.Registry.impl.ZooKeeperRegistry;
import com.myrpc.leafe.Registry.registry;
import com.myrpc.leafe.common.Constant;

public class RegistryConfig {
    private String registryAddress;
    private String registrytype;
    public RegistryConfig(String registrytype,String registryAddress)
    {
        this.registryAddress = registryAddress;
        this.registrytype = registrytype;
    }

    public String getRegistryAddress() {
        return registryAddress;
    }

    public void setRegistryAddress(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public String getRegistrytype() {
        return registrytype;
    }

    public void setRegistrytype(String registrytype) {
        this.registrytype = registrytype;
    }

    public registry getRegistry() {
        //通过注册中心的type来创建
        if(registrytype.equals("zookeeper")){
            return new ZooKeeperRegistry(registryAddress, Constant.ZK_SESSION_TIMEOUT);
        }
        if(registrytype.equals("dubbo")){
            return null;
        }
        throw new RuntimeException("找不到合适的注册中心");
    }
}
