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
import org.antframework.cache.common.DefaultKeyGenerator;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BinaryOperator;
import java.util.function.Function;

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
    public OnoffTransactionalCacheManager cacheManager(@Qualifier(CacheProperties.KEY_CONVERTER_BEAN_NAME) Function<Object, String> keyConverter,
                                                       SerializerManager serializerManager,
                                                       ConsistencyV5LockerManager lockerManager,
                                                       StorageManager storageManager,
                                                       CounterManager counterManager,
                                                       CacheProperties properties) {
        TransactionalCacheManager cacheManager = new DefensibleTransactionalCacheManager(
                ConfigurationUtils.toSupplier(properties.getAllowNull()),
                keyConverter,
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

    // key转换器
    @Bean(name = CacheProperties.KEY_CONVERTER_BEAN_NAME)
    @ConditionalOnMissingBean(name = CacheProperties.KEY_CONVERTER_BEAN_NAME)
    public DefaultKeyConverter keyConverter() {
        return new DefaultKeyConverter();
    }

    // Hessian序列化器管理器
    @Bean(name = "org.antframework.cache.serialize.SerializerManager")
    @ConditionalOnMissingBean(SerializerManager.class)
    public HessianSerializerManager serializerManager() {
        return new HessianSerializerManager();
    }

    // 加锁器管理器
    @Bean(name = "org.antframework.cache.lock.LockerManager")
    public ConsistencyV5LockerManager lockerManager(@Qualifier(CacheProperties.KEY_GENERATOR_BEAN_NAME) BinaryOperator<String> keyGenerator,
                                                    RedisExecutor redisExecutor,
                                                    CacheProperties properties) {
        ConsistencyV5RedisServer server = new ConsistencyV5RedisServer(
                redisExecutor,
                properties.getConsistencyStrategyV5().getLocker().getLiveTime(),
                readScopeAware,
                writeScopeAware);
        SyncContext syncContext = new SyncContext(
                new org.antframework.sync.common.DefaultKeyConverter(),
                server,
                properties.getConsistencyStrategyV5().getLocker().getMaxWaitTime());

        return new ConsistencyV5LockerManager(keyGenerator, syncContext);
    }

    // 仓库管理器
    @Bean(name = "org.antframework.cache.storage.StorageManager")
    @ConditionalOnProperty(name = CacheProperties.Local.ENABLE_KEY, havingValue = "false")
    public StorageManager storageManager(@Qualifier(CacheProperties.KEY_GENERATOR_BEAN_NAME) BinaryOperator<String> keyGenerator,
                                         ConsistencyV5LockerManager lockerManager,
                                         RedisExecutor redisExecutor,
                                         CounterManager counterManager,
                                         CacheProperties properties,
                                         Environment environment) {
        return StorageManagers.build(
                keyGenerator,
                readScopeAware,
                writeScopeAware,
                lockerManager,
                redisExecutor,
                counterManager,
                properties);
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
        static StorageManager build(BinaryOperator<String> keyGenerator,
                                    ReadScopeAware readScopeAware,
                                    WriteScopeAware writeScopeAware,
                                    ConsistencyV5LockerManager lockerManager,
                                    RedisExecutor redisExecutor,
                                    CounterManager counterManager,
                                    CacheProperties properties) {
            StorageManager remote = new ConsistencyV5StorageManager(
                    keyGenerator,
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
    public LocalRemoteStorageManager localRemoteStorageManager(@Qualifier(CacheProperties.KEY_GENERATOR_BEAN_NAME) BinaryOperator<String> keyGenerator,
                                                               ConsistencyV5LockerManager lockerManager,
                                                               RedisExecutor redisExecutor,
                                                               ChangePublisher publisher,
                                                               CounterManager counterManager,
                                                               CacheProperties properties) {
        return LocalRemoteStorageManagers.build(
                keyGenerator,
                readScopeAware,
                writeScopeAware,
                lockerManager,
                redisExecutor,
                publisher,
                counterManager,
                properties);
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
        static LocalRemoteStorageManager build(BinaryOperator<String> keyGenerator,
                                               ReadScopeAware readScopeAware,
                                               WriteScopeAware writeScopeAware,
                                               ConsistencyV5LockerManager lockerManager,
                                               RedisExecutor redisExecutor,
                                               ChangePublisher publisher,
                                               CounterManager counterManager,
                                               CacheProperties properties) {
            StorageManager local = buildLocal(counterManager, properties);
            StorageManager remote = buildRemote(
                    keyGenerator,
                    readScopeAware,
                    writeScopeAware,
                    lockerManager,
                    redisExecutor,
                    counterManager,
                    properties);

            LocalRemoteStorageManager storageManager = new LocalRemoteStorageManager(
                    local,
                    remote,
                    ConfigurationUtils.toLocalLiveTimeSupplier(properties.getLiveTime(), properties),
                    ConfigurationUtils.toLocalLiveTimeSupplier(properties.getNullValueLiveTime(), properties),
                    publisher,
                    properties.getLocal().getListenOrder());
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
        private static StorageManager buildRemote(BinaryOperator<String> keyGenerator,
                                                  ReadScopeAware readScopeAware,
                                                  WriteScopeAware writeScopeAware,
                                                  ConsistencyV5LockerManager lockerManager,
                                                  RedisExecutor redisExecutor,
                                                  CounterManager counterManager,
                                                  CacheProperties properties) {
            StorageManager remote = new ConsistencyV5StorageManager(
                    keyGenerator,
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
                    log.debug("刷新本地缓存");
                    storageManager.refreshLocals();
                } catch (Throwable e) {
                    log.error("刷新本地缓存出错", e);
                }
            }
        }
    }

    // key生成器
    @Bean(name = CacheProperties.KEY_GENERATOR_BEAN_NAME)
    @ConditionalOnMissingBean(name = CacheProperties.KEY_GENERATOR_BEAN_NAME)
    public DefaultKeyGenerator keyGenerator(CacheProperties properties, Environment environment) {
        String namespace = ConfigurationUtils.computeNamespace(properties, environment);
        return new DefaultKeyGenerator(namespace);
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

    // 导入修改发布器配置
    @Configuration
    @ConditionalOnProperty(name = CacheProperties.Local.ENABLE_KEY, havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(ChangePublisher.class)
    @Import(ChangePublisherConfiguration.class)
    public static class ChangePublisherConfigurationImporter {
    }
}
