/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-19 13:47 创建
 */
package org.antframework.cache.core;

import org.antframework.cache.Cache;

/**
 * 支持事务能力的缓存
 */
public interface TransactionalCache extends Cache {
    /**
     * 应用事务中缓存键值对的变更
     *
     * @param callback 执行应用时的回调
     */
    void flush(Runnable callback);
}
