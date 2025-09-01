package com.feale;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ZookeeperTest {
    private ZooKeeper zooKeeper;
    private CountDownLatch connectionLatch = new CountDownLatch(1);//同步工具，用于等待连接成功
    // 连接 ZooKeeper 服务器
    public void connect(String hosts, int sessionTimeout) throws IOException, InterruptedException {
        zooKeeper = new ZooKeeper(hosts, sessionTimeout, event -> {
            //event.getState()//获取连接状态
            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {//连接成功
                connectionLatch.countDown();//释放锁
            }
        });
        connectionLatch.await();//等待连接成功计数器为0
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

    /**
     *
     * @param path //节点路径
     * @param data //节点数据
     * @param acl  //访问控制列表
     * @param createMode //创建模式
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
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
        return zooKeeper.setData(path, data, -1);//乐观锁当当前znode的数据版本号等于我提供的这个数字时才行。
                                                        //-1表示不检查版本号
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
    //Watcher是一次性的，一旦触发，就会被移除，再次使用需要重新注册
    public void addWatcher(String path) throws KeeperException, InterruptedException {
        zooKeeper.exists(path, new Watcher() {//如果该路径节点发生变化，则注册监听器
            @Override
            public void process(WatchedEvent event) {
                System.out.println("Watcher received event: " + event);
                try {
                    zooKeeper.exists(path, this); // 使用this重新注册
                } catch (KeeperException | InterruptedException e) {
                    System.err.println("Failed to re-register watcher: " + e.getMessage());
                    // 处理异常，可以添加重试逻辑
                    if (e instanceof KeeperException.ConnectionLossException) {
                        try {
                            Thread.sleep(1000); // 等待后重试
                            zooKeeper.exists(path, this);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });

    }
}
