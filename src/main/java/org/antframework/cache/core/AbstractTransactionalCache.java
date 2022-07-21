/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-20 21:58 创建
 */
package org.antframework.cache.core;

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
    // 事务提交时加写锁最长等待时长（单位：毫秒，小于0表示永远等待直到加锁成功）
    private final long maxWaitTime;

    public AbstractTransactionalCache(String name,
                                      boolean allowNull,
                                      Function<Object, String> keyConverter,
                                      Serializer serializer,
                                      TransactionAware transactionAware,
                                      Locker locker,
                                      long maxWaitTime) {
        super(name, allowNull, keyConverter, serializer);
        this.transactionAware = transactionAware;
        this.locker = locker;
        this.maxWaitTime = maxWaitTime;
    }

    @Override
    protected byte[] get(String key) {
        if (isTransactionalRemoved(key)) {
            return null;
        }
        return getInStorage(key);
    }

    /**
     * 从仓库获取值
     *
     * @param key 键
     * @return 值（null表示无该键值对）
     */
    protected abstract byte[] getInStorage(String key);

    @Override
    protected <T> T load(String key, Callable<T> valueLoader, Consumer<T> putCallback) {
        T value;
        if (isTransactionalRemoved(key)) {
            value = valueLoader.call();
        } else {
            Lock readLock = locker.getRWLock(key).readLock();
            if (readLock.tryLock()) {
                try {
                    value = valueLoader.call();
                    putCallback.accept(value);
                } finally {
                    readLock.unlock();
                }
            } else {
                value = valueLoader.call();
            }
        }

        return value;
    }

    @Override
    protected void put(String key, byte[] value) {
        if (!isTransactionalRemoved(key)) {
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
            getTransactionalRemovedKeys().add(key);
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
            getTransactionalRemovedKeys().flush(callback);
        } else {
            callback.run();
        }
    }

    // 是否在事务中已被删除
    private boolean isTransactionalRemoved(String key) {
        return transactionAware.isActive() && getTransactionalRemovedKeys().contain(key);
    }

    // 获取在事务中被删除的键集合
    private RemovedKeys getTransactionalRemovedKeys() {
        Map<Object, Object> context = transactionAware.getContext();
        RemovedKeys removedKeys = (RemovedKeys) context.get(RemovedKeys.class);
        if (removedKeys == null) {
            removedKeys = new RemovedKeys();
            context.put(RemovedKeys.class, removedKeys);
        }
        return removedKeys;
    }

    // 被删除的键集合
    private class RemovedKeys {
        // 键集合
        private final Set<String> keys = new TreeSet<>();

        // 新增
        void add(String key) {
            keys.add(key);
        }

        // 是否包含
        boolean contain(String key) {
            return keys.contains(key);
        }

        // 应用
        void flush(Runnable callback) {
            List<Lock> writeLocks = new ArrayList<>(keys.size());
            try {
                for (String key : keys) {
                    Lock writeLock = locker.getRWLock(key).writeLock();
                    if (maxWaitTime < 0) {
                        writeLock.lock();
                    } else {
                        if (!writeLock.tryLock(maxWaitTime, TimeUnit.MILLISECONDS)) {
                            throw new RuntimeException("缓存[" + getName() + "]提交事务时对缓存key[" + key + "]加写锁等待超时[" + maxWaitTime + "ms]");
                        }
                    }
                    writeLocks.add(writeLock);
                    removeInStorage(key);
                }
                callback.run();
            } finally {
                for (int i = writeLocks.size() - 1; i >= 0; i--) {
                    try {
                        writeLocks.get(i).unlock();
                    } catch (Throwable e) {
                        // 忽略
                    }
                }
                keys.clear();
            }
        }
    }
}
