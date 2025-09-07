package com.myrpc.leafe.enumeration;

public enum RequestType {
    REQUEST((byte) 0x01, "请求"),
    RESPONSE((byte) 0x02, "响应"),
    HEARTBEAT(( byte)0x03, "心跳");
    private byte code;
    private String type;
    RequestType(byte code, String type) {
        this.code = code;
        this.type = type;
    }
    public byte getCode() {
        return code;
    }
    public String getType() {
        return type;
    }
}
