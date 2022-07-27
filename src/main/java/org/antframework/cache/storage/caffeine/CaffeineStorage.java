/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-26 22:23 创建
 */
package org.antframework.cache.storage.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.AllArgsConstructor;
import org.antframework.cache.storage.Storage;

/**
 * Caffeine仓库
 */
@AllArgsConstructor
public class CaffeineStorage implements Storage {
    // 名称
    private final String name;
    // Caffeine缓存
    private final Cache<String, byte[]> cache;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public byte[] get(String key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void put(String key, byte[] value, Long liveTime) {
        cache.put(key, value); // Caffeine不支持动态设置存活时长，故而忽略
    }

    @Override
    public void remove(String key) {
        cache.invalidate(key);
    }
}