/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-03 19:08 创建
 */
package org.antframework.cache.storage.localremote;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.manager.AbstractManager;
import org.antframework.cache.storage.Storage;
import org.antframework.cache.storage.StorageManager;

import java.util.function.Function;

/**
 * 本地和远程复合型仓库管理器
 */
@AllArgsConstructor
public class LocalRemoteStorageManager extends AbstractManager<Storage> implements StorageManager {
    // 本地仓库管理器
    private final StorageManager localStorageManager;
    // 远程仓库管理器
    private final StorageManager remoteStorageManager;
    // 本地键值对存活时长提供者（返回值单位：毫秒，返回值为null表示不过期）
    private final Function<String, Long> localLiveTimeSupplier;
    // 本地null值存活时长提供者（返回值单位：毫秒，返回值为null表示不过期）
    private final Function<String, Long> localNullValueLiveTimeSupplier;

    @Override
    protected Storage create(String name) {
        return new LocalRemoteStorage(
                name,
                localStorageManager.get(name),
                remoteStorageManager.get(name),
                localLiveTimeSupplier.apply(name),
                localNullValueLiveTimeSupplier.apply(name));
    }
}
