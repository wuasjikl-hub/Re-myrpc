package com.myrpc.leafe;

import com.myrpc.leafe.bootatrap.annotaion.RetryAnno;

public interface GreetingService {
    @RetryAnno
    String hello(String name);
}
