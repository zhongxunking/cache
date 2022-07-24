/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-19 14:21 创建
 */
package org.antframework.cache.core;

import java.util.Map;

/**
 * 事务感知
 */
public interface TransactionAware {
    /**
     * 当前是否处于事务中
     *
     * @return true 处于事务中；false 不处于事务中
     */
    boolean isActive();

    /**
     * 获取当前事务上下文
     *
     * @return 当前事务上下文
     * @throws IllegalStateException 当前不处于事务中
     */
    Map<Object, Object> getContext();
}
