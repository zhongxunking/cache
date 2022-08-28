/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-22 23:40 创建
 */
package org.antframework.cache.boot.configuration;

import org.antframework.cache.boot.CacheProperties;
import org.antframework.cache.common.redis.springdataredis.Redis;
import org.antframework.cache.serialize.SerializerManager;
import org.antframework.cache.storage.localremote.ChangeListener;
import org.antframework.cache.storage.localremote.change.empty.EmptyChangePublisher;
import org.antframework.cache.storage.localremote.change.springdataredis.SpringDataRedisAsyncChangePublisher;
import org.antframework.cache.storage.localremote.change.springdataredis.SpringDataRedisChangeListenerContainer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.List;

/**
 * 修改发布器配置
 */
@Configuration
public class ChangePublisherConfiguration {
    /**
     * 序列化器名称
     */
    public static final String SERIALIZER_NAME = "ant.cache.local";

    // 空修改发布器
    @Bean(name = "org.antframework.cache.storage.localremote.ChangePublisher")
    @ConditionalOnProperty(name = CacheProperties.Local.Publisher.ENABLE_KEY, havingValue = "false")
    public EmptyChangePublisher changePublisher() {
        return new EmptyChangePublisher();
    }

    /**
     * 基于spring-data-redis的异步修改发布器配置
     */
    @Configuration
    @ConditionalOnProperty(name = CacheProperties.Local.Publisher.ENABLE_KEY, havingValue = "true", matchIfMissing = true)
    public static class SpringDataRedisAsyncChangePublisherConfiguration {
        // 基于spring-data-redis的异步修改发布器
        @Bean(name = "org.antframework.cache.storage.localremote.ChangePublisher")
        public SpringDataRedisAsyncChangePublisher changePublisher(RedisConnectionFactory connectionFactory,
                                                                   SerializerManager serializerManager,
                                                                   CacheProperties properties,
                                                                   Environment environment) {
            return new SpringDataRedisAsyncChangePublisher(
                    properties.getLocal().getPublisher().getQueueSize(),
                    computeInQueueTimeout(properties),
                    properties.getLocal().getPublisher().getMaxBatchSize(),
                    properties.getLocal().getPublisher().getPublishThreads(),
                    computeChannel(properties, environment),
                    new Redis(connectionFactory),
                    serializerManager.get(SERIALIZER_NAME));
        }

        // 计算入队超时时长
        private Long computeInQueueTimeout(CacheProperties properties) {
            long timeout = properties.getLocal().getPublisher().getInQueueTimeout();
            if (timeout < 0) {
                return null;
            }
            return timeout;
        }

        // 基于spring-data-redis的修改监听器容器
        @Bean(name = "org.antframework.cache.storage.localremote.change.springdataredis.SpringDataRedisChangeListenerContainer")
        public SpringDataRedisChangeListenerContainer listenerContainer(RedisConnectionFactory connectionFactory,
                                                                        SerializerManager serializerManager,
                                                                        List<ChangeListener> listeners,
                                                                        CacheProperties properties,
                                                                        Environment environment) {
            SpringDataRedisChangeListenerContainer container = new SpringDataRedisChangeListenerContainer(
                    computeChannel(properties, environment),
                    connectionFactory,
                    serializerManager.get(SERIALIZER_NAME));
            if (listeners != null) {
                for (ChangeListener listener : listeners) {
                    container.addListener(listener);
                }
            }
            return container;
        }

        // 计算发布键值对变更消息的通道
        private String computeChannel(CacheProperties properties, Environment environment) {
            String channel = properties.getLocal().getPublisher().getRedis().getChannel();
            if (StringUtils.isBlank(channel)) {
                String namespace = ConfigurationUtils.computeNamespace(properties, environment);
                channel = namespace + "-cache-change";
            }
            return channel;
        }
    }
}
