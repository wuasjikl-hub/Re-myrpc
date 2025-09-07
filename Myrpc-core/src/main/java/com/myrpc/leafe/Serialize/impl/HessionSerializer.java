package com.myrpc.leafe.Serialize.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.myrpc.leafe.Serialize.Serializer;
import com.myrpc.leafe.enumeration.SerializerType;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
@Slf4j
public class HessionSerializer implements Serializer {

    @Override
    public byte[] serialize(Object object) {
        if(object == null){
            return null;
        }
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ){
            Hessian2Output hessian2Output = new Hessian2Output(bos);
            hessian2Output.writeObject(object);
            hessian2Output.flush();
            byte[] byteArray = bos.toByteArray();
            if(log.isDebugEnabled()){
                log.debug("对象{}Hession序列化成功序列化后的字节数为【{}】",object,byteArray.length);
            }
            return byteArray;
        }catch (IOException e){
            throw new RuntimeException("Hessian serialization failed",e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if(bytes == null || clazz == null){
            return null;
        }
        try(ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);){
            Hessian2Input hessian2Input = new Hessian2Input(byteArrayInputStream);
            if(log.isDebugEnabled()){
                log.debug("对象{}Hession反序列化成功",clazz);
            }
            return (T) hessian2Input.readObject();
        }catch (IOException e){
            throw new RuntimeException("Hessian deserialization failed",e);
        }
    }

    @Override
    public byte getSerializerType() {
        return SerializerType.SERIALIZERTYPE_HESSION.getCode();
    }
}
