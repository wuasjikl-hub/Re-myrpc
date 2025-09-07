package com.myrpc.leafe.enumeration;

public enum SerializerType {
    SERIALIZERTYPE_JSON((byte) 0x01,"JSON序列化"),
    SERIALIZERTYPE_JDK((byte) 0x02,"JDK序列化"),
    SERIALIZERTYPE_HESSION((byte) 0x03,"HESSION序列化");
    private byte code;
    private String type;
    SerializerType(byte code, String type) {
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
