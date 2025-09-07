package com.myrpc.leafe.LoadBalancer.impl;

import com.myrpc.leafe.LoadBalancer.AbstractLoadBalancer;
import com.myrpc.leafe.LoadBalancer.Selector;
import com.myrpc.leafe.LoadBalancer.UpdatableSelector;
import com.myrpc.leafe.exceptions.LoadBalanceException;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 轮询负载均衡
 * 维护一个内部类RoundRobinSelector用来实现轮询算法
 */
@Slf4j
public class RoundRobinLoadBalancer extends AbstractLoadBalancer {
    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceAddresses) {
        return new RoundRobinSelector(serviceAddresses);
    }
    //对外暴露一个可以获取Selector的接口
    private static class RoundRobinSelector implements UpdatableSelector {
        private final AtomicInteger index;
        private volatile List<InetSocketAddress> serviceAddresses;
        //添加读写锁确保线程安全          读锁是共享的，写锁是排他的
        private final ReadWriteLock lock=new ReentrantReadWriteLock();
        public RoundRobinSelector( List<InetSocketAddress> serviceAddresses) {
            this.index = new AtomicInteger(0);
            this.serviceAddresses = serviceAddresses;
        }
        //轮询算法的实现
        @Override
        public InetSocketAddress selectServiceAddress() {
            lock.readLock().lock();
            try {
                List<InetSocketAddress> currentAddresses = this.serviceAddresses;
                if (currentAddresses == null || currentAddresses.size() == 0) {
                    log.error("服务列表为空");
                    throw new LoadBalanceException("RoundRobinSelector异常：服务列表为空");
                }
                // 使用取模运算实现轮询
                int currentIndex = index.getAndUpdate(i -> (i + 1) % currentAddresses.size());
                log.info("currentIndex={}", currentIndex);
                return currentAddresses.get(currentIndex);
            }finally {
                lock.readLock().unlock();
            }
        }
        @Override
        public void updateAddresses(List<InetSocketAddress> addresses) {
            lock.writeLock().lock();
            try {
                if (addresses == null || addresses.isEmpty()) {
                    log.warn("尝试使用空地址列表更新Selector");
                    return;
                }
                this.serviceAddresses = new CopyOnWriteArrayList<>(addresses);
                // 重置索引，避免越界(即每次更新列表后都从列表的第一个元素开始)
                index.set(0);
                if (log.isDebugEnabled()) {
                    log.debug("Selector服务列表已更新，当前服务数量: {}", addresses.size());
                }
            }finally {
                lock.writeLock().unlock();
            }

        }
    }
}
