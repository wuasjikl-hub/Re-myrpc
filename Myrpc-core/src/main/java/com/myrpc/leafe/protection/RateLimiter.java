package com.myrpc.leafe.protection;

public interface RateLimiter {
    /**
     * 尝试获取令牌
     * @return
     */
    boolean tryAcquire();
}
