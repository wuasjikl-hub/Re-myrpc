package com.myrpc.leafe.protection;

import com.myrpc.leafe.exceptions.CircuitBreakerException;

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
    public CircuitBreaker(int failureThreshold, long resetTimeout, TimeUnit resetTimeoutUnit, int halfOpenSuccessCount) {
        this.failureThreshold = failureThreshold;
        this.resetTimeout = resetTimeoutUnit.toMillis(resetTimeout);
        this.halfopensuccessCount = halfOpenSuccessCount;
    }
    public CircuitBreaker() {
        this(3, 1, TimeUnit.SECONDS, 3);
    }
    public <T> T execute(Supplier<T> supplier) throws Exception {
        if(state == CircuitBreakerState.OPEN){
            //尝试切换为半开状态
            if(isTimetoresetTimeout()){
                transforToHalfOpen();
            }else{//拦截请求
                throw new CircuitBreakerException("服务已熔断");
            }
        }
        try{
            T res = supplier.get();
            recordSuccess();
            return res;
        }catch (Exception e){
            recordFailure();
            throw e;
        }
    }

    private void recordFailure() {
        if(state==CircuitBreakerState.HALF_OPEN){
            transforToOpen();
        }else{
            int count = failureCount.incrementAndGet();
            if(count>=failureThreshold){//失败数石否到达阙值
                transforToOpen();
            }
        }
    }
    private void transforToOpen() {
        state = CircuitBreakerState.OPEN;
        lastFailureTime.set(System.currentTimeMillis());
        //重置半开状态次数
        halfOpenSuccessCountCurrent.set(0);
    }
    private void recordSuccess() {
        if(state==CircuitBreakerState.HALF_OPEN){
            int successCount  = halfOpenSuccessCountCurrent.incrementAndGet();
            if(successCount>=halfopensuccessCount){
                resetToriginal();
            }
        }else{//CLOSED状态下
            failureCount.set(0);
        }
    }

    private void resetToriginal() {
        state = CircuitBreakerState.CLOSED;
        failureCount.set(0);
        lastFailureTime.set(0);
        halfOpenSuccessCountCurrent.set(0);
    }

    private void transforToHalfOpen() {
        state = CircuitBreakerState.HALF_OPEN;
        halfOpenSuccessCountCurrent.set(0);  //重置半开成功次数
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
    public static void main(String[] args) {
        CircuitBreaker circuitBreaker = new CircuitBreaker();

        // 模拟一个不稳定的服务
        Supplier<String> unstableService = () -> {
            double random = Math.random();
            if (random > 0.7) {
                throw new RuntimeException("Service unavailable");
            }
            return "Success: " + random;
        };

        // 测试熔断器
        for (int i = 0; i < 200; i++) {
            try {
                String result = circuitBreaker.execute(unstableService);
                System.out.println("Request " + i + ": " + result +
                        " [State: " + circuitBreaker.getState() + "]");
            } catch (CircuitBreakerException e) {
                System.out.println("Request " + i + ": " + e.getMessage() +
                        " [State: " + circuitBreaker.getState() + "]");
            } catch (Exception e) {
                System.out.println("Request " + i + ": " + e.getMessage() +
                        " [State: " + circuitBreaker.getState() +
                        ", Failures: " + circuitBreaker.getFailureCount() + "]");
            }

            // 添加一点延迟
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
