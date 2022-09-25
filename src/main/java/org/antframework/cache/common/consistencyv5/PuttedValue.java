/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-19 17:35 创建
 */
package org.antframework.cache.common.consistencyv5;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 被设置键值对的值
 */
@AllArgsConstructor
@Getter
public class PuttedValue {
    // 值
    private final byte[] value;
    // 存活时长（单位：毫秒，null表示不过期）
    private final Long liveTime;
}
