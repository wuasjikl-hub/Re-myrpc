package com.myrpc.leafe.config;

import com.myrpc.leafe.Registry.Registry;
import com.myrpc.leafe.Registry.impl.ZooKeeperRegistry;
import com.myrpc.leafe.common.Constant;

public class RegistryConfig {
    private final String registryAddress;
    private final String registrytype;
    private volatile Registry registry;
    public RegistryConfig(String registrytype,String registryAddress)
    {
        this.registryAddress = registryAddress;
        this.registrytype = registrytype;
    }

    public String getRegistryAddress() {
        return registryAddress;
    }

    public String getRegistryType() {
        return registrytype;
    }

    public Registry getRegistry() {
        //要加锁
        if(registry== null){
            synchronized (this){
                if(registry== null){
                    registry = getCreateRegistry();
                }
            }
        }
        return registry;
    }
    private Registry getCreateRegistry(){
        switch (registrytype){
            case "zookeeper":
                return new ZooKeeperRegistry(registryAddress, Constant.ZK_SESSION_TIMEOUT);
            //后续在扩展
            case "nacos":
            case "etcd":
            default:
                throw new IllegalArgumentException("Unsupported registry type: " + registrytype);
        }
    }
}
