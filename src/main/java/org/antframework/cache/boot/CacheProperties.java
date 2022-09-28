/*
 * 作者：钟勋 (email:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2022-08-01 19:25 创建
 */
package org.antframework.cache.boot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置
 */
@ConfigurationProperties("ant.cache")
@Validated
@Getter
@Setter
public class CacheProperties {
    /**
     * 是否启用Cache的key
     */
    public static final String ENABLE_KEY = "ant.cache.enable";
    /**
     * key转换器的bean名称
     */
    public static final String KEY_CONVERTER_BEAN_NAME = "org.antframework.cache.keyConverter";
    /**
     * key生成器的bean名称
     */
    public static final String KEY_GENERATOR_BEAN_NAME = "org.antframework.cache.keyGenerator";

    /**
     * 选填：是否启用Cache（true为开启，false为关闭；默认启用）
     */
    private boolean enable = true;
    /**
     * 选填：缓存开关（true为开启，false为关闭；默认开启）
     */
    private boolean cacheSwitch = true;
    /**
     * 选填：命名空间（默认为spring.application.name对应的值）
     */
    private String namespace = null;
    /**
     * 选填：各缓存是否允许缓存null（true为允许，false为不允许；默认允许）
     */
    @NotNull
    @Valid
    private CachesDetail<Boolean> allowNull = new CachesDetail<>(true, new HashMap<>());
    /**
     * 选填：各缓存加锁最长等待时长（单位：毫秒；-1表示永远等待直到加锁成功；默认为5秒）
     */
    @NotNull
    @Valid
    private CachesDetail<Long> maxLockWaitTime = new CachesDetail<>(5 * 1000L, new HashMap<>());
    /**
     * 选填：各缓存内键值对存活时长（单位：毫秒；-1表示永远有效不过期；默认为1小时）
     */
    @NotNull
    @Valid
    private CachesDetail<Long> liveTime = new CachesDetail<>(60 * 60 * 1000L, new HashMap<>());
    /**
     * 选填：各缓存内键值对值为null的存活时长（单位：毫秒；-1表示永远有效不过期；默认为5分钟）
     */
    @NotNull
    @Valid
    private CachesDetail<Long> nullValueLiveTime = new CachesDetail<>(5 * 60 * 1000L, new HashMap<>());
    /**
     * 选填：各缓存内键值对存活时长的动态浮动比例（正数为向上浮动，负数为向下浮动；比如：-0.1表示向下浮动10%；默认为-0.1）
     */
    @NotNull
    @Valid
    private CachesDetail<Double> liveTimeFloatRate = new CachesDetail<>(-0.1, new HashMap<>());
    /**
     * 选填：本地缓存配置
     */
    @NotNull
    @Valid
    private Local local = new Local();
    /**
     * 选填：缓存统计配置
     */
    @NotNull
    @Valid
    private Statistic statistic = new Statistic();
    /**
     * 选填：缓存一致性方案5相关配置
     */
    @NotNull
    @Valid
    private ConsistencyV5 consistencyV5 = new ConsistencyV5();
    /**
     * 选填：BeanPostProcessor相关配置
     */
    @NotNull
    @Valid
    private BeanProcessor beanProcessor = new BeanProcessor();

    /**
     * 本地缓存配置
     */
    @Getter
    @Setter
    public static class Local {
        /**
         * 是否启用本地缓存的key
         */
        public static final String ENABLE_KEY = "ant.cache.local.enable";
        /**
         * 默认的监听缓存键值对变更事件的优先级
         */
        public static final int DEFAULT_LISTEN_ORDER = 0;

        /**
         * 选填：是否启用本地缓存（true为启用，false为不启用；默认启用）
         */
        private boolean enable = true;
        /**
         * 选填：键值对存活时长配置
         */
        private LiveTime liveTime = new LiveTime();
        /**
         * 选填：各本地缓存的最大容量（-1表示无限制；默认为10000）
         */
        @NotNull
        @Valid
        private CachesDetail<Long> maxSize = new CachesDetail<>(10000L, new HashMap<>());
        /**
         * 选填：刷新器配置
         */
        @NotNull
        @Valid
        private Refresher refresher = new Refresher();
        /**
         * 选填：发布器配置
         */
        @NotNull
        @Valid
        private Publisher publisher = new Publisher();
        /**
         * 选填：监听缓存变更事件的优先级（默认为0）
         */
        private int listenOrder = DEFAULT_LISTEN_ORDER;

        /**
         * 存活时长配置
         */
        @Getter
        @Setter
        public static class LiveTime {
            /**
             * 选填：本地缓存内键值对最长存活时长（单位：毫秒；默认为5分钟）
             */
            @Min(0)
            private long max = 5 * 60 * 1000;
            /**
             * 选填：本地缓存内键值对存活时长比率（比如：0.1表示本地缓存内的存活时长为标准存活时长的10%；默认为0.1）
             */
            @Min(0)
            private double scaleRate = 0.1;
        }

        /**
         * 刷新器配置
         */
        @Getter
        @Setter
        public static class Refresher {
            /**
             * 选填：是否启用本地缓存刷新（true为启用，false为不启用；默认启用）
             */
            private boolean enable = true;
            /**
             * 选填：每隔多久将本地缓存与远程缓存不一致的键值对删掉（默认为5分钟）
             */
            @Min(0)
            private long period = 5 * 60 * 1000;
        }

        /**
         * 发布器配置
         */
        @Getter
        @Setter
        public static class Publisher {
            /**
             * 是否启用修改发布器的key
             */
            public static final String ENABLE_KEY = "ant.cache.local.publisher.enable";

            /**
             * 选填：各缓存内键值对有修改时，是否通知各本地缓存删除被修改的键值对（true为通知，false为不通知；默认通知）
             */
            private boolean enable = true;
            /**
             * 选填：各缓存键值对有变更时，发布消息的队列容量（默认为4096）
             */
            @Min(1)
            private int queueSize = 4096;
            /**
             * 选填：将通知消息放入队列时的超时时长（单位：毫秒；-1表示一直等待直到成功；默认为5秒）
             */
            @Min(-1)
            private long inQueueTimeout = 5000;
            /**
             * 选填；最多将多少个通知消息打包成一个发布消息（默认为100）
             */
            @Min(1)
            private int maxBatchSize = 100;
            /**
             * 选填：发布消息的线程数量（默认为4）
             */
            @Min(1)
            private int publishThreads = 4;
            /**
             * 选填：Redis发布器配置
             */
            @NotNull
            @Valid
            private Redis redis = new Redis();

            /**
             * Redis发布器配置
             */
            @Getter
            @Setter
            public static class Redis {
                /**
                 * 选填：Redis发布器发布消息时使用的通道名称（默认为${命名空间}-cache-change）
                 */
                private String channel;
            }
        }
    }

    /**
     * 缓存统计配置
     */
    @Getter
    @Setter
    public static class Statistic {
        /**
         * 选填：是否启用缓存统计（true为启用，false为不启用；默认启用）
         */
        private boolean enable = true;
        /**
         * 选填：统计的时间长度（单位：毫秒；默认为24小时）
         */
        @Min(1)
        private long timeLength = 24 * 60 * 60 * 1000;
        /**
         * 选填：统计的时间粒度（单位：毫秒；默认为1分钟）
         */
        @Min(1)
        private long timeGranularity = 60 * 1000;
    }

    /**
     * 缓存集配置
     */
    @AllArgsConstructor
    @Getter
    @Setter
    public static class CachesDetail<T> {
        /**
         * 默认值
         */
        @NotNull
        private T def;
        /**
         * 具体缓存对应的值
         */
        @NotNull
        private Map<String, T> caches;
    }

    /**
     * 缓存一致性方案5相关配置
     */
    @Getter
    @Setter
    public static class ConsistencyV5 {
        /**
         * 是否启用缓存一致性方案5的key
         */
        public static final String ENABLE_KEY = "ant.cache.consistency-v5.enable";

        /**
         * 选填：是否启用缓存一致性方案5（true为启用，false为不启用；默认启用）
         */
        private boolean enable = true;
        /**
         * 选填：加锁器相关配置
         */
        @NotNull
        @Valid
        private Locker locker = new Locker();

        /**
         * 加锁器相关配置
         */
        @Getter
        @Setter
        public static class Locker {
            /**
             * 选填：加锁器等待同步消息的最长时间（毫秒，默认为10秒）
             */
            @Min(0)
            private long maxWaitTime = 10 * 1000;
            /**
             * 选填：发生异常时redis中加锁器数据的存活时长（毫秒，默认为10分钟）
             */
            @Min(1)
            private long liveTime = 10 * 60 * 1000;
        }
    }

    /**
     * BeanPostProcessor相关配置
     */
    @Getter
    @Setter
    public static class BeanProcessor {
        /**
         * 默认装饰CacheInterceptor处理器的优先级
         */
        public static final int DEFAULT_DECORATE_CACHE_INTERCEPTOR_ORDER = Ordered.LOWEST_PRECEDENCE - 300;
        /**
         * 默认强制@Cacheable(sync=true)处理器的优先级
         */
        public static final int DEFAULT_FORCE_SYNC_ORDER = Ordered.LOWEST_PRECEDENCE - 200;
        /**
         * 默认装饰事务管理器BeanPostProcessor的优先级
         */
        public static final int DEFAULT_DECORATE_TRANSACTION_MANAGER_ORDER = Ordered.LOWEST_PRECEDENCE - 100;

        /**
         * 选填：装饰CacheInterceptor处理器的优先级（默认为Ordered.LOWEST_PRECEDENCE - 300）
         */
        private int decorateCacheInterceptorOrder = DEFAULT_DECORATE_CACHE_INTERCEPTOR_ORDER;
        /**
         * 选填：强制@Cacheable(sync=true)处理器的优先级（默认为Ordered.LOWEST_PRECEDENCE - 200）
         */
        private int forceSyncOrder = DEFAULT_FORCE_SYNC_ORDER;
        /**
         * 选填：装饰事务管理器BeanPostProcessor的优先级（默认为Ordered.LOWEST_PRECEDENCE - 100）
         */
        private int decorateTransactionManagerOrder = DEFAULT_DECORATE_TRANSACTION_MANAGER_ORDER;
    }
}
