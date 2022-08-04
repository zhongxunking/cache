/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-04 22:07 创建
 */
package org.antframework.cache.storage.localremote.change.springdataredis;

import org.antframework.cache.serialize.Serializer;
import org.antframework.cache.storage.localremote.change.AbstractAsyncChangePublisher;
import org.antframework.cache.storage.localremote.change.ChangeBatch;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.nio.charset.StandardCharsets;

/**
 * 基于spring-data-redis的异步修改发布器
 */
public class SpringDataRedisAsyncChangePublisher extends AbstractAsyncChangePublisher {
    // Redis消息通道
    private final byte[] channel;
    // Redis连接工厂
    private final RedisConnectionFactory connectionFactory;
    // 序列化器
    private final Serializer serializer;

    public SpringDataRedisAsyncChangePublisher(int queueSize,
                                               Long timeout,
                                               int batchSize,
                                               int publishThreads,
                                               String channel,
                                               RedisConnectionFactory connectionFactory,
                                               Serializer serializer) {
        super(queueSize, timeout, batchSize, publishThreads);
        this.channel = channel.getBytes(StandardCharsets.UTF_8);
        this.connectionFactory = connectionFactory;
        this.serializer = serializer;
    }

    @Override
    protected void doPublish(ChangeBatch batch) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.publish(channel, serializer.serialize(batch));
        }
    }
}
