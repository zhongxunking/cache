/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-23 16:14 创建
 */
package org.antframework.cache.boot.configuration;

import org.antframework.cache.boot.CacheProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

import java.util.function.Function;

/**
 * 配置工具
 */
public class ConfigurationUtils {
    /**
     * 获取本地仓库内键值对存活时长提供者
     *
     * @param cachesDetail 缓存集配置
     * @param properties   缓存配置
     * @return 本地仓库内键值对存活时长提供者
     */
    public static Function<String, Long> toLocalLiveTimeSupplier(CacheProperties.CachesDetail<Long> cachesDetail, CacheProperties properties) {
        return toLongSupplier(cachesDetail).andThen(value -> {
            if (value == null) {
                return properties.getLocal().getLiveTime().getMax();
            }
            long liveTime = (long) (value * properties.getLocal().getLiveTime().getScaleRate());
            return Math.min(liveTime, properties.getLocal().getLiveTime().getMax());
        });
    }

    /**
     * 获取缓存具体配置提供者
     *
     * @param cachesDetail 缓存集配置
     * @return 缓存具体配置提供者
     */
    public static Function<String, Long> toLongSupplier(CacheProperties.CachesDetail<Long> cachesDetail) {
        return toSupplier(cachesDetail).andThen(value -> {
            if (value != null && value < 0) {
                return null;
            }
            return value;
        });
    }

    /**
     * 获取缓存具体配置提供者
     *
     * @param cachesDetail 缓存集配置
     * @param <T>          配置类型
     * @return 缓存具体配置提供者
     */
    public static <T> Function<String, T> toSupplier(CacheProperties.CachesDetail<T> cachesDetail) {
        return cacheName -> {
            T value = cachesDetail.getCaches().get(cacheName);
            if (value == null) {
                value = cachesDetail.getDef();
            }
            return value;
        };
    }

    /**
     * 计算命名空间
     *
     * @param properties  缓存配置
     * @param environment env
     * @return 命名空间
     */
    public static String computeNamespace(CacheProperties properties, Environment environment) {
        String namespace = properties.getNamespace();
        if (StringUtils.isBlank(namespace)) {
            namespace = environment.getProperty("spring.application.name");
            if (StringUtils.isBlank(namespace)) {
                throw new IllegalArgumentException("未配置Cache命名空间，可通过ant.cache.namespace或者spring.application.name配置");
            }
        }
        return namespace;
    }
}
