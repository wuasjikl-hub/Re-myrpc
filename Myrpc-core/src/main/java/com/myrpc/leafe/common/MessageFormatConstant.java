package com.myrpc.leafe.common;
/**
 * 消息格式常量
 */
public class MessageFormatConstant {
    public static final int MAGIC_NUMBER = 0x12345678;
    // magic+version+headerlenth+fulllenth+serializer+compress+messageType+requestId
    public static final short HEADER_LENGTH = (byte)(4+1+2+4+1+1+1+8);
    //总长度字段偏移量
    public static final int LENGTH_FIELD_OFFSET = 7;
    //总长度字段长度
    public static final int LENGTH_FIELD_LENGTH = 4;
    //最大帧长度
    public  static int MAX_FRAME_LENGTH = 1024 * 1024;


    public static final Long REQUESTID = 1L;


}
