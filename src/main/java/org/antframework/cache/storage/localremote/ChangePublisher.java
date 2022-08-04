/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-04 21:51 创建
 */
package org.antframework.cache.storage.localremote;

/**
 * 修改发布器
 */
public interface ChangePublisher {
    /**
     * 发布消息
     *
     * @param name 仓库名称
     * @param key  被修改键值对的键
     */
    void publish(String name, String key);
}
