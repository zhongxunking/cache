/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-21 22:42 创建
 */
package org.antframework.cache.lock.consistencyv5;

import lombok.AllArgsConstructor;
import org.antframework.cache.lock.Locker;
import org.antframework.sync.SyncContext;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.BinaryOperator;

/**
 * 缓存一致性方案5的加锁器
 */
@AllArgsConstructor
public class ConsistencyV5Locker implements Locker {
    // 名称
    private final String name;
    // key生成器
    private final BinaryOperator<String> keyGenerator;
    // Sync上下文
    private final SyncContext syncContext;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ReadWriteLock getRWLock(String key) {
        return syncContext.getLockContext().getRWLock(keyGenerator.apply(name, key));
    }
}
