package com.myrpc.leafe.bootatrap;

import com.myrpc.leafe.bootatrap.Initializer.NettyServerBootstrapInitializer;
import com.myrpc.leafe.common.Constant;
import com.myrpc.leafe.config.ProtocolConfig;
import com.myrpc.leafe.config.ReferenceConfig;
import com.myrpc.leafe.config.RegistryConfig;
import com.myrpc.leafe.config.ServiceConfig;
import com.myrpc.leafe.utils.IdGenerator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
    private String application;
    // 注册中心配置
    private RegistryConfig registryConfig;
    // 协议
    private ProtocolConfig protocol;

    public static final IdGenerator idGenerator = new IdGenerator(1, 1);
    // 响应的缓存
    public static final Map<Long, CompletableFuture<Object>> PENDING_REQUESTS = new ConcurrentHashMap<>();


    //创建连接的缓存
    public final static Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);

    // 维护已经发布且暴露的服务列表 key-> interface的全限定名  value -> ServiceConfig
    //这里不能用类级别的泛型参数，因为这个类是静态的要用通配符
    public static final Map<String, ServiceConfig<?>> SERVER_MAP = new ConcurrentHashMap<>();

    private MyRpcBootstrap() {
        System.out.println("MyRpcBootstrap init");
    }
    /**
     * 设置应用名
     *
     * @param application
     * @return
     */
    public MyRpcBootstrap application(String application){
        this.application = application;
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
        this.registryConfig = registryConfig;
        if(log.isDebugEnabled()){
            log.debug("当前注册中心为:{}",registryConfig.getRegistryType());
        }
        //localregistry=registryConfig.getRegistry();
        return this;
    }
    /**
     * 注册服务提供者
     * @return
     */
    public MyRpcBootstrap protocol(ProtocolConfig protocol) {
        this.protocol = protocol;
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
        this.registryConfig.getRegistry().register(serviceConfig);
        //localregistry.register(serviceConfig);
        if(log.isDebugEnabled()){
            log.debug("服务:{}已经被注册",serviceConfig.getInterface().getName());
        }
        //当客户端通过接口名和参数列表发起调用时，服务端要调用对应的服务实现类
        //我们还要维护一个服务名和服务的实现类之间的映射关系
        SERVER_MAP.put(serviceConfig.getInterface().getName(),serviceConfig);
        return this;
    }
    /**
     * 注册多个服务
     * @return
     */
    public <T>MyRpcBootstrap service(List<ServiceConfig<T>> serviceConfigList){
        serviceConfigList.forEach(serviceConfig -> {
            this.registryConfig.getRegistry().register(serviceConfig);
            if(log.isDebugEnabled()){
                log.debug("服务:{}已经被注册",serviceConfig.getInterface().getName());
            }
            SERVER_MAP.put(serviceConfig.getInterface().getName(),serviceConfig);
        });
        return this;
    }

    public void start() {
        ServerBootstrap serverBootstrap = NettyServerBootstrapInitializer.getInstance().getBootstrap();
        ChannelFuture channelFuture = bind(serverBootstrap, Constant.PORT);//绑定端口
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
        referenceConfig.setAnRegistry(this.registryConfig.getRegistry());
        return this;
    }
    public RegistryConfig getregistryConfig(){
        return registryConfig;
    }
}

