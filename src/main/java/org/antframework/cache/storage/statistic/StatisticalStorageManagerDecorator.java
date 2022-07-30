/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-30 18:29 创建
 */
package org.antframework.cache.storage.statistic;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.manager.AbstractManager;
import org.antframework.cache.statistic.CounterManager;
import org.antframework.cache.storage.Storage;
import org.antframework.cache.storage.StorageManager;

/**
 * 具有统计能力的仓库装饰器管理器
 */
@AllArgsConstructor
public class StatisticalStorageManagerDecorator extends AbstractManager<Storage> implements StorageManager {
    // 目标仓库管理器
    private final StorageManager target;
    // 有次序的名称
    private final String orderedName;
    // 计数器管理器
    private final CounterManager counterManager;

    @Override
    protected Storage create(String name) {
        return new StatisticalStorageDecorator(target.get(name), orderedName, counterManager.get(name));
    }
}
