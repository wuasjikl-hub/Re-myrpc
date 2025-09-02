package com.myrpc.leafe;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyRpcBootstrap{
    /**
     * 获取实例
     *
     * @return
     */
    private static MyRpcBootstrap instance = new MyRpcBootstrap();
    private MyRpcBootstrap() {
        System.out.println("MyRpcBootstrap init");
    }
    /**
     * 设置应用名
     *
     * @param application
     * @return
     */
    public MyRpcBootstrap application(String application){
        return this;
    }
    public static MyRpcBootstrap getInstance() {
        return instance;
    }
    /**
     * 注册服务注册中心(zookeeper,dubbo,nacos...)
     * @return
     */
    public MyRpcBootstrap registry(RegistryConfig registryConfig) {
        if(log.isDebugEnabled()){
            log.debug("当前注册中心为:{}",registryConfig.getRegistrytype());
        }
        return this;
    }
    /**
     * 注册服务提供者
     * @return
     */
    public MyRpcBootstrap protocol(ProtocolConfig protocol) {
        if(log.isDebugEnabled()){
            log.debug("当前协议:{}",protocol.toString());
        }
        return this;
    }
    /**
     * 注册服务:将服务的接口以及实现类关系映射起来
     * @return
     */
    public <T>MyRpcBootstrap service(ServiceConfig<T> serviceConfig) {
        if(log.isDebugEnabled()){
            log.debug("服务:{}已经被注册",serviceConfig.getInterface().getName());
        }
        return this;
    }

    public void start() {

    }

    public <T>MyRpcBootstrap reference(ReferenceConfig<T> referenceConfig) {
        return this;
    }
}
