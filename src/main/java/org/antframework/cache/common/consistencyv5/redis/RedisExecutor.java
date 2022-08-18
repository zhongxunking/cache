/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-07 18:04 创建
 */
package org.antframework.cache.common.consistencyv5.redis;

import java.util.List;

/**
 * Redis执行器
 */
public interface RedisExecutor {
    /**
     * hash获取字段值
     *
     * @param key   key
     * @param field 字段名
     * @return 字段值
     */
    byte[] hGet(String key, String field);

    /**
     * hash设置字段值
     *
     * @param key   key
     * @param field 字段名
     * @param value 字段值
     */
    void hPut(String key, String field, byte[] value);

    /**
     * hash删除字段
     *
     * @param key   key
     * @param field 删除的字段名
     */
    void hDel(String key, String field);

    /**
     * 设置存活时长
     *
     * @param key      key
     * @param liveTime 存活时长（单位：毫秒）
     */
    void expire(String key, long liveTime);

    /**
     * 执行脚本
     *
     * @param script     脚本
     * @param keys       脚本中的KEYS
     * @param args       脚本中的ARGV
     * @param resultType 返回值类型
     * @param <T>        返回值类型
     * @return 脚本返回值
     */
    <T> T eval(String script, List<String> keys, List<Object> args, Class<T> resultType);

    /**
     * 新增消息监听器
     *
     * @param channel  通道
     * @param listener 监听器
     */
    void addMessageListener(String channel, Runnable listener);

    /**
     * 删除消息监听器
     *
     * @param channel  通道
     * @param listener 监听器
     */
    void removeMessageListener(String channel, Runnable listener);
}
