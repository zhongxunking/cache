/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-04 21:55 创建
 */
package org.antframework.cache.storage.localremote.change;

import lombok.extern.slf4j.Slf4j;
import org.antframework.cache.storage.localremote.ChangePublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 抽象异步修改发布器
 */
@Slf4j
public abstract class AbstractAsyncChangePublisher implements ChangePublisher {
    // 队列
    private final BlockingQueue<Change> queue;
    // 超时时间（单位：毫秒，null表示一直等待）
    private final Long timeout;
    // 异步任务
    private final AsyncTask asyncTask;

    public AbstractAsyncChangePublisher(int queueSize, Long timeout, int maxBatchSize, int publishThreads) {
        this.queue = new ArrayBlockingQueue<>(queueSize);
        this.timeout = timeout;
        this.asyncTask = new AsyncTask(maxBatchSize, publishThreads);
        this.asyncTask.start();
    }

    @Override
    public void publish(String name, String key) {
        Change change = new Change();
        change.setName(name);
        change.setKey(key);
        try {
            if (timeout == null) {
                queue.put(change);
            } else {
                boolean success = queue.offer(change, timeout, TimeUnit.MILLISECONDS);
                if (!success) {
                    log.error("将缓存变更消息放入发送队列超时，超时时长:{}ms", timeout);
                }
            }
        } catch (Throwable e) {
            log.error("将缓存变更消息放入发送队列失败", e);
        }
    }

    /**
     * 执行发布
     *
     * @param batch 一批修改
     */
    protected abstract void doPublish(ChangeBatch batch);

    // 异步任务
    private class AsyncTask extends Thread {
        // 一批修改的最大容量
        private final int maxBatchSize;
        // 线程池
        private final Executor executor;

        AsyncTask(int maxBatchSize, int publishThreads) {
            setDaemon(true);
            this.maxBatchSize = maxBatchSize;
            this.executor = new ThreadPoolExecutor(
                    publishThreads,
                    publishThreads,
                    5,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(publishThreads * 10),
                    new ThreadPoolExecutor.CallerRunsPolicy());
        }

        @Override
        public void run() {
            while (true) {  // daemon线程，程序关闭时自动结束
                try {
                    List<Change> changes = new ArrayList<>();
                    changes.add(queue.take());
                    for (int i = 0; i < maxBatchSize - 1; i++) {
                        Change change = queue.poll();
                        if (change == null) {
                            break;
                        }
                        changes.add(change);
                    }
                    ChangeBatch batch = new ChangeBatch();
                    batch.setChanges(changes);
                    executor.execute(() -> {
                        try {
                            doPublish(batch);
                        } catch (Throwable e) {
                            log.error("发送缓存变更消息失败", e);
                        }
                    });
                } catch (Throwable e) {
                    log.error("发送缓存变更消息失败", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        // 忽略
                    }
                }
            }
        }
    }
}
