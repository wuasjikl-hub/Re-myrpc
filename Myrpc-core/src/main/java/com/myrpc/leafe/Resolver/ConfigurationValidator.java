package com.myrpc.leafe.Resolver;

import com.myrpc.leafe.config.RegistryConfig;
import com.myrpc.leafe.config.Configration;
import com.myrpc.leafe.enumeration.CompressorType;
import com.myrpc.leafe.enumeration.LoadBalancerType;
import com.myrpc.leafe.enumeration.SerializerType;

public class ConfigurationValidator {
    public static void validate(Configration config) {
        // 检查必要配置项
        if (config.getPort() <= 0 || config.getPort() > 65535) {
            throw new IllegalArgumentException("端口号必须在1-65535之间");
        }

        if (config.getApplication() == null || config.getApplication().trim().isEmpty()) {
            throw new IllegalArgumentException("应用名不能为空");
        }

        if (config.getSerializeType() == null) {
            throw new IllegalArgumentException("序列化类型不能为空");
        }

        if (config.getCompressType() == null) {
            throw new IllegalArgumentException("压缩类型不能为空");
        }

        if (config.getBalancerName() == null) {
            throw new IllegalArgumentException("负载均衡器类型不能为空");
        }

        if (config.getRegistryConfig() == null) {
            throw new IllegalArgumentException("注册中心配置不能为空");
        }

        if (config.getIdGenerator() == null) {
            throw new IllegalArgumentException("ID生成器不能为空");
        }

        // 验证注册中心配置
        RegistryConfig registryConfig = config.getRegistryConfig();
        if (registryConfig.getRegistryType() == null || registryConfig.getRegistryType().trim().isEmpty()) {
            throw new IllegalArgumentException("注册中心类型不能为空");
        }

        if (registryConfig.getRegistryAddress() == null || registryConfig.getRegistryAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("注册中心地址不能为空");
        }

        // 验证序列化类型是否支持
        if (!SerializerType.isValidType(config.getSerializeType())) {
            throw new IllegalArgumentException("不支持的序列化类型: " + config.getSerializeType());
        }

        // 验证压缩类型是否支持
        if (!CompressorType.isValidType(config.getCompressType())) {
            throw new IllegalArgumentException("不支持的压缩类型: " + config.getCompressType());
        }

        // 验证负载均衡器类型是否支持
        if (!LoadBalancerType.isValidType(config.getBalancerName())) {
            throw new IllegalArgumentException("不支持的负载均衡器类型: " + config.getBalancerName());
        }
    }
}
