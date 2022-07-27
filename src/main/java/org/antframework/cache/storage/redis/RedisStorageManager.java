/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-27 22:16 创建
 */
package org.antframework.cache.storage.redis;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.manager.AbstractManager;
import org.antframework.cache.storage.Storage;
import org.antframework.cache.storage.StorageManager;

import java.util.function.BinaryOperator;

/**
 * Redis仓库管理器
 */
@AllArgsConstructor
public class RedisStorageManager extends AbstractManager<Storage> implements StorageManager {
    // key生成器
    private final BinaryOperator<String> keyGenerator;
    // Redis执行器
    private final RedisExecutor redisExecutor;

    @Override
    protected Storage create(String name) {
        return new RedisStorage(name, keyGenerator, redisExecutor);
    }
}
