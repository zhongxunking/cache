/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-04 21:58 创建
 */
package org.antframework.cache.storage.localremote.change;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 修改
 */
@Getter
@Setter
public class Change implements Serializable {
    // 仓库名称
    private String name;
    // 被修改键值对的键
    private String key;
}
