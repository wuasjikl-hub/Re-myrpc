package com.myrpc.leafe.enumeration;

public enum StatusCode {
    SUCCESS((byte)1,"成功"),
    FAIL((byte)2,"失败");

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
