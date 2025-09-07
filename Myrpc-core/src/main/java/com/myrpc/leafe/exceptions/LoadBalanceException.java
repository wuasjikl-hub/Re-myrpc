package com.myrpc.leafe.exceptions;

public class LoadBalanceException extends RuntimeException {
    public LoadBalanceException(String message) {
        super(message);
    }
    public LoadBalanceException(Throwable cause) {
        super(cause);
    }
    public LoadBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
