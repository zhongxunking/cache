/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-19 00:21 创建
 */
package org.antframework.cache.common.manager;

import java.util.Set;

/**
 * 实体管理器
 */
public interface Manager<T> {
    /**
     * 获取实体
     *
     * @param name 实体名称
     * @return 实体
     */
    T get(String name);

    /**
     * 获取已知的所有实体名称
     *
     * @return 已知的所有实体名称
     */
    Set<String> getNames();
}
