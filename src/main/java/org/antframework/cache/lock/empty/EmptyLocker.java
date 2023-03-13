/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2023-03-13 10:21 创建
 */
package org.antframework.cache.lock.empty;

import lombok.AllArgsConstructor;
import org.antframework.cache.lock.Locker;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * 空加锁器
 */
@AllArgsConstructor
public class EmptyLocker implements Locker {
    // 名称
    private final String name;
    // 读写锁
    private final ReadWriteLock rwLock = new EmptyReadWriteLock();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ReadWriteLock getRWLock(String key) {
        return rwLock;
    }

    // 空读写锁
    private static class EmptyReadWriteLock implements ReadWriteLock {
        // 读锁
        private final Lock readLock = new EmptyLock();
        // 写锁
        private final Lock writeLock = new EmptyLock();

        @Override
        public Lock readLock() {
            return readLock;
        }

        @Override
        public Lock writeLock() {
            return writeLock;
        }
    }

    // 空锁
    private static class EmptyLock implements Lock {
        @Override
        public void lock() {
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
        }

        @Override
        public boolean tryLock() {
            return true;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public void unlock() {
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }
}
