package com.myrpc.leafe.impl;

import com.myrpc.leafe.HelloService;
import com.myrpc.leafe.bootatrap.annotaion.MyrpcScan;

@MyrpcScan
public class HelloServiceimpl implements HelloService {

    @Override
    public String hello(String name) {
        return "hello";
    }
}
