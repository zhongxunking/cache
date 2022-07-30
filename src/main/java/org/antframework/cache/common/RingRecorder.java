/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-28 17:41 创建
 */
package org.antframework.cache.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Consumer;
import java.util.function.LongFunction;

/**
 * 环路记录器
 */
public class RingRecorder<T> {
    // 容量
    @Getter
    private final int size;
    // 内容创建器
    private final LongFunction<T> contentCreator;
    // 当前记录的单元
    private final Cell<T>[] cells;
    // 当前序号
    @Getter
    private volatile long currentIndex;

    /**
     * 创建环路记录器
     *
     * @param size           容量
     * @param contentCreator 内容创建器
     */
    public RingRecorder(int size, LongFunction<T> contentCreator) {
        if (size <= 0 || contentCreator == null) {
            throw new IllegalArgumentException("size必须大于0且contentCreator不能为null");
        }
        this.size = size;
        this.contentCreator = contentCreator;
        cells = new Cell[size];
        for (int i = 0; i < size; i++) {
            cells[i] = new Cell<>(i, contentCreator.apply(i));
        }
        currentIndex = size - 1;
    }

    /**
     * 记录
     *
     * @param index     序号
     * @param operation 操作
     */
    public void record(long index, Consumer<T> operation) {
        if (index < 0) {
            throw new IllegalArgumentException("index不能小于0");
        }
        Cell<T> cell = cells[computeArrayIndex(index)];
        if (cell.getIndex() == index) {
            operation.accept(cell.getContent());
        } else if (cell.getIndex() < index) {
            synchronized (this) {
                long currentIndexCopy = currentIndex;
                if (index > currentIndexCopy) {
                    for (long i = index; i > currentIndexCopy && index - i < size; i--) {
                        cells[computeArrayIndex(i)] = new Cell<>(i, contentCreator.apply(i));
                    }
                    currentIndex = index;
                }
            }
            cell = cells[computeArrayIndex(index)];
            if (cell.getIndex() == index) {
                operation.accept(cell.getContent());
            }
        }
    }

    /**
     * 从开始序号查看（观察的记录个数为size）
     *
     * @param startIndex 开始序号（包含）
     * @param viewer     观察者
     */
    public void viewFrom(long startIndex, Viewer<T> viewer) {
        view(startIndex, startIndex + size - 1, viewer);
    }

    /**
     * 查看到结束序号（观察的记录个数为size）
     *
     * @param endIndex 结束序号（包含）
     * @param viewer   观察者
     */
    public void viewTo(long endIndex, Viewer<T> viewer) {
        view(endIndex - size + 1, endIndex, viewer);
    }

    /**
     * 观察
     *
     * @param startIndex 开始序号（包含）
     * @param endIndex   结束序号（包含）
     * @param viewer     观察者
     */
    public void view(long startIndex, long endIndex, Viewer<T> viewer) {
        long currentIndexCopy = currentIndex;
        long left = Math.max(startIndex, currentIndexCopy - size + 1);
        long right = Math.min(endIndex, currentIndexCopy);
        for (long i = left; i <= right; i++) {
            Cell<T> cell = cells[computeArrayIndex(i)];
            long index = cell.getIndex();
            if (index >= startIndex && index <= endIndex) {
                viewer.view(index, cell.getContent());
            }
        }
    }

    // 计算数组序号
    private int computeArrayIndex(long index) {
        return (int) (index % size);
    }

    // 记录单元
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @Getter(AccessLevel.PACKAGE)
    private static class Cell<T> {
        // 序号
        private final long index;
        // 内容
        private final T content;
    }

    /**
     * 观察者
     *
     * @param <T> 观察的类型
     */
    @FunctionalInterface
    public interface Viewer<T> {
        /**
         * 观察
         *
         * @param index   序号
         * @param content 内容
         */
        void view(long index, T content);
    }
}
