/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-18 22:34 创建
 */
package org.antframework.cache.storage;

import java.util.Set;

/**
 * 仓库管理器
 */
public interface StorageManager {
    /**
     * 获取仓库
     *
     * @param storageName 仓库名称
     * @return 仓库
     */
    Storage getStorage(String storageName);

    /**
     * 获取已知的所有仓库名称
     *
     * @return 已知的所有仓库名称
     */
    Set<String> getStorageNames();
}
