/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-06 17:25 创建
 */
package org.antframework.cache.storage.localremote.change.empty;

import org.antframework.cache.storage.localremote.ChangePublisher;

/**
 * 空修改发布器
 */
public class EmptyChangePublisher implements ChangePublisher {
    @Override
    public void publish(String name, String key) {
    }
}
