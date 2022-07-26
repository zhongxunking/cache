/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-26 20:44 创建
 */
package org.antframework.cache.lock.sync;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.manager.AbstractManager;
import org.antframework.cache.lock.Locker;
import org.antframework.cache.lock.LockerManager;
import org.antframework.sync.SyncContext;

import java.util.function.BinaryOperator;

/**
 * Sync加锁器管理器
 */
@AllArgsConstructor
public class SyncLockerManager extends AbstractManager<Locker> implements LockerManager {
    // Sync上下文
    private final SyncContext syncContext;
    // key生成器
    private final BinaryOperator<String> keyGenerator;

    @Override
    protected Locker create(String name) {
        return new SyncLocker(name, keyGenerator, syncContext);
    }
}