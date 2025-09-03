package com.myrpc.leafe.utils.warchers;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 只监听节点状态变化
 */
@Slf4j
public class PathWatcher implements Watcher {
    private static final ScheduledExecutorService retryExecutor =
            Executors.newSingleThreadScheduledExecutor();
    private final ZooKeeper zooKeeper;
    private final String path;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 5;

    public PathWatcher(ZooKeeper zooKeeper, String path) {
        this.zooKeeper = zooKeeper;
        this.path = path;
    }
    @Override
    public void process(WatchedEvent event) {
        log.info("Watcher received event: {}", event);
        if (event.getType() == Event.EventType.NodeCreated ||
                event.getType() == Event.EventType.NodeDeleted ||
                event.getType() == Event.EventType.NodeDataChanged ||
                event.getType() == Event.EventType.NodeChildrenChanged){
            retryExecutor.schedule(()->{
                try{
                    //重新注册监听器
                    zooKeeper.exists(path,this);
                    retryCount=0;
                    log.info("重新注册监听器成功:{}",path);
                }catch (Exception e){
                    log.error("重新注册监听器失败:{}",path);
                    if(retryCount<MAX_RETRIES){
                        retryCount++;
                        long delay= 100 * retryCount;
                        retryExecutor.schedule(()->process( event),delay,TimeUnit.MILLISECONDS);
                    }else{
                        log.error("到达最大重试次数，不再注册监听器:{}",retryCount);
                    }
                }

            },0, TimeUnit.MILLISECONDS);
        }
    }
}
