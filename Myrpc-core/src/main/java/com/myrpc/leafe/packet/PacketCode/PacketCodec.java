package com.myrpc.leafe.packet.PacketCode;

import com.myrpc.leafe.Serialize.Serializer;
import com.myrpc.leafe.Serialize.SerializerFactory;
import com.myrpc.leafe.common.Constant;
import com.myrpc.leafe.compress.CompressFactory;
import com.myrpc.leafe.compress.Compressor;
import com.myrpc.leafe.packet.MessageFormatConstant;
import com.myrpc.leafe.packet.Packet;
import com.myrpc.leafe.packet.client.rpcRequestPacket;
import com.myrpc.leafe.packet.client.rpcRequestPayload;
import com.myrpc.leafe.packet.server.rpcResponsePacket;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author leafe
 * @Description:用来解码或编码数据包
 *
 *
 */
@Slf4j
public class PacketCodec {
    //自定义魔数
    public static final PacketCodec INSTANCE = new PacketCodec();
    private final Map<Byte, Class<? extends Packet>> packetTypeMap;

    private PacketCodec(){
        packetTypeMap = new HashMap<>();
        packetTypeMap.put(Constant.REQUEST, rpcRequestPacket.class);
        packetTypeMap.put(Constant.RESPONSE, rpcResponsePacket.class);
    }
    public void encode(ByteBuf byteBuf, Packet packet){

        //1.先获取有效负载
        Object payload = extractPayload(packet);

        //先序列化和压缩包
        byte[] body=null;
        if(payload!=null){
            //先拿到合适的序列化器
            Serializer serializer = SerializerFactory.getSerializer(packet.getSerializeType()).getObject();
            body = serializer.serialize(payload);
            //2.压缩
            Compressor compressor = CompressFactory.getCompressor(packet.getCompressType()).getObject();
            body = compressor.compress(body);
        }
        int fullLength = MessageFormatConstant.HEADER_LENGTH + (body == null ? 0 : body.length);

        byteBuf.writeInt(MessageFormatConstant.MAGIC_NUMBER);//魔术
        byteBuf.writeByte(packet.getVersion());//版本号
        byteBuf.writeShort(MessageFormatConstant.HEADER_LENGTH);//头长度
        //总长度还不清楚等序列化完再计算
        byteBuf.writeInt(fullLength);//总长度
        byteBuf.writeByte(packet.getSerializeType());//序列化类型
        byteBuf.writeByte(packet.getCompressType());//压缩类型
        byteBuf.writeByte(packet.getRequestType());//请求类型
        byteBuf.writeLong(packet.getRequestId());//请求id
        byteBuf.writeBytes(body);
    }
    //获取有效负载
    private Object extractPayload(Packet packet) {
        if (packet instanceof rpcRequestPacket) {
            return ((rpcRequestPacket) packet).getPayload();
        } else if (packet instanceof rpcResponsePacket) {
            return ((rpcResponsePacket) packet).getObject();
        }
        throw new IllegalArgumentException("Unsupported packet type: " + packet.getClass().getName());
    }
    public Packet decode(ByteBuf byteBuf){
        //先检查是否能读头部
        if(byteBuf.readableBytes()<MessageFormatConstant.HEADER_LENGTH){
            log.error("数据包长度不够,数据包发送错误");
            return null;
        }
        //标记当前位置
        byteBuf.markReaderIndex();
        //开始读取数据包
        //魔术
        int magic = byteBuf.readInt();
        if(magic!= MessageFormatConstant.MAGIC_NUMBER){
            log.error("Invalid magic number:数据包格式错误");
            return null;
        }
        //版本号
        byte version = byteBuf.readByte();
        //头长度
        short headerLength = byteBuf.readShort();
        if(headerLength!= MessageFormatConstant.HEADER_LENGTH){
            log.error("Invalid header length:数据包头错误");
            return null;
        }
        //总长度
        int fullLength = byteBuf.readInt();
        //三个类型
        byte serializeType = byteBuf.readByte();
        byte compressType = byteBuf.readByte();
        byte requestType = byteBuf.readByte();
        //请求id
        long requestId = byteBuf.readLong();
        log.info("requestType信息:{}",requestType);
        //查看数据包是否完整
        if(byteBuf.readableBytes()<fullLength-headerLength){
            log.error("Invalid data length:数据包不完整");
            return null;
        }
        byte[] body = new byte[fullLength-headerLength];
        byteBuf.readBytes(body);
        Packet packet = extractPayload(body,requestType);
        packet.setVersion(version);
        packet.setSerializeType(serializeType);
        packet.setCompressType(compressType);
        packet.setRequestType(requestType);
        packet.setRequestId(requestId);
        //处理body部分(解压缩和反序列化)
        if(body!=null&&body.length>0){
            Compressor compressor = CompressFactory.getCompressor(compressType).getObject();
            byte[] decompressbody = compressor.decompress(body);
            Serializer serializer = SerializerFactory.getSerializer(serializeType).getObject();
            //获取有效负载
            Object payload;
            if (packet instanceof rpcRequestPacket) {
                payload = serializer.deserialize(decompressbody, rpcRequestPayload.class);
                ((rpcRequestPacket) packet).setPayload((rpcRequestPayload) payload);
            } else if (packet instanceof rpcResponsePacket) {
                payload = serializer.deserialize(decompressbody, Object.class);
                ((rpcResponsePacket) packet).setObject(payload);
            }
        }
        return packet;
    }
    private Packet extractPayload(byte[] bytes,byte requestType){
        Packet packet=null ;
        if(requestType==Constant.REQUEST){
            log.info("对rpcRequestPacket序列化");
            packet = new rpcRequestPacket();
        }
        if(requestType==Constant.RESPONSE){
            log.info("对rpcResponsePacket序列化");
            packet = new rpcResponsePacket();
        }
        return packet;
    }
}
