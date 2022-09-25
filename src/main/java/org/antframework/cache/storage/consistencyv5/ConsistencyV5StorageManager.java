/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-22 22:31 创建
 */
package org.antframework.cache.storage.consistencyv5;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.consistencyv5.ReadScopeAware;
import org.antframework.cache.common.consistencyv5.WriteScopeAware;
import org.antframework.cache.common.consistencyv5.redis.RedisExecutor;
import org.antframework.cache.common.manager.AbstractManager;
import org.antframework.cache.lock.consistencyv5.ConsistencyV5Locker;
import org.antframework.cache.lock.consistencyv5.ConsistencyV5LockerManager;
import org.antframework.cache.storage.Storage;
import org.antframework.cache.storage.StorageManager;

import java.util.function.BinaryOperator;

/**
 * 缓存一致性方案5的仓库管理器
 */
@AllArgsConstructor
public class ConsistencyV5StorageManager extends AbstractManager<Storage> implements StorageManager {
    // key生成器
    private final BinaryOperator<String> keyGenerator;
    // 读作用域感知器
    private final ReadScopeAware readScopeAware;
    // 写作用域感知器
    private final WriteScopeAware writeScopeAware;
    // 缓存一致性方案5的加锁器管理器
    private final ConsistencyV5LockerManager lockerManager;
    // Redis执行器
    private final RedisExecutor redisExecutor;

    @Override
    protected Storage create(String name) {
        return new ConsistencyV5Storage(
                name,
                keyGenerator,
                readScopeAware,
                writeScopeAware,
                (ConsistencyV5Locker) lockerManager.get(name),
                redisExecutor);
    }
}
