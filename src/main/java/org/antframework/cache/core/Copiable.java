/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-25 10:10 创建
 */
package org.antframework.cache.core;

/**
 * 可复制的
 */
public interface Copiable {
    /**
     * 复制
     *
     * @return 复制出的对象
     */
    Copiable copy();
}
