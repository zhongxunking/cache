/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-14 14:00 创建
 */
package org.antframework.cache;

import java.util.Set;

/**
 * 缓存管理器
 */
public interface CacheManager {
    /**
     * 获取缓存
     *
     * @param cacheName 缓存名称
     * @return 缓存
     */
    Cache getCache(String cacheName);

    /**
     * 获取已知的所有缓存名称
     *
     * @return 已知的所有缓存名称
     */
    Set<String> getCacheNames();
}
