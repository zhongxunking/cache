/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2020-05-07 21:08 创建
 */
package org.antframework.cache.lock.consistencyv5.sync;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antframework.cache.common.consistencyv5.PuttedValue;
import org.antframework.cache.common.consistencyv5.ReadScopeAware;
import org.antframework.cache.common.consistencyv5.WriteScopeAware;
import org.antframework.cache.common.consistencyv5.redis.RedisExecutor;
import org.antframework.sync.common.SyncUtils;
import org.antframework.sync.extension.redis.support.SyncMaintainer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 缓存一致性方案5的基于redis的读写锁服务端
 */
@AllArgsConstructor
@Slf4j
public class ConsistencyV5RedisRWLockServer {
    // 加读锁脚本
    private static final String LOCK_FOR_READ_SCRIPT = SyncUtils.getScript("META-INF/consistencyv5/locker/sync/RWLock-lockForRead.lua");
    // 解读锁脚本
    private static final String UNLOCK_FOR_READ_SCRIPT = SyncUtils.getScript("META-INF/consistencyv5/locker/sync/RWLock-unlockForRead.lua");
    // 维护读锁脚本
    private static final String MAINTAIN_FOR_READ_SCRIPT = SyncUtils.getScript("META-INF/consistencyv5/locker/sync/RWLock-maintainForRead.lua");
    // 加写锁脚本
    private static final String LOCK_FOR_WRITE_SCRIPT = SyncUtils.getScript("META-INF/consistencyv5/locker/sync/RWLock-lockForWrite.lua");
    // 解写锁脚本
    private static final String UNLOCK_FOR_WRITE_SCRIPT = SyncUtils.getScript("META-INF/consistencyv5/locker/sync/RWLock-unlockForWrite.lua");
    // 维护写锁脚本
    private static final String MAINTAIN_FOR_WRITE_SCRIPT = SyncUtils.getScript("META-INF/consistencyv5/locker/sync/RWLock-maintainForWrite.lua");

    // 读锁维护器
    private final SyncMaintainer readLockMaintainer = new SyncMaintainer();
    // 写锁维护器
    private final SyncMaintainer writeLockMaintainer = new SyncMaintainer();
    // redis执行器
    private final RedisExecutor redisExecutor;
    // 存活时间（毫秒）
    private final long liveTime;
    // 维护执行器
    private final Executor maintainExecutor;
    // 读作用域感知器
    private final ReadScopeAware readScopeAware;
    // 写作用域感知器
    private final WriteScopeAware writeScopeAware;

    /**
     * 加读锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @return null 加锁成功；否则返回需等待的时间（毫秒）
     */
    public Long lockForRead(String key, String lockerId) {
        if (readScopeAware.isActive() && readScopeAware.isLockFailed()) {
            return liveTime;
        }

        String redisKey = computeRedisKey(key);
        long currentTime = System.currentTimeMillis();
        List<?> result = redisExecutor.eval(
                LOCK_FOR_READ_SCRIPT,
                Collections.singletonList(redisKey),
                Arrays.asList(lockerId, currentTime, liveTime),
                List.class);

        Long waitTime = (Long) result.get(0);
        if (waitTime < 0) {
            waitTime = null;
            readLockMaintainer.add(key, lockerId);
        } else if (result.size() > 1) {
            waitTime = liveTime;
            byte[] gotValue = (byte[]) result.get(1);
            if (readScopeAware.isActive() && gotValue != null) {
                readScopeAware.setGotValue(gotValue);
            }
        }

        return waitTime;
    }

    /**
     * 解读锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    public void unlockForRead(String key, String lockerId) {
        readLockMaintainer.remove(key, lockerId);
        String redisKey = computeRedisKey(key);
        long currentTime = System.currentTimeMillis();
        String syncChannel = computeSyncChannel(key);
        PuttedValue puttedValue = readScopeAware.isActive() ? readScopeAware.getPuttedValue() : null;
        try {
            boolean success = redisExecutor.eval(
                    UNLOCK_FOR_READ_SCRIPT,
                    Collections.singletonList(redisKey),
                    Arrays.asList(
                            lockerId,
                            currentTime,
                            syncChannel,
                            puttedValue == null ? null : puttedValue.getValue(),
                            puttedValue == null ? null : puttedValue.getLiveTime()),
                    Boolean.class);
            if (!success) {
                log.error("调用redis解读锁失败（锁不存在或已经易主），可能已经发生并发问题：key={},lockerId={}", key, lockerId);
            }
        } catch (Throwable e) {
            log.error("调用redis解读锁出错：", e);
        }
    }

    /**
     * 加写锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     * @param deadline 截止时间
     * @return null 加锁成功；否则返回需等待的时间（毫秒）
     */
    public Long lockForWrite(String key, String lockerId, long deadline) {
        String redisKey = computeRedisKey(key);
        long currentTime = System.currentTimeMillis();
        boolean removeValue = writeScopeAware.isActive();
        Long waitTime = redisExecutor.eval(
                LOCK_FOR_WRITE_SCRIPT,
                Collections.singletonList(redisKey),
                Arrays.asList(
                        lockerId,
                        deadline,
                        currentTime,
                        liveTime,
                        removeValue),
                Long.class);
        if (waitTime == null) {
            writeLockMaintainer.add(key, lockerId);
        }
        return waitTime;
    }

    /**
     * 解写锁
     *
     * @param key      锁标识
     * @param lockerId 加锁者id
     */
    public void unlockForWrite(String key, String lockerId) {
        writeLockMaintainer.remove(key, lockerId);
        String redisKey = computeRedisKey(key);
        String syncChannel = computeSyncChannel(key);
        PuttedValue puttedValue = writeScopeAware.isActive() ? writeScopeAware.getPuttedValue(key) : null;
        try {
            boolean success = redisExecutor.eval(
                    UNLOCK_FOR_WRITE_SCRIPT,
                    Collections.singletonList(redisKey),
                    Arrays.asList(
                            lockerId,
                            syncChannel,
                            puttedValue == null ? null : puttedValue.getValue(),
                            puttedValue == null ? null : puttedValue.getLiveTime()),
                    Boolean.class);
            if (!success) {
                log.error("调用redis解写锁失败（锁不存在或已经易主），可能已经发生并发问题：key={},lockerId={}", key, lockerId);
            }
        } catch (Throwable e) {
            log.error("调用redis解写锁出错：", e);
        }
    }

    /**
     * 维护
     */
    public void maintain() {
        Set<String> keys = readLockMaintainer.getKeys();
        for (String key : keys) {
            maintainExecutor.execute(() -> readLockMaintainer.maintain(key, (k, lockerId) -> doMaintain(k, lockerId, MAINTAIN_FOR_READ_SCRIPT, Arrays.asList(lockerId, System.currentTimeMillis(), liveTime))));
        }
        keys = writeLockMaintainer.getKeys();
        for (String key : keys) {
            maintainExecutor.execute(() -> writeLockMaintainer.maintain(key, (k, lockerId) -> doMaintain(k, lockerId, MAINTAIN_FOR_WRITE_SCRIPT, Arrays.asList(lockerId, liveTime))));
        }
    }

    // 执行维护
    private boolean doMaintain(String key, String lockerId, String script, List<Object> args) {
        String redisKey = computeRedisKey(key);
        boolean alive = true;
        try {
            alive = redisExecutor.eval(
                    script,
                    Collections.singletonList(redisKey),
                    args,
                    Boolean.class);
            if (alive) {
                log.debug("调用redis维护读写锁成功：key={},lockerId={}", key, lockerId);
            } else {
                log.error("调用redis维护读写锁失败（锁不存在或已经易主），可能已经发生并发问题：key={},lockerId={}", key, lockerId);
            }
        } catch (Throwable e) {
            log.error("调用redis维护读写锁出错：", e);
        }
        return alive;
    }

    /**
     * 计算同步通道
     *
     * @param key 锁标识
     * @return 同步通道
     */
    public String computeSyncChannel(String key) {
        return computeRedisKey(key);
    }

    // 计算在redis中key
    private String computeRedisKey(String key) {
        return key;
    }
}
