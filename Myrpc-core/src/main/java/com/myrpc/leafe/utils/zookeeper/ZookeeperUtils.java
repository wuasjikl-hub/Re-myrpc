package com.myrpc.leafe.utils.zookeeper;

import com.myrpc.leafe.common.Constant;
import com.myrpc.leafe.exceptions.ZookeeperException;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ZookeeperUtils {
    /**
     * 使用默认参数连接zk
     * @return
     */
    public Map<String,ZooKeeper> map=new HashMap<>();
    public static ZooKeeper connect(){
        return connect(Constant.ZK_CONNECT_STRING,Constant.ZK_SESSION_TIMEOUT);
    }
    /**
     * 连接zk
     * @param hosts
     * @param sessionTimeout
     * @return
     */
    public static ZooKeeper connect(String hosts, int sessionTimeout) {
        return executeRetry(() -> {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            ZooKeeper zooKeeper = new ZooKeeper(hosts, sessionTimeout, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    log.info("zookeeper连接成功:{}", hosts);
                    countDownLatch.countDown();
                }
            });
            // 修正等待逻辑
            if (!countDownLatch.await(sessionTimeout, TimeUnit.MILLISECONDS)) {
                zooKeeper.close(); // 超时后关闭连接
                throw new ZookeeperException("连接zookeeper超时");
            }
            return zooKeeper;
        }, 3);
    }
    /**
     * 创建节点
     * @param zookeeper
     * @param zookeeperNode
     * @param watcher
     * @return
     */
    public static Boolean createNode(ZooKeeper zookeeper,ZookeeperNode zookeeperNode,Watcher watcher) {
        try {
            if (zookeeper.exists(zookeeperNode.getNodepath(), watcher) == null) {
                String nodepath = zookeeper.create(zookeeperNode.getNodepath(), zookeeperNode.getData()
                        , ZooDefs.Ids.OPEN_ACL_UNSAFE, zookeeperNode.getCreateMode());
                log.info("创建节点成功：" + nodepath);
                return true;
            } else {
                if (log.isDebugEnabled()) {
                    log.info("节点已存在：" + zookeeperNode.getNodepath());
                }
                return false;
            }
        } catch (KeeperException | InterruptedException e) {
            log.error("创建基础目录时发生异常：", e);
            throw new ZookeeperException();
        }
    }
    public static Boolean deleteNode(ZooKeeper zookeeper, String nodePath, Watcher watcher){
        try{
            if(zookeeper.exists(nodePath,watcher) != null){
                zookeeper.delete(nodePath, -1);
                log.info("节点删除成功：" + nodePath);
                return true;
            }else{
                log.info("节点不存在，无需删除：" + nodePath);
                return false;
            }
        }catch (KeeperException | InterruptedException e) {
            log.error("删除节点时发生异常：", e);
            throw new ZookeeperException();
        }
    }
    public static void deleteNodeRecursive(ZooKeeper zookeeper, String nodePath) throws Exception {
        List<String> children = zookeeper.getChildren(nodePath, false);
        for (String child : children) {
            String childpath=nodePath+"/"+child;
            deleteNodeRecursive(zookeeper, childpath);
        }
        zookeeper.delete(nodePath,-1);
    }
        /**
         * 判断节点是否存在
         * @param zk
         * @param node
         * @param watcher
         * @return
         */
    public static boolean exists(ZooKeeper zk,String node,Watcher watcher){
        try {
            return zk.exists(node,watcher) != null;
        } catch (KeeperException | InterruptedException e) {
            log.error("判断节点[{}]是否存在是发生异常",node,e);
            throw new ZookeeperException(e);
        }
    }
    /**
     * 关闭zk连接
     * @param zooKeeper
     */
    public static void close(ZooKeeper zooKeeper){
        try {
            if (zooKeeper != null) {
                zooKeeper.close();
            }
        } catch (InterruptedException e) {
            log.error("关闭zk连接异常：",e);
            throw new ZookeeperException();
        }
    }
    /**
     * 获取子元素
     * @param zooKeeper
     * @param path
     * @param watch
     * @return
     */
    public static List<String> getChilderen(ZooKeeper zooKeeper, String path, Watcher watch){
        try{
            return zooKeeper.getChildren(path, watch);
        } catch (KeeperException | InterruptedException e) {
            log.error("获取节点【{}】的子元素时发生异常.",path,e);
            throw new ZookeeperException(e);
        }
    }
    //添加重试操作
    private static <T> T executeRetry(ZooKeeperOperation<T> operation,int maxRetries){
        int retries=0;//重试次数
        while(true){
            try{
                return operation.execute();//尝试zookeeper的操作
            }catch (Exception e){
                if(retries++>=maxRetries){
                    throw new ZookeeperException("操作失败，超过最大重试次数",e);
                }
                try {
                    Thread.sleep(1000*retries);
                    log.warn("进行第{}次重试", retries);
                }catch (InterruptedException ie){
                    Thread.currentThread().interrupt();
                    throw new ZookeeperException("操作被中断");
                }
            }
        }
    }
    @FunctionalInterface//ZooKeeperOperation函数式接口
    private interface ZooKeeperOperation<T>{
        T execute() throws Exception;
    }


}