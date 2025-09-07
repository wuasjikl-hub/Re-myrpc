package com.myrpc.leafe.wrapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectWrapper<T> {
    private byte code;
    private String name;
    private T object;
}
