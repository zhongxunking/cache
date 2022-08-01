/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-01 22:40 创建
 */
package org.antframework.cache.storage.composite;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.manager.AbstractManager;
import org.antframework.cache.storage.Storage;
import org.antframework.cache.storage.StorageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 复合型仓库管理器
 */
@AllArgsConstructor
public class CompositeStorageManager extends AbstractManager<Storage> implements StorageManager {
    // 默认的键值对存活时长提供者
    private final Function<String, Long> defaultLiveTimeSupplier;
    // 默认的null值存活时长提供者
    private final Function<String, Long> defaultNullValueLiveTimeSupplier;
    // 仓库管理器集
    private final List<StorageManager> storageManagers;

    @Override
    protected Storage create(String name) {
        List<Storage> storages = new ArrayList<>(storageManagers.size());
        for (StorageManager storageManager : storageManagers) {
            storages.add(storageManager.get(name));
        }

        return new CompositeStorage(
                name,
                defaultLiveTimeSupplier.apply(name),
                defaultNullValueLiveTimeSupplier.apply(name),
                storages);
    }
}
