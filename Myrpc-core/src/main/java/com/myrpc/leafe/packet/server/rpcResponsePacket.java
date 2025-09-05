package com.myrpc.leafe.packet.server;

import com.myrpc.leafe.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class rpcResponsePacket extends Packet {
    private Object object;
    private byte code;//0成功，1失败
    rpcResponsePacket(byte requestType, byte compressType, byte serializeType, long requestId,Object object,byte code){
        super(requestType, compressType, serializeType, requestId);
        this.object = object;
        this.code = code;
    }

}
