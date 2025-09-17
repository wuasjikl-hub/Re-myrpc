package com.myrpc.leafe.enumeration;

public enum SerializerType {
    SERIALIZERTYPE_JSON((byte) 0x01,"JSON"),
    SERIALIZERTYPE_JDK((byte) 0x02,"JDK"),
    SERIALIZERTYPE_HESSION((byte) 0x03,"HESSION");
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
    public static boolean isValidType(String type) {
        for (SerializerType t : values()) {
            if (t.getType().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
