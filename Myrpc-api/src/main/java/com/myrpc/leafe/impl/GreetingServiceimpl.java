package com.myrpc.leafe.impl;

import com.myrpc.leafe.GreetingService;

public class GreetingServiceimpl implements GreetingService {
    @Override
    public String hello(String name) {
        return "Hello, " + name + "!";
    }

}
