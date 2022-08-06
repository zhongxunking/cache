/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-06 12:02 创建
 */
package org.antframework.cache.boot.transaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.antframework.cache.common.Exceptions;
import org.antframework.cache.core.TransactionalCacheManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 具备缓存管理能力的事务管理器装饰器
 */
@AllArgsConstructor
public class CacheableTransactionManagerDecorator implements PlatformTransactionManager {
    // 目标
    private final PlatformTransactionManager target;
    // 缓存管理器
    private final TransactionalCacheManager cacheManager;

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        TransactionStatus status = target.getTransaction(definition);
        TransactionalCacheManager.TransactionStatus cacheStatus;
        try {
            cacheStatus = cacheManager.getTransaction(computePropagation(definition));
        } catch (Throwable e) {
            target.rollback(status);
            return Exceptions.rethrow(e);
        }
        return new CacheableTransactionStatusDecorator(status, cacheStatus);
    }

    // 计算缓存的事务传播行为
    private TransactionalCacheManager.Propagation computePropagation(TransactionDefinition definition) {
        int propagation;
        if (definition == null) {
            propagation = TransactionDefinition.withDefaults().getPropagationBehavior();
        } else {
            propagation = definition.getPropagationBehavior();
        }
        switch (propagation) {
            case TransactionDefinition.PROPAGATION_REQUIRED:
                return TransactionalCacheManager.Propagation.REQUIRED;
            case TransactionDefinition.PROPAGATION_SUPPORTS:
                return TransactionalCacheManager.Propagation.SUPPORTS;
            case TransactionDefinition.PROPAGATION_MANDATORY:
                return TransactionalCacheManager.Propagation.MANDATORY;
            case TransactionDefinition.PROPAGATION_REQUIRES_NEW:
                return TransactionalCacheManager.Propagation.REQUIRES_NEW;
            case TransactionDefinition.PROPAGATION_NOT_SUPPORTED:
                return TransactionalCacheManager.Propagation.NOT_SUPPORTED;
            case TransactionDefinition.PROPAGATION_NEVER:
                return TransactionalCacheManager.Propagation.NEVER;
            case TransactionDefinition.PROPAGATION_NESTED:
                return TransactionalCacheManager.Propagation.NESTED;
            default:
                throw new IllegalArgumentException(String.format("无法识别的事务传播行为[%d]", propagation));
        }
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        CacheableTransactionStatusDecorator statusDecorator = (CacheableTransactionStatusDecorator) status;
        AtomicBoolean targetCalled = new AtomicBoolean(false);
        try {
            cacheManager.commit(statusDecorator.getCacheStatus(), () -> {
                targetCalled.set(true);
                target.commit(statusDecorator.getTarget());
            });
        } catch (Throwable e) {
            if (!targetCalled.get()) {
                statusDecorator.getTarget().setRollbackOnly();
            }
            Exceptions.rethrow(e);
        } finally {
            if (!targetCalled.get()) {
                target.commit(statusDecorator.getTarget());
            }
        }
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        CacheableTransactionStatusDecorator statusDecorator = (CacheableTransactionStatusDecorator) status;
        try {
            cacheManager.rollback(statusDecorator.getCacheStatus());
        } finally {
            target.rollback(statusDecorator.getTarget());
        }
    }

    /**
     * 具备缓存管理能力的事务状态装饰器
     */
    @AllArgsConstructor
    @Getter
    public static class CacheableTransactionStatusDecorator implements TransactionStatus {
        // 目标
        private final TransactionStatus target;
        // 缓存状态
        private final TransactionalCacheManager.TransactionStatus cacheStatus;

        @Override
        public boolean hasSavepoint() {
            return target.hasSavepoint();
        }

        @Override
        public void flush() {
            target.flush();
        }

        @Override
        public Object createSavepoint() throws TransactionException {
            return target.createSavepoint();
        }

        @Override
        public void rollbackToSavepoint(Object savepoint) throws TransactionException {
            target.rollbackToSavepoint(savepoint);
        }

        @Override
        public void releaseSavepoint(Object savepoint) throws TransactionException {
            target.releaseSavepoint(savepoint);
        }

        @Override
        public boolean isNewTransaction() {
            return target.isNewTransaction();
        }

        @Override
        public void setRollbackOnly() {
            target.setRollbackOnly();
            // 设置缓存只能为回滚
            cacheStatus.setRollbackOnly();
        }

        @Override
        public boolean isRollbackOnly() {
            return target.isRollbackOnly();
        }

        @Override
        public boolean isCompleted() {
            return target.isCompleted();
        }
    }
}
