package com.myrpc.leafe.Serialize.impl;

import com.myrpc.leafe.Serialize.Serializer;
import com.myrpc.leafe.enumeration.SerializerType;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class JdkSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) {
        if(object == null){
            return null;
        }
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(byteArrayOutputStream);
        ){
            stream.writeObject(object);
            if(log.isDebugEnabled()){
                log.debug("对象{}jdk序列化成功序列化后的字节数为【{}】",object,byteArrayOutputStream.size());
            }
            return byteArrayOutputStream.toByteArray();
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if(bytes == null || clazz == null) {
            return null;
        }
        try(ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            ObjectInputStream stream = new ObjectInputStream(byteArrayInputStream);
        ){
            return (T) stream.readObject();
        }catch (IOException|ClassNotFoundException e){
            log.error("对象jdk反序列化失败");
            throw new RuntimeException(e);
        }
    }
    @Override
    public byte getSerializerType() {
        return SerializerType.SERIALIZERTYPE_JDK.getCode();
    }
}
