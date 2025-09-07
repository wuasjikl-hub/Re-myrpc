package com.myrpc.leafe.LoadBalancer;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 轮询负载均衡
 * @author leafe
 * Description:实现负载均衡by 轮询
 */
public interface LoadBalancer {
     /**
      * 根据服务名获取一个可用 服务
      * @Description:外部调用者调用
      * 算法实现主要是Selector类
      * @Author: leafe
      * @param serviceName
      * @return
      */
     InetSocketAddress selectServiceAddress(String serviceName);
     /**
      * 重新加载负载均衡
      * @Description:
      * @Author: leafe
      * @return
      */
     void reLoadBalance(String serviceName, List<InetSocketAddress> addresses);
}
