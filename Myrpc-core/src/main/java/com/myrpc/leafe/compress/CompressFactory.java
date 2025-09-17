package com.myrpc.leafe.compress;

import com.myrpc.leafe.compress.impl.GzipCompressor;
import com.myrpc.leafe.enumeration.CompressorType;
import com.myrpc.leafe.wrapper.ObjectWrapper;
import com.myrpc.leafe.exceptions.CompressException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
@Slf4j
public class CompressFactory {
    private static final ConcurrentHashMap<Byte, ObjectWrapper<Compressor>> COMPRESS_CACHE=new ConcurrentHashMap<>(16);
    private static final ConcurrentHashMap<String, ObjectWrapper<Compressor>> COMPRESS_CACHE_ByName=new ConcurrentHashMap<>(16);

    static {
        ObjectWrapper<Compressor> gzip = new ObjectWrapper<>(CompressorType.COMPRESSTYPE_GZIP.getCode(), CompressorType.COMPRESSTYPE_GZIP.getType(), new GzipCompressor());

        COMPRESS_CACHE.put(gzip.getCode(), gzip);
        COMPRESS_CACHE_ByName.put(gzip.getName(), gzip);
    }
    public static ObjectWrapper<Compressor> getCompressor(byte code) {
        if (!COMPRESS_CACHE.containsKey(code)) {
            throw new CompressException("未找到对应的压缩器");
        }
        return COMPRESS_CACHE.get(code);
    }
    public static ObjectWrapper<Compressor> getCompressorByName(String name) {
        if (!COMPRESS_CACHE_ByName.containsKey(name)) {
            throw new CompressException("未找到对应的压缩器");
        }
        return COMPRESS_CACHE_ByName.get(name);
    }
    public static void addCompressor(ObjectWrapper<Compressor> compressorObjectWrapper){
        if(compressorObjectWrapper == null){
            log.error("添加的压缩器不能为空");
            return;
        }
        COMPRESS_CACHE.computeIfAbsent(compressorObjectWrapper.getCode(), k->compressorObjectWrapper);
        COMPRESS_CACHE_ByName.computeIfAbsent(compressorObjectWrapper.getName(), k->compressorObjectWrapper);
    }
}
