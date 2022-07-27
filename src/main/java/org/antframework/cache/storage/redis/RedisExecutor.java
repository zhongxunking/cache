/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-27 22:02 创建
 */
package org.antframework.cache.storage.redis;

/**
 * Redis执行器
 */
public interface RedisExecutor {
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
     * @param value    值
     * @param liveTime 存活时长（单位：毫秒，null表示不过期）
     */
    void put(String key, byte[] value, Long liveTime);

    /**
     * 删除键值对
     *
     * @param key 键
     */
    void remove(String key);
}
