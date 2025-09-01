package com.feale;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class Mywatcher implements Watcher {

    @Override
    public void process(WatchedEvent watchedEvent) {
        if(watchedEvent.getType() == Event.EventType.None)//连接事件类型
        {
            if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
                System.out.println("连接成功");
            }
        }
    }
}
