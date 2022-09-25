/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-21 22:42 创建
 */
package org.antframework.cache.lock.consistencyv5;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.manager.AbstractManager;
import org.antframework.cache.lock.Locker;
import org.antframework.cache.lock.LockerManager;
import org.antframework.sync.SyncContext;

import java.util.function.BinaryOperator;

/**
 * 缓存一致性方案5的加锁器管理器
 */
@AllArgsConstructor
public class ConsistencyV5LockerManager extends AbstractManager<Locker> implements LockerManager {
    // key生成器
    private final BinaryOperator<String> keyGenerator;
    // Sync上下文
    private final SyncContext syncContext;

    @Override
    protected Locker create(String name) {
        return new ConsistencyV5Locker(name, keyGenerator, syncContext);
    }
}
