/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-03 19:00 创建
 */
package org.antframework.cache.storage.localremote;

import lombok.AllArgsConstructor;
import org.antframework.cache.common.Null;
import org.antframework.cache.storage.KeyEnumerable;
import org.antframework.cache.storage.Storage;

import java.util.Arrays;
import java.util.Set;

/**
 * 本地和远程复合型仓库
 */
@AllArgsConstructor
public class LocalRemoteStorage implements Storage {
    // 名称
    private final String name;
    // 本地仓库
    private final Storage localStorage;
    // 远程仓库
    private final Storage remoteStorage;
    // 本地键值对存活时长（单位：毫秒，null表示不过期）
    private final Long localLiveTime;
    // 本地null值存活时长（单位：毫秒，null表示不过期）
    private final Long localNullValueLiveTime;
    // 修改发布器
    private final ChangePublisher publisher;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public byte[] get(String key) {
        byte[] value = localStorage.get(key);
        if (value == null) {
            value = remoteStorage.get(key);
            if (value != null) {
                localStorage.put(key, value, computeLocalLiveTime(value, null), false);
            }
        }
        return value;
    }

    @Override
    public void put(String key, byte[] value, Long liveTime, boolean valueChanged) {
        localStorage.remove(key);
        remoteStorage.put(key, value, liveTime, valueChanged);
        localStorage.put(key, value, computeLocalLiveTime(value, liveTime), valueChanged);
        if (valueChanged) {
            // 发布消息
            publisher.publish(name, key);
        }
    }

    @Override
    public void remove(String key) {
        localStorage.remove(key);
        remoteStorage.remove(key);
        // 发布消息
        publisher.publish(name, key);
    }

    // 计算本地键值对存活时长
    private Long computeLocalLiveTime(byte[] value, Long liveTime) {
        Long time = Null.is(value) ? localNullValueLiveTime : localLiveTime;
        if (liveTime != null) {
            if (time == null) {
                time = liveTime;
            } else {
                time = Math.min(time, liveTime);
            }
        }
        return time;
    }

    /**
     * 删除本地仓库的键值对
     *
     * @param key 键
     */
    public void localRemove(String key) {
        localStorage.remove(key);
    }

    /**
     * 刷新本地仓库
     */
    public void refreshLocal() {
        if (localStorage instanceof KeyEnumerable) {
            Set<String> keys = ((KeyEnumerable) localStorage).getKeys();
            for (String key : keys) {
                byte[] localValue = localStorage.get(key);
                byte[] remoteValue = remoteStorage.get(key);
                if (!Arrays.equals(localValue, remoteValue)) {
                    localStorage.remove(key);
                }
            }
        }
    }
}
