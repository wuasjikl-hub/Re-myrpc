package com.myrpc.leafe.impl;

import com.myrpc.leafe.GreetingService;
import com.myrpc.leafe.bootatrap.annotaion.MyrpcScan;

@MyrpcScan
public class GreetingServiceimpl implements GreetingService {
    @Override
    public String hello(String name) {
        return "Hello, " + name + "!";
    }

}
