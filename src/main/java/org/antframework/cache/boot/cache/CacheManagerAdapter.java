/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-06 14:32 创建
 */
package org.antframework.cache.boot.cache;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.manager.AbstractManager;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;

/**
 * CacheManager适配器
 */
@AllArgsConstructor
public class CacheManagerAdapter extends AbstractManager<Cache> implements CacheManager {
    // 目标
    private final org.antframework.cache.CacheManager target;

    @Override
    public Cache getCache(String name) {
        return get(name);
    }

    @Override
    public Collection<String> getCacheNames() {
        return getNames();
    }

    @Override
    protected Cache create(String name) {
        return new CacheAdapter(target.getCache(name));
    }
}
