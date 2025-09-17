package com.myrpc.leafe;

import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.config.ReferenceConfig;
import com.myrpc.leafe.enumeration.CompressorType;
import com.myrpc.leafe.enumeration.SerializerType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsumerApplication {
    public static void main(String[] args) {
        ReferenceConfig<GreetingService> referenceConfig = new ReferenceConfig();
        referenceConfig.setInterface(GreetingService.class);
        MyRpcBootstrap.getInstance()
                .application("myrpc-consumer")  //应用名
                .registry()//注册中心
                .compress(CompressorType.COMPRESSTYPE_GZIP.getType())
                .serialize(SerializerType.SERIALIZERTYPE_HESSION.getType())
                .Group("Primary")
                .reference(referenceConfig);    //引用服务
        //Object object = MyRpcBootstrap.getInstance().getReferenceConfig().get();//获取代理对象
        GreetingService greetingService = referenceConfig.get();
        log.info("==========================================================>");
        for (int i = 0; i < 5; i++) {
            log.info("service.hello()"+greetingService.hello("leafe"));
        }
        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }
}
