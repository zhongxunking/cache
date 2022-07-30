/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-29 23:29 创建
 */
package org.antframework.cache.statistic;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.SortedMap;

/**
 * 计数器
 */
public interface Counter {
    /**
     * 获取名称
     *
     * @return 名称
     */
    String getName();

    /**
     * 增加一次加载命中
     *
     * @param time     时间戳
     * @param timeCost 耗时（单位：毫秒）
     */
    void incLoadHits(long time, long timeCost);

    /**
     * 增加一次加载未命中
     *
     * @param time     时间戳
     * @param timeCost 耗时（单位：毫秒）
     */
    void incLoadMisses(long time, long timeCost);

    /**
     * 增加一次仓库命中
     *
     * @param time        时间戳
     * @param orderedName 有次序的名称
     * @param timeCost    耗时（单位：毫秒）
     */
    void incStorageHits(long time, String orderedName, long timeCost);

    /**
     * 增加一次仓库未命中
     *
     * @param time        时间戳
     * @param orderedName 有次序的名称
     * @param timeCost    耗时（单位：毫秒）
     */
    void incStorageMisses(long time, String orderedName, long timeCost);

    /**
     * 统计
     *
     * @param startTime 开始时间戳
     * @param endTime   结束时间戳
     * @return 统计结果
     */
    Statistic count(long startTime, long endTime);

    /**
     * 统计结果
     */
    @AllArgsConstructor
    @Getter
    class Statistic {
        // 加载统计
        private final Detail load;
        // 有次序的名称与仓库统计的映射
        private final SortedMap<String, Detail> orderedNameStorages;
        // 功效（实际读数据耗时/无缓存时读数据耗时；-1表示无法计算；值越小功效越强，比如：0.1表示因为缓存的存在读数据耗时缩短到原来的10%）
        private final double efficacy;

        /**
         * 细节信息
         */
        @AllArgsConstructor
        @Getter
        public static class Detail {
            // 命中次数
            private final long hits;
            // 平均的命中耗时（单位：毫秒；-1表示无法计算）
            private final long averageHitTimeCost;
            // 未命中次数
            private final long misses;
            // 平均的未命中耗时（单位：毫秒；-1表示无法计算）
            private final long averageMissTimeCost;
        }
    }
}
