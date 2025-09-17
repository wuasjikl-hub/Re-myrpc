package com.myrpc.leafe.exceptions;

public class CompressException extends RuntimeException {
    public CompressException(String message) {
        super(message);
    }
    public CompressException(Throwable cause) {
        super(cause);
    }
    public CompressException(String message, Throwable cause) {
        super(message, cause);
    }
}
