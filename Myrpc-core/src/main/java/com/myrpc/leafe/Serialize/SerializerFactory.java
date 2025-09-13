package com.myrpc.leafe.Serialize;

import com.myrpc.leafe.Serialize.impl.HessionSerializer;
import com.myrpc.leafe.Serialize.impl.JSONSerializer;
import com.myrpc.leafe.Serialize.impl.JdkSerializer;
import com.myrpc.leafe.enumeration.SerializerType;
import com.myrpc.leafe.wrapper.ObjectWrapper;
import com.myrpc.leafe.exceptions.SerializerNOTFOUND;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: leafe
 * @Description:序列化工厂
 * @Date: 2022-03-09
 */
@Slf4j
public class SerializerFactory {
    //维护一个serializer类型和序列化器的映射关系的缓存
    private static final ConcurrentHashMap<Byte, ObjectWrapper<Serializer>> SERIALIZER_CACHE = new ConcurrentHashMap<>(16);
    private static final ConcurrentHashMap<String, ObjectWrapper<Serializer>> SERIALIZER_CACHE_ByName = new ConcurrentHashMap<>(16);

    static {
        //json的序列化器
        ObjectWrapper<Serializer> json = new ObjectWrapper<>(SerializerType.SERIALIZERTYPE_JSON.getCode(), SerializerType.SERIALIZERTYPE_JSON.getType(), new JSONSerializer());
        SERIALIZER_CACHE.put(json.getCode(), json);
        ObjectWrapper<Serializer> jdk = new ObjectWrapper<>(SerializerType.SERIALIZERTYPE_JDK.getCode(), SerializerType.SERIALIZERTYPE_JDK.getType(), new JdkSerializer());
        SERIALIZER_CACHE.put(jdk.getCode(), jdk);
        ObjectWrapper<Serializer> hession = new ObjectWrapper<>(SerializerType.SERIALIZERTYPE_HESSION.getCode(), SerializerType.SERIALIZERTYPE_HESSION.getType(), new HessionSerializer());
        SERIALIZER_CACHE.put(hession.getCode(), hession);

        SERIALIZER_CACHE_ByName.put(json.getName(), json);
        SERIALIZER_CACHE_ByName.put(jdk.getName(), jdk);
        SERIALIZER_CACHE_ByName.put(hession.getName(), hession);
    }
    //对外暴露一个获取序列化器的方法
    public static ObjectWrapper<Serializer> getSerializer(byte code) {
        ObjectWrapper<Serializer> serializerObjectWrapper = SERIALIZER_CACHE.get(code);
        if(serializerObjectWrapper == null){
            log.error("未找到该序列化器");
            throw new SerializerNOTFOUND("未找到该序列化器");
        }
        return serializerObjectWrapper;
    }
    public static ObjectWrapper<Serializer> getSerializerByName(String name) {
        ObjectWrapper<Serializer> serializerObjectWrapper = SERIALIZER_CACHE_ByName.get(name);
        if(serializerObjectWrapper == null){
            log.error("未找到该序列化器");
            throw new SerializerNOTFOUND("未找到该序列化器");
        }
        return serializerObjectWrapper;
    }

    //暴露添加序列化器的方法
    public static void addSerializer(ObjectWrapper<Serializer> serializerObjectWrapper) {
        SERIALIZER_CACHE.put(serializerObjectWrapper.getCode(), serializerObjectWrapper);
    }
}
