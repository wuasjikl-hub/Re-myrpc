package com.myrpc.leafe.utils.zookeeper;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

public class zookeeperdemo {
    public static void main(String[] args) {
        ZooKeeper zookeeper = ZookeeperUtils.connect();
        ZookeeperNode metanode = new ZookeeperNode("/metaNode", null, CreateMode.PERSISTENT);
        ZookeeperNode provider = new ZookeeperNode("/metaNode/providers", null, CreateMode.PERSISTENT);
        ZookeeperNode consumer = new ZookeeperNode("/metaNode/consumers", null, CreateMode.PERSISTENT);
        ZookeeperUtils.createNode(zookeeper, metanode, null);
        ZookeeperUtils.createNode(zookeeper, provider, null);
        ZookeeperUtils.createNode(zookeeper, consumer, null);
        ZookeeperUtils.close(zookeeper);
    }
}
