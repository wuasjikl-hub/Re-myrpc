package com.myrpc.leafe.utils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class IdGenerator {

    // 起始时间戳
    public static final long START_STAMP = DateUtil.get("2022-1-1").getTime();

    // 各部分的位数
    public static final long DATA_CENTER_BIT = 5L;
    public static final long MACHINE_BIT = 5L;
    public static final long SEQUENCE_BIT = 12L;

    // 最大值计算
    public static final long DATA_CENTER_MAX = ~(-1L << DATA_CENTER_BIT);
    public static final long MACHINE_MAX = ~(-1L << MACHINE_BIT);
    public static final long SEQUENCE_MAX = ~(-1L << SEQUENCE_BIT);

    // 偏移量计算
    public static final long TIMESTAMP_LEFT = DATA_CENTER_BIT + MACHINE_BIT + SEQUENCE_BIT;
    public static final long DATA_CENTER_LEFT = MACHINE_BIT + SEQUENCE_BIT;
    public static final long MACHINE_LEFT = SEQUENCE_BIT;

    private final long dataCenterId;
    private final long machineId;

    // 使用AtomicLong替代LongAdder，更适合此场景
    private final AtomicLong sequenceId = new AtomicLong(0);

    // 上次时间戳
    private long lastTimestamp = -1L;

    // 使用锁处理时钟回拨等临界情况
    private final Lock lock = new ReentrantLock();

    // 时钟回拨容忍阈值（毫秒）
    private static final long MAX_BACKWARD_MS = 5;

    public IdGenerator(long dataCenterId, long machineId) {
        if (dataCenterId > DATA_CENTER_MAX || dataCenterId < 0) {
            throw new IllegalArgumentException("数据中心ID必须在0和" + DATA_CENTER_MAX + "之间");
        }
        if (machineId > MACHINE_MAX || machineId < 0) {
            throw new IllegalArgumentException("机器ID必须在0和" + MACHINE_MAX + "之间");
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }

    public long getId() {
        long currentTimestamp = timeGen();
        long sequence = 0L;

        // 使用锁确保线程安全
        lock.lock();
        try {
            // 处理时钟回拨
            if (currentTimestamp < lastTimestamp) {
                long offset = lastTimestamp - currentTimestamp;
                if (offset <= MAX_BACKWARD_MS) {
                    // 轻微时钟回拨，等待时间追上来
                    try {
                        Thread.sleep(offset << 1);
                        currentTimestamp = timeGen();
                        // 等待后再次检查
                        if (currentTimestamp < lastTimestamp) {
                            throw new RuntimeException("时钟回拨异常，无法生成ID");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("时钟回拨等待中断", e);
                    }
                } else {
                    // 严重时钟回拨，无法恢复
                    throw new RuntimeException("时钟回拨过大，无法生成ID");
                }
            }

            // 如果是同一毫秒，序列号自增
            if (currentTimestamp == lastTimestamp) {
                sequence = sequenceId.incrementAndGet();
                if (sequence > SEQUENCE_MAX) {
                    // 当前毫秒序列号已用完，等待下一毫秒
                    currentTimestamp = tilNextMillis(lastTimestamp);
                    sequenceId.set(0);
                    sequence = 0;
                }
            } else {
                // 时间戳变化，重置序列号
                sequenceId.set(0);
                sequence = 0;
            }

            lastTimestamp = currentTimestamp;
        } finally {
            lock.unlock();
        }

        return ((currentTimestamp - START_STAMP) << TIMESTAMP_LEFT)
                | (dataCenterId << DATA_CENTER_LEFT)
                | (machineId << MACHINE_LEFT)
                | sequence;
    }

    /**
     * 获取当前时间戳
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 等待直到下一毫秒
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    public static void main(String[] args) {
        IdGenerator idGenerator = new IdGenerator(1, 2);

        // 测试并发性能
        int threadCount = 1000;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    long id = idGenerator.getId();
                    System.out.println(Thread.currentThread().getName() + ": " + id);
                }
            });
            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
