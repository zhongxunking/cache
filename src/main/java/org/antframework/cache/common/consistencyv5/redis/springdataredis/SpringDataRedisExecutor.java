/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-17 19:47 创建
 */
package org.antframework.cache.common.consistencyv5.redis.springdataredis;

import org.antframework.cache.common.consistencyv5.redis.RedisExecutor;
import org.antframework.cache.common.redis.springdataredis.Redis;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.List;

/**
 * 基于spring-data-redis的Redis执行器
 */
public class SpringDataRedisExecutor implements RedisExecutor {
    // Redis
    private final Redis redis;
    // Redis执行器
    private final org.antframework.sync.extension.redis.extension.RedisExecutor redisExecutor;

    public SpringDataRedisExecutor(RedisConnectionFactory connectionFactory) {
        this.redis = new Redis(connectionFactory);
        this.redisExecutor = new org.antframework.sync.extension.redis.extension.springdataredis.SpringDataRedisExecutor(connectionFactory);
    }

    @Override
    public byte[] hGet(String key, String field) {
        return redis.execute(connection -> connection.hGet(Redis.serialize(key), Redis.serialize(field)));
    }

    @Override
    public void hPut(String key, String field, byte[] value) {
        redis.execute(connection -> connection.hSet(Redis.serialize(key), Redis.serialize(field), value));
    }

    @Override
    public void hDel(String key, String field) {
        redis.execute(connection -> connection.hDel(Redis.serialize(key), Redis.serialize(field)));
    }

    @Override
    public void expire(String key, long livaTime) {
        redis.execute(connection -> connection.pExpire(Redis.serialize(key), livaTime));
    }

    @Override
    public <T> T eval(String script, List<String> keys, List<Object> args, Class<T> resultType) {
        return redisExecutor.eval(script, keys, args, resultType);
    }

    @Override
    public void addMessageListener(String channel, Runnable listener) {
        redisExecutor.addMessageListener(channel, listener);
    }

    @Override
    public void removeMessageListener(String channel, Runnable listener) {
        redisExecutor.removeMessageListener(channel, listener);
    }
}
