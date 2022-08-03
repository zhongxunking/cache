/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-03 20:26 创建
 */
package org.antframework.cache.storage.localremote;

/**
 * 远程监听器
 */
public interface RemoteListener {
    /**
     * 值被修改
     *
     * @param name 仓库名称
     * @param key  被修改值的键
     */
    void change(String name, String key);
}
