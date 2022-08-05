/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-04 22:31 创建
 */
package org.antframework.cache.storage.localremote.change.springdataredis;

import lombok.AllArgsConstructor;
import org.antframework.cache.serialize.Serializer;
import org.antframework.cache.storage.localremote.ChangeListener;
import org.antframework.cache.storage.localremote.change.Change;
import org.antframework.cache.storage.localremote.change.ChangeBatch;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.HashSet;
import java.util.Set;

/**
 * 基于spring-data-redis的修改监听器容器
 */
public class SpringDataRedisChangeListenerContainer {
    // Redis消息通道
    private final ChannelTopic channel;
    // Redis消息监听器
    private final MessageListener messageListener;
    // Redis监听器容器
    private final RedisMessageListenerContainer container;
    // 变更监听器集
    private volatile Set<ChangeListener> listeners = new HashSet<>();

    public SpringDataRedisChangeListenerContainer(String channel, RedisConnectionFactory connectionFactory, Serializer serializer) {
        this.channel = new ChannelTopic(channel);
        this.messageListener = new RedisListener(serializer);
        this.container = buildContainer(connectionFactory);
    }

    // 构建Redis监听器容器
    private RedisMessageListenerContainer buildContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.afterPropertiesSet();
        container.start();
        // 添加空监听器，防止容器报错
        container.addMessageListener((message, pattern) -> {
        }, new ChannelTopic("cache"));

        return container;
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
        Set<ChangeListener> newListeners = new HashSet<>(listeners.size() + 1);
        newListeners.addAll(listeners);
        newListeners.add(listener);
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
        Set<ChangeListener> newListeners = new HashSet<>(listeners);
        newListeners.remove(listener);
        listeners = newListeners;
        if (listeners.size() <= 0) {
            container.removeMessageListener(messageListener);
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
                Set<ChangeListener> listenersCopy = listeners;
                for (ChangeListener listener : listenersCopy) {
                    for (Change change : batch.getChanges()) {
                        listener.listen(change.getName(), change.getKey());
                    }
                }
            } catch (Throwable e) {
                // todo 打印日志
            }
        }
    }
}
