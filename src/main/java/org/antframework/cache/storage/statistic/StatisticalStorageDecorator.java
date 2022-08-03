/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-29 22:50 创建
 */
package org.antframework.cache.storage.statistic;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.Exceptions;
import org.antframework.cache.statistic.Counter;
import org.antframework.cache.storage.Storage;

/**
 * 具有统计能力的仓库装饰器
 */
@AllArgsConstructor
public class StatisticalStorageDecorator implements Storage {
    // 目标仓库
    private final Storage target;
    // 有次序的名称
    private final String orderedName;
    // 计数器
    private final Counter counter;

    @Override
    public String getName() {
        return target.getName();
    }

    @Override
    public byte[] get(String key) {
        long startTime = System.currentTimeMillis();
        byte[] bytes = null;
        Throwable ex = null;
        try {
            bytes = target.get(key);
        } catch (Throwable e) {
            ex = e;
        }
        long endTime = System.currentTimeMillis();

        if (ex == null && bytes != null) {
            counter.incStorageHits(endTime, orderedName, endTime - startTime);
        } else {
            counter.incStorageMisses(endTime, orderedName, endTime - startTime);
            if (ex != null) {
                return Exceptions.rethrow(ex);
            }
        }
        return bytes;
    }

    @Override
    public void put(String key, byte[] value, Long liveTime, boolean valueChanged) {
        target.put(key, value, liveTime, valueChanged);
    }

    @Override
    public void remove(String key) {
        target.remove(key);
    }
}
