/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-19 00:02 创建
 */
package org.antframework.cache.lock;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * 加锁器
 */
public interface Locker {
    /**
     * 获取名称
     *
     * @return 名称
     */
    String getName();

    /**
     * 获取读写锁
     *
     * @param key 锁标识
     * @return 读写锁
     */
    ReadWriteLock getRWLock(String key);
}
