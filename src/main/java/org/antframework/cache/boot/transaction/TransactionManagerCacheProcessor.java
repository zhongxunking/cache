/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-06 13:40 创建
 */
package org.antframework.cache.boot.transaction;

import lombok.RequiredArgsConstructor;
import org.antframework.cache.core.TransactionalCacheManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 事务管理器的缓存处理器
 */
@RequiredArgsConstructor
public class TransactionManagerCacheProcessor implements BeanPostProcessor, ApplicationContextAware, Ordered {
    // 优先级
    private final int order;
    // 应用上下文
    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof PlatformTransactionManager)) {
            return bean;
        }
        TransactionalCacheManager cacheManager = context.getBean(TransactionalCacheManager.class);
        return new CacheableTransactionManagerDecorator((PlatformTransactionManager) bean, cacheManager);
    }

    @Override
    public int getOrder() {
        return order;
    }
}
