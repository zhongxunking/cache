/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-04 22:31 创建
 */
package org.antframework.cache.storage.localremote.change.springdataredis;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antframework.cache.serialize.Serializer;
import org.antframework.cache.storage.localremote.ChangeListener;
import org.antframework.cache.storage.localremote.change.Change;
import org.antframework.cache.storage.localremote.change.ChangeBatch;
import org.antframework.sync.extension.redis.extension.springdataredis.support.RedisListenerContainer;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 基于spring-data-redis的修改监听器容器
 */
@Slf4j
public class SpringDataRedisChangeListenerContainer {
    // Redis消息通道
    private final ChannelTopic channel;
    // Redis监听器容器
    private final RedisListenerContainer container;
    // Redis消息监听器
    private final MessageListener messageListener;
    // 变更监听器集
    private volatile List<ChangeListener> listeners = new ArrayList<>();

    public SpringDataRedisChangeListenerContainer(String channel, RedisConnectionFactory connectionFactory, Serializer serializer) {
        this.channel = new ChannelTopic(channel);
        this.container = new RedisListenerContainer(connectionFactory);
        this.messageListener = new RedisListener(serializer);
    }

    /**
     * 添加修改监听器
     *
     * @param listener 修改监听器
     */
    public synchronized void addListener(ChangeListener listener) {
        if (listeners.contains(listener)) {
            return;
        }
        List<ChangeListener> newListeners = new ArrayList<>(listeners.size() + 1);
        newListeners.addAll(listeners);
        newListeners.add(listener);
        Collections.sort(newListeners, Comparator.comparingInt(ChangeListener::getOrder));
        listeners = newListeners;
        if (listeners.size() == 1) {
            container.addMessageListener(messageListener, channel);
        }
    }

    /**
     * 删除修改监听器
     *
     * @param listener 修改监听器
     */
    public synchronized void removeListener(ChangeListener listener) {
        if (!listeners.contains(listener)) {
            return;
        }
        List<ChangeListener> newListeners = new ArrayList<>(listeners);
        newListeners.remove(listener);
        listeners = newListeners;
        if (listeners.size() <= 0) {
            container.removeMessageListener(messageListener, channel);
        }
    }

    // Redis监听器
    @AllArgsConstructor
    private class RedisListener implements MessageListener {
        // 序列化器
        private final Serializer serializer;

        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                ChangeBatch batch = serializer.deserialize(message.getBody(), ChangeBatch.class);
                if (batch == null) {
                    return;
                }
                List<ChangeListener> listenersCopy = listeners;
                for (ChangeListener listener : listenersCopy) {
                    for (Change change : batch.getChanges()) {
                        listener.listen(change.getName(), change.getKey());
                    }
                }
            } catch (Throwable e) {
                log.error("接收到缓存变更消息后处理消息出错", e);
            }
        }
    }
}
