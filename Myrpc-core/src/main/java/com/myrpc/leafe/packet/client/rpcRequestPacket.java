package com.myrpc.leafe.packet.client;

import com.myrpc.leafe.enumeration.RequestType;
import com.myrpc.leafe.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author leafe
 * @description
 * @date 2022-03-09
 * 负载(方法名 参数类型 参数列表 返回值类型)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class rpcRequestPacket extends Packet {
    private rpcRequestPayload payload;
    public rpcRequestPacket(byte compressType,
                            byte serializeType, long requestId,rpcRequestPayload payload){
        super(compressType, serializeType, requestId);
        super.setRequestType(RequestType.REQUEST.getCode());
        this.payload = payload;
    }

    public rpcRequestPacket(byte request, Long requestid, rpcRequestPayload requestPayload) {
        super(request, requestid);
        this.payload = requestPayload;
    }
}
