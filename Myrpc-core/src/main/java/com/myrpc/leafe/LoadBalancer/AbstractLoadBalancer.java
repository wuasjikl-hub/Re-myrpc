package com.myrpc.leafe.LoadBalancer;

import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.exceptions.LoadBalanceException;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 抽象负载均衡类
 * 由于服务列表可能会动态变化，所以抽象类中定义了抽象方法，让子类来实现
 * 我们有两种办法1.每次服务列表动态变化时都新建Selector 2.更新Selector内部的服务地址列表。
 */
@Slf4j
public abstract class AbstractLoadBalancer implements LoadBalancer{

     //维护一个selector缓存
    private Map<String,Selector> SelectorCache=new ConcurrentHashMap<>(8);

    @Override
    public InetSocketAddress selectServiceAddress(String serviceName) {
        log.info("Servicename：{}",serviceName);
        log.info("SelectorCache：{}",SelectorCache);
        Selector selector=SelectorCache.computeIfAbsent(serviceName
                ,this::createSelector
        );
        return selector.selectServiceAddress();
    }
    private Selector createSelector(String serviceName) {
        // 获取服务地址列表
        List<InetSocketAddress> addresses = discoverServices(serviceName);

        if (addresses == null || addresses.isEmpty()) {
            throw new LoadBalanceException("找不到服务" + serviceName);
        }
        Selector selector = getSelector(addresses);
        return selector;
    }
    //当服务列表更新时，后续用watcher机制调用此方法更新selector内维护的服务列表
    @Override
    public void reLoadBalance(String serviceName, List<InetSocketAddress> addresses) {
        Selector selector = SelectorCache.get(serviceName);
        if(selector!=null&&selector instanceof UpdatableSelector updatableSelector){
            updatableSelector.updateAddresses(addresses);
        }else{
            SelectorCache.put(serviceName,getSelector(addresses));
        }
    }
    private List<InetSocketAddress> discoverServices(String serviceName) {
        return MyRpcBootstrap.getInstance()
                .getregistryConfig()
                .getRegistry()
                .discovery(serviceName);
    }
    protected abstract Selector getSelector(List<InetSocketAddress> serviceAddresses);
}
