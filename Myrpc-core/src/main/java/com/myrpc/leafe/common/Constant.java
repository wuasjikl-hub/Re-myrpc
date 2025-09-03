package com.myrpc.leafe.common;

public class Constant {
    //zookeeper的默认连接地址
    public static final String ZK_CONNECT_STRING = "localhost:2181";
    //zooKeeper的默认超时时间
    public static final int ZK_SESSION_TIMEOUT = 5000;
    //zookeeper的提供方和消费方的节点路径
    public static final String ZK_PROVIDERS_PATH="/metaNode/providers";
    public static final String ZK_CONSUMERS_PATH="/metaNode/consumers";
}
