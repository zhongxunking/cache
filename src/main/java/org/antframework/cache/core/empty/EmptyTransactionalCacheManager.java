/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-25 22:56 创建
 */
package org.antframework.cache.core.empty;

import org.antframework.cache.core.TransactionalCacheManager;
import org.antframework.cache.core.common.AbstractCacheManager;

/**
 * 空缓存管理器（不执行任何缓存操作）
 */
public class EmptyTransactionalCacheManager extends AbstractCacheManager<EmptyTransactionalCache> implements TransactionalCacheManager {
    /**
     * 事务状态是否是我的
     *
     * @param status 事务状态
     * @return true：属于；false：不属于
     */
    public static boolean isMine(TransactionStatus status) {
        return status instanceof InnerTransactionStatus;
    }

    @Override
    protected EmptyTransactionalCache createCache(String cacheName) {
        return new EmptyTransactionalCache(cacheName);
    }

    @Override
    public TransactionStatus getTransaction(Propagation propagation) {
        return new InnerTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status, Runnable callback) {
        if (status.isCompleted()) {
            throw new IllegalStateException("事务已经结束，不能再调用commit或rollback");
        }
        try {
            if (!status.isRollbackOnly()) {
                callback.run();
            }
        } finally {
            ((InnerTransactionStatus) status).setCompleted();
        }
    }

    @Override
    public void rollback(TransactionStatus status) {
        if (status.isCompleted()) {
            throw new IllegalStateException("事务已经结束，不能再调用commit或rollback");
        }
        status.setRollbackOnly();
        ((InnerTransactionStatus) status).setCompleted();
    }

    // 内部的事务状态
    private static class InnerTransactionStatus implements TransactionStatus {
        // 是否只能回滚
        private boolean rollbackOnly = false;
        // 是否已完成
        private boolean completed = false;

        @Override
        public void setRollbackOnly() {
            rollbackOnly = true;
        }

        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        void setCompleted() {
            completed = true;
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }
    }
}
