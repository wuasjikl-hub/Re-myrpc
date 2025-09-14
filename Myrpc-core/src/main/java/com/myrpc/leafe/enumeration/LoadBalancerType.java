package com.myrpc.leafe.enumeration;

public enum LoadBalancerType {
    LoadBalancerType_ConsistentHash((byte) 0x01,"ConsistentHash"),
    LoadBalancerType_MinimumResponseTime((byte) 0x02,"MinimumResponseTime"),
    LoadBalancerType_RoundRobin((byte) 0x03,"RoundRobin");

    private byte code;
    private String type;
    LoadBalancerType(byte code, String type) {
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
        for (LoadBalancerType t : values()) {
            if (t.getType().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
