package com.myrpc.leafe.exceptions;

public class ZookeeperException extends RuntimeException{

    public ZookeeperException() {
    }

    public ZookeeperException(String msg,Throwable cause) {
        super(msg,cause);
    }
}
