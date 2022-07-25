/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-23 14:24 创建
 */
package org.antframework.cache.core.common;

import org.antframework.cache.Cache;
import org.antframework.cache.CacheManager;
import org.antframework.cache.common.manager.AbstractManager;

import java.util.Set;

/**
 * 抽象缓存管理器
 */
public abstract class AbstractCacheManager<T extends Cache> extends AbstractManager<T> implements CacheManager {
    @Override
    public T getCache(String cacheName) {
        return get(cacheName);
    }

    @Override
    public Set<String> getCacheNames() {
        return getNames();
    }

    @Override
    protected T create(String name) {
        return createCache(name);
    }

    /**
     * 创建缓存
     *
     * @param cacheName 缓存名称
     * @return 缓存
     */
    protected abstract T createCache(String cacheName);
}
