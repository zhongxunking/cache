/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-25 11:12 创建
 */
package org.antframework.cache.boot.annotation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.antframework.cache.boot.cache.ValueTypeAware;
import org.antframework.cache.common.Exceptions;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 能感知值类型的CacheInterceptor装饰器
 */
@AllArgsConstructor
public class AwareValueTypeCacheInterceptorDecorator extends CacheInterceptor {
    // 目标
    private final CacheInterceptor target;
    // 值类型感知器
    private final ValueTypeAware valueTypeAware;
    // 值类型缓存
    private final Map<Key, Class<?>> keyValueTypes = new ConcurrentHashMap<>();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        AtomicReference<Object> result = new AtomicReference<>(null);
        Class<?> valueType = getValueType(invocation);
        valueTypeAware.doAware(valueType, () -> {
            try {
                result.set(target.invoke(invocation));
            } catch (Throwable e) {
                Exceptions.rethrow(e);
            }
        });
        return result.get();
    }

    // 获取值类型
    private Class<?> getValueType(MethodInvocation invocation) {
        Key key = new Key(invocation.getThis().getClass(), invocation.getMethod());
        Class<?> valueType = keyValueTypes.get(key);
        if (valueType == null) {
            valueType = keyValueTypes.computeIfAbsent(key, k -> computeValueType(invocation));
        }
        return valueType;
    }

    // 计算值类型
    private Class<?> computeValueType(MethodInvocation invocation) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(invocation.getThis());
        Method method = AopUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        Class<?> returnType = method.getReturnType();
        if (returnType == Optional.class) {
            ResolvableType resolvableType = ResolvableType.forMethodReturnType(method, targetClass);
            returnType = resolvableType.getGeneric(0).resolve(Object.class);
        }
        return returnType;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        target.setBeanFactory(beanFactory);
    }

    @Override
    public void afterPropertiesSet() {
        target.afterPropertiesSet();
    }

    @Override
    public void afterSingletonsInstantiated() {
        target.afterSingletonsInstantiated();
    }

    // 缓存key
    @AllArgsConstructor
    @Getter
    private static final class Key {
        // 类
        private final Class<?> clazz;
        // 方法
        private final Method method;

        @Override
        public int hashCode() {
            return Objects.hash(clazz, method);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Key)) {
                return false;
            }
            Key other = (Key) obj;
            return Objects.equals(clazz, other.clazz) && Objects.equals(method, other.method);
        }
    }
}
