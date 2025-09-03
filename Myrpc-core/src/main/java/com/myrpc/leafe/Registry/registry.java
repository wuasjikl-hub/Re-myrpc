package com.myrpc.leafe.Registry;

import com.myrpc.leafe.ServiceConfig;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @Description:
 * @Author: leafe
 * @Date: 2022/3/27
 * @Version: v1.0
 * 维护了服务注册方法和服务发现方法
 */
public interface registry {
    /**
     * 注册服务
     * @param serviceConfig
     */
    <T>void register(ServiceConfig<T> serviceConfig);
    List<InetSocketAddress> discovery(String serviceName);
}
