/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-18 22:24 创建
 */
package org.antframework.cache.storage;

/**
 * 仓库
 */
public interface Storage {
    /**
     * 获取仓库名称
     *
     * @return 仓库名称
     */
    String getName();

    /**
     * 获取值
     *
     * @param key 键
     * @return 值（null表示不存在该键值对）
     */
    byte[] get(String key);

    /**
     * 设置键值对
     *
     * @param key      键
     * @param bytes    值
     * @param liveTime 存活时长（单位：毫秒，负数表示不过期）
     */
    void put(String key, byte[] bytes, long liveTime);

    /**
     * 删除键值对
     *
     * @param key 键
     */
    void remove(String key);
}
