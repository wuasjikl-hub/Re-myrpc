package com.myrpc.leafe.spi;

import com.myrpc.leafe.compress.Compressor;
import com.myrpc.leafe.exceptions.SpiException;
import com.myrpc.leafe.wrapper.ObjectWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class SpiHandler {
    // 定义一个basePath
    private static final String BASE_PATH = "META-INF/services";

    //定义一个缓存，存放所有实现类
    private static final Map<Class<?>,List<ObjectWrapper<?>>> SPI_IMPLEMENT = new ConcurrentHashMap<>(32);

    //存放所有文件名称
    private static final Map<String, List<String>> SPI_CONTENT = new ConcurrentHashMap<>(8);
    static {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL fileUrl = classLoader.getResource(BASE_PATH);
        if(fileUrl != null) {
            File file = new File(fileUrl.getPath());
            File[] children = file.listFiles();
            if(children != null) {
                for (File child : children) {
                    String key = child.getName();
                    List<String> value = getfilecontent(child);
                    SPI_CONTENT.put(key, value);
                }
            }
        }
    }

    private static List<String> getfilecontent(File f) {
        try(FileReader fileReader = new FileReader(f);
            BufferedReader bufferedReader = new BufferedReader(fileReader)){
            List<String> values = new ArrayList<>();
            while(true) {
                String s = bufferedReader.readLine();
                if(s==null||s.equals(""))break;
                values.add(s);
            }
            return values;
        }catch (IOException e){
            throw new SpiException("read file error",e);
        }
    }
    /**
     * 通过类名初始化出所有实现类并加入缓存中
     * @param clazz
     */
    private static void buildCache(Class<?> clazz) {
        String name = clazz.getName();
        List<String> classimpls = SPI_CONTENT.get(name);
        if(classimpls == null||classimpls.size()==0)return;
        List<ObjectWrapper<?>> objlist = new ArrayList<>();
        for (String classimpl : classimpls) {
            try {
                String[] split = classimpl.split("-");
                if (split.length != 3) {
                    throw new RuntimeException("配置文件不合法");
                }
                Byte code = Byte.valueOf(split[0]);
                String type = split[1];
                String implName = split[2];
                Class<?> aClass = Class.forName(implName);
                Object implobj = aClass.getDeclaredConstructor().newInstance();
                ObjectWrapper<?> objectObjectWrapper = new ObjectWrapper<>(code, type, implobj);
                objlist.add(objectObjectWrapper);
            }catch (Exception e){
                throw new SpiException("配置文件解析错误",e);
            }
        }
        SPI_IMPLEMENT.put(clazz,objlist);
    }
    /**
     * 获取第一个和当前服务相关的实例
     * @param clazz 一个服务接口的class实例
     * @return      实现类的实例
     * @param <T>
     */
    public synchronized static <T> ObjectWrapper<T> get(Class<T> clazz) {

        // 1、优先走缓存
        List<ObjectWrapper<?>> objectWrappers = SPI_IMPLEMENT.get(clazz);
        if(objectWrappers != null && objectWrappers.size() > 0){
            return (ObjectWrapper<T>)objectWrappers.get(0);
        }

        // 2、构建缓存
        buildCache(clazz);

        List<ObjectWrapper<?>>  result = SPI_IMPLEMENT.get(clazz);
        if (result == null || result.size() == 0){
            return null;
        }

        // 3、再次尝试获取第一个
        return (ObjectWrapper<T>)result.get(0);
    }


    /**
     * 获取所有和当前服务相关的实例
     * @param clazz 一个服务接口的class实例
     * @return       实现类的实例集合
     * @param <T>
     */
    public synchronized static <T> List<ObjectWrapper<T>> getList(Class<T> clazz) {

        // 1、优先走缓存
        List<ObjectWrapper<?>> objectWrappers = SPI_IMPLEMENT.get(clazz);
        if(objectWrappers != null && objectWrappers.size() > 0){
            return objectWrappers.stream().map( wrapper -> (ObjectWrapper<T>)wrapper )
                    .collect(Collectors.toList());
        }

        // 2、构建缓存
        buildCache(clazz);

        // 3、再次获取
        objectWrappers = SPI_IMPLEMENT.get(clazz);
        if(objectWrappers != null && objectWrappers.size() > 0){
            return objectWrappers.stream().map( wrapper -> (ObjectWrapper<T>)wrapper )
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }


    public static void main(String[] args) {
        buildCache(Compressor.class);
    }

}
