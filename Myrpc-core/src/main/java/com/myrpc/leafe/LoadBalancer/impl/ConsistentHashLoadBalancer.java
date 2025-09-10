package com.myrpc.leafe.LoadBalancer.impl;

import com.myrpc.leafe.LoadBalancer.AbstractLoadBalancer;
import com.myrpc.leafe.LoadBalancer.Selector;
import com.myrpc.leafe.LoadBalancer.UpdatableSelector;
import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.common.Constant;
import com.myrpc.leafe.packet.client.rpcRequestPacket;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class ConsistentHashLoadBalancer extends AbstractLoadBalancer {
    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceAddresses) {
        return new ConsistentHashSelector(serviceAddresses, Constant.VIRTUAL_NODE_NUM);
    }
    private static class ConsistentHashSelector implements UpdatableSelector {
        //维护一个hash环用来存储服务节点
        private SortedMap<Long,InetSocketAddress> hashCircle=new TreeMap<>();
        //虚拟节点的个数 todo 为了解决单个节点压力过大的问题
        private final int virtualNodes;
        private volatile List<InetSocketAddress> serviceAddresses;
        private final ReadWriteLock lock=new ReentrantReadWriteLock();
        public ConsistentHashSelector(List<InetSocketAddress> serviceAddresses, int virtualNodes) {
            this.virtualNodes = virtualNodes;
            this.serviceAddresses = serviceAddresses;
            //将对应的虚拟节点添加到hash环中
                addNodeToHashCircle();
        }

        private void addNodeToHashCircle() {
            lock.writeLock().lock();
            try {
                //为每个节点生成匹配的虚拟节点
                for (InetSocketAddress serviceAddress : this.serviceAddresses) {
                    for (int i = 0; i < virtualNodes; i++) {
                        Long hash = hash(serviceAddress.toString() + "-" + i);
                        hashCircle.put(hash, serviceAddress);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("为{}添加虚拟节点", serviceAddress);
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }

        }
        //移除节点
        private void removeNodeFromHashCircle(InetSocketAddress serviceAddress) {
            for (int i = 0; i < virtualNodes; i++) {
                Long hash = hash(serviceAddress.toString()+"-"+ i);
                hashCircle.remove(hash);
            }
            if(log.isDebugEnabled()){
                log.debug("为{}删除虚拟节点", serviceAddress);
            }
        }
        private static final ThreadLocal<MessageDigest> MD5_DIGEST = ThreadLocal.withInitial(() -> {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        });
        private Long hash(String str){
            MessageDigest md5;
            md5 = MD5_DIGEST.get();
            //通过MD5计算的是一个128位
            byte[] digest = md5.digest(str.getBytes(StandardCharsets.UTF_8));
            //先整型提升并清除前24位后左移再或运算
            //如果没有整型提升,由于符号扩展（sign extension），负数的符号位会被复制到所有的高位
            //eg. byte b=-86的二进制表示为10101010
            //直接左移8位，int result = b << 8; // 期望: 1010101000000000 (43520)
            //                     // 实际: 11111111111111111010101000000000 (-22016)
            long res = ((long)(digest[0] & 0xFF) << 56) |
                    ((long)(digest[1] & 0xFF) << 48) |
                    ((long)(digest[2] & 0xFF) << 40) |
                    ((long)(digest[3] & 0xFF) << 32) |
                    ((long)(digest[4] & 0xFF) << 24) |
                    ((long)(digest[5] & 0xFF) << 16) |
                    ((long)(digest[6] & 0xFF) << 8) |
                    ((long)(digest[7] & 0xFF));
            log.info("res:"+res);
            return res;
        }

        @Override
        public InetSocketAddress selectServiceAddress() {
            lock.readLock().lock();
            try {
                //hash环已经构建完成,接下来我们通过请求id获取hash值
                rpcRequestPacket rpcRequestPacket = MyRpcBootstrap.REQUEST_THREAD_LOCAL.get();
                Long hash = hash(Long.toString(rpcRequestPacket.getRequestId()));
                if (!hashCircle.containsKey(hash)) {
                    //找到距离计算出来的hash的最近服务节点
                    //该方法返回比该hash值大的服务节点
                    SortedMap<Long, InetSocketAddress> tailMap = hashCircle.tailMap(hash);
                    //有比该hash值大的服务节点就选择该服务节点没有就选择第一个红黑树最小的节点
                    hash = tailMap.isEmpty() ? hashCircle.firstKey() : tailMap.firstKey();
                }
                return hashCircle.get(hash);
            }finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public void updateAddresses(List<InetSocketAddress> addresses) {
            lock.writeLock().lock();
            try {
                if (addresses == null || addresses.isEmpty()) {
                    {
                        log.warn("尝试使用空地址列表更新Selector");
                    }
                } else {
                    hashCircle.clear();
                    this.serviceAddresses = new CopyOnWriteArrayList<>(addresses);
                    //将对应的虚拟节点添加到hash环中
                    addNodeToHashCircle();

                    if (log.isDebugEnabled()) {
                        log.debug("Selector服务列表已更新，当前服务数量: {}", addresses.size());
                    }
                }
            }finally {
                lock.writeLock().unlock();
            }
        }
    }
}
