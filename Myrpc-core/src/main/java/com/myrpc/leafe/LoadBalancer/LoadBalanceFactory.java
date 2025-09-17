package com.myrpc.leafe.LoadBalancer;

import com.myrpc.leafe.LoadBalancer.impl.ConsistentHashLoadBalancer;
import com.myrpc.leafe.LoadBalancer.impl.MinimumResponseTimeLoadBalancer;
import com.myrpc.leafe.LoadBalancer.impl.RoundRobinLoadBalancer;
import com.myrpc.leafe.enumeration.LoadBalancerType;
import com.myrpc.leafe.exceptions.SerializerNOTFOUND;
import com.myrpc.leafe.wrapper.ObjectWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
@Slf4j
public class LoadBalanceFactory {
    //维护一个cache缓存器
    private static final ConcurrentHashMap<String, ObjectWrapper<LoadBalancer>> LoadBalancer_CACHE = new ConcurrentHashMap<>(16);
    static{
        ObjectWrapper<LoadBalancer> consistentHashLoadBalancerObjectWrapper=new ObjectWrapper<>(LoadBalancerType.LoadBalancerType_ConsistentHash.getCode(),LoadBalancerType.LoadBalancerType_ConsistentHash.getType(), new ConsistentHashLoadBalancer());
        ObjectWrapper<LoadBalancer> roundRobinLoadBalancerObjectWrapper=new ObjectWrapper<>(LoadBalancerType.LoadBalancerType_RoundRobin.getCode(),LoadBalancerType.LoadBalancerType_RoundRobin.getType(), new RoundRobinLoadBalancer());
        ObjectWrapper<LoadBalancer> minimumResponseTimeLoadBalancerObjectWrapper=new ObjectWrapper<>(LoadBalancerType.LoadBalancerType_MinimumResponseTime.getCode(),LoadBalancerType.LoadBalancerType_MinimumResponseTime.getType(), new MinimumResponseTimeLoadBalancer());
        LoadBalancer_CACHE.put(consistentHashLoadBalancerObjectWrapper.getName(), consistentHashLoadBalancerObjectWrapper);
        LoadBalancer_CACHE.put(roundRobinLoadBalancerObjectWrapper.getName(), roundRobinLoadBalancerObjectWrapper);
        LoadBalancer_CACHE.put(minimumResponseTimeLoadBalancerObjectWrapper.getName(), minimumResponseTimeLoadBalancerObjectWrapper);
    }
    //对外暴露一个获取序列化器的方法
    public static ObjectWrapper<LoadBalancer> getLoadBalancer(String name) {
        ObjectWrapper<LoadBalancer> loadBalancerObjectWrapper = LoadBalancer_CACHE.get(name);
        if(loadBalancerObjectWrapper == null){
            log.error("未找到该负载均衡器");
            throw new SerializerNOTFOUND("未找到该序列化器");
        }
        return loadBalancerObjectWrapper;
    }

    //暴露添加序列化器的方法
    public static void addLoadBalancer(ObjectWrapper<LoadBalancer> LoadBalance) {
        if(LoadBalance == null){
            log.error("添加的负载均衡器不能为空");
            return;
        }
        LoadBalancer_CACHE.computeIfAbsent(LoadBalance.getName(), k->LoadBalance);
    }
}
