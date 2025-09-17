package com.myrpc.leafe.bootatrap.Initializer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

public class ShutdownHolder {
    //挡板
    public static AtomicBoolean isShutdown=new AtomicBoolean(false);
    //计数器
    public static LongAdder longAdder=new LongAdder();

}
