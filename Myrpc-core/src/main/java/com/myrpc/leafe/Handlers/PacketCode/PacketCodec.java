package com.myrpc.leafe.Handlers.PacketCode;

import com.myrpc.leafe.Serialize.Serializer;
import com.myrpc.leafe.Serialize.SerializerFactory;
import com.myrpc.leafe.common.MessageFormatConstant;
import com.myrpc.leafe.compress.CompressFactory;
import com.myrpc.leafe.compress.Compressor;
import com.myrpc.leafe.enumeration.RequestType;
import com.myrpc.leafe.exceptions.PacketDecoderException;
import com.myrpc.leafe.packet.Packet;
import com.myrpc.leafe.packet.client.rpcRequestPacket;
import com.myrpc.leafe.packet.client.rpcRequestPayload;
import com.myrpc.leafe.packet.server.rpcResponsePacket;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    //因为序列化器和压缩器都是无状态的，所以可以缓存起来
    private final Map<Byte, Serializer> serializerCache = new ConcurrentHashMap<>();
    private final Map<Byte, Compressor> compressorCache = new ConcurrentHashMap<>();

    private PacketCodec(){
    }
    public void encode(ByteBuf byteBuf, Packet packet){

        //1.先获取有效负载
        Object payload = extractPayload(packet);

        //先序列化和压缩包
        byte[] body=null;
        if(payload!=null){
            //先拿到合适的序列化器
            Serializer serializer = getSerializer(packet.getSerializeType());
            body = serializer.serialize(payload);
            //2.压缩
            Compressor compressor = getCompressor(packet.getCompressType());
            body = compressor.compress(body);
        }
        int fullLength = MessageFormatConstant.HEADER_LENGTH + (body == null ? 0 : body.length);

        byteBuf.writeInt(MessageFormatConstant.MAGIC_NUMBER);//魔术
        byteBuf.writeByte(packet.getVersion());//版本号
        byteBuf.writeShort(MessageFormatConstant.HEADER_LENGTH);//头长度
        byteBuf.writeInt(fullLength);//总长度
        byteBuf.writeByte(packet.getSerializeType());//序列化类型
        byteBuf.writeByte(packet.getCompressType());//压缩类型
        byteBuf.writeByte(packet.getRequestType());//请求类型
        byteBuf.writeLong(packet.getRequestId());//请求id
        if(packet.getRequestType()==RequestType.HEARTBEAT.getCode()&&body== null){
            return;
        }
        byteBuf.writeBytes(body);
    }
    //获取有效负载
    private Object extractPayload(Packet packet) {
        if (packet.getRequestType() == RequestType.REQUEST.getCode()) {
            return ((rpcRequestPacket) packet).getPayload();
        } else if (packet.getRequestType() == RequestType.RESPONSE.getCode()) {
            return ((rpcResponsePacket) packet).getObject();
        }else if(packet.getRequestType() == RequestType.HEARTBEAT.getCode()){
            return null;
        }
        throw new IllegalArgumentException("Unsupported packet type: " + packet.getClass().getName());
    }
    public Packet decode(ByteBuf byteBuf){
        //先检查是否能读头部
        if(byteBuf.readableBytes()<MessageFormatConstant.HEADER_LENGTH){
            throw new PacketDecoderException("PacketDecoderException:数据包太短");
        }
        //标记当前位置
        byteBuf.markReaderIndex();
        //开始读取数据包
        //魔术
        int magic = byteBuf.readInt();
        if(magic!= MessageFormatConstant.MAGIC_NUMBER){
            throw new PacketDecoderException("Invalid magic number:数据包格式错误");
        }
        //版本号
        byte version = byteBuf.readByte();
        //头长度
        short headerLength = byteBuf.readShort();
        if(headerLength!= MessageFormatConstant.HEADER_LENGTH){
            throw new PacketDecoderException("Invalid header length:数据包头错误");
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
            throw new PacketDecoderException("Invalid data length:数据包不完整");
        }
        byte[] body = new byte[fullLength-headerLength];
        byteBuf.readBytes(body);
        Packet packet = extractPacket(body,requestType);
        packet.setVersion(version);
        packet.setSerializeType(serializeType);
        packet.setCompressType(compressType);
        packet.setRequestType(requestType);
        packet.setRequestId(requestId);
        if(requestType==RequestType.HEARTBEAT.getCode()){
            //心跳包没有负载直接返回
            log.debug("收到心跳包");
            return packet;
        }
        //处理body部分(解压缩和反序列化)
        if(body!=null&&body.length>0){
            Compressor compressor = CompressFactory.getCompressor(compressType).getObject();
            byte[] decompressbody = compressor.decompress(body);
            Serializer serializer = SerializerFactory.getSerializer(serializeType).getObject();
            //获取有效负载
            Object payload;
            if (packet instanceof rpcRequestPacket requestPacket) {
                payload = serializer.deserialize(decompressbody, rpcRequestPayload.class);
                requestPacket.setPayload((rpcRequestPayload) payload);
            } else if (packet instanceof rpcResponsePacket responsePacket) {
                payload = serializer.deserialize(decompressbody, Object.class);
                responsePacket.setObject(payload);
            }
        }
        return packet;
    }
    private Serializer getSerializer(byte serializeType) {
        return serializerCache.computeIfAbsent(serializeType, type ->
                SerializerFactory.getSerializer(type).getObject());
    }

    private Compressor getCompressor(byte compressType) {
        return compressorCache.computeIfAbsent(compressType, type ->
                CompressFactory.getCompressor(type).getObject());
    }
    private Packet extractPacket(byte[] bytes,byte requestType){
        Packet packet=null ;
        if(requestType== RequestType.REQUEST.getCode()){
            packet = new rpcRequestPacket();
        }
        if(requestType==RequestType.RESPONSE.getCode()){
            packet = new rpcResponsePacket();
        }
        return packet;
    }
}
