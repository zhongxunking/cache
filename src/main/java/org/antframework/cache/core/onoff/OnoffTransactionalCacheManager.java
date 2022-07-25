/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-25 23:46 创建
 */
package org.antframework.cache.core.onoff;

import lombok.AllArgsConstructor;
import org.antframework.cache.core.TransactionalCacheManager;
import org.antframework.cache.core.common.AbstractCacheManager;
import org.antframework.cache.core.empty.EmptyTransactionalCacheManager;

import java.util.function.BooleanSupplier;

/**
 * 具有开关能力的缓存管理器
 */
@AllArgsConstructor
public class OnoffTransactionalCacheManager extends AbstractCacheManager<OnoffTransactionalCache> implements TransactionalCacheManager {
    // 开关提供者
    private final BooleanSupplier onoffSupplier;
    // 开启时的缓存管理器
    private final TransactionalCacheManager onCacheManager;
    // 关闭时的缓存管理器
    private final TransactionalCacheManager offCacheManager = new EmptyTransactionalCacheManager();

    @Override
    protected OnoffTransactionalCache createCache(String cacheName) {
        return new OnoffTransactionalCache(
                cacheName,
                onoffSupplier,
                onCacheManager.getCache(cacheName),
                offCacheManager.getCache(cacheName));
    }

    @Override
    public TransactionStatus getTransaction(Propagation propagation) {
        if (onoffSupplier.getAsBoolean()) {
            return onCacheManager.getTransaction(propagation);
        } else {
            return offCacheManager.getTransaction(propagation);
        }
    }

    @Override
    public void commit(TransactionStatus status, Runnable callback) {
        if (!EmptyTransactionalCacheManager.isMine(status)) {
            onCacheManager.commit(status, callback);
        } else {
            offCacheManager.commit(status, callback);
        }
    }

    @Override
    public void rollback(TransactionStatus status) {
        if (!EmptyTransactionalCacheManager.isMine(status)) {
            onCacheManager.rollback(status);
        } else {
            offCacheManager.rollback(status);
        }
    }
}
