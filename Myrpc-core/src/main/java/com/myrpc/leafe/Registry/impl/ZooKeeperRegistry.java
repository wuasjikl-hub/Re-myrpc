package com.myrpc.leafe.Registry.impl;

import com.myrpc.leafe.Registry.AbstractRegistry;
import com.myrpc.leafe.bootatrap.MyRpcBootstrap;
import com.myrpc.leafe.common.Constant;
import com.myrpc.leafe.config.ServiceConfig;
import com.myrpc.leafe.exceptions.ZookeeperException;
import com.myrpc.leafe.utils.net.NetUtil;
import com.myrpc.leafe.utils.zookeeper.ZookeeperNode;
import com.myrpc.leafe.utils.zookeeper.ZookeeperUtils;
import com.myrpc.leafe.watcher.ServiceUpAndDownWatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.List;
@Slf4j
public class ZooKeeperRegistry extends AbstractRegistry {
    //不需要每次使用都去连接zookeeper
    private ZooKeeper zooKeeper;
    public ZooKeeperRegistry() {
        zooKeeper=ZookeeperUtils.connect();
    }
    public ZooKeeperRegistry(String hosts,int timeout) {
        zooKeeper=ZookeeperUtils.connect(hosts,timeout);
    }

    @Override
    public <T> void register(ServiceConfig<T> serviceConfig) {
        String serviceName = serviceConfig.getInterface().getName();
        //创建服务的节点(持久化节点)
        String ServiceNode= Constant.ZK_PROVIDERS_PATH+"/"+serviceName;

        if(!ZookeeperUtils.exists(zooKeeper,ServiceNode,null)){
            ZookeeperNode zookeeperNode = new ZookeeperNode(ServiceNode, null, CreateMode.PERSISTENT);
            ZookeeperUtils.createNode(zooKeeper,zookeeperNode,null);
            log.info("创建服务节点：{}",ServiceNode);
        }
        //创建分组的节点
        String groupNode=ServiceNode+"/"+serviceConfig.getGroupinfo();
        if(!ZookeeperUtils.exists(zooKeeper,groupNode,null)){
            ZookeeperNode zookeeperNode = new ZookeeperNode(groupNode, null, CreateMode.PERSISTENT);
            ZookeeperUtils.createNode(zooKeeper,zookeeperNode,null);
            log.info("创建服务分组节点：{}",groupNode);
        }
        //创建服务的本机节点(临时节点)
        //todo 这里的端口先假装是port:8088
        //端口由netty决定
        String tempServiceNode=groupNode+"/"+ NetUtil.getIp()+":"+ MyRpcBootstrap.getInstance().getConfigration().getPort();
        if(!ZookeeperUtils.exists(zooKeeper,tempServiceNode,null)){
            ZookeeperNode zookeeperNode2 = new ZookeeperNode(tempServiceNode, null, CreateMode.EPHEMERAL);
            ZookeeperUtils.createNode(zooKeeper,zookeeperNode2,null);
            log.info("创建服务节点：{}",tempServiceNode);
        }
        if(log.isDebugEnabled()) {
            log.info("注册服务：{}", serviceConfig.getInterface().getName());
        }
    }
    /*
    服务发现拉取 存活服务的host
     */
    @Override
    public List<InetSocketAddress> discovery(String serviceName,String group) {
        // 1、找到服务对应的节点
        String serviceNode = Constant.ZK_PROVIDERS_PATH + "/" + serviceName + "/" +group;

        // 2、从zk中获取他的子节点, 192.168.12.123:2151
        List<String> children = ZookeeperUtils.getChilderen(zooKeeper, serviceNode,new ServiceUpAndDownWatcher());
        // 获取了所有的可用的服务列表
        List<InetSocketAddress> inetSocketAddresses = children.stream().map(ipString -> {
            String[] ipAndPort = ipString.split(":");
            String ip = ipAndPort[0];
            int port = Integer.parseInt(ipAndPort[1]);
            return new InetSocketAddress(ip, port);
        }).toList();

        if(inetSocketAddresses.size() == 0){
            throw new ZookeeperException("未发现任何可用的服务主机.");
        }

        return inetSocketAddresses;
    }

}
