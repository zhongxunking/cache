/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-22 14:45 创建
 */
package org.antframework.cache.common;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * 懒汉
 */
public class Idler {
    // 目标标识与等待点的映射
    private final Map<Object, WaitPoint> targetIdWaitPoints = new ConcurrentHashMap<>();

    /**
     * 获取目标（当多个线程同时获取同一个目标时，只会允许第一个线程去加载目标，其他线程等待加载结果并直接获取）
     *
     * @param targetId      目标标识
     * @param targetLoader  需获取的目标的加载器
     * @param idleConverter 为等待线程准备的目标转换器
     * @param <T>           目标类型
     * @return 目标
     */
    public <T> T acquire(Object targetId, Callable<T> targetLoader, Function<T, T> idleConverter) {
        WaitPoint waitPoint = targetIdWaitPoints.compute(targetId, (k, v) -> {
            if (v == null) {
                v = new WaitPoint();
            }
            v.ready();
            return v;
        });
        ObjectReference<T> target = new ObjectReference<>(null);
        if (waitPoint.amIRunner()) {
            ObjectReference<Throwable> ex = new ObjectReference<>(null);
            try {
                target.set(targetLoader.call());
            } catch (Throwable e) {
                ex.set(e);
            } finally {
                targetIdWaitPoints.computeIfPresent(targetId, (k, v) -> {
                    if (v.amIRunner()) {
                        if (ex.get() == null) {
                            v.awakeWaiters(target.get());
                        } else {
                            v.awakeWaitersExceptionally(ex.get());
                        }
                        v = null;
                    }
                    return v;
                });
            }
            if (ex.get() != null) {
                return Exceptions.rethrow(ex.get());
            }
        } else {
            T value = (T) waitPoint.waitTarget();
            value = idleConverter.apply(value);
            target.set(value);
        }
        return target.get();
    }

    // 等待点
    private static class WaitPoint {
        // 运行者（运行目标加载器的线程）
        private final long runner = Thread.currentThread().getId();
        // 运行者给等待者交付目标的场所
        private CompletableFuture<Object> completableFuture = null;

        // 是否是运行者
        boolean amIRunner() {
            return Thread.currentThread().getId() == runner;
        }

        // 准备好交付目标的场所
        void ready() {
            if (!amIRunner() && completableFuture == null) {
                completableFuture = new CompletableFuture<>();
            }
        }

        // 等待目标
        Object waitTarget() {
            try {
                return completableFuture.get();
            } catch (InterruptedException e) {
                return Exceptions.rethrow(e);
            } catch (ExecutionException e) {
                return Exceptions.rethrow(e.getCause());
            }
        }

        // 用目标唤醒所有等待者
        void awakeWaiters(Object target) {
            if (completableFuture != null) {
                completableFuture.complete(target);
            }
        }

        // 用异常唤醒所有等待者
        void awakeWaitersExceptionally(Throwable e) {
            if (completableFuture != null) {
                completableFuture.completeExceptionally(e);
            }
        }
    }
}
