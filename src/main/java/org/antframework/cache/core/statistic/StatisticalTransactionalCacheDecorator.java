/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-30 18:44 创建
 */
package org.antframework.cache.core.statistic;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.Exceptions;
import org.antframework.cache.core.TransactionalCache;
import org.antframework.cache.statistic.Counter;

import java.util.concurrent.Callable;

/**
 * 具有统计能力的缓存装饰器
 */
@AllArgsConstructor
public class StatisticalTransactionalCacheDecorator implements TransactionalCache {
    // 目标缓存
    private final TransactionalCache target;
    // 计数器
    private final Counter counter;

    @Override
    public String getName() {
        return target.getName();
    }

    @Override
    public CachedValue get(Object key) {
        return target.get(key);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return target.get(key, type);
    }

    @Override
    public <T> T get(Object key, Class<T> type, Callable<T> valueLoader) {
        return target.get(key, type, () -> {
            long startTime = System.currentTimeMillis();
            T result = null;
            Throwable ex = null;
            try {
                result = valueLoader.call();
            } catch (Throwable e) {
                ex = e;
            }
            long endTime = System.currentTimeMillis();

            if (ex == null && result != null) {
                counter.incLoadHits(endTime, endTime - startTime);
            } else {
                counter.incLoadMisses(endTime, endTime - startTime);
                if (ex != null) {
                    return Exceptions.rethrow(ex);
                }
            }
            return result;
        });
    }

    @Override
    public void put(Object key, Object value) {
        target.put(key, value);
    }

    @Override
    public void remove(Object key) {
        target.remove(key);
    }

    @Override
    public void flush(Runnable callback) {
        target.flush(callback);
    }
}
