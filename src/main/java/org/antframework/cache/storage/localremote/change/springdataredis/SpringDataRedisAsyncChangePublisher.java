/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-04 22:07 创建
 */
package org.antframework.cache.storage.localremote.change.springdataredis;

import org.antframework.cache.common.redis.springdataredis.Redis;
import org.antframework.cache.serialize.Serializer;
import org.antframework.cache.storage.localremote.change.AbstractAsyncChangePublisher;
import org.antframework.cache.storage.localremote.change.ChangeBatch;

import java.nio.charset.StandardCharsets;

/**
 * 基于spring-data-redis的异步修改发布器
 */
public class SpringDataRedisAsyncChangePublisher extends AbstractAsyncChangePublisher {
    // Redis消息通道
    private final byte[] channel;
    // Redis
    private final Redis redis;
    // 序列化器
    private final Serializer serializer;

    public SpringDataRedisAsyncChangePublisher(int queueSize,
                                               Long timeout,
                                               int maxBatchSize,
                                               int publishThreads,
                                               String channel,
                                               Redis redis,
                                               Serializer serializer) {
        super(queueSize, timeout, maxBatchSize, publishThreads);
        this.channel = channel.getBytes(StandardCharsets.UTF_8);
        this.redis = redis;
        this.serializer = serializer;
    }

    @Override
    protected void doPublish(ChangeBatch batch) {
        redis.execute(connection -> connection.publish(channel, serializer.serialize(batch)));
    }
}
