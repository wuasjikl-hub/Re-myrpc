package com.myrpc.leafe.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZookeeperNode {
    private String Nodepath;
    private byte[] data;
}
