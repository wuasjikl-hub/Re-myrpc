package com.myrpc.leafe.bootatrap;

import com.myrpc.leafe.Resolver.SpiResolver;
import com.myrpc.leafe.Resolver.XmlResolver;
import com.myrpc.leafe.bootatrap.Initializer.NettyServerBootstrapInitializer;
import com.myrpc.leafe.bootatrap.annotaion.MyrpcScan;
import com.myrpc.leafe.config.*;
import com.myrpc.leafe.detector.HeartBeatDetector;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class MyRpcBootstrap{
    /**
     * 获取实例
     *
     * @return
     */
    private static MyRpcBootstrap instance = new MyRpcBootstrap();
    /**
     * 应用名
     */
    private final Configration configration;
    //xml解析器
    private XmlResolver xmlResolver;
    //spi解析器
    private SpiResolver spiResolver;

    private MyRpcBootstrap() {
        //先用SPI机制加载新旧配置
        spiResolver = new SpiResolver();
        spiResolver.loadFromSpi();

        this.configration = new Configration();
        try {
            // 尝试加载XML配置
            xmlResolver = new XmlResolver();
            if (xmlResolver.loadFromXml(this.configration)) {
                log.info("成功加载XML配置");
            } else {
                log.warn("XML配置加载失败，使用默认配置");
                resetToDefaultConfiguration();
            }
        } catch (Exception e) {
            log.error("配置加载过程中发生异常，使用默认配置", e);
            // 确保配置是默认值
            resetToDefaultConfiguration();
        }
        System.out.println("MyRpcBootstrap init");
    }
    private void resetToDefaultConfiguration() {
        Configration defaultConfig = new Configration();
        this.configration.setPort(defaultConfig.getPort());
        this.configration.setSerializeType(defaultConfig.getSerializeType());
        this.configration.setCompressType(defaultConfig.getCompressType());
        this.configration.setBalancerName(defaultConfig.getBalancerName());
        this.configration.setApplication(defaultConfig.getApplication());
        this.configration.setRegistryType(defaultConfig.getRegistryType());
        this.configration.setRegistryAddress(defaultConfig.getRegistryAddress());
        this.configration.setIdGenerator(defaultConfig.getIdGenerator());
        this.configration.setRegistryConfig(defaultConfig.getRegistryConfig());
    }
    /**
     * 设置应用名
     *
     * @param application
     * @return
     */
    public MyRpcBootstrap application(String application){
        this.configration.setApplication(application);
        return this;
    }
    public static MyRpcBootstrap getInstance() {
        return instance;
    }
    /**
     * 注册服务注册中心(zookeeper,dubbo,nacos...)
     * @return
     */
    public MyRpcBootstrap registry(RegistryConfig registryConfig) {
        this.configration.setRegistryConfig(registryConfig);
        if(log.isDebugEnabled()){
            log.debug("当前注册中心为:{}",registryConfig.getRegistryType());
        }
        //localregistry=registryConfig.getRegistry();
        return this;
    }
    public MyRpcBootstrap registry() {
//        RegistryConfig zookeeper = new RegistryConfig(configration.getRegistryType(), configration.getRegistryAddress());
//        this.configration.setRegistryConfig(zookeeper);
        if(log.isDebugEnabled()){
            log.debug("当前注册中心为:{}",this.configration.getRegistryConfig().getRegistryType());
        }
        //localregistry=registryConfig.getRegistry();
        return this;
    }
    /**
     * 注册服务提供者
     * @return
     */
    public MyRpcBootstrap protocol(ProtocolConfig protocol) {
        this.configration.setProtocol(protocol);
        if(log.isDebugEnabled()){
            log.debug("当前协议:{}",protocol.toString());
        }
        return this;
    }
    /**
     * 注册服务:将服务的接口以及实现类注册发布到服务注册中心
     * @return
     */
    public <T>MyRpcBootstrap service(ServiceConfig<T> serviceConfig) {
        //todo 等会放到共同的配置类中
        this.configration.getRegistryConfig().getRegistry().register(serviceConfig);
        //localregistry.register(serviceConfig);
        if(log.isDebugEnabled()){
            log.debug("服务:{}已经被注册",serviceConfig.getInterface().getName());
        }
        //当客户端通过接口名和参数列表发起调用时，服务端要调用对应的服务实现类
        //我们还要维护一个服务名和服务的实现类之间的映射关系
        this.configration.getSERVER_MAP().put(serviceConfig.getInterface().getName(),serviceConfig);
        return this;
    }
    /**
     * 注册多个服务
     * @return
     */
    public MyRpcBootstrap service(List<ServiceConfig<?>> serviceConfigList){
        for (ServiceConfig<?> serviceConfig : serviceConfigList) {
            try {
                this.configration.getRegistryConfig().getRegistry().register(serviceConfig);
                if (log.isDebugEnabled()) {
                    log.debug("服务: {} 已被注册", serviceConfig.getInterface().getName());
                }
                this.configration.getSERVER_MAP().put(serviceConfig.getInterface().getName(), serviceConfig);
            } catch (Exception e) {
                log.error("注册服务 {} 失败: {}",
                        serviceConfig.getInterface().getName(), e.getMessage());
            }
        }
        return this;
    }

    public void start() {
        ServerBootstrap serverBootstrap = NettyServerBootstrapInitializer.getInstance().getBootstrap();
        ChannelFuture channelFuture = bind(serverBootstrap, this.configration.getPort());//绑定端口
        // 等待直到服务器通道关闭
        try {
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private static ChannelFuture bind(final ServerBootstrap serverBootstrap, final int port){
        return serverBootstrap.bind(port).addListener(future -> {
            if(future.isSuccess()){
                log.info("启动服务成功，端口:{}",port);
            }else{
                log.error("启动服务失败，端口:{}",port);
            }
        });
    }

    public <T>MyRpcBootstrap reference(ReferenceConfig<T> referenceConfig) {

        //在此进行心跳检测
        HeartBeatDetector.detectHeartBeat(referenceConfig.getInterface().getName());
        referenceConfig.setAnRegistry(this.configration.getRegistryConfig().getRegistry());
        return this;
    }
    public MyRpcBootstrap scan(String packgeName) {
        //扫描包 通过name获得所有的类的全限定名
        List<String> classNames=getAllServiceName(packgeName);
        //2.通过反射获取类的接口
        List<? extends Class<?>> classList = classNames.stream().map(className -> {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }//过滤掉没有注解的类
                }).filter(Objects::nonNull)
                  .filter(clazz -> clazz.getAnnotation(MyrpcScan.class) != null)
                  .filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
                  .collect(Collectors.toList());
        for (Class<?> clazz : classList) {
            try {
                Class<?>[] interfaces = clazz.getInterfaces();
                Object object = clazz.getDeclaredConstructor().newInstance();

                List<ServiceConfig<?>> ServiceConfiglist = new ArrayList<>();
                for (Class<?> anInterface : interfaces) {
                    ServiceConfig<Object> ServiceConfig = new ServiceConfig<>();
                    @SuppressWarnings("unchecked") //强制转换
                    Class<Object>interfaceType =(Class<Object>)anInterface;
                    ServiceConfig.setInterface(interfaceType);
                    ServiceConfig.setRef(object);
                    ServiceConfiglist.add(ServiceConfig);
                }
                service(ServiceConfiglist);
            } catch (InstantiationException | IllegalAccessException |
                     InvocationTargetException | NoSuchMethodException e) {
                log.error("创建类 {} 的实例失败: {}", clazz.getName(), e.getMessage());
            }
        }
        return this;
    }

    private List<String> getAllServiceName(String packgeName) {
        //1.获取基础路径
        String basePath=packgeName.replaceAll("\\.","/");
        //System.out.println(basePath);
        //2.获取url
        URL url = ClassLoader.getSystemClassLoader().getResource(basePath);
        if(url==null){
            log.error("包名不存在");
            return Collections.EMPTY_LIST;
        }
        String absolutePath = url.getPath();
        //3.递归获取所有类名
        List<String> classNames = new ArrayList<>();
        recursionFile(absolutePath,packgeName,classNames);

        return classNames;
    }

    private void recursionFile(String absolutePath,String packegeName,List<String> classNames) {
        File file = new File(absolutePath);
        if(file.isDirectory()){
            File[] files = file.listFiles(pathname ->
                    pathname.isDirectory() || pathname.getName().endsWith(".class"));
            if(files==null){
                return;
            }
            for (File f : files) {
                if(f.isDirectory()){
                    String subPackgename=packegeName+"."+f.getName();
                    recursionFile(f.getAbsolutePath(),subPackgename,classNames);
                }else{//.class文件
                    if(!f.getName().contains("$")) {//过滤掉内部类
                        String className = getClassNameByFileName(packegeName, f.getName());
                        classNames.add(className);
                    }
                }
            }
        }else{//如果是单文件
            if(file.getName().endsWith(".class")&&!file.getName().contains("$")){
                String className = getClassNameByFileName(packegeName, file.getName());
                classNames.add(className);
            }
        }

    }
    private String getClassNameByFileName(String packgeName,String fileName){
        return packgeName+"."+fileName.replace(".class", "");
    }
    public Configration getConfigration() {
        return configration;
    }
    /**
     * 配置序列化的方式
     * @param serializeType 序列化的方式
     */
    public MyRpcBootstrap serialize(String serializeType) {
        this.configration.setSerializeType(serializeType);
        if (log.isDebugEnabled()) {
            log.debug("我们配置了使用的序列化的方式为【{}】.", serializeType);
        }
        return this;
    }

    public MyRpcBootstrap compress(String compressType) {
        this.configration.setCompressType(compressType);
        if (log.isDebugEnabled()) {
            log.debug("我们配置了使用的压缩算法为【{}】.", compressType);
        }
        return this;
    }
}

