/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-27 22:09 创建
 */
package org.antframework.cache.storage.redis;

import lombok.AllArgsConstructor;
import org.antframework.cache.storage.Storage;

import java.util.function.BinaryOperator;

/**
 * Redis仓库
 */
@AllArgsConstructor
public class RedisStorage implements Storage {
    // 名称
    private final String name;
    // key生成器
    private final BinaryOperator<String> keyGenerator;
    // Redis执行器
    private final RedisExecutor redisExecutor;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public byte[] get(String key) {
        return redisExecutor.get(keyGenerator.apply(name, key));
    }

    @Override
    public void put(String key, byte[] value, Long liveTime) {
        redisExecutor.put(keyGenerator.apply(name, key), value, liveTime);
    }

    @Override
    public void remove(String key) {
        redisExecutor.remove(keyGenerator.apply(name, key));
    }
}
