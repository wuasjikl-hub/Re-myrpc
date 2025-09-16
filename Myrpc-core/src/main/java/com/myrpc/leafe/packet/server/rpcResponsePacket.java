package com.myrpc.leafe.packet.server;

import com.myrpc.leafe.enumeration.RequestType;
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
    public rpcResponsePacket(byte compressType, byte serializeType, long requestId,byte code,Object object){
        super(compressType, serializeType, requestId);
        super.setRequestType(RequestType.RESPONSE.getCode());
        this.code = code;
        this.object = object;

    }

}
