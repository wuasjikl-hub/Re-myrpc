package com.myrpc.leafe.exceptions;

public class SpiException extends RuntimeException{

    public SpiException() {
    }
    public SpiException(Throwable cause) {
        super(cause);
    }
    public SpiException(String msg) {
        super(msg);
    }
    public SpiException(String msg, Throwable cause) {
        super(msg,cause);
    }
}
