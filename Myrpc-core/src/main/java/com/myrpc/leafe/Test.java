package com.myrpc.leafe;

public class Test {
    public static void main(String[] args) {
        byte b = -86; // 二进制: 10101010
        //int result = b << 8;
        int unsigned = b & 0xFF; // 结果为 170 (00000000000000000000000010101010)
        int res= unsigned << 8;
        System.out.println(res);
    }
}
