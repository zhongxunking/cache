/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-17 19:03 创建
 */
package org.antframework.cache.common.consistencyv5;

/**
 * 缓存一致性方案5的抽象的作用域感知器
 *
 * @param <T> 上下文类型
 */
public abstract class AbstractScopeAware<T> {
    // 上下文持有器
    private final ThreadLocal<T> contextHolder = new ThreadLocal<>();

    /**
     * 进入作用域
     *
     * @param callback 回调
     */
    public void activate(Runnable callback) {
        T oldContext = contextHolder.get();
        T newContext = createContext();
        try {
            contextHolder.set(newContext);
            callback.run();
        } finally {
            try {
                onActivateEnd(newContext);
            } finally {
                contextHolder.set(oldContext);
            }
        }
    }

    /**
     * 创建上下文
     *
     * @return 上下文
     */
    protected abstract T createContext();

    /**
     * 离开作用域时
     *
     * @param context 上下文
     */
    protected void onActivateEnd(T context) {
    }

    /**
     * 进入非作用域
     *
     * @param callback 回调
     */
    public void deactivate(Runnable callback) {
        T context = contextHolder.get();
        try {
            contextHolder.remove();
            callback.run();
        } finally {
            contextHolder.set(context);
        }
    }

    /**
     * 是否处在作用域中
     *
     * @return true:处在; false:不处在
     */
    public boolean isActive() {
        return getContext() != null;
    }

    /**
     * 获取上下文
     *
     * @return 上下文
     */
    protected T getContext() {
        return contextHolder.get();
    }
}
