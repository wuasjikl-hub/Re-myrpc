package com.myrpc.leafe.protection.impl;

import com.myrpc.leafe.protection.RateLimiter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: leafe
 * @Description:基于令牌桶算法的限流器
 * @Date: 2022/4/23 10:04
 */
@Slf4j
public class TokenBarrelRateLimiter implements RateLimiter {
    private final int capacity; // 默认容量
    private double tokens;     // 令牌数(用double提高精确度)
    private final double refillRate; // 令牌填充速率
    private long lastRefillTime;// 上一次填充令牌的时间
    public TokenBarrelRateLimiter(int capacity, int refillRatePerSecond) {
        if (capacity <= 0 || refillRatePerSecond <= 0) {
            throw new IllegalArgumentException("容量和填充速率必须大于0");
        }
        this.capacity = capacity;
        this.refillRate = refillRatePerSecond/1000.0;
        this.tokens = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }

    @Override
    public synchronized boolean tryAcquire() {
        //补充令牌
        refillTokens();
        //尝试获取令牌
        if(tokens >= 1){
            tokens--;
            return true;
        }
        return false;
    }

    /**
     *
     * @param permits 请求的令牌数
     * @return
     */
    public synchronized boolean tryAcquire(int permits) {
        if (permits <= 0) {
            throw new IllegalArgumentException("请求的令牌数必须大于0");
        }
        //补充令牌
        refillTokens();
        //尝试获取令牌
        if(tokens >= permits){
            tokens-= permits;
            return true;
        }
        return false;
    }

    private void refillTokens() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastRefillTime;
        if(elapsedTime<=0){
            log.error("时间间隔必须大于0,可能发生了时间回退");
            return;
        }
        double tokensToAdd = elapsedTime * refillRate;
        if(tokensToAdd>0){
            tokens = Math.min(capacity, tokens + tokensToAdd);
            log.info("补充后的令牌数:{}",tokens);
            lastRefillTime = currentTime;
        }
    }
    public synchronized double getAvailableTokens() {
        refillTokens();
        return tokens;
    }

    public static void main(String[] args) {
        TokenBarrelRateLimiter rateLimiter = new TokenBarrelRateLimiter(10, 10);
        for (int i = 0; i < 100; i++) {
            try{
                Thread.sleep(10);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            boolean result = rateLimiter.tryAcquire();
            System.out.println("尝试获取令牌结果：" + result);
        }
    }
}
