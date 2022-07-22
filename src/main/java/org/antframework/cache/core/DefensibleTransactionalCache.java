/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-21 13:21 创建
 */
package org.antframework.cache.core;

import org.antframework.cache.common.Idler;
import org.antframework.cache.common.Null;
import org.antframework.cache.lock.Locker;
import org.antframework.cache.serialize.Serializer;
import org.antframework.cache.storage.Storage;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * 具备防御能力（防击穿、防雪崩、防穿透）和支持事务能力的缓存
 */
public class DefensibleTransactionalCache extends AbstractTransactionalCache {
    // 随机数
    private static final Random RANDOM = new Random();

    // 仓库懒汉
    private final Idler storageIdler = new Idler();
    // 加载值懒汉
    private final Idler loadingValueIdler = new Idler();
    // 序列化器
    private final Serializer serializer;
    // 仓库
    private final Storage storage;
    // 键值对存活时长（单位：毫秒，null表示不过期）
    private final Long liveTime;
    // null值的存活时长（单位：毫秒）
    private final long nullValueLiveTime;
    // 存活时长的浮动率（负数表示向下浮动，正数表示向上浮动）
    private final double liveTimeFloatRate;

    public DefensibleTransactionalCache(String name,
                                        boolean allowNull,
                                        Function<Object, String> keyConverter,
                                        Serializer serializer,
                                        TransactionAware transactionAware,
                                        Locker locker,
                                        long maxWaitTime,
                                        Storage storage,
                                        Long liveTime,
                                        long nullValueLiveTime,
                                        double liveTimeFloatRate) {
        super(name, allowNull, keyConverter, serializer, transactionAware, locker, maxWaitTime);
        this.serializer = serializer;
        this.storage = storage;
        this.liveTime = liveTime;
        this.nullValueLiveTime = nullValueLiveTime;
        this.liveTimeFloatRate = Math.max(liveTimeFloatRate, -1);
    }

    @Override
    protected byte[] getFromStorage(String key) {
        return storageIdler.acquire(key, () -> storage.get(key), Function.identity());
    }

    @Override
    protected <T> T loadInSafe(String key, Callable<T> valueLoader) {
        return loadingValueIdler.acquire(key, () -> super.loadInSafe(key, valueLoader), value -> {
            if (value == null) {
                return null;
            }
            byte[] bytes = serializer.serialize(value);
            return serializer.deserialize(bytes, (Class<T>) value.getClass());
        });
    }

    @Override
    protected void putInStorage(String key, byte[] value) {
        Long realLiveTime = computeLiveTime(value);
        if (realLiveTime == null) {
            storage.put(key, value, -1);
        } else if (realLiveTime > 0) {
            storage.put(key, value, realLiveTime);
        }
    }

    // 计算存活时长（null表示不过期）
    private Long computeLiveTime(byte[] value) {
        long realLiveTime;
        if (Null.is(value)) {
            realLiveTime = nullValueLiveTime;
        } else {
            if (liveTime == null) {
                return null;
            }
            realLiveTime = liveTime;
        }
        int maxFloat = (int) (realLiveTime * liveTimeFloatRate);
        if (maxFloat > 0) {
            realLiveTime += RANDOM.nextInt(maxFloat);
        } else if (maxFloat < 0) {
            realLiveTime -= RANDOM.nextInt(-maxFloat);
        }

        return realLiveTime;
    }

    @Override
    protected void removeInStorage(String key) {
        storage.remove(key);
    }
}
