/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-14 17:06 创建
 */
package org.antframework.cache;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.Exceptions;
import org.antframework.cache.core.TransactionalCacheManager;

import java.util.function.Consumer;

/**
 * 缓存操作模板
 */
@AllArgsConstructor
public class CacheTemplate {
    // 缓存管理器
    private final TransactionalCacheManager cacheManager;

    /**
     * 一致性执行
     *
     * @param cacheOperation 缓存操作
     * @param dataOperation  数据操作
     */
    public void consistentDo(Consumer<CacheManager> cacheOperation, Runnable dataOperation) {
        TransactionalCacheManager.TransactionStatus status = cacheManager.getTransaction(TransactionalCacheManager.Propagation.REQUIRES_NEW);
        try {
            cacheOperation.accept(cacheManager);
            cacheManager.commit(status, dataOperation);
        } catch (Throwable e) {
            if (!status.isCompleted()) {
                cacheManager.rollback(status);
            }
            Exceptions.rethrow(e);
        }
    }
}
