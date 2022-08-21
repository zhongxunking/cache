/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-21 22:10 创建
 */
package org.antframework.cache.lock.consistencyv5.sync;

import lombok.extern.slf4j.Slf4j;
import org.antframework.cache.common.consistencyv5.ReadScopeAware;
import org.antframework.cache.common.consistencyv5.WriteScopeAware;
import org.antframework.cache.common.consistencyv5.redis.RedisExecutor;
import org.antframework.sync.extension.Server;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 缓存一致性方案5的Redis服务端
 */
@Slf4j
public class ConsistencyV5RedisServer implements Server {
    // 定时触发器
    private final Timer timer = new Timer("ConsistencyV5RedisServer-sync-maintainer", true);
    // Redis执行器
    private final RedisExecutor redisExecutor;
    // 读写锁服务端
    private final ConsistencyV5RedisRWLockServer rwLockServer;

    public ConsistencyV5RedisServer(RedisExecutor redisExecutor,
                                    long liveTime,
                                    ReadScopeAware readScopeAware,
                                    WriteScopeAware writeScopeAware) {
        if (redisExecutor == null || liveTime <= 0) {
            throw new IllegalArgumentException("redisExecutor不能为null且liveTime必须大于0");
        }
        Executor maintainExecutor = new ThreadPoolExecutor(
                1,
                10,
                5,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1024),
                new ThreadPoolExecutor.CallerRunsPolicy());
        this.redisExecutor = redisExecutor;
        this.rwLockServer = new ConsistencyV5RedisRWLockServer(
                redisExecutor,
                liveTime,
                maintainExecutor,
                readScopeAware,
                writeScopeAware);
        this.timer.schedule(new MaintainTask(), liveTime / 10, liveTime / 10);

    }

    @Override
    public Long lockForMutex(String key, String lockerId, long deadline) {
        throw new UnsupportedOperationException("不支持加互斥锁");
    }

    @Override
    public void unlockForMutex(String key, String lockerId) {
        throw new UnsupportedOperationException("不支持解互斥锁");
    }

    @Override
    public Long lockForRead(String key, String lockerId, long deadline) {
        return rwLockServer.lockForRead(key, lockerId);
    }

    @Override
    public void unlockForRead(String key, String lockerId) {
        rwLockServer.unlockForRead(key, lockerId);
    }

    @Override
    public Long lockForWrite(String key, String lockerId, long deadline) {
        return rwLockServer.lockForWrite(key, lockerId, deadline);
    }

    @Override
    public void unlockForWrite(String key, String lockerId) {
        rwLockServer.unlockForWrite(key, lockerId);
    }

    @Override
    public Long acquireForSemaphore(String key, String semaphorerId, int newPermits, int totalPermits, long deadline) {
        throw new UnsupportedOperationException("不支持获取信号量许可");
    }

    @Override
    public void releaseForSemaphore(String key, String semaphorerId, int newPermits, int totalPermits) {
        throw new UnsupportedOperationException("不支持释放信号量许可");
    }

    @Override
    public void addSyncListener(SyncType syncType, String key, Runnable listener) {
        String channel = computeSyncChannel(syncType, key);
        redisExecutor.addMessageListener(channel, listener);
    }

    @Override
    public void removeSyncListener(SyncType syncType, String key, Runnable listener) {
        String channel = computeSyncChannel(syncType, key);
        redisExecutor.removeMessageListener(channel, listener);
    }

    // 计算同步通道
    private String computeSyncChannel(SyncType syncType, String key) {
        String channel;
        switch (syncType) {
            case MUTEX_LOCK:
                throw new UnsupportedOperationException("不支持监听互斥锁同步事件");
            case RW_LOCK:
                channel = rwLockServer.computeSyncChannel(key);
                break;
            case SEMAPHORE:
                throw new UnsupportedOperationException("不支持监听信号量同步事件");
            default:
                throw new IllegalArgumentException("无法识别的Sync类型：" + syncType);
        }
        return channel;
    }

    // 维护任务
    private class MaintainTask extends TimerTask {
        @Override
        public void run() {
            try {
                rwLockServer.maintain();
            } catch (Throwable e) {
                log.error("定时维护互斥锁、读写锁、信号量在redis中的有效期出错：", e);
            }
        }
    }
}
