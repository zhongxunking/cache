/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-19 00:05 创建
 */
package org.antframework.cache.lock;

import java.util.Set;

/**
 * 加锁器管理器
 */
public interface LockerManager {
    /**
     * 获取加锁器
     *
     * @param lockerName 加锁器名称
     * @return 加锁器
     */
    Locker getLocker(String lockerName);

    /**
     * 获取已知的所有加锁器名称
     *
     * @return 已知的所有加锁器名称
     */
    Set<String> getLockerNames();
}
