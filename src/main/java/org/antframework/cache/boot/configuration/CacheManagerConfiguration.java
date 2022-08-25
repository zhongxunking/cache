/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-23 23:43 创建
 */
package org.antframework.cache.boot.configuration;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antframework.cache.boot.CacheProperties;
import org.antframework.cache.common.DefaultKeyConverter;
import org.antframework.cache.common.redis.springdataredis.Redis;
import org.antframework.cache.core.TransactionalCacheManager;
import org.antframework.cache.core.defense.DefensibleTransactionalCacheManager;
import org.antframework.cache.core.onoff.OnoffTransactionalCacheManager;
import org.antframework.cache.core.statistic.StatisticalTransactionalCacheManagerDecorator;
import org.antframework.cache.lock.LockerManager;
import org.antframework.cache.lock.sync.SyncLockerManager;
import org.antframework.cache.serialize.SerializerManager;
import org.antframework.cache.serialize.jackson.JacksonSerializerManager;
import org.antframework.cache.statistic.CounterManager;
import org.antframework.cache.statistic.ring.RingCounterManager;
import org.antframework.cache.storage.StorageManager;
import org.antframework.cache.storage.caffeine.CaffeineStorageManager;
import org.antframework.cache.storage.localremote.ChangePublisher;
import org.antframework.cache.storage.localremote.LocalRemoteStorageManager;
import org.antframework.cache.storage.redis.RedisExecutor;
import org.antframework.cache.storage.redis.RedisStorageManager;
import org.antframework.cache.storage.redis.springdataredis.SpringDataRedisExecutor;
import org.antframework.cache.storage.statistic.StatisticalStorageManagerDecorator;
import org.antframework.sync.SyncContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 缓存管理器配置
 */
@Slf4j
public class CacheManagerConfiguration {
    // 缓存管理器
    @Bean(name = "org.antframework.cache.core.TransactionalCacheManager")
    public OnoffTransactionalCacheManager cacheManager(SerializerManager serializerManager,
                                                       LockerManager lockerManager,
                                                       StorageManager storageManager,
                                                       CounterManager counterManager,
                                                       CacheProperties properties) {
        TransactionalCacheManager cacheManager = new DefensibleTransactionalCacheManager(
                ConfigurationUtils.toSupplier(properties.getAllowNull()),
                new DefaultKeyConverter(),
                serializerManager,
                lockerManager,
                ConfigurationUtils.toLongSupplier(properties.getMaxLockWaitTime()),
                storageManager,
                ConfigurationUtils.toLongSupplier(properties.getLiveTime()),
                ConfigurationUtils.toLongSupplier(properties.getNullValueLiveTime()),
                ConfigurationUtils.toSupplier(properties.getLiveTimeFloatRate()));
        if (properties.getStatistic().isEnable()) {
            cacheManager = new StatisticalTransactionalCacheManagerDecorator(cacheManager, counterManager);
        }
        return new OnoffTransactionalCacheManager(properties::isCacheSwitch, cacheManager);
    }

    // Jackson序列化器管理器
    @Bean(name = "org.antframework.cache.serialize.SerializerManager")
    @ConditionalOnMissingBean(SerializerManager.class)
    public JacksonSerializerManager serializerManager() {
        return new JacksonSerializerManager();
    }

    // Sync加锁器管理器
    @Bean(name = "org.antframework.cache.lock.LockerManager")
    @ConditionalOnMissingBean(LockerManager.class)
    public SyncLockerManager lockerManager(SyncContext syncContext, CacheProperties properties, Environment environment) {
        return new SyncLockerManager(ConfigurationUtils.buildKeyGenerator(properties, environment), syncContext);
    }

    /**
     * 仓库管理器配置
     */
    @Configuration
    @ConditionalOnMissingBean(StorageManager.class)
    public static class StorageManagerConfiguration {
        /**
         * 仓库有次序的名称
         */
        public static final String ORDERED_NAME = "0-Remote-Redis";

        // Redis仓库管理器
        @Bean(name = "org.antframework.cache.storage.StorageManager")
        @ConditionalOnProperty(name = CacheProperties.Local.ENABLE_KEY, havingValue = "false")
        public StorageManager storageManager(RedisExecutor redisExecutor,
                                             CounterManager counterManager,
                                             CacheProperties properties,
                                             Environment environment) {
            StorageManager storageManager = new RedisStorageManager(ConfigurationUtils.buildKeyGenerator(properties, environment), redisExecutor);
            if (properties.getStatistic().isEnable()) {
                storageManager = new StatisticalStorageManagerDecorator(storageManager, ORDERED_NAME, counterManager);
            }
            return storageManager;
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
             * 本地仓库刷新器名称
             */
            public static final String LOCAL_REFRESHER_NAME = "ant.cache.local.refresher";

            // 本地和远程复合型仓库管理器
            @Bean(name = "org.antframework.cache.storage.StorageManager")
            public LocalRemoteStorageManager storageManager(RedisExecutor redisExecutor,
                                                            ChangePublisher publisher,
                                                            CounterManager counterManager,
                                                            CacheProperties properties,
                                                            Environment environment) {
                StorageManager local = buildLocal(counterManager, properties);
                StorageManager remote = buildRemote(
                        redisExecutor,
                        counterManager,
                        properties,
                        environment);

                LocalRemoteStorageManager storageManager = new LocalRemoteStorageManager(
                        local,
                        remote,
                        ConfigurationUtils.toLocalLiveTimeSupplier(properties.getLiveTime(), properties),
                        ConfigurationUtils.toLocalLiveTimeSupplier(properties.getNullValueLiveTime(), properties),
                        publisher);
                if (properties.getLocal().getRefresher().isEnable()) {
                    initLocalRefresher(storageManager, properties);
                }
                return storageManager;
            }

            // 构建本地仓库
            private StorageManager buildLocal(CounterManager counterManager, CacheProperties properties) {
                StorageManager local = new CaffeineStorageManager(
                        ConfigurationUtils.toLocalLiveTimeSupplier(properties.getLiveTime(), properties),
                        ConfigurationUtils.toLongSupplier(properties.getLocal().getMaxSize()));
                if (properties.getStatistic().isEnable()) {
                    local = new StatisticalStorageManagerDecorator(local, LOCAL_ORDERED_NAME, counterManager);
                }
                return local;
            }

            // 构建远程仓库
            private StorageManager buildRemote(RedisExecutor redisExecutor,
                                               CounterManager counterManager,
                                               CacheProperties properties,
                                               Environment environment) {
                StorageManager remote = new RedisStorageManager(ConfigurationUtils.buildKeyGenerator(properties, environment), redisExecutor);
                if (properties.getStatistic().isEnable()) {
                    remote = new StatisticalStorageManagerDecorator(remote, REMOTE_ORDERED_NAME, counterManager);
                }
                return remote;
            }

            // 初始化本地仓库刷新器
            private static void initLocalRefresher(LocalRemoteStorageManager storageManager, CacheProperties properties) {
                Timer refresher = new Timer(LOCAL_REFRESHER_NAME, true);
                refresher.schedule(
                        new LocalRefreshTask(storageManager),
                        properties.getLocal().getRefresher().getPeriod(),
                        properties.getLocal().getRefresher().getPeriod());
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
                        log.error("刷新本地缓存出错", e);
                    }
                }
            }

            // 导入修改发布器配置
            @Configuration
            @ConditionalOnMissingBean(ChangePublisher.class)
            @Import(ChangePublisherConfiguration.class)
            public static class ChangePublisherConfigurationImporter {
            }
        }

        // 基于spring-data-redis的Redis执行器
        @Bean(name = "org.antframework.cache.storage.redis.RedisExecutor")
        @ConditionalOnMissingBean(RedisExecutor.class)
        public SpringDataRedisExecutor redisExecutor(RedisConnectionFactory connectionFactory) {
            return new SpringDataRedisExecutor(new Redis(connectionFactory));
        }
    }

    // 环路计数器管理器
    @Bean(name = "org.antframework.cache.statistic.CounterManager")
    @ConditionalOnMissingBean(CounterManager.class)
    public RingCounterManager counterManager(CacheProperties properties) {
        return new RingCounterManager(properties.getStatistic().getTimeLength(), properties.getStatistic().getTimeGranularity());
    }
}
