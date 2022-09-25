/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-09-25 16:12 创建
 */
package org.antframework.cache.common;

/**
 * 对象引用（类似AtomicReference，但不提供线程安全只能在单线程内使用，作为AtomicReference在单线程场景下的替代，提高性能）
 */
public class ObjectReference<T> {
    // 值
    private T value;

    /**
     * 用初始值创建ObjectReference
     *
     * @param initialValue 初始值
     */
    public ObjectReference(T initialValue) {
        this.value = initialValue;
    }

    /**
     * 获取当前值
     *
     * @return 当前值
     */
    public T get() {
        return value;
    }

    /**
     * 设置新值
     *
     * @param newValue 新值
     */
    public void set(T newValue) {
        value = newValue;
    }
}
