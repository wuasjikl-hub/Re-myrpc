package com.myrpc.leafe.proxy;

import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.config.ReferenceConfig;
import com.myrpc.leafe.enumeration.CompressorType;
import com.myrpc.leafe.enumeration.SerializerType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class proxyFactory {
    public static Map<Class<?>,Object> proxyMap = new ConcurrentHashMap<>(8);
    public static <T> T getproxy(Class<T> clazz){
        return (T)proxyMap.computeIfAbsent(clazz, k -> {
            ReferenceConfig<T> referenceConfig = new ReferenceConfig();
            referenceConfig.setInterface(clazz);
            MyRpcBootstrap.getInstance()
                    .application("myrpc-consumer")  //应用名
                    .registry()//注册中心
                    .compress(CompressorType.COMPRESSTYPE_GZIP.getType())
                    .serialize(SerializerType.SERIALIZERTYPE_HESSION.getType())
                    .Group("Primary")
                    .reference(referenceConfig);    //引用服务
            //Object object = MyRpcBootstrap.getInstance().getReferenceConfig().get();//获取代理对象
            return (T) referenceConfig.get();

        });
    }
}
