package com.myrpc.leafe.exceptions;

public class NetworkException extends RuntimeException {
    public NetworkException(String message) {
        super(message);
    }
    public NetworkException() {
        super();
    }
    public NetworkException(Throwable  cause) {
        super(cause);
    }
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }

}
