/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-06 19:06 创建
 */
package org.antframework.cache.boot;

import lombok.AllArgsConstructor;
import org.antframework.cache.CacheManager;
import org.antframework.cache.boot.cache.CacheManagerAdapter;
import org.antframework.cache.boot.transaction.TransactionManagerCacheProcessor;
import org.antframework.cache.common.DefaultKeyConverter;
import org.antframework.cache.common.DefaultKeyGenerator;
import org.antframework.cache.core.TransactionalCacheManager;
import org.antframework.cache.core.defense.DefensibleTransactionalCacheManager;
import org.antframework.cache.core.onoff.OnoffTransactionalCacheManager;
import org.antframework.cache.core.statistic.StatisticalTransactionalCacheManagerDecorator;
import org.antframework.cache.lock.LockerManager;
import org.antframework.cache.lock.sync.SyncLockerManager;
import org.antframework.cache.serialize.SerializerManager;
import org.antframework.cache.serialize.hessian.HessianSerializerManager;
import org.antframework.cache.statistic.CounterManager;
import org.antframework.cache.statistic.ring.RingCounterManager;
import org.antframework.cache.storage.StorageManager;
import org.antframework.cache.storage.caffeine.CaffeineStorageManager;
import org.antframework.cache.storage.localremote.ChangeListener;
import org.antframework.cache.storage.localremote.ChangePublisher;
import org.antframework.cache.storage.localremote.LocalRemoteStorageManager;
import org.antframework.cache.storage.localremote.change.empty.EmptyChangePublisher;
import org.antframework.cache.storage.localremote.change.springdataredis.SpringDataRedisAsyncChangePublisher;
import org.antframework.cache.storage.localremote.change.springdataredis.SpringDataRedisChangeListenerContainer;
import org.antframework.cache.storage.redis.RedisExecutor;
import org.antframework.cache.storage.redis.RedisStorageManager;
import org.antframework.cache.storage.redis.springdataredis.SpringDataRedisExecutor;
import org.antframework.cache.storage.statistic.StatisticalStorageManagerDecorator;
import org.antframework.sync.SyncContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;

/**
 * Cache自动配置
 */
@Configuration
@ConditionalOnProperty(name = CacheProperties.ENABLE_KEY, havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class)
@EnableConfigurationProperties(CacheProperties.class)
@EnableCaching
public class CacheAutoConfiguration {
    // 事务管理器的缓存处理器
    @Bean(name = "org.antframework.cache.boot.transaction.TransactionManagerCacheProcessor")
    @ConditionalOnMissingBean(TransactionManagerCacheProcessor.class)
    public TransactionManagerCacheProcessor cacheProcessor(CacheProperties properties) {
        return new TransactionManagerCacheProcessor(properties.getDecorateTransactionManagerOrder());
    }

    // CacheManager适配器
    @Bean
    @ConditionalOnMissingBean(org.springframework.cache.CacheManager.class)
    public CacheManagerAdapter cacheManager(CacheManager cacheManager) {
        return new CacheManagerAdapter(cacheManager);
    }

    /**
     * 具备事务管理能力的缓存管理器配置
     */
    @Configuration
    @ConditionalOnMissingBean(TransactionalCacheManager.class)
    public static class TransactionalCacheManagerConfiguration {
        // 具备事务管理能力的缓存管理器
        @Bean(name = "org.antframework.cache.core.TransactionalCacheManager")
        public OnoffTransactionalCacheManager transactionalCacheManager(SerializerManager serializerManager,
                                                                        LockerManager lockerManager,
                                                                        StorageManager storageManager,
                                                                        CounterManager counterManager,
                                                                        CacheProperties properties) {
            TransactionalCacheManager cacheManager = new DefensibleTransactionalCacheManager(
                    getSupplier(properties.getAllowNull()),
                    new DefaultKeyConverter(),
                    serializerManager,
                    lockerManager,
                    getLongSupplier(properties.getMaxLockWaitTime()),
                    storageManager,
                    getLongSupplier(properties.getLiveTime()),
                    getLongSupplier(properties.getNullValueLiveTime()),
                    getSupplier(properties.getLiveTimeFloatRate()));
            if (properties.getStatistic().isEnable()) {
                cacheManager = new StatisticalTransactionalCacheManagerDecorator(cacheManager, counterManager);
            }
            return new OnoffTransactionalCacheManager(properties::isCacheSwitch, cacheManager);
        }

        // Hessian序列化器管理器
        @Bean(name = "org.antframework.cache.serialize.SerializerManager")
        @ConditionalOnMissingBean(SerializerManager.class)
        public HessianSerializerManager serializerManager() {
            return new HessianSerializerManager();
        }

        // Sync加锁器管理器
        @Bean(name = "org.antframework.cache.lock.LockerManager")
        @ConditionalOnMissingBean(LockerManager.class)
        public SyncLockerManager lockerManager(SyncContext syncContext, CacheProperties properties, Environment environment) {
            String namespace = computeNamespace(properties, environment);
            return new SyncLockerManager(new DefaultKeyGenerator(namespace), syncContext);
        }

        /**
         * 仓库管理器配置
         */
        @Configuration
        @ConditionalOnMissingBean(StorageManager.class)
        public static class StorageManagerConfiguration {
            // Redis仓库管理器
            @Bean(name = "org.antframework.cache.storage.StorageManager")
            @ConditionalOnProperty(name = CacheProperties.Local.ENABLE_KEY, havingValue = "false")
            public RedisStorageManager storageManager(RedisExecutor redisExecutor, CacheProperties properties, Environment environment) {
                String namespace = computeNamespace(properties, environment);
                return new RedisStorageManager(new DefaultKeyGenerator(namespace), redisExecutor);
            }

            /**
             * 本地和远程复合型仓库管理器配置
             */
            @Configuration
            @ConditionalOnProperty(name = CacheProperties.Local.ENABLE_KEY, havingValue = "true", matchIfMissing = true)
            public static class LocalRemoteStorageManagerConfiguration {
                /**
                 * 本地仓库有次序的名称
                 */
                public static final String LOCAL_ORDERED_NAME = "0-Local-Caffeine";
                /**
                 * 远程仓库有次序的名称
                 */
                public static final String REMOTE_ORDERED_NAME = "1-Remote-Redis";
                /**
                 * 序列化器名称
                 */
                public static final String SERIALIZER_NAME = "ant.cache.local";

                // 本地仓库刷新器
                private Timer localRefresher = null;

                // 本地和远程复合型仓库管理器
                @Bean(name = "org.antframework.cache.storage.StorageManager")
                public LocalRemoteStorageManager storageManager(RedisExecutor redisExecutor,
                                                                ChangePublisher publisher,
                                                                CounterManager counterManager,
                                                                CacheProperties properties,
                                                                Environment environment) {
                    LocalRemoteStorageManager storageManager = new LocalRemoteStorageManager(
                            buildLocal(counterManager, properties),
                            buildRemote(redisExecutor, counterManager, properties, environment),
                            getLocalLiveTimeSupplier(properties.getLiveTime(), properties),
                            getLocalLiveTimeSupplier(properties.getNullValueLiveTime(), properties),
                            publisher);
                    if (properties.getLocal().getRefresher().isEnable()) {
                        localRefresher = new Timer("ant.cache.local.refresher", true);
                        localRefresher.schedule(
                                new LocalRefreshTask(storageManager),
                                properties.getLocal().getRefresher().getPeriod(),
                                properties.getLocal().getRefresher().getPeriod());
                    }
                    return storageManager;
                }

                // 构建本地仓库
                private StorageManager buildLocal(CounterManager counterManager, CacheProperties properties) {
                    StorageManager local = new CaffeineStorageManager(
                            getLocalLiveTimeSupplier(properties.getLiveTime(), properties),
                            getLongSupplier(properties.getLocal().getMaxSize()));
                    if (properties.getStatistic().isEnable()) {
                        local = new StatisticalStorageManagerDecorator(local, LOCAL_ORDERED_NAME, counterManager);
                    }
                    return local;
                }

                // 获取本地仓库内键值对存活时长提供者
                private static Function<String, Long> getLocalLiveTimeSupplier(CacheProperties.CachesDetail<Long> cachesDetail, CacheProperties properties) {
                    return getLongSupplier(cachesDetail).andThen(value -> {
                        if (value == null) {
                            return properties.getLocal().getLiveTime().getMax();
                        }
                        long liveTime = (long) (value * properties.getLocal().getLiveTime().getScaleRate());
                        return Math.min(liveTime, properties.getLocal().getLiveTime().getMax());
                    });
                }

                // 构建远程仓库
                private StorageManager buildRemote(RedisExecutor redisExecutor,
                                                   CounterManager counterManager,
                                                   CacheProperties properties,
                                                   Environment environment) {
                    String namespace = computeNamespace(properties, environment);
                    StorageManager remote = new RedisStorageManager(new DefaultKeyGenerator(namespace), redisExecutor);
                    if (properties.getStatistic().isEnable()) {
                        remote = new StatisticalStorageManagerDecorator(remote, REMOTE_ORDERED_NAME, counterManager);
                    }
                    return remote;
                }

                // 本地仓库刷新任务
                @AllArgsConstructor
                private static class LocalRefreshTask extends TimerTask {
                    // 本地和远程复合型仓库管理器
                    private final LocalRemoteStorageManager storageManager;

                    @Override
                    public void run() {
                        try {
                            storageManager.refreshLocals();
                        } catch (Throwable e) {
                            // todo 打印日志
                        }
                    }
                }

                /**
                 * 修改发布器配置
                 */
                @Configuration
                @ConditionalOnMissingBean(ChangePublisher.class)
                public static class ChangePublisherConfiguration {
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
                        @Bean(name = "org.antframework.cache.storage.localremote.change.springdataredis.SpringDataRedisAsyncChangePublisher")
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
                                    connectionFactory,
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
                            String namespace = computeNamespace(properties, environment);
                            return namespace + "-cache-change";
                        }
                    }
                }
            }

            // 基于spring-data-redis的Redis执行器
            @Bean(name = "org.antframework.cache.storage.redis.springdataredis.SpringDataRedisExecutor")
            @ConditionalOnMissingBean(RedisExecutor.class)
            public SpringDataRedisExecutor redisExecutor(RedisConnectionFactory connectionFactory) {
                return new SpringDataRedisExecutor(connectionFactory);
            }
        }

        // 计算命名空间
        private static String computeNamespace(CacheProperties properties, Environment environment) {
            String namespace = properties.getNamespace();
            if (StringUtils.isBlank(namespace)) {
                namespace = environment.getProperty("spring.application.name");
                if (StringUtils.isBlank(namespace)) {
                    throw new IllegalArgumentException("未配置缓存命名空间，可通过ant.cache.namespace或者spring.application.name配置");
                }
            }
            return namespace;
        }

        // 获取缓存具体配置提供者
        private static Function<String, Long> getLongSupplier(CacheProperties.CachesDetail<Long> cachesDetail) {
            return getSupplier(cachesDetail).andThen(value -> {
                if (value != null && value < 0) {
                    return null;
                }
                return value;
            });
        }

        // 获取缓存具体配置提供者
        private static <T> Function<String, T> getSupplier(CacheProperties.CachesDetail<T> cachesDetail) {
            return cacheName -> {
                T value = cachesDetail.getCaches().get(cacheName);
                if (value == null) {
                    value = cachesDetail.getDef();
                }
                return value;
            };
        }

        // 环路计数器管理器
        @Bean(name = "org.antframework.cache.statistic.CounterManager")
        @ConditionalOnMissingBean(CounterManager.class)
        public RingCounterManager counterManager(CacheProperties properties) {
            return new RingCounterManager(properties.getStatistic().getTimeLength(), properties.getStatistic().getTimeGranularity());
        }
    }
}
