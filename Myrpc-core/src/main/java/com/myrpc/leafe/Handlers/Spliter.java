package com.myrpc.leafe.Handlers;

import com.myrpc.leafe.common.MessageFormatConstant;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class Spliter extends LengthFieldBasedFrameDecoder {
    //编码器帧长度计算公式为:
    //frameLength = lengthFieldOffset + lengthFieldLength + lengthFieldValue + lengthAdjustment
    public Spliter() {
        super(MessageFormatConstant.MAX_FRAME_LENGTH
                ,MessageFormatConstant.LENGTH_FIELD_OFFSET
                ,MessageFormatConstant.LENGTH_FIELD_LENGTH
                ,-(MessageFormatConstant.LENGTH_FIELD_OFFSET+MessageFormatConstant.LENGTH_FIELD_LENGTH)
                ,0
                );
    }
}
