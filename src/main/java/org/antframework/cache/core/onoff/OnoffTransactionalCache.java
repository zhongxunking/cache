/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-25 23:34 创建
 */
package org.antframework.cache.core.onoff;

import lombok.AllArgsConstructor;
import org.antframework.cache.core.TransactionalCache;

import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;

/**
 * 具有开关能力的缓存
 */
@AllArgsConstructor
public class OnoffTransactionalCache implements TransactionalCache {
    // 名称
    private final String name;
    // 开关提供者
    private final BooleanSupplier onoffSupplier;
    // 开启时的缓存
    private final TransactionalCache onCache;
    // 关闭时的缓存
    private final TransactionalCache offCache;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CachedValue get(Object key) {
        return chooseCache().get(key);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return chooseCache().get(key, type);
    }

    @Override
    public <T> T get(Object key, Class<T> type, Callable<T> valueLoader) {
        return chooseCache().get(key, type, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        chooseCache().put(key, value);
    }

    @Override
    public void remove(Object key) {
        chooseCache().remove(key);
    }

    @Override
    public void flush(Runnable callback) {
        chooseCache().flush(callback);
    }

    // 选择缓存
    private TransactionalCache chooseCache() {
        if (onoffSupplier.getAsBoolean()) {
            return onCache;
        } else {
            return offCache;
        }
    }
}
