package com.myrpc.leafe;

public class ConsumerApplication {
    public static void main(String[] args) {
        ReferenceConfig<GreetingService> referenceConfig = new ReferenceConfig();
        referenceConfig.setInterface(GreetingService.class);
        MyRpcBootstrap.getInstance()
                .application("myrpc-consumer")  //应用名
                .registry(new RegistryConfig("zookeeper","127.0.0.1:2181"))//注册中心
                .reference(referenceConfig);    //引用服务
        GreetingService service = referenceConfig.get();//获取代理对象
        service.hello("leafe");
    }
}
