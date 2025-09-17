package com.myrpc.leafe.LoadBalancer.impl;

import com.myrpc.leafe.LoadBalancer.AbstractLoadBalancer;
import com.myrpc.leafe.LoadBalancer.Selector;
import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class MinimumResponseTimeLoadBalancer extends AbstractLoadBalancer {
    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceAddresses) {
        return new MinimumResponseTimeSelector();
    }
    private static class MinimumResponseTimeSelector implements Selector{
        private final ThreadLocalRandom random = ThreadLocalRandom.current();
        @Override
        public InetSocketAddress selectServiceAddress() {
            ConcurrentSkipListMap<Long, List<Channel>> cache = MyRpcBootstrap.getInstance().getConfigration().getANSWER_CHANNEL_CACHE();
            // 使用迭代器而不是entrySet()，可以更灵活地控制遍历
            Iterator<Map.Entry<Long, List<Channel>>> iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, List<Channel>> entry = iterator.next();
                List<Channel> channels = entry.getValue();
                // 预先检查列表是否为空，避免不必要的流操作
                if (channels.isEmpty()) {
                    // 可选：移除空列表的条目，减少后续遍历
                    iterator.remove();
                    continue;
                }
                // 使用并行流处理大型列表（如果列表很大）
                List<Channel> activeChannels = channels.parallelStream()
                        .filter(Channel::isActive)
                        .collect(Collectors.toList());
                if (!activeChannels.isEmpty()) {
                    // 使用ThreadLocalRandom提高性能
                    Channel channel = activeChannels.get(random.nextInt(activeChannels.size()));
                    return (InetSocketAddress) channel.remoteAddress();
                } else {// 没有活跃的Channel
                    // 可选：移除没有活跃Channel的条目
                    iterator.remove();
                }
             }
// 没有可用的连接就返回null
            throw new RuntimeException("MinimumResponseTime没有可用的连接");
        }
    }
}
