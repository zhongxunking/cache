/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-26 22:06 创建
 */
package org.antframework.cache.serialize.hessian;

import org.antframework.cache.common.manager.AbstractManager;
import org.antframework.cache.serialize.Serializer;
import org.antframework.cache.serialize.SerializerManager;

/**
 * Hessian序列化器管理器
 */
public class HessianSerializerManager extends AbstractManager<Serializer> implements SerializerManager {
    @Override
    protected Serializer create(String name) {
        return new HessianSerializer(name);
    }
}
