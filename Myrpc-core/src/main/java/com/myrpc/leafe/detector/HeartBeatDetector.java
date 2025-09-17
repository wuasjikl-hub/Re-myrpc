package com.myrpc.leafe.detector;

import com.myrpc.leafe.Registry.Registry;
import com.myrpc.leafe.Serialize.SerializerFactory;
import com.myrpc.leafe.bootatrap.Initializer.NettyBootstrapInitializer;
import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.compress.CompressFactory;
import com.myrpc.leafe.exceptions.NotFoundedEnableNodeException;
import com.myrpc.leafe.packet.heartBeat.heartBeatPacket;
import com.myrpc.leafe.res.HeartBeatResult;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class HeartBeatDetector {
    //private static volatile Timer timer;
    // 替换Timer为ScheduledExecutorService
    private static volatile ScheduledExecutorService scheduler;
    private static final Object lock = new Object();
    //设置线程池
    //这里用缓存线程池当执行耗时任务时候，线程池会自动创建线程，
    // 当线程池中的线程数达到最大值时，会自动创建新的线程，
    //也可用固定大小线程池 FixedThreadPool
    //todo :注意线程的核心数如果大于最大线程数就会报错
    private static final int MAX_POOL_SIZE = 20;
    private static final int CORE_POOL_SIZE;
    static {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        // 确保核心线程数至少为1，且不超过最大线程数
        CORE_POOL_SIZE = Math.max(1, Math.min(availableProcessors * 2, MAX_POOL_SIZE));
    }
//    private static final ExecutorService HEARTBEAT_EXECUTOR = Executors.newCachedThreadPool(
//            new ThreadFactory() {
//                private final AtomicInteger threadCount = new AtomicInteger(1);
//                @Override
//                public Thread newThread(Runnable r) {
//                    Thread thread = new Thread(r, "HeartBeat-Thread-" + threadCount.getAndIncrement());
//                    thread.setDaemon(true); // 设置为守护线程也就是当主线程退出时，守护线程会自动停止不会阻止主线程推退出
//                    return thread;
//                }
//            }
//
//    );
    //就是说当线程数小于核心线程数，则创建新线程执行任务；当线程数达到核心线程数且队列已满且当前线程数小于最大线程数则创建新线程执行任务；
    // 如果队列已满且当前线程数达到最大线程数，则根据拒绝策略处理
private static final ExecutorService HEARTBEAT_EXECUTOR = new ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAX_POOL_SIZE,
        60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(100),
        new ThreadFactory() {
            private final AtomicInteger threadCount = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "HeartBeat-Thread-" + threadCount.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        },
        new ThreadPoolExecutor.CallerRunsPolicy()
);
    // 用于跟踪已调度的任务
    private static ScheduledFuture<?> scheduledTask;
    public static void detectHeartBeat(String serviceName,String group) {
//        if (timer != null) {
//            log.debug("心跳检测器已在运行，忽略重复启动");
//            return;
//        }
//
//        synchronized (lock) {
//            if (timer != null) {
//                return;
//            }
//
//            log.info("启动心跳检测器，服务: {}", serviceName);
//            timer = new Timer("HeartBeatTimer", true);
//            TimerTask task = new HeartBeatTimerTask(serviceName);
//            timer.scheduleAtFixedRate(task, 1000, 3000);
//        }
        if (scheduler != null) {
            log.debug("心跳检测器已在运行，忽略重复启动");
            return;
        }
        synchronized (lock) {
            if (scheduler != null) {
                return;
            }
            //创建单线程调度器
            scheduler=Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                private final AtomicInteger threadCount = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "HeartBeat-Scheduler-" + threadCount.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                }
            });
            // 调度任务，，之后每5秒执行一次
            scheduledTask = scheduler.scheduleAtFixedRate(
                    new HeartBeatRunnable(serviceName,group),
                    0,
                    5000,
                    TimeUnit.MILLISECONDS
            );

        }
    }

    public static void stop() {
        synchronized (lock) {
            if (scheduler != null) {
                log.info("停止心跳检测器");
                // 取消已调度的任务
                if (scheduledTask != null) {
                    scheduledTask.cancel(true);
                }
                // 关闭调度器
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                scheduler = null;
            }
        }
        // 清理待处理请求
        MyRpcBootstrap.getInstance().getConfigration().getHEARTBEAT_PENDING_REQUESTS().forEach((id, future) -> {
            if (!future.isDone()) {
                future.completeExceptionally(new RuntimeException("心跳检测器已停止"));
            }
        });
        MyRpcBootstrap.getInstance().getConfigration().getHEARTBEAT_PENDING_REQUESTS().clear();
        // 优雅关闭线程池
        HEARTBEAT_EXECUTOR.shutdown();
        try {
            if (!HEARTBEAT_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                HEARTBEAT_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            HEARTBEAT_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class HeartBeatRunnable implements Runnable {
        private final String serviceName;
        private final String group;

        public HeartBeatRunnable(String serviceName,String group) {
            this.serviceName = serviceName;
            this.group = group;
        }

        @Override
        public void run() {
            try{
                if(log.isDebugEnabled()){
                    log.debug("开始执行心跳检测");
                }
                //先获取服务提供者地址
                List<InetSocketAddress> addresses =null;

                Registry registry = MyRpcBootstrap.getInstance().getConfigration().getRegistryConfig().getRegistry();
                try {
                    addresses = registry.discovery(serviceName, group);
                } catch (NotFoundedEnableNodeException e) {
                    log.warn("没有可用的服务提供者地址: {}", e.getMessage());
                    addresses = Collections.emptyList();
                } catch (Exception e) {
                    log.error("服务发现失败", e);
                    addresses = Collections.emptyList();
                }
                if(addresses == null || addresses.isEmpty()){
                    log.warn("没有可用的服务提供者地址");
                }
                if(log.isDebugEnabled()){
                    log.debug("服务{}有提供者: {}个", serviceName,addresses.size());
                }
                //使用并行任务处理所有地址的心跳检测
                //supplyAsync()会创建一个CompletableFuture<HeartBeatResult>对象。
                List<CompletableFuture<HeartBeatResult>> futures = addresses.stream()
                        .map(address -> CompletableFuture.supplyAsync(
                                () -> checkAddressHeartbeat(address), HEARTBEAT_EXECUTOR))
                        .collect(Collectors.toList());
                //将futures转换为CompletableFuture数组并等待所有任务的完成
                //                          new CompletableFuture[0]是为了确定数组类型
                // （无论是正常完成还是异常完成）时，它自己才会完成
                //创建一个总Future对象来监视futures任务的完成当全部任务完成时他将取消阻塞
                //当任务完成时，它将返回一个CompletableFuture对象，该对象将完成（正常完成或异常完成）
                //如果正常返回：框架会自动调用 future.complete(heartBeatResult)，将这个结果设置到 Future 中
                //如果抛出异常：框架会自动调用 future.completeExceptionally(e)，将异常封装到 Future 中，并将其状态标记为“异常完成”
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(10, TimeUnit.SECONDS);

                //获取所有任务的结果
                TreeMap<Long, List<Channel>> newTreeMap = new TreeMap<>();
                for (CompletableFuture<HeartBeatResult> future : futures) {
                    try{
                        HeartBeatResult result = future.get();
                        if(result.isSuccess()&&result.getResponseTime()>0){
                            newTreeMap.computeIfAbsent(result.getResponseTime(), k->new ArrayList<>())
                                            .add(result.getChannel());
                        }else{
                            log.warn("服务{}提供者心跳检测失败", result.getAddress());
                        }
                    } catch (Exception e) {
                        log.error("获取心跳检测结果异常", e);
                    }
                }
                //原子更新channelcache中的值
                synchronized (MyRpcBootstrap.getInstance().getConfigration().getANSWER_CHANNEL_CACHE()) {
                    MyRpcBootstrap.getInstance().getConfigration().getANSWER_CHANNEL_CACHE().clear();
                    MyRpcBootstrap.getInstance().getConfigration().getANSWER_CHANNEL_CACHE().putAll(newTreeMap);
                }
                //打印一下MyRpcBootstrap.ANSWER_CHANNEL_CACHE
                MyRpcBootstrap.getInstance().getConfigration().getANSWER_CHANNEL_CACHE().forEach((time, channels) -> {
                    for (Channel channel : channels) {
                        log.info("提供者{}的响应时间为:{}ms",  channel.remoteAddress(), time);
                    }
                });
            }catch (TimeoutException e){
                log.warn("心跳检测总时间超时");
            }catch (Exception e){
                log.error("心跳检测任务执行失败", e);
            }
        }

        private HeartBeatResult checkAddressHeartbeat(InetSocketAddress address) {
            HeartBeatResult heartBeatResult = new HeartBeatResult(address,false,-1);
            //获取或创建channel
            Channel channel = MyRpcBootstrap.getInstance().getConfigration().getCHANNEL_CACHE().get(address);
            try{
                if (channel == null || !channel.isActive()) {
                    if (channel != null) {
                        // 移除无效通道
                        MyRpcBootstrap.getInstance().getConfigration().getCHANNEL_CACHE().remove(address);
                        channel.close();
                    }
                    ChannelFuture channelFuture = NettyBootstrapInitializer.getInstance().getBootstrap()
                            .connect(address.getHostName(), address.getPort());
                    // 等待连接完成，最多等待3秒
                    if (!channelFuture.awaitUninterruptibly(3000, TimeUnit.MILLISECONDS) ||
                            !channelFuture.isSuccess()) {
                        log.error("连接服务【{}】失败", address);
                        return heartBeatResult;
                    }
                    channel = channelFuture.channel();
                    MyRpcBootstrap.getInstance().getConfigration().getCHANNEL_CACHE().put(address, channel);
                }
                //发送心跳包
                heartBeatResult.setResponseTime(sendHeartBeat(channel));
                heartBeatResult.setSuccess(heartBeatResult.getResponseTime()>0);
                heartBeatResult.setChannel( channel);
            }catch (Exception e){
                log.error("checkAddressHeartbeat失败", e);
                if(channel != null){
                    channel.close();
                }
                if(MyRpcBootstrap.getInstance().getConfigration().getCHANNEL_CACHE().containsKey(address)){
                    MyRpcBootstrap.getInstance().getConfigration().getCHANNEL_CACHE().remove(address);
                }
            }
            return heartBeatResult;
        }

        private long sendHeartBeat(Channel channel) {
            int retryCount = 3;
            long responseTime = -1;
            long retryDelay = 100; // 初始延迟100ms
            long maxRetryDelay = 5000; // 最大5秒
            while(retryCount > 0){
                long newrequestId=0L;
                CompletableFuture<HeartBeatResult> future=null;
                try{
                    long startTime = System.currentTimeMillis();
                    //发送心跳报文
                    heartBeatPacket heartBeatPacket = new heartBeatPacket( CompressFactory.getCompressorByName(MyRpcBootstrap.getInstance().getConfigration().getCompressType()).getCode(),
                            SerializerFactory.getSerializerByName(MyRpcBootstrap.getInstance().getConfigration().getSerializeType()).getCode(),
                            MyRpcBootstrap.getInstance().getConfigration().idGenerator.getId(),
                            startTime);

                    future= new CompletableFuture<>();
                    newrequestId=heartBeatPacket.getRequestId();

                    final long currentRequestId = newrequestId;
                    final CompletableFuture<HeartBeatResult> currentFuture = future;
                    MyRpcBootstrap.getInstance().getConfigration().getHEARTBEAT_PENDING_REQUESTS().put(newrequestId,future);
                    channel.writeAndFlush(heartBeatPacket).addListener((ChannelFutureListener) promise -> {
                        if (!promise.isSuccess()) {
                            CompletableFuture<HeartBeatResult> removeFuture = MyRpcBootstrap.getInstance().getConfigration().getHEARTBEAT_PENDING_REQUESTS().remove(currentRequestId);
                            currentFuture.completeExceptionally(promise.cause());
                        }
                    });
                    // 等待响应，超时时间2秒
                    HeartBeatResult heartBeatResult = future.get(2000, TimeUnit.MILLISECONDS);
                    responseTime = heartBeatResult.getResponseTime();
                    break; // 成功则跳出重试循环
                }catch (InterruptedException e){
                    log.warn("心跳检测被中断");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    //一旦失败就直接清除缓存中的Future,只要成功的
                    if(MyRpcBootstrap.getInstance().getConfigration().getHEARTBEAT_PENDING_REQUESTS().containsKey(newrequestId)) {
                        MyRpcBootstrap.getInstance().getConfigration().getHEARTBEAT_PENDING_REQUESTS().remove(newrequestId);
                    }
                    retryCount--;
                    log.warn("发送心跳包失败，剩余重试次数: {}", retryCount);
                    log.warn("与 {}的主机连接发生问题", channel.remoteAddress());

                    if (retryCount <= 0) {
                        log.warn("重试次数用完，无法发送心跳包");
                        //可能是服务端挂了，或者网络问题把服务端给的连接断掉
                        log.warn("与 {}主机断开连接", channel.remoteAddress());
                        channel.close();
                        MyRpcBootstrap.getInstance().getConfigration().getCHANNEL_CACHE().remove(channel.remoteAddress());
                        break;
                    }
                    //使用指数退避
                    try {
                        Thread.sleep(Math.min(retryDelay, maxRetryDelay));
                        retryDelay *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            return responseTime;
        }
    }
}