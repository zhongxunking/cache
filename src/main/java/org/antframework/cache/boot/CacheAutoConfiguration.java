/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-06 19:06 创建
 */
package org.antframework.cache.boot;

import org.antframework.cache.CacheManager;
import org.antframework.cache.CacheTemplate;
import org.antframework.cache.boot.annotation.AwareValueTypeProcessor;
import org.antframework.cache.boot.annotation.ForceSyncProcessor;
import org.antframework.cache.boot.cache.CacheManagerAdapter;
import org.antframework.cache.boot.cache.ValueTypeAware;
import org.antframework.cache.boot.configuration.CacheManagerConfiguration;
import org.antframework.cache.boot.configuration.ConsistencyV5CacheManagerConfiguration;
import org.antframework.cache.boot.transaction.CacheableTransactionManagerProcessor;
import org.antframework.cache.core.TransactionalCacheManager;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Cache自动配置
 */
@Configuration
@ConditionalOnProperty(name = CacheProperties.ENABLE_KEY, havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class)
@EnableConfigurationProperties(CacheProperties.class)
@EnableCaching
public class CacheAutoConfiguration {
    // 值类型感知器
    private final ValueTypeAware valueTypeAware = new ValueTypeAware();

    // 使CacheInterceptor具有值类型感知能力的的处理器
    @Bean(name = "org.antframework.cache.boot.annotation.AwareValueTypeProcessor")
    @ConditionalOnMissingBean(AwareValueTypeProcessor.class)
    public AwareValueTypeProcessor awareValueTypeProcessor(CacheProperties properties) {
        return new AwareValueTypeProcessor(valueTypeAware, properties.getBeanProcessor().getDecorateCacheInterceptorOrder());
    }

    // 强制@Cacheable(sync=true)的处理器
    @Bean(name = "org.antframework.cache.boot.annotation.ForceSyncProcessor")
    @ConditionalOnMissingBean(ForceSyncProcessor.class)
    public ForceSyncProcessor forceSyncProcessor(CacheProperties properties) {
        return new ForceSyncProcessor(properties.getBeanProcessor().getForceSyncOrder());
    }

    // 使事务管理器具备缓存管理能力的处理器
    @Bean(name = "org.antframework.cache.boot.transaction.CacheableTransactionManagerProcessor")
    @ConditionalOnMissingBean(CacheableTransactionManagerProcessor.class)
    public CacheableTransactionManagerProcessor transactionProcessor(CacheProperties properties) {
        return new CacheableTransactionManagerProcessor(properties.getBeanProcessor().getDecorateTransactionManagerOrder());
    }

    // CacheManager适配器
    @Bean
    @ConditionalOnMissingBean(org.springframework.cache.CacheManager.class)
    public CacheManagerAdapter cacheManager(CacheManager cacheManager) {
        return new CacheManagerAdapter(cacheManager, valueTypeAware);
    }

    // 缓存操作模板
    @Bean(name = "org.antframework.cache.CacheTemplate")
    @ConditionalOnMissingBean(CacheTemplate.class)
    public CacheTemplate cacheTemplate(TransactionalCacheManager cacheManager) {
        return new CacheTemplate(cacheManager);
    }

    // 导入缓存一致性方案5的缓存管理器配置
    @Configuration
    @ConditionalOnMissingBean(TransactionalCacheManager.class)
    @ConditionalOnProperty(name = CacheProperties.ConsistencyV5.ENABLE_KEY, havingValue = "true", matchIfMissing = true)
    @Import(ConsistencyV5CacheManagerConfiguration.class)
    public static class ConsistencyV5CacheManagerConfigurationImporter {
    }

    // 导入缓存管理器配置
    @Configuration
    @ConditionalOnMissingBean(TransactionalCacheManager.class)
    @ConditionalOnProperty(name = CacheProperties.ConsistencyV5.ENABLE_KEY, havingValue = "false")
    @Import(CacheManagerConfiguration.class)
    public static class CacheManagerConfigurationImporter {
    }
}
