package com.myrpc.leafe.packet.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/*
 * @Description:描述请求方法
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class rpcRequestPayload implements Serializable {
    //1.接口名
    private String interfaceName;
    //2.方法名
    private String methodName;
    //3.参数类型
    private Class<?>[] parameterTypes;
    //4.参数列表
    private Object[] parameters;
    //5.返回值类型
    private Class<?> returnType;
}
