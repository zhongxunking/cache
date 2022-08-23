/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-22 22:54 创建
 */
package org.antframework.cache.boot.configuration;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antframework.cache.boot.CacheProperties;
import org.antframework.cache.common.DefaultKeyConverter;
import org.antframework.cache.common.consistencyv5.ReadScopeAware;
import org.antframework.cache.common.consistencyv5.WriteScopeAware;
import org.antframework.cache.common.consistencyv5.redis.RedisExecutor;
import org.antframework.cache.common.consistencyv5.redis.springdataredis.SpringDataRedisExecutor;
import org.antframework.cache.core.TransactionalCacheManager;
import org.antframework.cache.core.consistencyv5.ConsistencyV5TransactionalCacheManagerDecorator;
import org.antframework.cache.core.defense.DefensibleTransactionalCacheManager;
import org.antframework.cache.core.onoff.OnoffTransactionalCacheManager;
import org.antframework.cache.core.statistic.StatisticalTransactionalCacheManagerDecorator;
import org.antframework.cache.lock.consistencyv5.ConsistencyV5LockerManager;
import org.antframework.cache.lock.consistencyv5.sync.ConsistencyV5RedisServer;
import org.antframework.cache.serialize.SerializerManager;
import org.antframework.cache.serialize.hessian.HessianSerializerManager;
import org.antframework.cache.statistic.CounterManager;
import org.antframework.cache.statistic.ring.RingCounterManager;
import org.antframework.cache.storage.StorageManager;
import org.antframework.cache.storage.caffeine.CaffeineStorageManager;
import org.antframework.cache.storage.consistencyv5.ConsistencyV5StorageManager;
import org.antframework.cache.storage.localremote.ChangePublisher;
import org.antframework.cache.storage.localremote.LocalRemoteStorageManager;
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
 * 缓存一致性方案5的缓存管理器配置
 */
@Configuration
@Slf4j
public class ConsistencyV5CacheManagerConfiguration {
    // 读作用域感知器
    private final ReadScopeAware readScopeAware = new ReadScopeAware();
    // 写作用域感知器
    private final WriteScopeAware writeScopeAware = new WriteScopeAware();

    // 缓存管理器
    @Bean(name = "org.antframework.cache.core.TransactionalCacheManager")
    public OnoffTransactionalCacheManager cacheManager(SerializerManager serializerManager,
                                                       ConsistencyV5LockerManager lockerManager,
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
        cacheManager = new ConsistencyV5TransactionalCacheManagerDecorator(cacheManager, readScopeAware, writeScopeAware);
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

    // 加锁器管理器
    @Bean(name = "org.antframework.cache.lock.LockerManager")
    public ConsistencyV5LockerManager lockerManager(RedisExecutor redisExecutor,
                                                    CacheProperties properties,
                                                    Environment environment) {
        ConsistencyV5RedisServer server = new ConsistencyV5RedisServer(
                redisExecutor,
                properties.getConsistencyV5().getLocker().getLiveTime(),
                readScopeAware,
                writeScopeAware);
        SyncContext syncContext = new SyncContext(server, properties.getConsistencyV5().getLocker().getMaxWaitTime());

        return new ConsistencyV5LockerManager(ConfigurationUtils.buildKeyGenerator(properties, environment), syncContext);
    }

    // 仓库管理器
    @Bean(name = "org.antframework.cache.storage.StorageManager")
    @ConditionalOnProperty(name = CacheProperties.Local.ENABLE_KEY, havingValue = "false")
    public StorageManager storageManager(ConsistencyV5LockerManager lockerManager,
                                         RedisExecutor redisExecutor,
                                         CounterManager counterManager,
                                         CacheProperties properties,
                                         Environment environment) {
        return StorageManagers.build(
                readScopeAware,
                writeScopeAware,
                lockerManager,
                redisExecutor,
                counterManager,
                properties,
                environment);
    }

    /**
     * 仓库管理器工具
     */
    public static class StorageManagers {
        /**
         * 仓库有次序的名称
         */
        public static final String ORDERED_NAME = "0-Remote-Redis";

        // 构建仓库管理器
        static StorageManager build(ReadScopeAware readScopeAware,
                                    WriteScopeAware writeScopeAware,
                                    ConsistencyV5LockerManager lockerManager,
                                    RedisExecutor redisExecutor,
                                    CounterManager counterManager,
                                    CacheProperties properties,
                                    Environment environment) {
            StorageManager remote = new ConsistencyV5StorageManager(
                    ConfigurationUtils.buildKeyGenerator(properties, environment),
                    readScopeAware,
                    writeScopeAware,
                    lockerManager,
                    redisExecutor);
            if (properties.getStatistic().isEnable()) {
                remote = new StatisticalStorageManagerDecorator(remote, ORDERED_NAME, counterManager);
            }
            return remote;
        }
    }

    // 本地和远程复合型仓库管理器
    @Bean(name = "org.antframework.cache.storage.StorageManager")
    @ConditionalOnProperty(name = CacheProperties.Local.ENABLE_KEY, havingValue = "true", matchIfMissing = true)
    public LocalRemoteStorageManager localRemoteStorageManager(ConsistencyV5LockerManager lockerManager,
                                                               RedisExecutor redisExecutor,
                                                               ChangePublisher publisher,
                                                               CounterManager counterManager,
                                                               CacheProperties properties,
                                                               Environment environment) {
        return LocalRemoteStorageManagers.build(
                readScopeAware,
                writeScopeAware,
                lockerManager,
                redisExecutor,
                publisher,
                counterManager,
                properties,
                environment);
    }

    /**
     * 本地和远程复合型仓库管理器工具
     */
    public static class LocalRemoteStorageManagers {
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

        // 构建仓库管理器
        static LocalRemoteStorageManager build(ReadScopeAware readScopeAware,
                                               WriteScopeAware writeScopeAware,
                                               ConsistencyV5LockerManager lockerManager,
                                               RedisExecutor redisExecutor,
                                               ChangePublisher publisher,
                                               CounterManager counterManager,
                                               CacheProperties properties,
                                               Environment environment) {
            StorageManager local = buildLocal(counterManager, properties);
            StorageManager remote = buildRemote(
                    readScopeAware,
                    writeScopeAware,
                    lockerManager,
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
        private static StorageManager buildLocal(CounterManager counterManager, CacheProperties properties) {
            StorageManager local = new CaffeineStorageManager(
                    ConfigurationUtils.toLocalLiveTimeSupplier(properties.getLiveTime(), properties),
                    ConfigurationUtils.toLongSupplier(properties.getLocal().getMaxSize()));
            if (properties.getStatistic().isEnable()) {
                local = new StatisticalStorageManagerDecorator(local, LOCAL_ORDERED_NAME, counterManager);
            }
            return local;
        }

        // 构建远程仓库
        private static StorageManager buildRemote(ReadScopeAware readScopeAware,
                                                  WriteScopeAware writeScopeAware,
                                                  ConsistencyV5LockerManager lockerManager,
                                                  RedisExecutor redisExecutor,
                                                  CounterManager counterManager,
                                                  CacheProperties properties,
                                                  Environment environment) {
            StorageManager remote = new ConsistencyV5StorageManager(
                    ConfigurationUtils.buildKeyGenerator(properties, environment),
                    readScopeAware,
                    writeScopeAware,
                    lockerManager,
                    redisExecutor);
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
    }

    // 环路计数器管理器
    @Bean(name = "org.antframework.cache.statistic.CounterManager")
    @ConditionalOnMissingBean(CounterManager.class)
    public RingCounterManager counterManager(CacheProperties properties) {
        return new RingCounterManager(properties.getStatistic().getTimeLength(), properties.getStatistic().getTimeGranularity());
    }

    // 基于spring-data-redis的Redis执行器
    @Bean(name = "org.antframework.cache.common.consistencyv5.redis.RedisExecutor")
    @ConditionalOnMissingBean(RedisExecutor.class)
    public SpringDataRedisExecutor redisExecutor(RedisConnectionFactory connectionFactory) {
        return new SpringDataRedisExecutor(connectionFactory);
    }

    // 修改发布器配置导入器
    @Configuration
    @ConditionalOnMissingBean(ChangePublisher.class)
    @Import(ChangePublisherConfiguration.class)
    public static class ChangePublisherConfigurationImporter {
    }
}
