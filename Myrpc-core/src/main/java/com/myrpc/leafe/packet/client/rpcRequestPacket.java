package com.myrpc.leafe.packet.client;

import com.myrpc.leafe.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author leafe
 * @description
 * @date 2022-03-09
 * 属性:1. 请求id  2.负载(方法名 参数类型 参数列表 返回值类型) 5. 附加信息类型（心跳/请求）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class rpcRequestPacket extends Packet {
    private long requestId;
    private byte requestType;
    private byte serializerType;
    private byte compressType;
    private rpcRequestPayload payload;
}
