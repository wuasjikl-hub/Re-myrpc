package com.myrpc.leafe.exceptions;

public class RegistryCenter extends RuntimeException {
    public RegistryCenter(String message) {
        super(message);
    }
    public RegistryCenter(Throwable cause) {
        super(cause);
    }
    public RegistryCenter(String message, Throwable cause) {
        super(message, cause);
    }
}
