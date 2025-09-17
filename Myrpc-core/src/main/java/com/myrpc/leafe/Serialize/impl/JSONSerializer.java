package com.myrpc.leafe.Serialize.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.myrpc.leafe.Serialize.Serializer;
import com.myrpc.leafe.enumeration.SerializerType;
import com.myrpc.leafe.packet.Packet;
import com.myrpc.leafe.packet.client.rpcRequestPacket;
import com.myrpc.leafe.packet.server.rpcResponsePacket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JSONSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {
        if (object == null) {
            return null;
        }
        byte[] res = JSON.toJSONBytes(object);
        //byte[] res = SerializeUtil.serialize(object);
        if (log.isDebugEnabled()) {
            log.debug("对象【{}】json序列化成功，序列化后的字节数为【{}】", object, res.length);
        }
        return res;
    }
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || clazz == null) {
            return null;
        }
        T t = JSON.parseObject(bytes, clazz,JSONReader.Feature.SupportClassForName);
        if (log.isDebugEnabled()) {
            log.debug("类【{}】已经完成了反序列化操作.", clazz);
        }
        return t;
    }

    @Override
    public byte getSerializerType() {
        return SerializerType.SERIALIZERTYPE_JSON.getCode();
    }

    public static Object extractPayload(Packet packet) {
        if (packet instanceof rpcRequestPacket) {
            return ((rpcRequestPacket) packet).getPayload();
        } else if (packet instanceof rpcResponsePacket) {
            return ((rpcResponsePacket) packet).getObject();
        }
        throw new IllegalArgumentException("Unsupported packet type: " + packet.getClass().getName());
    }
//    public static void main(String[] args) {
//        Serializer serializer = new JSONSerializer();
//
//        rpcRequestPayload requestPayload = new rpcRequestPayload();
//        requestPayload.setInterfaceName("xxxx");
//        requestPayload.setMethodName("yyy");
//        requestPayload.setParameters(new Object[]{"xxxx"});
//        requestPayload.setReturnType(String.class);
//        rpcRequestPacket requestPacket = new rpcRequestPacket((byte) 1,  // requestType
//                (byte) 1,  // compressType
//                (byte) 1,  // serializeType
//                12345L,     // requestId
//                requestPayload);
//        Object object = extractPayload(requestPacket);
//
//        byte[] serialize = serializer.serialize(object);
//        System.out.println(Arrays.toString(serialize));
//
////        RequestPayload deserialize = serializer.deserialize(serialize, RequestPayload.class);
////        System.out.println(deserialize);
//    }

}
