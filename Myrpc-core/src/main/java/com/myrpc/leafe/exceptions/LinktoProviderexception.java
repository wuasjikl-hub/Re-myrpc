package com.myrpc.leafe.exceptions;

public class LinktoProviderexception extends RuntimeException{

    public LinktoProviderexception() {
    }
    public LinktoProviderexception(Throwable cause) {
        super(cause);
    }
    public LinktoProviderexception(String msg) {
        super(msg);
    }
    public LinktoProviderexception(String msg, Throwable cause) {
        super(msg,cause);
    }
}
