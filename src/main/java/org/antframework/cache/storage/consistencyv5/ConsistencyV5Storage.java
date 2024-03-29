/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-15 17:07 创建
 */
package org.antframework.cache.storage.consistencyv5;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.consistencyv5.PuttedValue;
import org.antframework.cache.common.consistencyv5.ReadScopeAware;
import org.antframework.cache.common.consistencyv5.WriteScopeAware;
import org.antframework.cache.common.consistencyv5.redis.RedisExecutor;
import org.antframework.cache.lock.consistencyv5.ConsistencyV5Locker;
import org.antframework.cache.storage.Storage;

import java.util.concurrent.locks.Lock;
import java.util.function.BinaryOperator;

/**
 * 缓存一致性方案5的仓库
 */
@AllArgsConstructor
public class ConsistencyV5Storage implements Storage {
    // 值在hash结构中的字段名
    private static final String VALUE_FIELD = "value";

    // 名称
    private final String name;
    // key生成器
    private final BinaryOperator<String> keyGenerator;
    // 读作用域感知器
    private final ReadScopeAware readScopeAware;
    // 写作用域感知器
    private final WriteScopeAware writeScopeAware;
    // 加锁器
    private final ConsistencyV5Locker locker;
    // Redis执行器
    private final RedisExecutor redisExecutor;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public byte[] get(String key) {
        byte[] value;
        if (readScopeAware.isActive()) {
            Lock readLock = locker.getRWLock(key).readLock();
            if (readLock.tryLock()) {
                readScopeAware.lockSuccess(readLock);
                value = null;
            } else if (readScopeAware.isGetSuccess()) {
                value = readScopeAware.getGotValue();
            } else {
                readScopeAware.lockFail();
                value = null;
            }
        } else {
            value = redisExecutor.hGet(keyGenerator.apply(name, key), VALUE_FIELD);
        }
        return value;
    }

    @Override
    public void put(String key, byte[] value, Long liveTime, boolean valueChanged) {
        if (readScopeAware.isActive()) {
            readScopeAware.setPuttedValue(new PuttedValue(value, liveTime));
        } else {
            String redisKey = keyGenerator.apply(name, key);
            if (writeScopeAware.isActive()) {
                writeScopeAware.addPuttedValue(redisKey, new PuttedValue(value, liveTime));
            } else {
                redisExecutor.hPut(redisKey, VALUE_FIELD, value, liveTime);
            }
        }
    }

    @Override
    public void remove(String key) {
        if (!writeScopeAware.isActive()) {
            redisExecutor.remove(keyGenerator.apply(name, key));
        }
    }
}
