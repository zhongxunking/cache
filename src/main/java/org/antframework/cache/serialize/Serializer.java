/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-18 23:37 创建
 */
package org.antframework.cache.serialize;

/**
 * 序列化器
 */
public interface Serializer {
    /**
     * 获取名称
     *
     * @return 名称
     */
    String getName();

    /**
     * 序列化
     *
     * @param obj 需被序列化的对象
     * @return 序列化后的字节数组
     */
    byte[] serialize(Object obj);

    /**
     * 反序列化
     *
     * @param bytes 序列化后的字节数组
     * @param type  对象类型
     * @param <T>   对象类型
     * @return 反序列化后的对象
     */
    <T> T deserialize(byte[] bytes, Class<T> type);
}
