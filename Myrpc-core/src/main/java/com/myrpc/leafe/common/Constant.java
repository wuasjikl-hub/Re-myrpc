package com.myrpc.leafe.common;

public class Constant {
    //zookeeper的默认连接地址
    public static final String ZK_CONNECT_STRING = "localhost:2181";
    //zooKeeper的默认超时时间
    public static final int ZK_SESSION_TIMEOUT = 5000;
    //zookeeper的提供方和消费方的节点路径
    public static final String ZK_PROVIDERS_PATH="/metaNode/providers";
    public static final String ZK_CONSUMERS_PATH="/metaNode/consumers";
    //设置客户端连接的超时时间
    public static final int CLIENT_CONNECT_TIMEOUT = 5000;
    //设置客户端最大重连次数
    public static final int CLIENT_MAX_RETRY = 3;
    //服务提供方默认端口
    public static final int PORT = 8001;
    //虚拟节点个数
    public static final int VIRTUAL_NODE_NUM = 128;
}
