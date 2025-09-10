package com.myrpc.leafe.detector;

import com.myrpc.leafe.Registry.Registry;
import com.myrpc.leafe.bootatrap.Initializer.NettyBootstrapInitializer;
import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.enumeration.CompressorType;
import com.myrpc.leafe.enumeration.SerializerType;
import com.myrpc.leafe.packet.heartBeat.heartBeatPacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class HeartBeatDetector {
    private static volatile Timer timer;
    private static final Object lock = new Object();
    //设置线程池
    //这里用缓存线程池当执行耗时任务时候，线程池会自动创建线程，
    // 当线程池中的线程数达到最大值时，会自动创建新的线程，
    //也可用固定大小线程池 FixedThreadPool
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final int MAX_POOL_SIZE = 20; // 最大线程数限制
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
        CORE_POOL_SIZE,      // 核心线程数
        MAX_POOL_SIZE,       // 最大线程数
        60L, TimeUnit.SECONDS, // 空闲线程存活时间
        new LinkedBlockingQueue<>(100), // 任务队列，设置容量限制
        new ThreadFactory() {
            private final AtomicInteger threadCount = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "HeartBeat-Thread-" + threadCount.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        },              //这样的好处有保证任务不丢失
                //其他拒绝策略还有：DiscardPolicy(不接受新任务)、DiscardOldestPolicy(丢弃任务队列中最老的任务)、AbortPolicy(直接抛异常)
        new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：当线程池满载时，会执行策略新提交的任务会由提交任务的线程自己执行。
);

    public static void detectHeartBeat(String serviceName) {
        if (timer != null) {
            log.debug("心跳检测器已在运行，忽略重复启动");
            return;
        }

        synchronized (lock) {
            if (timer != null) {
                return;
            }

            log.info("启动心跳检测器，服务: {}", serviceName);
            timer = new Timer("HeartBeatTimer", true);
            TimerTask task = new HeartBeatTimerTask(serviceName);
            timer.scheduleAtFixedRate(task, 1000, 3000);
        }
    }

    public static void stop() {
        synchronized (lock) {
            if (timer != null) {
                log.info("停止心跳检测器");
                timer.cancel();
                timer = null;
            }
        }

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

    private static class HeartBeatTimerTask extends TimerTask {
        private final String serviceName;

        public HeartBeatTimerTask(String serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        public void run() {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("开始执行心跳检测任务");
                }

                // 先获取服务列表
                Registry registry = MyRpcBootstrap.getInstance().getregistryConfig().getRegistry();
                List<InetSocketAddress> addresses = registry.discovery(serviceName);

                if (addresses.isEmpty()) {
                    log.warn("未找到服务【{}】的实例", serviceName);
                    return;
                }

                if (log.isDebugEnabled()) {
                    log.debug("服务【{}】共有{}个实例", serviceName, addresses.size());
                }

                // 使用并行流处理所有地址的心跳检测
                List<CompletableFuture<HeartBeatResult>> futures = addresses.stream()
                        .map(address -> CompletableFuture.supplyAsync(
                                () -> checkAddressHeartbeat(address),
                                HEARTBEAT_EXECUTOR
                        ))
                        .collect(Collectors.toList());

                // 等待所有任务完成     先转换成CompletableFuture数组new CompletableFuture[0]为了确定数组类型
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(10, TimeUnit.SECONDS); // 设置总体超时时间

                // 收集结果
                TreeMap<Long, Channel> newTreeMap = new TreeMap<>();
                for (CompletableFuture<HeartBeatResult> future : futures) {
                    try {
                        HeartBeatResult result = future.get();
                        if (result.success && result.responseTime >= 0) {
                            newTreeMap.put(result.responseTime, result.channel);
                            log.info("服务提供者【{}】响应时间: {} ms", result.address, result.responseTime);
                        } else {
                            log.warn("服务提供者【{}】心跳检测失败", result.address);
                        }
                    } catch (Exception e) {
                        log.error("获取心跳检测结果异常", e);
                    }
                }

                // 原子性更新ANSWER_CHANNEL_CACHE
                synchronized (MyRpcBootstrap.ANSWER_CHANNEL_CACHE) {
                    MyRpcBootstrap.ANSWER_CHANNEL_CACHE.clear();
                    MyRpcBootstrap.ANSWER_CHANNEL_CACHE.putAll(newTreeMap);
                }

                if (log.isDebugEnabled()) {
                    log.debug("心跳检测完成，有效连接数: {}", newTreeMap.size());
                }
            } catch (TimeoutException e) {
                log.error("心跳检测总体超时", e);
            } catch (Exception e) {
                log.error("心跳检测任务执行失败", e);
            }
        }

        private HeartBeatResult checkAddressHeartbeat(InetSocketAddress address) {
            HeartBeatResult result = new HeartBeatResult();
            result.address = address;
            result.success = false;
            result.responseTime = -1;

            Channel channel = MyRpcBootstrap.CHANNEL_CACHE.get(address);
            try {
                if (channel == null || !channel.isActive()) {
                    // 如果不活跃或缓存中没有该地址，则创建新channel
                    log.info("创建到服务【{}】的新连接", address);
                    ChannelFuture channelFuture = NettyBootstrapInitializer.getInstance().getBootstrap()
                            .connect(address.getHostName(), address.getPort());

                    // 等待连接完成，最多等待3秒
                    if (!channelFuture.awaitUninterruptibly(3000, TimeUnit.MILLISECONDS) ||
                            !channelFuture.isSuccess()) {
                        log.error("连接服务【{}】失败", address);
                        return result;
                    }

                    Channel newChannel = channelFuture.channel();
                    MyRpcBootstrap.CHANNEL_CACHE.put(address, newChannel);
                    channel = newChannel;
                }

                // 发送心跳包
                result.responseTime = sendHeartBeat(channel);
                result.channel = channel;
                result.success = result.responseTime >= 0;
            } catch (Exception e) {
                log.error("检测服务【{}】心跳失败", address, e);
                // 移除不活跃或无效的channel
                if (channel != null) {
                    try {
                        channel.close();
                    } catch (Exception ex) {
                        log.debug("关闭channel时发生异常", ex);
                    }
                }
                MyRpcBootstrap.CHANNEL_CACHE.remove(address);
            }

            return result;
        }

        private long sendHeartBeat(Channel channel) {
            int retryCount = 3;
            long responseTime = -1;

            while (retryCount > 0) {
                try {
                    long startTime = System.currentTimeMillis();
                    // 发送心跳报文
                    heartBeatPacket heartBeatPacket = new heartBeatPacket(
                            CompressorType.COMPRESSTYPE_GZIP.getCode(),
                            SerializerType.SERIALIZERTYPE_HESSION.getCode(),
                            MyRpcBootstrap.idGenerator.getId(),
                            startTime
                    );

                    CompletableFuture<Object> future = new CompletableFuture<>();
                    MyRpcBootstrap.PENDING_REQUESTS.put(heartBeatPacket.getRequestId(), future);

                    channel.writeAndFlush(heartBeatPacket).addListener((ChannelFutureListener) promise -> {
                        if (!promise.isSuccess()) {
                            future.completeExceptionally(promise.cause());
                        }
                    });

                    // 等待响应，超时时间2秒
                    future.get(2000, TimeUnit.MILLISECONDS);
                    responseTime = System.currentTimeMillis() - startTime;
                    break; // 成功则跳出重试循环
                } catch (InterruptedException e) {
                    log.warn("心跳检测被中断");
                    Thread.currentThread().interrupt();
                    break;
                } catch (ExecutionException e) {
                    retryCount--;
                    log.warn("心跳请求执行失败，剩余重试次数: {}", retryCount);
                    if (retryCount == 0) {
                        log.error("服务心跳检测失败，已放弃重试");
                        break;
                    }
                } catch (TimeoutException e) {
                    retryCount--;
                    log.warn("服务心跳响应超时，剩余重试次数: {}", retryCount);
                    if (retryCount == 0) {
                        log.error("服务心跳检测失败，已放弃重试");
                        break;
                    }
                }

                // 等待随机时间后重试
                try {
                    Thread.sleep(new Random().nextInt(50));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            return responseTime;
        }

        private static class HeartBeatResult {
            InetSocketAddress address;
            Channel channel;
            boolean success;
            long responseTime;
        }
    }
}