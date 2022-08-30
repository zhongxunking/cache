/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-04 21:59 创建
 */
package org.antframework.cache.storage.localremote.change;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 一批修改
 */
@Getter
@Setter
public class ChangeBatch implements Serializable {
    // 修改集
    private List<Change> changes;
}
