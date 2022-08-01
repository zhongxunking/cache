/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-01 22:27 创建
 */
package org.antframework.cache.storage.composite;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.Null;
import org.antframework.cache.storage.Storage;

import java.util.List;

/**
 * 复合型仓库
 */
@AllArgsConstructor
public class CompositeStorage implements Storage {
    // 名称
    private final String name;
    // 默认的键值对存活时长
    private final Long defaultLiveTime;
    // 默认的null值存活时长
    private final Long defaultNullValueLiveTime;
    // 仓库集
    private final List<Storage> storages;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public byte[] get(String key) {
        byte[] bytes = null;

        int index;
        for (index = 0; index < storages.size(); index++) {
            bytes = storages.get(index).get(key);
            if (bytes != null) {
                break;
            }
        }
        if (bytes != null) {
            Long liveTime = Null.is(bytes) ? defaultNullValueLiveTime : defaultLiveTime;
            for (int i = index - 1; i >= 0; i--) {
                storages.get(i).put(key, bytes, liveTime);
            }
        }

        return bytes;
    }

    @Override
    public void put(String key, byte[] value, Long liveTime) {
        for (int i = 0; i < storages.size() - 1; i++) {
            storages.get(i).remove(key);
        }
        for (int i = storages.size() - 1; i >= 0; i--) {
            storages.get(i).put(key, value, liveTime);
        }
    }

    @Override
    public void remove(String key) {
        for (Storage storage : storages) {
            storage.remove(key);
        }
    }
}
