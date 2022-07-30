/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-30 19:00 创建
 */
package org.antframework.cache.core.statistic;

import lombok.AllArgsConstructor;
import org.antframework.cache.core.TransactionalCacheManager;
import org.antframework.cache.core.common.AbstractCacheManager;
import org.antframework.cache.statistic.CounterManager;

/**
 * 具有统计能力的缓存管理器装饰器
 */
@AllArgsConstructor
public class StatisticalTransactionalCacheManagerDecorator extends AbstractCacheManager<StatisticalTransactionalCacheDecorator> implements TransactionalCacheManager {
    // 目标缓存管理器
    private final TransactionalCacheManager target;
    // 计数器管理器
    private final CounterManager counterManager;

    @Override
    protected StatisticalTransactionalCacheDecorator createCache(String cacheName) {
        return new StatisticalTransactionalCacheDecorator(target.getCache(cacheName), counterManager.get(cacheName));
    }

    @Override
    public TransactionStatus getTransaction(Propagation propagation) {
        return target.getTransaction(propagation);
    }

    @Override
    public void commit(TransactionStatus status, Runnable callback) {
        target.commit(status, callback);
    }

    @Override
    public void rollback(TransactionStatus status) {
        target.rollback(status);
    }
}
