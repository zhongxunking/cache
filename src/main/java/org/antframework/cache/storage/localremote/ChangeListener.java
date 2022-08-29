/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-04 21:53 创建
 */
package org.antframework.cache.storage.localremote;

/**
 * 修改监听器
 */
public interface ChangeListener {
    /**
     * 监听消息
     *
     * @param name 仓库名称
     * @param key  被修改键值对的键
     */
    void listen(String name, String key);

    /**
     * 获取优先级（值越小优先级越高）
     *
     * @return 优先级
     */
    int getOrder();
}
