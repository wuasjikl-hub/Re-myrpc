package com.myrpc.leafe.Resolver;

import com.myrpc.leafe.LoadBalancer.LoadBalanceFactory;
import com.myrpc.leafe.LoadBalancer.LoadBalancer;
import com.myrpc.leafe.Serialize.Serializer;
import com.myrpc.leafe.Serialize.SerializerFactory;
import com.myrpc.leafe.compress.CompressFactory;
import com.myrpc.leafe.compress.Compressor;
import com.myrpc.leafe.spi.SpiHandler;
import com.myrpc.leafe.wrapper.ObjectWrapper;

import java.util.List;

public class SpiResolver {
    public void loadFromSpi(){
        // 加载序列化器
        List<ObjectWrapper<Serializer>> list = SpiHandler.getList(Serializer.class);
        //将其放入工厂
        list.forEach(SerializerFactory::addSerializer);
        // 加载压缩工具器
        List<ObjectWrapper<Compressor>> compressorList = SpiHandler.getList(Compressor.class);
        //将其放入工厂
        compressorList.forEach(CompressFactory::addCompressor);
        // 加载负载均衡器
        List<ObjectWrapper<LoadBalancer>> loadBalancerlist = SpiHandler.getList(LoadBalancer.class);
        //将其放入工厂
        loadBalancerlist.forEach(LoadBalanceFactory::addLoadBalancer);
    }
}
