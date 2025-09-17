package com.myrpc.leafe.exceptions;

public class RpcResponseException extends RuntimeException{

    private byte code;
    private String msg;

    public RpcResponseException(byte code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }
}
