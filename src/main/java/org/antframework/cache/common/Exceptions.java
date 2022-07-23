/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-23 11:39 创建
 */
package org.antframework.cache.common;

import java.util.concurrent.Callable;

/**
 * 异常工具
 */
public final class Exceptions {
    /**
     * 调用接口（如有异常，则直接抛出）
     *
     * @param callable 需调用的接口
     * @param <T>      返回值类型
     * @return 接口返回结果
     */
    public static <T> T call(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            return rethrow(e);
        }
    }

    /**
     * 重新抛出异常（不管是否是运行时异常）
     *
     * @param e   需重新抛出的异常
     * @param <T> 匹配调用方的返回值类型
     * @return 不会有返回值
     */
    public static <T> T rethrow(Throwable e) {
        return typeErasure(e);
    }

    // 异常类型抹除
    private static <T, E extends Throwable> T typeErasure(Throwable e) throws E {
        throw (E) e;
    }
}
