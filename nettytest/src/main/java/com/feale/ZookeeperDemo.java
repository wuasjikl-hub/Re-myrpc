package com.feale;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;

import java.util.List;

public class ZookeeperDemo {
//    NodeCreated：节点被创建时触发。
//    NodeDeleted：节点被删除时触发。
//    NodeDataChanged：节点数据被修改时触发。
//    NodeChildrenChanged：子节点被创建或者删除时触发。
    public static void main(String[] args) throws Exception {
        ZookeeperTest zkExample = new ZookeeperTest();
        try {
            // 连接 ZooKeeper
            zkExample.connect("192.168.13.131:2181,192.168.13.132:2181,192.168.13.133:2181", 5000);

            // 创建节点
            String nodePath = zkExample.createNode(
                    "/example_node",
                    "Hello ZooKeeper".getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT
            );
            System.out.println("Created node: " + nodePath);

            // 检查节点是否存在
            boolean exists = zkExample.exists("/example_node");
            System.out.println("Node exists: " + exists);
            // 添加监听器
            zkExample.addWatcher("/example_node");
            System.out.println("Watcher added");

            // 获取节点数据
            byte[] data = zkExample.getData("/example_node");
            System.out.println("Node data: " + new String(data));

            // 更新节点数据
            zkExample.setData("/example_node", "Updated data".getBytes());
            System.out.println("Data updated");

            // 获取子节点列表（如果有）
            List<String> children = zkExample.getChildren("/");
            System.out.println("Root children: " + children);

            // 等待一段时间以便观察监听器效果
            Thread.sleep(30000);

            // 删除节点
            zkExample.deleteNode("/example_node");
            System.out.println("Node deleted");
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                zkExample.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
