/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-25 13:00 创建
 */
package org.antframework.cache.serialize.jackson;

import org.antframework.cache.common.manager.AbstractManager;
import org.antframework.cache.serialize.Serializer;
import org.antframework.cache.serialize.SerializerManager;

/**
 * Jackson序列化器管理器
 */
public class JacksonSerializerManager extends AbstractManager<Serializer> implements SerializerManager {
    @Override
    protected Serializer create(String name) {
        return new JacksonSerializer(name);
    }
}
