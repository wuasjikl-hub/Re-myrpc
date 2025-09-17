package com.myrpc.leafe.utils.zookeeper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.zookeeper.CreateMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZookeeperNode {
    private String Nodepath;
    private byte[] data;
    private CreateMode createMode;
}
