/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-25 13:45 创建
 */
package org.antframework.cache.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * 抽象具备事务管理能力的缓存管理器
 */
public abstract class AbstractTransactionalCacheManager<T extends TransactionalCache> extends AbstractCacheManager<T> implements TransactionalCacheManager {
    // 栈持有器
    private final ThreadLocal<Deque<Frame>> stackHolder = ThreadLocal.withInitial(LinkedList::new);

    @Override
    protected T createCache(String cacheName) {
        return createTransactionalCache(cacheName, new InnerTransactionAware(cacheName));
    }

    /**
     * 创建支持事务能力的缓存
     *
     * @param cacheName        缓存名称
     * @param transactionAware 事务感知
     * @return 支持事务能力的缓存
     */
    protected abstract T createTransactionalCache(String cacheName, TransactionAware transactionAware);

    @Override
    public TransactionStatus getTransaction(Propagation propagation) {
        TransactionStatus status;

        Frame currentFrame = getCurrentFrame();
        if (currentFrame.isTxEnabled()) {
            Frame newFrame;
            switch (propagation) {
                case REQUIRED:
                case SUPPORTS:
                case MANDATORY:
                    status = new InnerTransactionStatus(currentFrame, false, null);
                    break;
                case NESTED:
                    status = new InnerTransactionStatus(currentFrame, false, currentFrame.createSavepoint());
                    break;
                case REQUIRES_NEW:
                    newFrame = new Frame(true);
                    stackHolder.get().push(newFrame);
                    status = new InnerTransactionStatus(newFrame, true, null);
                    break;
                case NOT_SUPPORTED:
                    newFrame = new Frame(false);
                    stackHolder.get().push(newFrame);
                    status = new InnerTransactionStatus(newFrame, true, null);
                    break;
                case NEVER:
                    throw new IllegalStateException("当前已存在事务，无法根据传播行为[NEVER]获取事务");
                default:
                    throw new IllegalArgumentException("无法识别事务传播行为[" + propagation + "]");
            }
        } else {
            Frame newFrame;
            switch (propagation) {
                case SUPPORTS:
                case NOT_SUPPORTED:
                case NEVER:
                    status = new InnerTransactionStatus(currentFrame, false, null);
                    break;
                case REQUIRED:
                case REQUIRES_NEW:
                case NESTED:
                    newFrame = new Frame(true);
                    stackHolder.get().push(newFrame);
                    status = new InnerTransactionStatus(newFrame, true, null);
                    break;
                case MANDATORY:
                    throw new IllegalStateException("当前不存在事务，无法根据传播行为[MANDATORY]获取事务");
                default:
                    throw new IllegalArgumentException("无法识别事务传播行为[" + propagation + "]");
            }
        }

        return status;
    }

    @Override
    public void commit(TransactionStatus status, Runnable callback) {
        if (status.isCompleted()) {
            throw new IllegalStateException("事务已经结束，不能再调用commit或rollback");
        }
        complete((InnerTransactionStatus) status, callback);
    }

    @Override
    public void rollback(TransactionStatus status) {
        if (status.isCompleted()) {
            throw new IllegalStateException("事务已经结束，不能再调用commit或rollback");
        }
        status.setRollbackOnly();
        complete((InnerTransactionStatus) status, null);
    }

    // 完成
    private void complete(InnerTransactionStatus status, Runnable commitCallback) {
        Frame currentFrame = getCurrentFrame();
        if (status.getFrame() != currentFrame) {
            throw new IllegalArgumentException("status对应的事务与当前事务不匹配");
        }
        try {
            if (status.isNewFrame()) {
                try {
                    if (currentFrame.isTxEnabled()) {
                        if (status.isRollbackOnly()) {
                            currentFrame.getTxContext().setRollbackOnly();
                        }
                        currentFrame.getTxContext().complete(commitCallback);
                    } else {
                        commitCallback.run();
                    }
                } finally {
                    stackHolder.get().pop();
                }
            } else {
                if (currentFrame.isTxEnabled()) {
                    if (status.isRollbackOnly()) {
                        if (status.getSavepoint() == null) {
                            currentFrame.getTxContext().setRollbackOnly();
                        } else {
                            currentFrame.rollbackToSavepoint(status.getSavepoint());
                        }
                    } else {
                        commitCallback.run();
                    }
                } else {
                    commitCallback.run();
                }
            }
        } finally {
            status.setCompleted();
        }
    }

    // 获取当前栈桢（如果没有，则创建一个未开启事务的栈桢）
    private Frame getCurrentFrame() {
        Frame frame = stackHolder.get().peek();
        if (frame == null) {
            frame = new Frame(false);
            stackHolder.get().push(frame);
        }
        return frame;
    }

    /**
     * 内部的事务感知
     */
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    private class InnerTransactionAware implements TransactionAware {
        // 缓存名称
        private final String cacheName;

        @Override
        public boolean isActive() {
            return getCurrentFrame().isTxEnabled();
        }

        @Override
        public Map<Copiable, Copiable> getContext() {
            Frame currentFrame = getCurrentFrame();
            if (!currentFrame.isTxEnabled()) {
                throw new IllegalStateException(String.format("当前不处于事务中，无法获取缓存[%s]的事务上下文", cacheName));
            }
            return currentFrame.getTxContext().getCacheContext(cacheName);
        }
    }

    /**
     * 内部的事务状态
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    @Getter(AccessLevel.PACKAGE)
    private class InnerTransactionStatus implements TransactionStatus {
        // 栈桢
        private final Frame frame;
        // 是否是新栈桢
        private final boolean newFrame;
        // 保存点
        private final TxContext savepoint;
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

    /**
     * 栈桢
     */
    @Getter
    private class Frame {
        // 事务是否已开启
        private final boolean txEnabled;
        // 事务上下文
        private TxContext txContext;

        Frame(boolean enableTx) {
            this.txEnabled = enableTx;
            this.txContext = enableTx ? new TxContext() : null;
        }

        // 创建事务保存点
        TxContext createSavepoint() {
            assertTxEnabled("未开启事务，无法创建临时保存点");
            return txContext.copy();
        }

        // 回滚事务到保存点
        void rollbackToSavepoint(TxContext savepoint) {
            assertTxEnabled("未开启事务，无法回滚到临时保存点");
            txContext = savepoint;
        }

        // 断言事务已开启
        private void assertTxEnabled(String message) {
            if (!txEnabled) {
                throw new IllegalStateException(message);
            }
        }
    }

    // 事务上下文
    private class TxContext implements Copiable {
        // 缓存名称与缓存上下文的映射
        private final Map<String, Map<Copiable, Copiable>> cacheNameContexts = new HashMap<>();
        // 是否只能回滚
        private boolean rollbackOnly = false;

        // 获取缓存上下文
        Map<Copiable, Copiable> getCacheContext(String cacheName) {
            return cacheNameContexts.computeIfAbsent(cacheName, k -> new HashMap<>());
        }

        // 设置为只能回滚
        void setRollbackOnly() {
            rollbackOnly = true;
        }

        // 完成
        void complete(Runnable commitCallback) {
            try {
                if (!rollbackOnly) {
                    CacheChain chain = new CacheChain(cacheNameContexts.keySet().iterator());
                    chain.nextFlush(commitCallback);
                }
            } finally {
                cacheNameContexts.clear();
            }
        }

        @Override
        public TxContext copy() {
            TxContext copiedThis = new TxContext();
            cacheNameContexts.forEach((cacheName, context) -> context.forEach((k, v) -> copiedThis.getCacheContext(cacheName).put(k.copy(), v.copy())));
            if (rollbackOnly) {
                copiedThis.setRollbackOnly();
            }
            return copiedThis;
        }

        // 缓存链
        @AllArgsConstructor(access = AccessLevel.PACKAGE)
        private class CacheChain {
            // 缓存名称迭代器
            private final Iterator<String> cacheIterator;

            // flush下一个缓存
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
}
