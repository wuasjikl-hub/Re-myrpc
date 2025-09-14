package com.myrpc.leafe.enumeration;

public enum CompressorType {
    COMPRESSTYPE_GZIP((byte) 0x01,"Gzip");
    private byte code;
    private String type;
    CompressorType(byte code, String type) {
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
        for (CompressorType t : values()) {
            if (t.getType().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
