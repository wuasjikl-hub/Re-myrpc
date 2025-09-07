package com.myrpc.leafe.packet;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class Packet {
    @JSONField(deserialize = false, serialize = false)
    private Byte version = 1;
    // 请求类型
    // 请求的类型，压缩的类型，序列化的方式
    private byte requestType;
    private byte compressType;
    private byte serializeType;
    // 请求id
    private long requestId;
    public Packet(byte requestType, byte compressType, byte serializeType, long requestId) {
        this.requestType = requestType;
        this.compressType = compressType;
        this.serializeType = serializeType;
        this.requestId = requestId;
    }
    public Packet(byte compressType, byte serializeType, long requestId) {
        this.compressType = compressType;
        this.serializeType = serializeType;
        this.requestId = requestId;
    }

    public Packet(byte request, Long requestid) {
        this.requestType = request;
        this.requestId = requestid;
    }
}
