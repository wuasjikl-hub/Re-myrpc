package com.myrpc.leafe.exceptions;

public class NotFoundedEnableNodeException extends RuntimeException{

    public NotFoundedEnableNodeException() {
    }
    public NotFoundedEnableNodeException(Throwable cause) {
        super(cause);
    }
    public NotFoundedEnableNodeException(String msg) {
        super(msg);
    }
    public NotFoundedEnableNodeException(String msg, Throwable cause) {
        super(msg,cause);
    }
}
