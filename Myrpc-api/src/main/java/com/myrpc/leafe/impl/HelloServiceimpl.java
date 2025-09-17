package com.myrpc.leafe.impl;

import com.myrpc.leafe.HelloService;

public class HelloServiceimpl implements HelloService {

    @Override
    public String hello(String name) {
        return "hello";
    }
}
