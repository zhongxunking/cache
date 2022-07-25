/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-25 22:51 创建
 */
package org.antframework.cache.core.empty;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.Exceptions;
import org.antframework.cache.core.TransactionalCache;

import java.util.concurrent.Callable;

/**
 * 空缓存（不执行任何缓存操作）
 */
@AllArgsConstructor
public class EmptyTransactionalCache implements TransactionalCache {
    // 名称
    private final String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CachedValue get(Object key) {
        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type, Callable<T> valueLoader) {
        return Exceptions.call(valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
    }

    @Override
    public void remove(Object key) {
    }

    @Override
    public void flush(Runnable callback) {
        callback.run();
    }
}
