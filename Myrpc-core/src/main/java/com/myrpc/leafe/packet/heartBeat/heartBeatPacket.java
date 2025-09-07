package com.myrpc.leafe.packet.heartBeat;

import com.myrpc.leafe.enumeration.RequestType;
import com.myrpc.leafe.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class heartBeatPacket extends Packet {
    private long timeStamp;
    public heartBeatPacket(byte compressType
            , byte serializeType, long requestId,long timeStamp){
        super(compressType, serializeType, requestId);
        super.setRequestType(RequestType.HEARTBEAT.getCode());
        this.timeStamp = timeStamp;
    }
}
