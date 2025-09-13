package com.myrpc.leafe.watcher;

import com.myrpc.leafe.Registry.Registry;
import com.myrpc.leafe.bootatrap.Initializer.NettyBootstrapInitializer;
import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ServiceUpAndDownWatcher implements Watcher {
    // 锁包装类，包含锁对象和引用计数
    private static class LockWrapper {
        final Object lock = new Object();
        int refCount = 0;
    }
    private final Map<String,LockWrapper> serviceLock=new ConcurrentHashMap<>();
    private final Map<InetSocketAddress, LockWrapper> addressLocks = new ConcurrentHashMap<>();

    @Override
    public void process(WatchedEvent watchedEvent) {
        if(watchedEvent.getType() == Event.EventType.NodeChildrenChanged){
            //子节点改变
            if(log.isDebugEnabled()){
                log.debug("服务节点发生改变:-----------");
            }
            String path[] = watchedEvent.getPath().split("/");
            String serviceName = path[path.length-1];
            //尝试获取或创建锁对象
            LockWrapper lockWrapper = serviceLock.computeIfAbsent(serviceName, k -> new LockWrapper());
            synchronized (lockWrapper){
                lockWrapper.refCount++;//引用计数加1
            }
            try {
                synchronized (lockWrapper.lock){
                    Registry registry = MyRpcBootstrap.getInstance()
                            .getConfigration().getRegistryConfig().getRegistry();
                    List<InetSocketAddress> addresses = registry.discovery(serviceName);
                    for (InetSocketAddress address : addresses) {
                        handleAddress( address);
                    }
                    cleanupInvalidAddresses(addresses);
                    //打印一下cache
                    if(log.isDebugEnabled()){
                        MyRpcBootstrap.getInstance().getConfigration().getCHANNEL_CACHE().forEach((adress, channel) -> {
                            log.info("服务提供者: {}", channel.remoteAddress());
                        });
                    }
                    //刷新负载均衡
                    MyRpcBootstrap.getInstance().getConfigration().reLoadBalance(serviceName,addresses);
                }
            }finally {
                synchronized (lockWrapper){
                    lockWrapper.refCount--;
                    if (lockWrapper.refCount == 0) {
                        serviceLock.remove(serviceName);
                    }
                }
            }
        }
    }
    private void handleAddress(InetSocketAddress address) {
        // 获取地址级别的锁
        LockWrapper addressLock = addressLocks.computeIfAbsent(address, a -> new LockWrapper());
        synchronized (addressLock){
            addressLock.refCount++;
        }
        try {
            synchronized (addressLock.lock) {
                MyRpcBootstrap.getInstance().getConfigration().getCHANNEL_CACHE().compute(address, (key, existingChannel) -> {
                    if (existingChannel != null && existingChannel.isActive()) {
                        return existingChannel;
                    }
                    if (existingChannel != null) {
                        existingChannel.close();
                        return null;
                    }
                    //创建新连接
                    try {
                        ChannelFuture channelFuture = NettyBootstrapInitializer.getInstance()
                                .getBootstrap().connect(address.getHostName(), address.getPort());
                        // 同步等待连接结果
                        if (channelFuture.await(5, TimeUnit.SECONDS)) {
                            if (channelFuture.isSuccess()) {
                                return channelFuture.channel();
                            } else {
                                throw new RuntimeException("连接失败", channelFuture.cause());
                            }
                        } else {
                            throw new RuntimeException("连接超时");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("连接被中断", e);
                    } catch (Exception e) {
                        throw new RuntimeException("连接异常", e);
                    }
                });
            }
        } finally {
            synchronized (addressLock){
                addressLock.refCount--;
                if (addressLock.refCount == 0) {
                    addressLocks.remove(address);
                }
            }
         }
    }

    private void cleanupInvalidAddresses(List<InetSocketAddress>  addresses){
        Set<InetSocketAddress> validSet = new HashSet<>(addresses);
        // 清理不再属于当前服务的地址
        Iterator<Map.Entry<InetSocketAddress, Channel>> iterator = MyRpcBootstrap.getInstance().getConfigration().getCHANNEL_CACHE()
                .entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<InetSocketAddress, Channel> entry = iterator.next();
            InetSocketAddress address = entry.getKey();
            Channel channel = entry.getValue();
            // 如果是其他服务的地址如果是不活跃的，则关闭连接并删除缓存
            if (!validSet.contains(address) && channel != null&& !channel.isActive()) {
                LockWrapper addressLock = addressLocks.computeIfAbsent(address, a -> new LockWrapper());
                synchronized (addressLock) {
                    addressLock.refCount++;
                }
                try {
                    synchronized (addressLock.lock) {
                        if (channel.isActive()) {
                            channel.close();
                        }
                        iterator.remove();
                    }
                } finally {
                    synchronized (addressLock) {
                        addressLock.refCount--;
                        if (addressLock.refCount == 0) {
                            addressLocks.remove(address);
                        }
                    }
                }
            }
        }
    }
}
