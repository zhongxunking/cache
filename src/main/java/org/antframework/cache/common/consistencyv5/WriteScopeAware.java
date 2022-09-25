/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-15 11:48 创建
 */
package org.antframework.cache.common.consistencyv5;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存一致性方案5的写作用域感知器
 */
public class WriteScopeAware extends AbstractScopeAware<WriteScopeAware.Context> {
    @Override
    protected Context createContext() {
        return new Context();
    }

    /**
     * 添加设置的值
     *
     * @param key         键
     * @param puttedValue 设置的值
     */
    public void addPuttedValue(String key, PuttedValue puttedValue) {
        getContext().keyPuttedValues.put(key, puttedValue);
    }

    /**
     * 删除设置的值
     *
     * @param key 键
     * @return 设置的值
     */
    public PuttedValue removePuttedValue(String key) {
        return getContext().keyPuttedValues.remove(key);
    }

    // 上下文
    static class Context {
        // 键与设置的值的映射
        private final Map<String, PuttedValue> keyPuttedValues = new HashMap<>();
    }
}
