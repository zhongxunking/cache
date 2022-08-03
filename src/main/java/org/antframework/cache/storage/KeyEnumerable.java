/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-03 21:39 创建
 */
package org.antframework.cache.storage;

import java.util.Set;

/**
 * 键可被列举的
 */
public interface KeyEnumerable {
    /**
     * 获取所有键的集合
     *
     * @return 所有键的集合
     */
    Set<String> getKeys();
}
