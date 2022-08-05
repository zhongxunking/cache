/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-05 21:51 创建
 */
package org.antframework.cache.storage.empty;

import org.antframework.cache.common.manager.AbstractManager;
import org.antframework.cache.storage.Storage;
import org.antframework.cache.storage.StorageManager;

/**
 * 空仓库管理器
 */
public class EmptyStorageManager extends AbstractManager<Storage> implements StorageManager {
    @Override
    protected Storage create(String name) {
        return new EmptyStorage(name);
    }
}
