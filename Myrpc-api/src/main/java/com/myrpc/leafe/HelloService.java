package com.myrpc.leafe;

import com.myrpc.leafe.bootatrap.annotaion.RetryAnno;

public interface HelloService {
    @RetryAnno
    String hello(String name);
}
