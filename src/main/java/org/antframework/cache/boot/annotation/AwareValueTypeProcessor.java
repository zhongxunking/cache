/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-25 12:19 创建
 */
package org.antframework.cache.boot.annotation;

import lombok.AllArgsConstructor;
import org.antframework.cache.boot.cache.ValueTypeAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.core.Ordered;

/**
 * 使CacheInterceptor具有值类型感知能力的的处理器
 */
@AllArgsConstructor
public class AwareValueTypeProcessor implements BeanPostProcessor, Ordered {
    // 值类型感知器
    private final ValueTypeAware valueTypeAware;
    // 优先级
    private final int order;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof CacheInterceptor)) {
            return bean;
        }
        return new AwareValueTypeCacheInterceptorDecorator((CacheInterceptor) bean, valueTypeAware);
    }

    @Override
    public int getOrder() {
        return order;
    }
}
