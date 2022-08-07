/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-07 16:17 创建
 */
package org.antframework.cache.boot.annotation;

import lombok.AllArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.core.Ordered;

/**
 * CacheOperationSource的处理器，强制@Cacheable(sync=true)，迫使Spring通过org.springframework.cache.Cache#get(java.lang.Object, java.util.concurrent.Callable)方法获取缓存
 */
@AllArgsConstructor
public class ForceSyncProcessor implements BeanPostProcessor, Ordered {
    // 优先级
    private final int order;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof CacheOperationSource)) {
            return bean;
        }
        return new ForcedSyncAnnotationCacheOperationSource();  // 替换
    }

    @Override
    public int getOrder() {
        return order;
    }
}
