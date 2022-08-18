/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-15 10:55 创建
 */
package org.antframework.cache.common.consistencyv5;

import java.util.concurrent.locks.Lock;

/**
 * 缓存一致性方案5读作用域感知器
 */
public class ReadScopeAware extends AbstractScopeAware<ReadScopeAware.Context> {
    @Override
    protected Context createContext() {
        return new Context();
    }

    @Override
    protected void onActivateEnd(Context context) {
        if (context.status == Status.LOCK_SUCCESS) {
            context.readLock.unlock();
        }
    }

    /**
     * 是否获取成功
     *
     * @return true:成功;false:失败
     */
    public boolean isGetSuccess() {
        return getContext().gotValue != null;
    }

    /**
     * 获取获取的值
     *
     * @return 获取的值
     */
    public byte[] getGotValue() {
        return getContext().gotValue;
    }

    /**
     * 设置获取的值
     *
     * @param gotValue 获取的值
     */
    public void setGotValue(byte[] gotValue) {
        getContext().gotValue = gotValue;
    }

    /**
     * 加锁成功
     *
     * @param readLock 加的读锁
     */
    public void lockSuccess(Lock readLock) {
        getContext().status = Status.LOCK_SUCCESS;
        getContext().readLock = readLock;
    }

    /**
     * 加锁失败
     */
    public void lockFail() {
        getContext().status = Status.LOCK_FAILED;
    }

    /**
     * 是否已经加锁失败过
     *
     * @return true:已经失败过;false:未失败过
     */
    public boolean isLockFailed() {
        return getContext().status == Status.LOCK_FAILED;
    }

    /**
     * 设置设置的值
     *
     * @param puttedValue 设置的值
     */
    public void setPuttedValue(PuttedValue puttedValue) {
        getContext().puttedValue = puttedValue;
    }

    /**
     * 获取设置的值
     *
     * @return 设置的值
     */
    public PuttedValue getPuttedValue() {
        return getContext().puttedValue;
    }

    // 上下文
    static class Context {
        // 状态
        private Status status = Status.UN_TRY_LOCK;
        // 获取的值
        private byte[] gotValue = null;
        // 读锁
        private Lock readLock = null;
        // 设置的值
        private PuttedValue puttedValue = null;
    }

    // 状态
    private enum Status {
        // 还未尝试加锁
        UN_TRY_LOCK,
        // 加锁成功
        LOCK_SUCCESS,
        // 加锁失败过
        LOCK_FAILED
    }
}
