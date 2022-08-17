/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-02 11:30 创建
 */
package org.antframework.cache.storage.redis.springdataredis;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.redis.springdataredis.Redis;
import org.antframework.cache.storage.redis.RedisExecutor;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.types.Expiration;

/**
 * 基于spring-data-redis的Redis执行器
 */
@AllArgsConstructor
public class SpringDataRedisExecutor implements RedisExecutor {
    // Redis
    private final Redis redis;

    @Override
    public byte[] get(String key) {
        return redis.execute(connection -> connection.get(Redis.serialize(key)));
    }

    @Override
    public void put(String key, byte[] value, Long liveTime) {
        redis.execute(connection -> {
            if (liveTime == null) {
                return connection.set(Redis.serialize(key), value);
            } else {
                return connection.set(Redis.serialize(key), value, Expiration.milliseconds(liveTime), RedisStringCommands.SetOption.upsert());
            }
        });
    }

    @Override
    public void remove(String key) {
        redis.execute(connection -> connection.del(Redis.serialize(key)));
    }
}
