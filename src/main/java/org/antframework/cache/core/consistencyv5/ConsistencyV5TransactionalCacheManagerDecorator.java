/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-22 22:04 创建
 */
package org.antframework.cache.core.consistencyv5;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.consistencyv5.ReadScopeAware;
import org.antframework.cache.common.consistencyv5.WriteScopeAware;
import org.antframework.cache.core.TransactionalCacheManager;
import org.antframework.cache.core.common.AbstractCacheManager;

/**
 * 缓存一致性方案5的缓存装饰器管理器
 */
@AllArgsConstructor
public class ConsistencyV5TransactionalCacheManagerDecorator extends AbstractCacheManager<ConsistencyV5TransactionalCacheDecorator> implements TransactionalCacheManager {
    // 目标缓存管理器
    private final TransactionalCacheManager target;
    // 读作用域感知器
    private final ReadScopeAware readScopeAware;
    // 写作用域感知器
    private final WriteScopeAware writeScopeAware;

    @Override
    protected ConsistencyV5TransactionalCacheDecorator createCache(String cacheName) {
        return new ConsistencyV5TransactionalCacheDecorator(target.getCache(cacheName), readScopeAware, writeScopeAware);
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
