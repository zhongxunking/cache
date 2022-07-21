/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-20 23:19 创建
 */
package org.antframework.cache.common;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * null的等值替代
 */
public final class Null {
    // null的等值二进制
    private final static byte[] BYTES = getString().getBytes(StandardCharsets.UTF_8);

    private Null() {
    }

    /**
     * 二进制是否等值为null
     *
     * @param bytes 被比较的二进制
     * @return true:等值为null；false:不等值为null
     */
    public static boolean is(byte[] bytes) {
        return Arrays.equals(bytes, BYTES);
    }

    /**
     * 获取null的等值二进制
     *
     * @return null的等值二进制
     */
    public static byte[] getBytes() {
        return Arrays.copyOf(BYTES, BYTES.length);
    }

    /**
     * 字符串是否等值为null
     *
     * @param str 被比较的字符串
     * @return true:等值为null；false:不等值为null
     */
    public static boolean is(String str) {
        return Objects.equals(str, getString());
    }

    /**
     * 获取null的等值字符串
     *
     * @return null的等值字符串
     */
    public static String getString() {
        return Null.class.getName();
    }
}
