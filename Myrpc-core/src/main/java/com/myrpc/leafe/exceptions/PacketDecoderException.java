package com.myrpc.leafe.exceptions;

public class PacketDecoderException extends RuntimeException {
    public PacketDecoderException(String message) {
        super(message);
    }
    public PacketDecoderException(Throwable cause) {
        super(cause);
    }
    public PacketDecoderException(String message, Throwable cause) {
        super(message, cause);
    }
}
