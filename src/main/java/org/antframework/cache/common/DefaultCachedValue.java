/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-19 15:14 创建
 */
package org.antframework.cache.common;

import lombok.AllArgsConstructor;
import org.antframework.cache.Cache;
import org.antframework.cache.serialize.Serializer;

/**
 * 默认的缓存的值
 */
@AllArgsConstructor
public class DefaultCachedValue implements Cache.CachedValue {
    // 存储的值
    private final byte[] storedValue;
    // 序列化器
    private final Serializer serializer;

    @Override
    public <T> T get(Class<T> type) {
        return serializer.deserialize(storedValue, type);
    }
}
