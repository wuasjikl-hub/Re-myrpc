package com.myrpc.leafe.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryAnno {
    int retryCount() default 3;
    long retryDelay() default 100;
    long maxRetryDelay() default 5000;
}
