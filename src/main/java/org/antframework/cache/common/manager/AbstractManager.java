/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-23 14:07 创建
 */
package org.antframework.cache.common.manager;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 抽象实体管理器
 */
public abstract class AbstractManager<T> implements Manager<T> {
    // 名称与实体的映射
    private final Map<String, T> nameEntities = new ConcurrentHashMap<>();

    @Override
    public T get(String name) {
        T entity = nameEntities.get(name);
        if (entity == null) {
            entity = nameEntities.computeIfAbsent(name, this::create);
        }
        return entity;
    }

    /**
     * 创建实体
     *
     * @param name 实体名称
     * @return 实体
     */
    protected abstract T create(String name);

    @Override
    public Set<String> getNames() {
        return Collections.unmodifiableSet(nameEntities.keySet());
    }
}
