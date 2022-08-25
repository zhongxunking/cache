/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-06 14:18 创建
 */
package org.antframework.cache.boot.cache;

import lombok.AllArgsConstructor;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * Cache适配器
 */
@AllArgsConstructor
public class CacheAdapter implements Cache {
    // 目标
    private final org.antframework.cache.Cache target;
    // 值类型感知器
    private final ValueTypeAware valueTypeAware;

    @Override
    public String getName() {
        return target.getName();
    }

    @Override
    public Object getNativeCache() {
        return target;
    }

    @Override
    public ValueWrapper get(Object key) {
        org.antframework.cache.Cache.CachedValue cachedValue = target.get(key);
        if (cachedValue == null) {
            return null;
        }
        return new ValueWrapperAdapter(cachedValue, computeValueType());
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return target.get(key, type);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return target.get(key, (Class<T>) computeValueType(), valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        target.put(key, value);
    }

    @Override
    public void evict(Object key) {
        target.remove(key);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("不支持clear操作");
    }

    // 计算值类型
    private Class<?> computeValueType() {
        Class<?> valueType = valueTypeAware.getValueType();
        if (valueType == null) {
            valueType = Object.class;
        }
        return valueType;
    }

    /**
     * ValueWrapper适配器
     */
    @AllArgsConstructor
    public static class ValueWrapperAdapter implements ValueWrapper {
        // 目标
        private final org.antframework.cache.Cache.CachedValue target;
        // 缓存值的类型
        private final Class<?> type;

        @Override
        public Object get() {
            return target.get(type);
        }
    }
}
