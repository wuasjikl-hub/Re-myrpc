package com.myrpc.leafe.config;

import com.myrpc.leafe.LoadBalancer.LoadBalanceFactory;
import com.myrpc.leafe.LoadBalancer.LoadBalancer;
import com.myrpc.leafe.enumeration.CompressorType;
import com.myrpc.leafe.enumeration.LoadBalancerType;
import com.myrpc.leafe.enumeration.SerializerType;
import com.myrpc.leafe.packet.client.rpcRequestPacket;
import com.myrpc.leafe.protection.CircuitBreaker;
import com.myrpc.leafe.protection.RateLimiter;
import com.myrpc.leafe.res.HeartBeatResult;
import com.myrpc.leafe.utils.IdGenerator;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Data
public class Configration {
    //分组
    private String Groupinfo="default";
    //服务提供方默认端口
    private int Port=8002;
    //序列化类型
    private String serializeType= SerializerType.SERIALIZERTYPE_HESSION.getType();
    //压缩类型
    private String compressType= CompressorType.COMPRESSTYPE_GZIP.getType();
    //balancername todo
    private String balancerName= LoadBalancerType.LoadBalancerType_ConsistentHash.getType();
    //应用名
    private String application="default";
    //注册中心类型
    private String registryType="zookeeper";
    //注册中心地址
    private String registryAddress="localhost:2181";
    //id生成器 全局唯一
    public IdGenerator idGenerator = new IdGenerator(1, 1);
    //注册中心
    private RegistryConfig registryConfig=new RegistryConfig(registryType,registryAddress);

    //以上都可以从配置文件中配置
//--------------------------------------------------------------------
// 创建一个包含单个线程的调度线程池
    ScheduledExecutorService singleThreadScheduler = Executors.newSingleThreadScheduledExecutor();

    // 创建一个包含固定数量线程的调度线程池
    ScheduledExecutorService multiThreadScheduler = Executors.newScheduledThreadPool(4);
    // 协议
    private ProtocolConfig protocol;
    //public static final Map<Long, CompletableFuture<Object>> PENDING_REQUESTS = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<Object>> PNDING_REQUESTS = new ConcurrentHashMap<>();
    //心跳响应结果缓存
    private final Map<Long, CompletableFuture<HeartBeatResult>> HEARTBEAT_PENDING_REQUESTS = new ConcurrentHashMap<>();
    //存储当前线程请求的rpcRequestPacket
    private final ThreadLocal<rpcRequestPacket> REQUEST_THREAD_LOCAL = new ThreadLocal<>();
    //存储服务名和服务配置的cache
    private final Map<String, ServiceConfig<?>> SERVER_MAP = new ConcurrentHashMap<>();
    //响应时间的cache
    private volatile ConcurrentSkipListMap<Long, List<Channel>> ANSWER_CHANNEL_CACHE = new ConcurrentSkipListMap<>();

    private final Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);
    private final Map<String, LoadBalancer> loadBalancerCache = new ConcurrentHashMap<>();
    // 记录每个服务使用了哪些负载均衡器
    private final Map<String, String> serviceToLoadBalancers = new ConcurrentHashMap<>();
    //缓存与服务提供者对应的限流器
    private final Map<SocketAddress, RateLimiter> SERVICE_TO_RATELIMITER = new ConcurrentHashMap<>();
    //缓存与服务提供者对应的熔断器
    private final Map<InetSocketAddress, CircuitBreaker> SERVICE_TO_CIRCUITBREAKER = new ConcurrentHashMap<>();
    public Configration(){}
    // 工厂方法

    public static void main(String[] args) {
        Configration configration = new Configration();
    }
    public LoadBalancer getLoadBalancer(String ServiceName) {
        LoadBalancer loadBalancer = loadBalancerCache.computeIfAbsent(balancerName,
                name -> LoadBalanceFactory.getLoadBalancer(name).getObject());
        serviceToLoadBalancers.put(ServiceName, balancerName);
        return loadBalancer;
    }
    public void reLoadBalance(String serviceName, List<InetSocketAddress> addresses) {
        String balancerName = serviceToLoadBalancers.get(serviceName);
        if (balancerName != null) {
            LoadBalancer lb = loadBalancerCache.get(balancerName);
            if (lb != null) {
                lb.reLoadBalance(serviceName, addresses);
            }
        } else {
            // 如果没有记录，可能是服务还没有被引用过，或者没有通过getLoadBalancerForService方法获取
            // 可以选择忽略，或者遍历所有负载均衡器进行更新（保持兼容性，但不推荐）
            // 这里建议日志警告
            log.warn("服务 {} 没有对应的负载均衡器映射，无法更新", serviceName);
        }
    }
    public void cleanup() {
        CHANNEL_CACHE.clear();
        PNDING_REQUESTS.clear();
        HEARTBEAT_PENDING_REQUESTS.clear();
        REQUEST_THREAD_LOCAL.remove();
        SERVER_MAP.clear();
        ANSWER_CHANNEL_CACHE.clear();
    }


    public ScheduledExecutorService getTimeoutScheduler() {
        return this.singleThreadScheduler;
    }
}
