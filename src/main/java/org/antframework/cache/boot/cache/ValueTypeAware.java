/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-25 10:49 创建
 */
package org.antframework.cache.boot.cache;

/**
 * 值类型感知器
 */
public class ValueTypeAware {
    // 值类型持有器
    private final ThreadLocal<Class<?>> valueTypeHolder = new ThreadLocal<>();

    /**
     * 执行
     *
     * @param valueType 值类型
     * @param callback  回调
     */
    public void doAware(Class<?> valueType, Runnable callback) {
        Class<?> old = valueTypeHolder.get();
        try {
            valueTypeHolder.set(valueType);
            callback.run();
        } finally {
            valueTypeHolder.set(old);
        }
    }

    /**
     * 获取值类型
     *
     * @return 值类型
     */
    public Class<?> getValueType() {
        return valueTypeHolder.get();
    }
}
