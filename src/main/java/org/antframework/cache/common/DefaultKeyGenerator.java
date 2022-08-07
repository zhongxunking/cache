/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-27 22:23 创建
 */
package org.antframework.cache.common;

import lombok.AllArgsConstructor;

import java.util.function.BinaryOperator;

/**
 * 默认的key生成器
 */
@AllArgsConstructor
public class DefaultKeyGenerator implements BinaryOperator<String> {
    /**
     * 分隔符
     */
    public static final String SEPARATOR = "::";

    // 命名空间
    private final String namespace;

    @Override
    public String apply(String name, String key) {
        return namespace + SEPARATOR + name + SEPARATOR + key;
    }
}
