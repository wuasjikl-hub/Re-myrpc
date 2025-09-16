package com.myrpc.leafe.protection;

import com.myrpc.leafe.exceptions.CircuitBreakerException;
import io.netty.channel.Channel;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class CircuitBreaker {
    //熔断器状态
    public enum CircuitBreakerState {
        CLOSED, OPEN, HALF_OPEN
    }
    private final int failureThreshold; //失败阈值(达到后直接触发熔断)
    private final long resetTimeout;     //重试时间，熔断时间
    private final int halfopensuccessCount; //半开状态下成功阈值(达到后恢复服务)

    private volatile CircuitBreakerState state=CircuitBreakerState.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicInteger halfOpenSuccessCountCurrent = new AtomicInteger(0);
    private final Object lock = new Object(); // 添加同步锁

    public CircuitBreaker(int failureThreshold, long resetTimeout, TimeUnit resetTimeoutUnit, int halfOpenSuccessCount) {
        this.failureThreshold = failureThreshold;
        this.resetTimeout = resetTimeoutUnit.toMillis(resetTimeout);
        this.halfopensuccessCount = halfOpenSuccessCount;
    }
    public CircuitBreaker() {
        this(3, 1, TimeUnit.SECONDS, 3);
    }
    public <T> T execute(Supplier<T> supplier, Channel channel) throws Exception {
        if(state == CircuitBreakerState.OPEN){
            synchronized ( lock) {
                //尝试切换为半开状态
                if (isTimetoresetTimeout()) {
                    transforToHalfOpen();
                } else {//拦截请求
                    throw new CircuitBreakerException("服务已熔断");
                }
            }
        }
        try{
            T res = supplier.get();
            recordSuccess();
            return res;
        }catch (Exception e){
            recordFailure();
            throw new CircuitBreakerException("熔断出现异常",e);
        }
    }

    private void recordFailure() {
        synchronized ( lock) {
            if (state == CircuitBreakerState.HALF_OPEN) {
                transforToOpen();
            } else {
                int count = failureCount.incrementAndGet();
                if (count >= failureThreshold) {//失败数石否到达阙值
                    transforToOpen();
                }
            }
        }
    }
    private void transforToOpen() {
        synchronized ( lock) {
            state = CircuitBreakerState.OPEN;
            lastFailureTime.set(System.currentTimeMillis());
            //重置半开状态次数
            halfOpenSuccessCountCurrent.set(0);
        }
    }
    private void recordSuccess() {
        synchronized ( lock) {
            if (state == CircuitBreakerState.HALF_OPEN) {
                int successCount = halfOpenSuccessCountCurrent.incrementAndGet();
                if (successCount >= halfopensuccessCount) {
                    resetToriginal();
                }
            } else {//CLOSED状态下
                failureCount.set(0);
            }
        }
    }

    private void resetToriginal() {
        synchronized ( lock) {
            state = CircuitBreakerState.CLOSED;
            failureCount.set(0);
            lastFailureTime.set(0);
            halfOpenSuccessCountCurrent.set(0);
        }
    }

    private void transforToHalfOpen() {
        synchronized ( lock) {
            state = CircuitBreakerState.HALF_OPEN;
            halfOpenSuccessCountCurrent.set(0);  //重置半开成功次数
        }
    }

    private boolean isTimetoresetTimeout() {
        long now = System.currentTimeMillis();
        return now - lastFailureTime.get() > resetTimeout;
    }
    /**
     * 获取当前状态
     */
    public CircuitBreakerState getState() {
        return state;
    }

    /**
     * 获取当前失败计数
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * 获取半开状态下的成功计数
     */
    public int getHalfOpenSuccessCount() {
        return halfOpenSuccessCountCurrent.get();
    }
    // 测试用例


}
