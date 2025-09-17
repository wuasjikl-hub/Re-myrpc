package com.myrpc.leafe.LoadBalancer;

import java.net.InetSocketAddress;

/**
 * 不同算法都有对应的Selector
 * 他们内部
 */
public interface Selector {
    InetSocketAddress selectServiceAddress();
}
