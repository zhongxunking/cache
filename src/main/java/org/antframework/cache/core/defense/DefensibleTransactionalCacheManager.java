/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-25 22:19 创建
 */
package org.antframework.cache.core.defense;

import lombok.AllArgsConstructor;
import org.antframework.cache.core.TransactionAware;
import org.antframework.cache.core.common.AbstractTransactionalCacheManager;
import org.antframework.cache.lock.LockerManager;
import org.antframework.cache.serialize.SerializerManager;
import org.antframework.cache.storage.StorageManager;

import java.util.function.Function;

/**
 * 具备防御能力（防击穿、防雪崩、防穿透）和支持事务能力的缓存管理器
 */
@AllArgsConstructor
public class DefensibleTransactionalCacheManager extends AbstractTransactionalCacheManager<DefensibleTransactionalCache> {
    // 键和值是否允许为null提供者
    private final Function<String, Boolean> allowNullSupplier;
    // 键转换器
    private final Function<Object, String> keyConverter;
    // 序列化器管理器
    private final SerializerManager serializerManager;
    // 加锁器管理器
    private final LockerManager lockerManager;
    // 事务提交时加写锁最长等待时长提供者（返回值单位：毫秒，返回值为null表示永远等待直到加锁成功）
    private final Function<String, Long> maxLockWaitTimeSupplier;
    // 仓库管理器
    private final StorageManager storageManager;
    // 键值对存活时长提供者（返回值单位：毫秒，返回值为null表示不过期）
    private final Function<String, Long> liveTimeSupplier;
    // null值的存活时长提供者（返回值单位：毫秒，返回值为null表示不过期）
    private final Function<String, Long> nullValueLiveTimeSupplier;
    // 存活时长的浮动率提供者（返回值为负数表示向下浮动，返回值为正数表示向上浮动）
    private final Function<String, Double> liveTimeFloatRateSupplier;

    @Override
    protected DefensibleTransactionalCache createTransactionalCache(String cacheName, TransactionAware transactionAware) {
        return new DefensibleTransactionalCache(
                cacheName,
                allowNullSupplier.apply(cacheName),
                keyConverter,
                serializerManager.get(cacheName),
                transactionAware,
                lockerManager.get(cacheName),
                maxLockWaitTimeSupplier.apply(cacheName),
                storageManager.get(cacheName),
                liveTimeSupplier.apply(cacheName),
                nullValueLiveTimeSupplier.apply(cacheName),
                liveTimeFloatRateSupplier.apply(cacheName));
    }
}
