package com.myrpc.leafe.exceptions;

public class CircuitBreakerException extends RuntimeException {
    public CircuitBreakerException(String message) {
        super(message);
    }
    public CircuitBreakerException(Throwable cause) {
        super(cause);
    }
    public CircuitBreakerException(String message, Throwable cause) {
        super(message, cause);
    }
}
