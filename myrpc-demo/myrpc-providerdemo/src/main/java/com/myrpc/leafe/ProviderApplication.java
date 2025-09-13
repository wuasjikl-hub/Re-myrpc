package com.myrpc.leafe;

import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.config.ProtocolConfig;
import com.myrpc.leafe.config.ServiceConfig;
import com.myrpc.leafe.enumeration.CompressorType;
import com.myrpc.leafe.enumeration.SerializerType;
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
                .registry()   //注册中心
                .protocol(new ProtocolConfig("jdk"))   //协议
                .compress(CompressorType.COMPRESSTYPE_GZIP.getType())
                .serialize(SerializerType.SERIALIZERTYPE_HESSION.getType())
                //.service(serviceConfig)    // 发布服务
                .scan("com.myrpc.leafe.impl")//服务实例一定要有无参构造函数否则无法创建实例
                .start();
    }
}
