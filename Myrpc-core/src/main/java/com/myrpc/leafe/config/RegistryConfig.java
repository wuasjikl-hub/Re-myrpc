package com.myrpc.leafe.config;

import com.myrpc.leafe.Registry.Registry;
import com.myrpc.leafe.Registry.impl.ZooKeeperRegistry;
import com.myrpc.leafe.common.Constant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        if (registry == null) {
            synchronized (this) { // 添加同步，防止多线程问题
                if (registry == null) {
                    try {
                        registry = getCreateRegistry();
                        log.info("成功创建注册中心实例: {}", registrytype);
                    } catch (Exception e) {
                        log.error("创建注册中心实例失败", e);
                        throw new RuntimeException("无法创建注册中心实例", e);
                    }
                }
            }
        }
        return registry;
    }

    private Registry getCreateRegistry() {
        // 添加参数验证
        if (registrytype == null || registrytype.trim().isEmpty()) {
            throw new IllegalArgumentException("注册中心类型不能为空");
        }
        if (registryAddress == null || registryAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("注册中心地址不能为空");
        }

        switch (registrytype.toLowerCase()) {
            case "zookeeper":
                try {
                    return new ZooKeeperRegistry(registryAddress, Constant.ZK_SESSION_TIMEOUT);
                } catch (Exception e) {
                    log.error("创建 ZooKeeper 注册中心失败", e);
                    throw new RuntimeException("无法连接 ZooKeeper: " + registryAddress, e);
                }
            case "nacos":
            case "etcd":
            default:
                throw new IllegalArgumentException("不支持的注册中心类型: " + registrytype);
        }
    }
}
