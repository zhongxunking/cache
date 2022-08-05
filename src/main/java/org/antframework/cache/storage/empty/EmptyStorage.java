/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-05 21:48 创建
 */
package org.antframework.cache.storage.empty;

import lombok.AllArgsConstructor;
import org.antframework.cache.storage.Storage;

/**
 * 空仓库
 */
@AllArgsConstructor
public class EmptyStorage implements Storage {
    // 名称
    private final String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public byte[] get(String key) {
        return null;
    }

    @Override
    public void put(String key, byte[] value, Long liveTime, boolean valueChanged) {
    }

    @Override
    public void remove(String key) {
    }
}
