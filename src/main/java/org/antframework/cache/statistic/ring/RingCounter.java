/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-07-30 14:06 创建
 */
package org.antframework.cache.statistic.ring;

import org.antframework.cache.common.RingRecorder;
import org.antframework.cache.statistic.Counter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 环路计数器
 */
public class RingCounter implements Counter {
    // 名称
    private final String name;
    // 环路记录器
    private final RingRecorder<Statistic> ringRecorder;
    // 时间粒度（单位：毫秒）
    private final long timeGranularity;

    public RingCounter(String name, long timeLength, long timeGranularity) {
        this.name = name;
        this.ringRecorder = new RingRecorder<>(computeRingSize(timeLength, timeGranularity), i -> new Statistic());
        this.timeGranularity = timeGranularity;
    }

    // 计算环路容量
    private int computeRingSize(long timeLength, long timeGranularity) {
        int size = (int) (timeLength / timeGranularity);
        if (timeLength % timeGranularity > 0) {
            size++;
        }
        return size;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void incLoadHits(long time, long timeCost) {
        ringRecorder.record(computeIndex(time), statistic -> {
            statistic.load.hits.add(1);
            statistic.load.hitsTimeCost.add(timeCost);
        });
    }

    @Override
    public void incLoadMisses(long time, long timeCost) {
        ringRecorder.record(computeIndex(time), statistic -> {
            statistic.load.misses.add(1);
            statistic.load.missesTimeCost.add(timeCost);
        });
    }

    @Override
    public void incStorageHits(long time, String orderedName, long timeCost) {
        ringRecorder.record(computeIndex(time), statistic -> {
            Statistic.Detail storage = statistic.orderedNameStorages.computeIfAbsent(orderedName, k -> new Statistic.Detail());
            storage.hits.add(1);
            storage.hitsTimeCost.add(timeCost);
        });
    }

    @Override
    public void incStorageMisses(long time, String orderedName, long timeCost) {
        ringRecorder.record(computeIndex(time), statistic -> {
            Statistic.Detail storage = statistic.orderedNameStorages.computeIfAbsent(orderedName, k -> new Statistic.Detail());
            storage.misses.add(1);
            storage.missesTimeCost.add(timeCost);
        });
    }

    @Override
    public Counter.Statistic count(long startTime, long endTime) {
        // 计算总量
        Statistic total = new Statistic();
        ringRecorder.view(computeIndex(startTime), computeIndex(endTime), (index, statistic) -> {
            total.load.hits.add(statistic.load.hits.sum());
            total.load.hitsTimeCost.add(statistic.load.hitsTimeCost.sum());
            total.load.misses.add(statistic.load.misses.sum());
            total.load.missesTimeCost.add(statistic.load.missesTimeCost.sum());
            statistic.orderedNameStorages.forEach((k, v) -> {
                Statistic.Detail totalStorage = total.orderedNameStorages.computeIfAbsent(k, key -> new Statistic.Detail());
                totalStorage.hits.add(v.hits.sum());
                totalStorage.hitsTimeCost.add(v.hitsTimeCost.sum());
                totalStorage.misses.add(v.misses.sum());
                totalStorage.missesTimeCost.add(v.missesTimeCost.sum());
            });
        });
        // 计算加载统计
        Counter.Statistic.Detail load = new Counter.Statistic.Detail(total.load.hits.sum(), total.load.hitsTimeCost.sum() / total.load.hits.sum(), total.load.misses.sum(), total.load.missesTimeCost.sum() / total.load.misses.sum());
        // 计算所有仓库统计
        SortedMap<String, Counter.Statistic.Detail> orderedNameStorages = new TreeMap<>();
        total.orderedNameStorages.forEach((k, v) -> {
            Counter.Statistic.Detail storage = new Counter.Statistic.Detail(v.hits.sum(), v.hitsTimeCost.sum() / v.hits.sum(), v.misses.sum(), v.missesTimeCost.sum() / v.misses.sum());
            orderedNameStorages.put(k, storage);
        });
        // 计算功效
        double efficacy = computeEfficacy(total);

        return new Counter.Statistic(load, orderedNameStorages, efficacy);
    }

    // 计算功效
    private double computeEfficacy(Statistic statistic) {
        // 计算平均加载耗时
        long loadTimes = statistic.load.hits.sum() + statistic.load.misses.sum();
        if (loadTimes <= 0) {
            return -1;
        }
        double averageLoadTimeCost = ((double) (statistic.load.hitsTimeCost.sum() + statistic.load.missesTimeCost.sum())) / loadTimes;
        // 计算总的读次数和缓存未命中次数
        if (statistic.orderedNameStorages.size() <= 0) {
            return -1;
        }
        List<String> sortedNames = new ArrayList<>(statistic.orderedNameStorages.keySet());
        Collections.sort(sortedNames);
        Statistic.Detail firstStorage = statistic.orderedNameStorages.get(sortedNames.get(0));
        Statistic.Detail lastStorage = statistic.orderedNameStorages.get(sortedNames.get(sortedNames.size() - 1));

        long totalTimes = firstStorage.hits.sum() + firstStorage.misses.sum();
        long storageMissTimes = lastStorage.misses.sum();
        // 计算缓存总耗时
        double totalStorageTimeCost = 0;
        for (Map.Entry<String, Statistic.Detail> entry : statistic.orderedNameStorages.entrySet()) {
            totalStorageTimeCost += entry.getValue().hitsTimeCost.sum();
            totalStorageTimeCost += entry.getValue().missesTimeCost.sum();
        }

        return (averageLoadTimeCost * storageMissTimes + totalStorageTimeCost) / (averageLoadTimeCost * totalTimes);
    }

    // 计算序号
    private long computeIndex(long time) {
        return time / timeGranularity;
    }

    // 统计
    private static class Statistic {
        // 加载统计
        final Detail load = new Detail();
        // 有次序的仓库名称与仓库统计的映射
        final Map<String, Detail> orderedNameStorages = new ConcurrentHashMap<>();

        // 细节信息
        static class Detail {
            // 命中次数
            final LongAdder hits = new LongAdder();
            // 总的命中耗时（单位：毫秒）
            final LongAdder hitsTimeCost = new LongAdder();
            // 未命中次数
            final LongAdder misses = new LongAdder();
            // 总的未命中耗时（单位：毫秒）
            final LongAdder missesTimeCost = new LongAdder();
        }
    }
}
