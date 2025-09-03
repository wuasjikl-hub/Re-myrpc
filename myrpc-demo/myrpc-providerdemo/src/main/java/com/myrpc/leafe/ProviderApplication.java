package com.myrpc.leafe;

import com.myrpc.leafe.impl.GreetingServiceimpl;
import com.myrpc.leafe.impl.HelloServiceimpl;

public class ProviderApplication {
    public static void main(String[] args) {
        //定义具体的服务
        ServiceConfig<GreetingService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(GreetingService.class);
        serviceConfig.setRef(new GreetingServiceimpl());

        ServiceConfig<HelloService> helloServiceConfig = new ServiceConfig<>();
        helloServiceConfig.setInterface(HelloService.class);
        helloServiceConfig.setRef(new HelloServiceimpl());




        //启动rpc服务
        MyRpcBootstrap.getInstance()
                .application("myrpc-provider")//应用名
                .registry(new RegistryConfig("zookeeper", "127.0.0.1:2181"))   //注册中心
                .protocol(new ProtocolConfig("jdk"))   //协议
                .service(serviceConfig)    // 服务
                .start();
    }
}
