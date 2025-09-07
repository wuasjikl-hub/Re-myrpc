package com.myrpc.leafe.Registry.impl;

import com.myrpc.leafe.Registry.AbstractRegistry;
import com.myrpc.leafe.config.ServiceConfig;
import com.myrpc.leafe.common.Constant;
import com.myrpc.leafe.exceptions.NotFoundedEnableNodeException;
import com.myrpc.leafe.utils.net.NetUtil;
import com.myrpc.leafe.utils.zookeeper.ZookeeperNode;
import com.myrpc.leafe.utils.zookeeper.ZookeeperUtils;
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
        //创建服务的本机节点(临时节点)
        //todo 这里的端口先假装是port:8088
        //端口由netty决定
        String tempServiceNode=ServiceNode+"/"+ NetUtil.getIp()+":"+Constant.PORT;
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
    public List<InetSocketAddress> discovery(String serviceName) {
        //拿到可用的服务节点可能是集群
        log.info("服务发现：{}",serviceName);
        String ennableServiceNode=Constant.ZK_PROVIDERS_PATH+"/"+serviceName;
        //获取可用服务节点
        List<String> childeren = ZookeeperUtils.getChilderen(zooKeeper, ennableServiceNode, null);
        if(childeren.size()==0){
            log.warn("没有可用的服务节点");
            throw new NotFoundedEnableNodeException("没有可用的服务节点");
        }
        // eg.192.168.1.1:8088
        return childeren.stream().map(child -> {
            String[] split = child.split(":");
            String ip=split[0];
            int port=Integer.parseInt(split[1]);
            return new InetSocketAddress(ip,port);
        }).toList();
    }
}
