package com.myrpc.leafe;

import com.myrpc.leafe.Registry.registry;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MyRpcBootstrap{
    /**
     * 获取实例
     *
     * @return
     */
    private static MyRpcBootstrap instance = new MyRpcBootstrap();
    /**
     * 应用名
     */
    private String application;
    // 注册中心配置
    private RegistryConfig registryConfig;
    // 协议
    private ProtocolConfig protocol;
    // 注册中心
    private registry localregistry;

    // 维护已经发布且暴露的服务列表 key-> interface的全限定名  value -> ServiceConfig
    //这里不能用类级别的泛型参数，因为这个类是静态的要用通配符
    private static final Map<String, ServiceConfig<?>> SERVER_MAP = new ConcurrentHashMap<>();

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
        this.application = application;
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
        this.registryConfig = registryConfig;
        if(log.isDebugEnabled()){
            log.debug("当前注册中心为:{}",registryConfig.getRegistrytype());
        }
        localregistry=registryConfig.getRegistry();
        return this;
    }
    /**
     * 注册服务提供者
     * @return
     */
    public MyRpcBootstrap protocol(ProtocolConfig protocol) {
        this.protocol = protocol;
        if(log.isDebugEnabled()){
            log.debug("当前协议:{}",protocol.toString());
        }
        return this;
    }
    /**
     * 注册服务:将服务的接口以及实现类注册发布到服务注册中心
     * @return
     */
    public <T>MyRpcBootstrap service(ServiceConfig<T> serviceConfig) {
        //todo 等会放到共同的配置类中
        localregistry.register(serviceConfig);
        if(log.isDebugEnabled()){
            log.debug("服务:{}已经被注册",serviceConfig.getInterface().getName());
        }
        //当客户端通过接口名和参数列表发起调用时，服务端要调用对应的服务实现类
        //我们还要维护一个服务名和服务的实现类之间的映射关系
        SERVER_MAP.put(serviceConfig.getInterface().getName(),serviceConfig);
        return this;
    }
    /**
     * 注册多个服务
     * @return
     */
    public <T>MyRpcBootstrap service(List<ServiceConfig<T>> serviceConfigList){
        serviceConfigList.forEach(serviceConfig -> {
            localregistry.register(serviceConfig);
            if(log.isDebugEnabled()){
                log.debug("服务:{}已经被注册",serviceConfig.getInterface().getName());
            }
            SERVER_MAP.put(serviceConfig.getInterface().getName(),serviceConfig);
        });
        return this;
    }

    public void start() {
        try {
            Thread.sleep(1000000000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public <T>MyRpcBootstrap reference(ReferenceConfig<T> referenceConfig) {
        referenceConfig.setAnRegistry(localregistry);
        return this;
    }
}
