/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-14 14:04 创建
 */
package org.antframework.cache;

import java.util.concurrent.Callable;

/**
 * 缓存
 */
public interface Cache {
    /**
     * 获取名称
     *
     * @return 名称
     */
    String getName();

    /**
     * 获取缓存的值
     *
     * @param key 缓存键
     * @return 缓存的值（null表示无该缓存）
     */
    CachedValue get(Object key);

    /**
     * 获取缓存值
     *
     * @param key  缓存键
     * @param type 缓存值类型
     * @param <T>  缓存值类型
     * @return 缓存值（null表示无缓存或缓存值为null）
     */
    <T> T get(Object key, Class<T> type);

    /**
     * 获取缓存值（如果不存在缓存，则通过值加载器创建缓存）
     *
     * @param key         缓存键
     * @param type        缓存值类型
     * @param valueLoader 值加载器
     * @param <T>         缓存值类型
     * @return 缓存值（缓存值为null或值加载器返回null）
     */
    <T> T get(Object key, Class<T> type, Callable<T> valueLoader);

    /**
     * 设置缓存键值对
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void put(Object key, Object value);

    /**
     * 删除缓存键值对
     *
     * @param key 缓存键
     */
    void remove(Object key);

    /**
     * 缓存的值
     */
    interface CachedValue {
        /**
         * 获取缓存值
         *
         * @param type 缓存值类型
         * @param <T>  缓存值类
         * @return 缓存值
         */
        <T> T get(Class<T> type);
    }
}
