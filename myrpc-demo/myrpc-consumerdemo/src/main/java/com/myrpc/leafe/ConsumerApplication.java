package com.myrpc.leafe;

import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.config.ReferenceConfig;
import com.myrpc.leafe.config.RegistryConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsumerApplication {
    public static void main(String[] args) {
        ReferenceConfig<GreetingService> referenceConfig = new ReferenceConfig();
        referenceConfig.setInterface(GreetingService.class);
        MyRpcBootstrap.getInstance()
                .application("myrpc-consumer")  //应用名
                .registry(new RegistryConfig("zookeeper","127.0.0.1:2181"))//注册中心
                .reference(referenceConfig);    //引用服务
        GreetingService service = referenceConfig.get();//获取代理对象
        for (int i = 0; i < 4; i++) {
            log.info("service.hello()"+service.hello("leafe"));
        }
    }
}
