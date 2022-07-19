/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-14 14:20 创建
 */
package org.antframework.cache.core;

import org.antframework.cache.CacheManager;

/**
 * 具备事务管理能力的缓存管理器
 */
public interface TransactionalCacheManager extends CacheManager {
    /**
     * 获取事务
     *
     * @param propagation 事务传播行为
     * @return 事务状态
     */
    TransactionStatus getTransaction(Propagation propagation);

    /**
     * 提交事务
     *
     * @param status   事务状态
     * @param callback 执行提交时的回调
     */
    void commit(TransactionStatus status, Runnable callback);

    /**
     * 回滚
     *
     * @param status 事务状态
     */
    void rollback(TransactionStatus status);

    /**
     * 转播行为
     */
    enum Propagation {
        REQUIRED, SUPPORTS, MANDATORY, REQUIRES_NEW, NOT_SUPPORTED, NEVER
    }

    /**
     * 事务状态
     */
    interface TransactionStatus {
        /**
         * 是否是新事务
         */
        boolean isNewTransaction();

        /**
         * 设置为只能回滚
         */
        void setRollbackOnly();

        /**
         * 是否设置为只能回滚
         */
        boolean isRollbackOnly();

        /**
         * 是否已完成
         */
        boolean isCompleted();
    }
}
