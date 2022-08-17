/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-17 21:44 创建
 */
package org.antframework.cache.common.redis.springdataredis;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Redis
 */
@AllArgsConstructor
public class Redis {
    // Redis连接工厂
    private final RedisConnectionFactory connectionFactory;

    /**
     * 执行
     *
     * @param callback 回调
     * @param <T>      执行结果类型
     * @return 执行结果
     */
    public <T> T execute(Function<RedisConnection, T> callback) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            return callback.apply(connection);
        }
    }

    /**
     * 序列化key
     *
     * @param key 被序列化的key
     * @return 序列化结果
     */
    public static byte[] serialize(String key) {
        if (key == null) {
            return null;
        }
        return key.getBytes(StandardCharsets.UTF_8);
    }
}
