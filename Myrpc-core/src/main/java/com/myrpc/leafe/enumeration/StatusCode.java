package com.myrpc.leafe.enumeration;

public enum StatusCode {
    SUCCESS((byte)1,"成功"),
    FAIL((byte)2,"失败"),
    RATE_LIMIT((byte)3,"服务被限流" ),
    RESOURCE_NOT_FOUND((byte)4,"请求的资源不存在" ),
    BECOLSING((byte)5,"调用方法发生异常");
    private byte code;
    private String type;

    StatusCode(byte code, String type) {
        this.code = code;
        this.type = type;
    }

    public byte getCode() {
        return code;
    }

    public void setCode(byte code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
