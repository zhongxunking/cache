/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-30 17:50 创建
 */
package org.antframework.cache.statistic.ring;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.manager.AbstractManager;
import org.antframework.cache.statistic.Counter;
import org.antframework.cache.statistic.CounterManager;

/**
 * 环路计数器管理器
 */
@AllArgsConstructor
public class RingCounterManager extends AbstractManager<Counter> implements CounterManager {
    // 计数的时间长度（单位：毫秒）
    private final long timeLength;
    // 计数的时间粒度（单位：毫秒）
    private final long timeGranularity;

    @Override
    protected Counter create(String name) {
        return new RingCounter(name, timeLength, timeGranularity);
    }
}
