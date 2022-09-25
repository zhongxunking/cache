/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-15 11:23 创建
 */
package org.antframework.cache.core.consistencyv5;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.Exceptions;
import org.antframework.cache.common.ObjectReference;
import org.antframework.cache.common.consistencyv5.ReadScopeAware;
import org.antframework.cache.common.consistencyv5.WriteScopeAware;
import org.antframework.cache.core.TransactionalCache;

import java.util.concurrent.Callable;

/**
 * 缓存一致性方案5的缓存装饰器
 */
@AllArgsConstructor
public class ConsistencyV5TransactionalCacheDecorator implements TransactionalCache {
    // 目标
    private final TransactionalCache target;
    // 读作用域感知器
    private final ReadScopeAware readScopeAware;
    // 写作用域感知器
    private final WriteScopeAware writeScopeAware;

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
        ObjectReference<T> value = new ObjectReference<>(null);
        readScopeAware.activate(() -> {
            T v = target.get(key, type, () -> {
                ObjectReference<T> loadedValue = new ObjectReference<>(null);
                readScopeAware.deactivate(() -> {
                    T t = Exceptions.call(valueLoader);
                    loadedValue.set(t);
                });
                return loadedValue.get();
            });
            value.set(v);
        });
        return value.get();
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
        writeScopeAware.activate(() -> target.flush(() -> writeScopeAware.deactivate(callback)));
    }
}
