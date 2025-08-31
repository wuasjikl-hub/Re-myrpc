package com.feale;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ZookeeperTest {
    private ZooKeeper zooKeeper;
    private CountDownLatch connectionLatch = new CountDownLatch(1);
    // 连接 ZooKeeper 服务器
    public void connect(String hosts, int sessionTimeout) throws IOException, InterruptedException {
        zooKeeper = new ZooKeeper(hosts, sessionTimeout, event -> {
            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                connectionLatch.countDown();
            }
        });
        connectionLatch.await();
        this.close();
        System.out.println("Connected to ZooKeeper successfully");
    }

    // 关闭连接
    public void close() throws InterruptedException {
        if (zooKeeper != null) {
            System.out.println("ZooKeeper connection closed");
        }
    }
    // 创建节点
    public String createNode(String path, byte[] data, List<ACL> acl, CreateMode createMode)
            throws KeeperException, InterruptedException {
        return zooKeeper.create(path, data, acl, createMode);
    }

    // 检查节点是否存在
    public boolean exists(String path) throws KeeperException, InterruptedException {
        Stat stat = zooKeeper.exists(path, false);
        return stat != null;
    }

    // 获取节点数据
    public byte[] getData(String path) throws KeeperException, InterruptedException {
        return zooKeeper.getData(path, false, null);
    }

    // 设置节点数据
    public Stat setData(String path, byte[] data) throws KeeperException, InterruptedException {
        return zooKeeper.setData(path, data, -1);
    }

    // 删除节点
    public void deleteNode(String path) throws KeeperException, InterruptedException {
        zooKeeper.delete(path, -1);
    }

    // 获取子节点列表
    public List<String> getChildren(String path) throws KeeperException, InterruptedException {
        return zooKeeper.getChildren(path, false);
    }

    // 添加监听器
    public void addWatcher(String path) throws KeeperException, InterruptedException {
        zooKeeper.exists(path, event -> {
            System.out.println("Watcher received event: " + event);

            // 重新注册监听器以继续监听
            try {
                zooKeeper.exists(path, watchedEvent -> {
                    try {
                        addWatcher(path);
                    } catch (KeeperException | InterruptedException e) {
                        // 处理异常，可以选择重试或记录日志
                        System.err.println("Failed to re-register watcher: " + e.getMessage());
                        // 如果是连接问题，可以尝试重新连接
                        if (e instanceof KeeperException.ConnectionLossException) {
                            System.out.println("Connection loss, reconnecting...");
                        }
                    }
                });
            } catch (KeeperException | InterruptedException e) {
                System.err.println("Error in watcher: " + e.getMessage());
            }
        });
    }
}
