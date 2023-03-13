/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2023-03-13 10:47 创建
 */
package org.antframework.cache.lock.empty;

import org.antframework.cache.common.manager.AbstractManager;
import org.antframework.cache.lock.Locker;
import org.antframework.cache.lock.LockerManager;

/**
 * 空加锁器管理器
 */
public class EmptyLockerManager extends AbstractManager<Locker> implements LockerManager {
    @Override
    protected Locker create(String name) {
        return new EmptyLocker(name);
    }
}
