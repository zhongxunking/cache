/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-26 22:35 创建
 */
package org.antframework.cache.storage.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AllArgsConstructor;
import org.antframework.cache.common.manager.AbstractManager;
import org.antframework.cache.storage.Storage;
import org.antframework.cache.storage.StorageManager;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Caffeine仓库管理器
 */
@AllArgsConstructor
public class CaffeineStorageManager extends AbstractManager<Storage> implements StorageManager {
    // 存活时长转换器（转换成null表示永远有效）
    private final Function<String, Long> liveTimeFunction;
    // 最大容量转换器（转换成null表示无限制）
    private final Function<String, Long> maxSizeFunction;

    @Override
    protected Storage create(String name) {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder();
        Long liveTime = liveTimeFunction.apply(name);
        if (liveTime != null) {
            caffeine.expireAfterWrite(liveTime, TimeUnit.MILLISECONDS);
        }
        Long maxSize = maxSizeFunction.apply(name);
        if (maxSize != null) {
            caffeine.maximumSize(maxSize);
        }

        return new CaffeineStorage(name, caffeine.build());
    }
}
