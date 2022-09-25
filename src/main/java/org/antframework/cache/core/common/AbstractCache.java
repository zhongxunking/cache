/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-20 17:55 创建
 */
package org.antframework.cache.core.common;

import lombok.AllArgsConstructor;
import org.antframework.cache.Cache;
import org.antframework.cache.common.CachedNull;
import org.antframework.cache.common.DefaultCachedValue;
import org.antframework.cache.common.Null;
import org.antframework.cache.serialize.Serializer;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 抽象缓存
 */
@AllArgsConstructor
public abstract class AbstractCache implements Cache {
    // 名称
    private final String name;
    // 键和值是否允许为null
    private final boolean allowNull;
    // 键转换器
    private final Function<Object, String> keyConverter;
    // 序列化器
    private final Serializer serializer;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CachedValue get(Object key) {
        byte[] storedValue = get(convertKey(key));
        if (storedValue == null) {
            return null;
        }
        if (Null.is(storedValue)) {
            return CachedNull.INSTANCE;
        }
        return new DefaultCachedValue(storedValue, serializer);
    }

    /**
     * 获取缓存值
     *
     * @param key 键
     * @return 获取缓存的值
     */
    protected abstract byte[] get(String key);

    @Override
    public <T> T get(Object key, Class<T> type) {
        CachedValue cachedValue = get(key);
        return cachedValue == null ? null : cachedValue.get(type);
    }

    @Override
    public <T> T get(Object key, Class<T> type, Callable<T> valueLoader) {
        CachedValue cachedValue = get(key);
        if (cachedValue != null) {
            return cachedValue.get(type);
        }
        return load(convertKey(key), valueLoader, value -> {
            if (value != null || allowNull) {
                put(convertKey(key), serializeValue(value), true);
            }
        });
    }

    /**
     * 加载值
     *
     * @param key         缓存键
     * @param valueLoader 值加载器
     * @param putCallback 设置缓存键值对的回调
     * @param <T>         值类型
     * @return 值
     */
    protected abstract <T> T load(String key, Callable<T> valueLoader, Consumer<T> putCallback);

    @Override
    public void put(Object key, Object value) {
        if (value == null && !allowNull) {
            throw new IllegalArgumentException("value不能为null");
        }
        put(convertKey(key), serializeValue(value), false);
    }

    // 序列化值
    private byte[] serializeValue(Object value) {
        if (value == null) {
            return Null.getBytes();
        }
        return serializer.serialize(value);
    }

    /**
     * 设置缓存键值对
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param loading 是否正处于加载值中
     */
    protected abstract void put(String key, byte[] value, boolean loading);

    @Override
    public void remove(Object key) {
        remove(convertKey(key));
    }

    /**
     * 删除缓存键值对
     *
     * @param key 缓存键
     */
    protected abstract void remove(String key);

    // 转换缓存键
    private String convertKey(Object key) {
        String convertedKey = keyConverter.apply(key);
        if (convertedKey == null) {
            if (!allowNull) {
                throw new IllegalArgumentException(String.format("转换后的key不能为null(原始key:%s)", key));
            }
            convertedKey = Null.getString();
        }
        return convertedKey;
    }
}
