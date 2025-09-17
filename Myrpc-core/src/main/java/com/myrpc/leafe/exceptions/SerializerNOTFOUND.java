package com.myrpc.leafe.exceptions;

public class SerializerNOTFOUND extends RuntimeException {
    public SerializerNOTFOUND(String message) {
        super(message);
    }
    public SerializerNOTFOUND(Throwable cause) {
        super(cause);
    }
    public SerializerNOTFOUND(String message, Throwable cause) {
        super(message, cause);
    }
}
