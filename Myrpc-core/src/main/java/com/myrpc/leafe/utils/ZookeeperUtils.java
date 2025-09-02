package com.myrpc.leafe.utils;

import com.myrpc.leafe.exceptions.ZookeeperException;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.List;
import java.util.concurrent.Executors;

@Slf4j
public class ZookeeperUtils {
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_RETRY_INTERVAL_MS = 1000;

    // 连接方法保持不变，但修正异常处理

    /**
     * 安全关闭连接
     */
    public static void closeQuietly(ZooKeeper zooKeeper) {
        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                log.warn("关闭ZooKeeper连接时被中断", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("关闭ZooKeeper连接时发生异常", e);
            }
        }
    }

    /**
     * 带重试的执行操作
     */
    public static <T> T executeWithRetry(ZooKeeper zooKeeper, ZooKeeperOperation<T> operation) {
        return executeWithRetry(zooKeeper, operation, DEFAULT_MAX_RETRIES, DEFAULT_RETRY_INTERVAL_MS);
    }

    public static <T> T executeWithRetry(ZooKeeper zooKeeper, ZooKeeperOperation<T> operation,
                                         int maxRetries, long retryIntervalMs) {
        int retries = 0;
        while (true) {
            try {
                ensureConnected(zooKeeper);
                return operation.execute();
            } catch (KeeperException.ConnectionLossException e) {
                if (retries++ >= maxRetries) {
                    throw new ZookeeperException("操作失败，超过最大重试次数", e);
                }
                log.warn("连接丢失，进行第{}次重试", retries);
                try {
                    Thread.sleep(retryIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ZookeeperException("重试被中断", ie);
                }
            } catch (Exception e) {
                throw new ZookeeperException("操作失败", e);
            }
        }
    }

    /**
     * 检查连接状态
     */
    private static void ensureConnected(ZooKeeper zooKeeper) {
        if (zooKeeper == null || !zooKeeper.getState().isAlive()) {
            throw new ZookeeperException("ZooKeeper连接不可用", null);
        }
    }

    /**
     * 获取子节点（带重试）
     */
    public static List<String> getChildren(ZooKeeper zooKeeper, String path, Watcher watch) {
        return executeWithRetry(zooKeeper, () -> {
            try {
                return zooKeeper.getChildren(path, watch);
            } catch (Exception e) {
                log.error("获取子节点失败：", e);
                throw new ZookeeperException("获取子节点失败", e);
            }
        });
    }

    // 其他方法类似改造...

    /**
     * 添加永久性监听器
     */
    public static void addPersistentWatcher(ZooKeeper zooKeeper, String path, Watcher watcher) {
        executeWithRetry(zooKeeper, () -> {
            zooKeeper.exists(path, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    try {
                        // 处理事件
                        watcher.process(event);

                        // 重新注册（避免在事件线程中阻塞）
                        if (event.getType() != Event.EventType.None) {
                            Executors.newSingleThreadExecutor().submit(() -> {
                                try {
                                    Thread.sleep(100); // 稍作延迟
                                    zooKeeper.exists(path, this);
                                } catch (Exception e) {
                                    log.error("重新注册监听器失败: {}", e.getMessage());
                                }
                            });
                        }
                    } catch (Exception e) {
                        log.error("处理监听事件失败: {}", e.getMessage());
                    }
                }
            });
            return null;
        });
    }

    @FunctionalInterface
    public interface ZooKeeperOperation<T> {
        T execute() throws Exception;
    }
}