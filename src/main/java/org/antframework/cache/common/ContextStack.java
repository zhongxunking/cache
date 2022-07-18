/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-14 13:15 创建
 */
package org.antframework.cache.common;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 上下文栈
 */
public class ContextStack {
    // 栈
    private final Deque<Frame> stack = new LinkedList<>();

    /**
     * 入栈
     *
     * @param requireNew 是否需要新的上下文入栈
     */
    public void push(boolean requireNew) {
        Frame frame = null;
        if (!requireNew) {
            frame = stack.peek();
        }
        if (frame == null) {
            frame = new Frame();
            stack.push(frame);
        }
        frame.depth++;
    }

    /**
     * 出栈
     */
    public void pop() {
        Frame frame = stack.peek();
        if (frame != null) {
            frame.depth--;
            if (frame.depth <= 0) {
                stack.pop();
            }
        }
    }

    /**
     * 获取栈头
     *
     * @return 栈头
     */
    public Map<Object, Object> peek() {
        Frame frame = stack.peek();
        if (frame == null) {
            return null;
        }
        return frame.context;
    }

    /**
     * 获取栈深度
     *
     * @return 栈深度
     */
    public int getDepth() {
        int depth = 0;
        for (Frame frame : stack) {
            depth += frame.depth;
        }
        return depth;
    }

    // 栈桢
    private static class Frame {
        // 上下文
        private final Map<Object, Object> context = new HashMap<>();
        // 深度
        private int depth = 0;
    }
}
