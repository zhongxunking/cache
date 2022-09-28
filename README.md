# Cache

### 简介

Cache是一款分布式场景下基于Redis的高性能强一致的缓存组件，提供缓存高性能强一致能力、本地缓存能力、缓存防击穿能力、缓存防穿透能力、缓存防雪崩能力。 使用简单，兼容spring-cache，可与spring-boot无缝集成。

> 本组件已经上传到[maven中央库](https://search.maven.org/search?q=org.antframework.cache)

### 环境要求
* JDK1.8及以上

### 技术支持
欢迎加我微信（zhong_xun_）入群交流。<br/>
<img src="https://note.youdao.com/yws/api/personal/file/WEBbca9e0a9a6e1ea2d9ab9def1cc90f839?method=download&shareKey=00e90849ae0d3b5cb8ed7dd12bc6842e" width=150 />


## 1. 将Cache引入进你的系统
引入Cache很简单，按照以下操作即可。

### 1.1 引入Maven依赖
```xml
<dependency>
    <groupId>org.antframework.cache</groupId>
    <artifactId>cache</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <version>${对应的spring-boot版本}</version>
</dependency>
```

### 1.2 配置
在application.properties或application.yaml中配置Redis和Cache
```properties
# 必填：配置Redis地址
spring.redis.host=localhost
spring.redis.port=6379

# 必填：命名空间（也可以通过ant.cache.namespace配置）
spring.application.name=customer    #这里使用customer（会员系统）作为举例

# Cache提供了灵活多样的配置，包括：开关相关配置、缓存有效期相关配置、本地缓存相关配置、统计相关配置等
# 以下配置均是选填配置，使用方一般使用默认配置即可，无需自定义配置
# 默认配置提供：缓存键值对在Redis的有效期为1小时，本地缓存键值对最大容量为10000，允许缓存null（更多细节配置可通过下面的配置查看和定制）

# 开关相关配置
# 选填：是否启用Cache（true为开启，false为关闭；默认启用）
ant.cache.enable=true
# 选填：缓存开关（true为开启，false为关闭；默认开启）
ant.cache.cache-switch=true
# 选填：默认是否允许缓存null（true为允许，false为不允许；默认允许）
#      可以通过ant.cache.allow-null.caches.${cacheName}=false 来定制某个cache不允许缓存null
ant.cache.allow-null.def=true

# 缓存有效期相关配置
# 选填：默认的缓存键值对存活时长（单位：毫秒；-1表示永远有效不过期；默认为1小时）
#      可以通过ant.cache.live-time.caches.${cacheName}=${具体时长} 来定制某个具体cache的键值对存活时长
ant.cache.live-time.def=3600000
# 选填：默认的缓存键值对值为null的存活时长（单位：毫秒；-1表示永远有效不过期；默认为5分钟）
#      可以通过ant.cache.null-value-live-time.caches.${cacheName}=${具体时长} 来定制某个具体cache的缓存键值对值为null的存活时长
ant.cache.null-value-live-time.def=300000
# 选填：默认的缓存键值对存活时长的动态浮动比例（正数为向上浮动，负数为向下浮动；比如：-0.1表示向下浮动10%；默认为-0.1）
#      可以通过ant.cache.live-time-float-rate.caches.${cacheName}=${具体浮动比例} 来定制某个具体cache的键值对存活时长的动态浮动比例
ant.cache.live-time-float-rate.def=-0.1

# 缓存加锁相关配置
# 选填：默认的缓存加写锁最长等待时长（单位：毫秒；-1表示永远等待直到加锁成功；默认为5秒）
#      可以通过ant.cache.max-lock-wait-time.caches.${cacheName}=${具体时长} 来定制某个具体cache的最长等待时长
ant.cache.max-lock-wait-time.def=5000

# 本地缓存相关配置
# 选填：是否启用本地缓存（true为启用，false为不启用；默认启用）
ant.cache.local.enable=true
# 选填：本地缓存内键值对最长存活时长（单位：毫秒；默认为5分钟）
ant.cache.local.live-time.max=300000
# 选填：本地缓存键值对存活时长比率（比如：0.1表示本地缓存内的存活时长为标准存活时长的10%；默认为0.1）
ant.cache.local.live-time.scale-rate=0.1
# 选填：默认的本地缓存的最大容量（-1表示无限制；默认为10000）
#      可以通过ant.cache.local.max-size.caches.${cacheName}=${具体容量} 来定制某个具体本地缓存的最大容量
ant.cache.local.max-size.def=10000
# 选填：是否启用本地缓存刷新（true为启用，false为不启用；默认启用）
ant.cache.local.refresher.enable=true
# 选填：每隔多久将本地缓存与远程缓存不一致的键值对删掉（默认为5分钟）
ant.cache.local.refresher.period=300000
# 选填：各缓存内键值对有修改时，是否通知各本地缓存删除被修改的键值对（true为通知，false为不通知；默认通知）
ant.cache.local.publisher.enable=true
# 选填：各缓存键值对有变更时，发布消息的本地队列容量（默认为4096）
ant.cache.local.publisher.queue-size=4096
# 选填：将通知消息放入本地队列时的超时时长（单位：毫秒；-1表示一直等待直到成功；默认为5秒）
ant.cache.local.publisher.in-queue-timeout=5000
# 选填；最多将多少个通知消息打包成一个发布消息（默认为100）
ant.cache.local.publisher.max-batch-size=100
# 选填：发布消息的线程数量（默认为4）
ant.cache.local.publisher.publish-threads=4
# 选填：Redis发布器发布消息时使用的通道名称（默认为${命名空间}-cache-change）
ant.cache.local.publisher.redis.channel=customer-cache-change  #这里使用customer（会员系统）作为举例
# 选填：监听缓存变更事件的优先级（默认为0）
ant.cache.local.listen-order=0

# 缓存统计相关配置
# 选填：是否启用缓存统计（true为启用，false为不启用；默认启用）
ant.cache.statistic.enable=true
# 选填：统计的时间长度（单位：毫秒；默认为24小时）
ant.cache.statistic.time-length=86400000
# 选填：统计的时间粒度（单位：毫秒；默认为1分钟）
ant.cache.statistic.time-granularity=60000

# 缓存一致性方案5相关配置
# 选填：是否启用缓存一致性方案5（true为启用，false为不启用；默认启用）
ant.cache.consistency-v5.enable=true
# 选填：加锁器等待同步消息的最长时间（毫秒，默认为10秒）
ant.cache.consistency-v5.locker.max-wait-time=10000
# 选填：发生异常时Redis中加锁器数据的存活时长（毫秒，默认为10分钟）
ant.cache.consistency-v5.locker.live-time=600000

# 缓存BeanPostProcessor相关配置
# 选填：装饰CacheInterceptor处理器的优先级（默认为Ordered.LOWEST_PRECEDENCE - 300）
ant.cache.bean-processor.decorate-cache-interceptor-order=2147483347
# 选填：强制@Cacheable(sync=true)处理器的优先级（默认为Ordered.LOWEST_PRECEDENCE - 200）
ant.cache.bean-processor.force-sync-order=2147483447
# 选填：装饰事务管理器BeanPostProcessor的优先级（默认为Ordered.LOWEST_PRECEDENCE - 100）
ant.cache.bean-processor.decorate-transaction-manager-order=2147483547
```

## 2. 使用Cache
Cache提供的各种能力对使用放来说几乎是透明的，使用方无需感知到Cache的存在，按照常规的使用spring-cache来操作缓存即可。
具体如下：
* 本地缓存能力、缓存防击穿能力、缓存防穿透能力、缓存防雪崩能力：对使用方来说是透明化的支持，使用方无需感知到Cache的存在。
* 缓存高性能强一致能力：1、对于被缓存的对象是数据库中的数据，且数据库事务是通过spring-transaction来管理的场景（即95%以上的场景），对使用方来说是透明化的支持，使用方无需感知到Cache的存在。2、对于被缓存的对象不是数据库中的数据，或事务不是通过spring-transaction来管理的场景，则可以通过org.antframework.cache.CacheTemplate.consistentDo()方式来达到缓存强一致。

Cache支持和兼容spring-cache的绝大部分能力，你可以直接使用spring-cache的接口和注解来透明的使用本Cache。 当然你也可以使用本Cache的接口和CacheTemplate来使用缓存。

### 2.1 通过spring-cache使用
* 通过spring-cache的缓存注解使用
```java
@org.springframework.stereotype.Service
public class UserService {
 // 获取缓存
 @org.springframework.cache.annotation.Cacheable(cacheNames = "users", key = "#id")
 public User findById(long id) {
     // TODO 从数据库查询数据
 }
 // 设置缓存
 @org.springframework.cache.annotation.CachePut(cacheNames = "users", key = "#user.id")
 @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确运行，二是为了保证缓存强一致
 public User update(User user) {
     // TODO 更新数据库里的数据
 }
 // 删除缓存
 @org.springframework.cache.annotation.CacheEvict(cacheNames = "users", key = "#user.id")
 @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确运行，二是为了保证缓存强一致
 public void delete(User user) {
     // TODO 删除数据库里的数据
 }
}
```
> 注意：
> 1. 本Cache不支持clear操作，所以@CacheEvict的allEntries属性不能设置为true
> 2. @Cacheable的sync属性已默认强制设置为true，所以cacheNames参数只能配置一个cacheName，配置多个会报类似这样错误：java.lang.IllegalStateException: @Cacheable(sync=true) only allows a single cache on 'Builder[public abstract demo.dal.App demo.dal.AppDao.findByAppId(java.lang.String)] caches=[app, app2] | key='#p0' | keyGenerator='' | cacheManager='' | cacheResolver='' | condition='' | unless='' | sync='true''

* 通过spring-cache的缓存接口使用
```java
@org.springframework.stereotype.Service
public class UserService {
    // 获取spring-cache的缓存接口
    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    // 获取缓存
    public User findById(long id) {
        Cache cache = cacheManager.getCache("user");
        return cache.get(id, () -> {
            // TODO 从数据库查询数据
        });
    }

    // 设置缓存
    @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确运行，二是为了保证缓存强一致
    public User update(User user) {
        // TODO 更新数据库里的数据
        Cache cache = cacheManager.getCache("user");
        cache.put(user.getId(), user);
    }

    // 删除缓存
    @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确运行，二是为了保证缓存强一致
    public void delete(User user) {
        // TODO 删除数据库里的数据
        Cache cache = cacheManager.getCache("user");
        cache.evict(user.getId());
    }
}
```
> 注意：本Cache不支持clear操作，所以不能调用cache.clear()方法

### 2.1 通过本Cache的接口和CacheTemplate使用
* 通过CacheManager接口使用
```java
@org.springframework.stereotype.Service
public class UserService {
    // 获取CacheManager接口
    @Autowired
    private org.antframework.cache.CacheManager cacheManager;

    // 获取缓存
    public User findById(long id) {
        Cache cache = cacheManager.getCache("user");
        return cache.get(id, User.class, () -> {
            // TODO 从数据库查询数据
        });
    }

    // 设置缓存
    @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确运行，二是为了保证缓存强一致
    public User update(User user) {
        // TODO 更新数据库里的数据
        Cache cache = cacheManager.getCache("user");
        cache.put(user.getId(), user);
    }

    // 删除缓存
    @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确运行，二是为了保证缓存强一致
    public void delete(User user) {
        // TODO 删除数据库里的数据
        Cache cache = cacheManager.getCache("user");
        cache.remove(user.getId());
    }
}
```
> 注意：本Cache的接口和spring-cache接口的名称、方法、作用都很相似

* 通过CacheTemplate使用
```java
@org.springframework.stereotype.Service
public class UserService {
    // 获取CacheTemplate
    @Autowired
    private CacheTemplate cacheTemplate;

    public void doBiz(long id) {
        // 一致性执行（保证缓存与底层数据的一致性）
        cacheTemplate.consistentDo(cacheManager -> {
            // 缓存回调（缓存相关操作在这个回调里操作）

            Cache cache= cacheManager.getCache("user");
            cache.remove(id);
            // 其他缓存操作。。。
        }, () -> {
            // 数据回调（数据相关操作在这个回调里操作）

            // TODO 删除数据库里的ID为123的用户
            // 其他数据库操作
        });
    }
}
```
> 注意：通过CacheTemplate.consistentDo()方法可以不用预先开启事务，数据也可以不是数据库数据，可以是文件数据或网络数据等等，Cache会保证缓存的强一致。
