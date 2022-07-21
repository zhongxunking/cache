/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-21 00:13 创建
 */
package org.antframework.cache.common;

import org.antframework.cache.Cache;

/**
 * 缓存的null
 */
public final class CachedNull implements Cache.CachedValue {
    /**
     * 实例
     */
    public static final CachedNull INSTANCE = new CachedNull();

    private CachedNull() {
    }

    @Override
    public <T> T get(Class<T> type) {
        return null;
    }
}
