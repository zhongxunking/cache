/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-02 11:30 创建
 */
package org.antframework.cache.storage.redis.springdataredis;

import lombok.AllArgsConstructor;
import org.antframework.cache.storage.redis.RedisExecutor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.types.Expiration;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * 基于spring-data-redis的Redis执行器
 */
@AllArgsConstructor
public class SpringDataRedisExecutor implements RedisExecutor {
    // Redis连接工厂
    private final RedisConnectionFactory connectionFactory;

    @Override
    public byte[] get(String key) {
        return execute(connection -> connection.get(convertKey(key)));
    }

    @Override
    public void put(String key, byte[] value, Long liveTime) {
        execute(connection -> {
            if (liveTime == null) {
                return connection.set(convertKey(key), value);
            } else {
                return connection.set(convertKey(key), value, Expiration.milliseconds(liveTime), RedisStringCommands.SetOption.upsert());
            }
        });
    }

    @Override
    public void remove(String key) {
        execute(connection -> connection.del(convertKey(key)));
    }

    // 执行
    private <T> T execute(Function<RedisConnection, T> callback) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            return callback.apply(connection);
        }
    }

    // 转换key
    private byte[] convertKey(String key) {
        if (key == null) {
            return null;
        }
        return key.getBytes(StandardCharsets.UTF_8);
    }
}
