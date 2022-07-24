/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-24 13:38 创建
 */
package org.antframework.cache.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 *
 */
public abstract class AbstractTransactionalCacheManager2<T extends TransactionalCache> extends AbstractCacheManager<T> implements TransactionalCacheManager {

    private final ThreadLocal<Deque<TransactionContext>> contextStackHolder = ThreadLocal.withInitial(LinkedList::new);

    @Override
    protected T createCache(String cacheName) {
        return createTransactionalCache(cacheName, new InnerTransactionAware(cacheName));
    }

    protected abstract T createTransactionalCache(String cacheName, TransactionAware transactionAware);

    @Override
    public TransactionStatus getTransaction(Propagation propagation) {
        TransactionStatus status;

        TransactionContext currentContext = getCurrentContext();
        if (currentContext.isActive()) {
            TransactionContext newContext;
            switch (propagation) {
                case REQUIRED:
                case SUPPORTS:
                case MANDATORY:
                    status = new InnerTransactionStatus(currentContext, false);
                    break;
                case REQUIRES_NEW:
                    newContext = new TransactionContext(true);
                    contextStackHolder.get().push(newContext);
                    status = new InnerTransactionStatus(newContext, true);
                    break;
                case NOT_SUPPORTED:
                    newContext = new TransactionContext(false);
                    contextStackHolder.get().push(newContext);
                    status = new InnerTransactionStatus(newContext, true);
                case NEVER:
                    throw new IllegalStateException("当前已存在事务，无法根据传播行为[NEVER]创建事务");
                default:
                    throw new IllegalArgumentException("无法识别事务传播行为[" + propagation + "]");
            }
        } else {
            TransactionContext newContext;
            switch (propagation) {
                case REQUIRED:
                case REQUIRES_NEW:
                    newContext = new TransactionContext(true);
                    contextStackHolder.get().push(newContext);
                    status = new InnerTransactionStatus(newContext, true);
                    break;
                case SUPPORTS:
                case NOT_SUPPORTED:
                case NEVER:
                    status = new InnerTransactionStatus(currentContext, false);
                    break;
                case MANDATORY:
                    throw new IllegalStateException("当前不存在事务，无法根据传播行为[MANDATORY]创建事务");
                default:
                    throw new IllegalArgumentException("无法识别事务传播行为[" + propagation + "]");
            }
        }

        return status;
    }

    @Override
    public void commit(TransactionStatus status, Runnable callback) {
        InnerTransactionStatus innerTransactionStatus = (InnerTransactionStatus) status;
        if (innerTransactionStatus.isCompleted()) {
            throw new IllegalStateException("事务已经结束，不能再调用commit或rollback");
        }
        complete(innerTransactionStatus, callback);
    }

    @Override
    public void rollback(TransactionStatus status) {
        InnerTransactionStatus innerTransactionStatus = (InnerTransactionStatus) status;
        if (innerTransactionStatus.isCompleted()) {
            throw new IllegalStateException("事务已经结束，不能再调用commit或rollback");
        }
        innerTransactionStatus.setRollbackOnly();
        complete(innerTransactionStatus, null);
    }

    void complete(InnerTransactionStatus status, Runnable commitCallBack) {
        TransactionContext currentContext = getCurrentContext();
        if (status.getContext() != currentContext) {
            throw new IllegalArgumentException("status对应的事务与当前事务不匹配");
        }
        try {
            if (status.isNewContext()) {
                try {
                    currentContext.complete(commitCallBack);
                } finally {
                    contextStackHolder.get().pop();
                }
            }
        } finally {
            status.setCompleted();
        }
    }

    private TransactionContext getCurrentContext() {
        TransactionContext currentContext = contextStackHolder.get().peek();
        if (currentContext == null) {
            currentContext = new TransactionContext(false);
            contextStackHolder.get().push(currentContext);
        }
        return currentContext;
    }


    @AllArgsConstructor
    private class InnerTransactionAware implements TransactionAware {

        private final String cacheName;

        @Override
        public boolean isActive() {
            return getCurrentContext().isActive();
        }

        @Override
        public Map<Object, Object> getContext() {
            return getCurrentContext().getCacheContext(cacheName);
        }
    }

    private class TransactionContext {

        @Getter(AccessLevel.PACKAGE)
        private final boolean active;

        private final Map<String, Map<Object, Object>> cacheContexts;

        @Getter(AccessLevel.PACKAGE)
        private boolean rollbackOnly = false;

        TransactionContext(boolean active) {
            this.active = active;
            this.cacheContexts = active ? new HashMap<>() : null;
        }

        Map<Object, Object> getCacheContext(String cacheName) {
            if (!active) {
                throw new IllegalStateException("不处于事务中，无法获取事务上下文");
            }
            return cacheContexts.computeIfAbsent(cacheName, k -> new HashMap<>());
        }

        void setRollbackOnly() {
            rollbackOnly = true;
        }

        void complete(Runnable commitCallback) {
            if (active) {
                try {
                    if (!rollbackOnly) {
                        CacheChain cacheChain = new CacheChain(cacheContexts.keySet().iterator());
                        cacheChain.nextFlush(commitCallback);
                    }
                } finally {
                    cacheContexts.clear();
                }
            }
        }

        @AllArgsConstructor(access = AccessLevel.PACKAGE)
        private class CacheChain {
            private final Iterator<String> cacheIterator;

            void nextFlush(Runnable callback) {
                if (cacheIterator.hasNext()) {
                    TransactionalCache cache = getCache(cacheIterator.next());
                    cache.flush(() -> nextFlush(callback));
                } else {
                    callback.run();
                }
            }
        }
    }

    @RequiredArgsConstructor
    @Getter
    private class InnerTransactionStatus implements TransactionStatus {

        private final TransactionContext context;

        private final boolean newContext;

        private boolean completed = false;

        @Override
        public void setRollbackOnly() {
            if (completed) {
                throw new IllegalStateException("事务已完成，无法设置为只能回滚");
            }
            context.setRollbackOnly();
        }

        @Override
        public boolean isRollbackOnly() {
            return context.isRollbackOnly();
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }

        void setCompleted() {
            completed = true;
        }
    }
}
