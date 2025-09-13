package com.myrpc.leafe.bootatrap.annotaion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)// 注解作用在类上
@Retention(RetentionPolicy.RUNTIME)// 注解保留在运行时
public @interface MyrpcScan {
}
