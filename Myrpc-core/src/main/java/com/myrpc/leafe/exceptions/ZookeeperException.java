package com.myrpc.leafe.exceptions;

public class ZookeeperException extends RuntimeException{

    public ZookeeperException() {
    }
    public ZookeeperException(Throwable cause) {
        super(cause);
    }
    public ZookeeperException(String msg) {
        super(msg);
    }
    public ZookeeperException(String msg,Throwable cause) {
        super(msg,cause);
    }
}
