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
     * 获取缓存
     *
     * @param cacheName 缓存名称
     * @return 支持事务能力的缓存
     */
    @Override
    TransactionalCache getCache(String cacheName);

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
     * 事务传播行为
     */
    enum Propagation {
        /**
         * 融合事务（如果已存在事务，则使用已存在事务；否则创建新事务）
         */
        REQUIRED,
        /**
         * 新事务（不管是否已存在事务，都创建新事务）
         */
        REQUIRES_NEW,
        /**
         * 嵌套事务（如果已存在事务，则在事务内部进行嵌套；否则创建新事务）
         */
        NESTED,
        /**
         * 支持事务（如果已存在事务，则使用已存在事务；否则不使用事务）
         */
        SUPPORTS,
        /**
         * 不支持事务（不使用事务（如果已存在事务，则挂起已存在的事务））
         */
        NOT_SUPPORTED,
        /**
         * 强制事务（如果已存在事务，则使用已存在事务；否则报错）
         */
        MANDATORY,
        /**
         * 强制非事务（不使用事务（如果已存在事务，则报错））
         */
        NEVER
    }

    /**
     * 事务状态
     */
    interface TransactionStatus {
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
