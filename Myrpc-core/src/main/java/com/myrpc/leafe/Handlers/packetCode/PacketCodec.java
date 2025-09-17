package com.myrpc.leafe.Handlers.packetCode;

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
import com.myrpc.leafe.packet.heartBeat.heartBeatPacket;
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
    public void encode(ByteBuf byteBuf, Packet packet) {
        try {
            // 1. 先获取有效负载
            Object payload = extractPayload(packet);
            // 先序列化和压缩包
            byte[] body = null;
            if (payload != null) {
                // 先拿到合适的序列化器
                Serializer serializer = getSerializer(packet.getSerializeType());
                if (serializer == null) {
                    throw new PacketDecoderException("找不到序列化器: " + packet.getSerializeType());
                }
                body = serializer.serialize(payload);

                // 2. 压缩
                Compressor compressor = getCompressor(packet.getCompressType());
                if (compressor == null) {
                    throw new PacketDecoderException("找不到压缩器: " + packet.getCompressType());
                }
                body = compressor.compress(body);
            }

            // 计算头长度
            short headLength = MessageFormatConstant.HEADER_LENGTH;
            if (packet.getRequestType() == RequestType.RESPONSE.getCode()) {
                headLength += 1; // 响应包多一个状态码字段
            }

            // 计算总长度
            int fullLength = headLength + (body == null ? 0 : body.length);
            if (packet.getRequestType() == RequestType.HEARTBEAT.getCode() && body == null) {
                fullLength = MessageFormatConstant.HEADER_LENGTH + MessageFormatConstant.TIMESTAMP_FIELD_LENGTH;
            }

            // 写入固定头部
            byteBuf.writeInt(MessageFormatConstant.MAGIC_NUMBER); // 魔术
            byteBuf.writeByte(packet.getVersion());               // 版本号
            byteBuf.writeShort(headLength);                       // 头长度
            byteBuf.writeInt(fullLength);                         // 总长度
            byteBuf.writeByte(packet.getSerializeType());         // 序列化类型
            byteBuf.writeByte(packet.getCompressType());          // 压缩类型
            byteBuf.writeByte(packet.getRequestType());           // 请求类型
            byteBuf.writeLong(packet.getRequestId());             // 请求id

            // 处理特殊包类型
            if (packet.getRequestType() == RequestType.HEARTBEAT.getCode() && body == null) {
                byteBuf.writeLong(((heartBeatPacket) packet).getTimeStamp());
                return;
            }

            if (packet.getRequestType() == RequestType.RESPONSE.getCode()) {
                byteBuf.writeByte(((rpcResponsePacket) packet).getCode());

                // 如果body为空，直接返回
                if (body == null) {
                    return;
                }
            }
            // 写入body
            if (body != null) {
                byteBuf.writeBytes(body);
            }
        } catch (Exception e) {
            throw new PacketDecoderException("编码数据包时发生错误", e);
        }
    }
    public static Object extractPayload(Packet packet) {
        if (packet instanceof rpcRequestPacket) {
            return ((rpcRequestPacket) packet).getPayload();
        } else if (packet instanceof rpcResponsePacket) {
            return ((rpcResponsePacket) packet).getObject();
        }else if(packet instanceof heartBeatPacket){
            return null;
        }
        throw new IllegalArgumentException("Unsupported packet type: " + packet.getClass().getName());
    }
    public Packet decode(ByteBuf byteBuf) {
        try {
            // 先检查是否能读头部
            if (byteBuf.readableBytes() < MessageFormatConstant.HEADER_LENGTH) {
                throw new PacketDecoderException("数据包太短，无法读取头部");
            }

            // 标记当前位置，以便回溯
            byteBuf.markReaderIndex();

            // 读取并验证魔术字
            int magic = byteBuf.readInt();
            if (magic != MessageFormatConstant.MAGIC_NUMBER) {
                throw new PacketDecoderException("魔术字不匹配: " + magic);
            }

            // 读取头部字段
            byte version = byteBuf.readByte();
            short headerLength = byteBuf.readShort();
            int fullLength = byteBuf.readInt();
            byte serializeType = byteBuf.readByte();
            byte compressType = byteBuf.readByte();
            byte requestType = byteBuf.readByte();
            long requestId = byteBuf.readLong();

            // 检查数据包是否完整
            if (byteBuf.readableBytes() < fullLength - headerLength) {
                byteBuf.resetReaderIndex(); // 重置读取位置
                throw new PacketDecoderException("数据包不完整，期望长度: " + fullLength + ", 实际可读: " +
                        (byteBuf.readableBytes() + headerLength));
            }

            // 创建对应类型的包
            Packet packet = extractPacket(requestType);
            packet.setVersion(version);
            packet.setSerializeType(serializeType);
            packet.setCompressType(compressType);
            packet.setRequestType(requestType);
            packet.setRequestId(requestId);

            // 处理特殊包类型
            if (requestType == RequestType.HEARTBEAT.getCode()) {
                long timeStamp = byteBuf.readLong();
                ((heartBeatPacket) packet).setTimeStamp(timeStamp);
                return packet;
            }

            // 读取响应状态码
            if (requestType == RequestType.RESPONSE.getCode()) {
                ((rpcResponsePacket) packet).setCode(byteBuf.readByte());
            }

            // 读取并处理body
            int bodyLength = fullLength - headerLength;
            if (bodyLength > 0) {
                byte[] body = new byte[bodyLength];
                byteBuf.readBytes(body);

                // 解压缩
                Compressor compressor = getCompressor(compressType);
                if (compressor == null) {
                    throw new PacketDecoderException("找不到解压缩器: " + compressType);
                }
                byte[] decompressedBody = compressor.decompress(body);

                // 反序列化
                Serializer serializer = getSerializer(serializeType);
                if (serializer == null) {
                    throw new PacketDecoderException("找不到反序列化器: " + serializeType);
                }

                // 根据包类型处理负载
                if (packet instanceof rpcRequestPacket) {
                    rpcRequestPayload payload = serializer.deserialize(decompressedBody, rpcRequestPayload.class);
                    ((rpcRequestPacket) packet).setPayload(payload);
                } else if (packet instanceof rpcResponsePacket) {
                    Object payload = serializer.deserialize(decompressedBody, Object.class);
                    ((rpcResponsePacket) packet).setObject(payload);
                }
            }

            return packet;
        } catch (Exception e) {
            if (e instanceof PacketDecoderException) {
                throw (PacketDecoderException) e;
            }
            throw new PacketDecoderException("解码数据包时发生错误", e);
        }
    }

    // 辅助方法
    private Serializer getSerializer(byte serializeType) {
        // 实现获取序列化器的逻辑
        return SerializerFactory.getSerializer(serializeType).getObject();
    }

    private Compressor getCompressor(byte compressType) {
        // 实现获取压缩器的逻辑
        return CompressFactory.getCompressor(compressType).getObject();
    }

    private Packet extractPacket(byte requestType) {
        if (requestType == RequestType.REQUEST.getCode()) {
            return new rpcRequestPacket();
        } else if (requestType == RequestType.RESPONSE.getCode()) {
            return new rpcResponsePacket();
        } else if (requestType == RequestType.HEARTBEAT.getCode()) {
            return new heartBeatPacket();
        }
        throw new IllegalArgumentException("不支持的包类型: " + requestType);
    }
}
