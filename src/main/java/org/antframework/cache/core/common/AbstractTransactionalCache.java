/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-20 21:58 创建
 */
package org.antframework.cache.core.common;

import org.antframework.cache.common.Exceptions;
import org.antframework.cache.core.Copiable;
import org.antframework.cache.core.TransactionAware;
import org.antframework.cache.core.TransactionalCache;
import org.antframework.cache.lock.Locker;
import org.antframework.cache.serialize.Serializer;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 抽象支持事务能力的缓存
 */
public abstract class AbstractTransactionalCache extends AbstractCache implements TransactionalCache {
    // 事务感知
    private final TransactionAware transactionAware;
    // 加锁器
    private final Locker locker;
    // 事务提交时加写锁最长等待时长（单位：毫秒，null表示永远等待直到加锁成功）
    private final Long maxLockWaitTime;

    public AbstractTransactionalCache(String name,
                                      boolean allowNull,
                                      Function<Object, String> keyConverter,
                                      Serializer serializer,
                                      TransactionAware transactionAware,
                                      Locker locker,
                                      Long maxLockWaitTime) {
        super(name, allowNull, keyConverter, serializer);
        this.transactionAware = transactionAware;
        this.locker = locker;
        this.maxLockWaitTime = maxLockWaitTime;
    }

    @Override
    protected byte[] get(String key) {
        if (isTransactionalModified(key)) {
            return getTransactionalModifiedSet().getModifiedValue(key);
        }
        return getFromStorage(key);
    }

    /**
     * 从仓库获取值
     *
     * @param key 键
     * @return 值（null表示无该键值对）
     */
    protected abstract byte[] getFromStorage(String key);

    @Override
    protected <T> T load(String key, Callable<T> valueLoader, Consumer<T> putCallback) {
        T value;
        if (isTransactionalModified(key)) {
            value = Exceptions.call(valueLoader);
        } else {
            value = loadInSafe(key, () -> {
                T loadedValue;
                Lock readLock = locker.getRWLock(key).readLock();
                if (readLock.tryLock()) {
                    try {
                        loadedValue = valueLoader.call();
                        putCallback.accept(loadedValue);
                    } finally {
                        readLock.unlock();
                    }
                } else {
                    loadedValue = valueLoader.call();
                }
                return loadedValue;
            });
        }

        return value;
    }

    /**
     * 加载值（已在安全区域内）
     *
     * @param key         缓存键
     * @param valueLoader 值加载器
     * @param <T>         值类型
     * @return 值
     */
    protected <T> T loadInSafe(String key, Callable<T> valueLoader) {
        return Exceptions.call(valueLoader);
    }

    @Override
    protected void put(String key, byte[] value) {
        if (transactionAware.isActive()) {
            getTransactionalModifiedSet().addPut(key, value);
        } else {
            putInStorage(key, value);
        }
    }

    /**
     * 向仓库设置键值对
     *
     * @param key   键
     * @param value 值
     */
    protected abstract void putInStorage(String key, byte[] value);

    @Override
    protected void remove(String key) {
        if (transactionAware.isActive()) {
            getTransactionalModifiedSet().addRemovedKey(key);
        } else {
            removeInStorage(key);
        }
    }

    /**
     * 向仓库删除键值对
     *
     * @param key 键
     */
    protected abstract void removeInStorage(String key);

    @Override
    public void flush(Runnable callback) {
        if (transactionAware.isActive()) {
            getTransactionalModifiedSet().flush(callback);
        } else {
            callback.run();
        }
    }

    // 键值对是否在事务中被修改
    private boolean isTransactionalModified(String key) {
        return transactionAware.isActive() && getTransactionalModifiedSet().isModified(key);
    }

    // 获取事务中的修改集
    private ModifiedSet getTransactionalModifiedSet() {
        Map<Copiable, Copiable> context = transactionAware.getContext();
        ModifiedSet modifiedSet = (ModifiedSet) context.get(ModifiedSetKey.INSTANCE);
        if (modifiedSet == null) {
            modifiedSet = new ModifiedSet();
            context.put(ModifiedSetKey.INSTANCE, modifiedSet);
        }
        return modifiedSet;
    }

    // 修改集在上下文中的key
    private static class ModifiedSetKey implements Copiable {
        // 实例
        static final ModifiedSetKey INSTANCE = new ModifiedSetKey();

        private ModifiedSetKey() {
        }

        @Override
        public Copiable copy() {
            return this;
        }
    }

    // 修改集
    private class ModifiedSet implements Copiable {
        // 被删除的键集合
        private final Set<String> removedKeys = new HashSet<>();
        // 被设置的键值对集合
        private final Map<String, byte[]> puts = new HashMap<>();

        // 新增被删除的key
        void addRemovedKey(String removedKey) {
            removedKeys.add(removedKey);
            puts.remove(removedKey);
        }

        // 新增键值对
        void addPut(String key, byte[] value) {
            puts.put(key, value);
            removedKeys.remove(key);
        }

        // 键值对是否被修改
        boolean isModified(String key) {
            return removedKeys.contains(key) || puts.containsKey(key);
        }

        // 获取被修改的值
        byte[] getModifiedValue(String key) {
            return puts.get(key);
        }

        @Override
        public Copiable copy() {
            ModifiedSet copiedThis = new ModifiedSet();
            copiedThis.removedKeys.addAll(removedKeys);
            copiedThis.puts.putAll(puts);
            return copiedThis;
        }

        // 应用
        void flush(Runnable callback) {
            // 获取所有被修改的键值对key
            Set<String> modifiedKeys = new TreeSet<>();
            modifiedKeys.addAll(removedKeys);
            modifiedKeys.addAll(puts.keySet());
            // 加锁
            boolean callbackSuccess = false;
            List<Lock> writeLocks = new ArrayList<>(modifiedKeys.size());
            try {
                for (String key : modifiedKeys) {
                    Lock writeLock = locker.getRWLock(key).writeLock();
                    if (maxLockWaitTime == null) {
                        writeLock.lock();
                    } else {
                        boolean locked = false;
                        try {
                            locked = writeLock.tryLock(maxLockWaitTime, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            Exceptions.rethrow(e);
                        }
                        if (!locked) {
                            throw new RuntimeException(String.format("缓存[%s]提交事务时对缓存key[%s]加写锁等待超时[%dms]", getName(), key, maxLockWaitTime));
                        }
                    }
                    writeLocks.add(writeLock);
                }
                // 删除所有被修改的键值对
                for (String key : modifiedKeys) {
                    removeInStorage(key);
                }
                // 回调
                callback.run();
                callbackSuccess = true;
            } finally {
                try {
                    if (callbackSuccess) {
                        // 设置put操作的键值对
                        puts.forEach(AbstractTransactionalCache.this::putInStorage);
                    }
                } finally {
                    // 解锁
                    for (int i = writeLocks.size() - 1; i >= 0; i--) {
                        try {
                            writeLocks.get(i).unlock();
                        } catch (Throwable e) {
                            // 忽略
                        }
                    }
                    // 清理上下文
                    removedKeys.clear();
                    puts.clear();
                }
            }
        }
    }
}
