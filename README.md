# Cache

1. 简介
> Cache是一款分布式场景下基于Redis的高性能强一致缓存组件，透明化的提供缓存高性能强一致能力、缓存防脏读能力、本地缓存能力、缓存防击穿能力、缓存防穿透能力、缓存防雪崩能力、缓存热key防御能力。 使用简单，兼容spring-cache，可与spring-boot无缝集成。

> 本组件已经上传到[Maven中央库](https://search.maven.org/search?q=g:org.antframework.cache%20AND%20a:cache)

2. 环境要求
> * JDK1.8及以上

3. [整体设计](https://mp.weixin.qq.com/s/RC_uxBE6TjEUjR6QGbN0IA)

4. 技术支持
> 欢迎加我微信（zhong_xun_）入群交流。<br/>
<img src="https://note.youdao.com/yws/api/personal/file/WEB6b849e698db2a635b43eba5bc949ce1c?method=download&shareKey=27623320b5ca82cbf768b61130c81de0" width=150 />

## 1. 将Cache引入进你的系统
通过引入Maven依赖和进行少量配置即可将Cache引入进你的系统。

### 1.1 引入Maven依赖
Cache支持SpringBoot v2.x，也支持SpringBoot v1.x
```xml
<dependency>
    <groupId>org.antframework.cache</groupId>
    <artifactId>cache</artifactId>
    <version>1.1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <version>${spring-boot版本}</version>
</dependency>
```

### 1.2 配置
在application.properties或application.yaml中配置Redis和Cache
```properties
# 必填：命名空间（也可以通过ant.cache.namespace配置）
spring.application.name=customer    #这里使用customer（会员系统）作为举例

# 必填：配置Redis（Cache采用的是spring-boot原生的Redis，所以原生的所有Redis配置都有效，以下以最简洁的配置举例）
# Redis单例模式
spring.redis.host=192.168.0.1
spring.redis.port=6379
# Redis集群模式
#spring.redis.cluster.nodes=192.168.0.1:6379,192.168.0.2:6379,192.168.0.3:6379
# Redis哨兵模式
#spring.redis.sentinel.master=mymaster
#spring.redis.sentinel.nodes=192.168.0.1:26379,192.168.0.2:26379,192.168.0.3:26379


# 以下配置均是选填配置，使用方一般使用默认配置即可，无需自定义配置
# Cache提供了灵活多样的配置，包括：开关相关配置、缓存有效期相关配置、本地缓存相关配置、统计相关配置等
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

# 选填：缓存一致性策略（默认：v5）（v1:方案1（不强一致）、v4:方案4（较高性能强一致）、v5:方案5（高性能强一致）、other:其他方案）
ant.cache.consistency-strategy=v5

# 缓存一致性策略方案v5相关配置
# 选填：加锁器等待同步消息的最长时间（毫秒，默认为10秒）
ant.cache.consistency-strategy-v5.locker.max-wait-time=10000
# 选填：发生异常时Redis中加锁器数据的存活时长（毫秒，默认为10分钟）
ant.cache.consistency-strategy-v5.locker.live-time=600000

# 缓存BeanPostProcessor相关配置
# 选填：装饰CacheInterceptor处理器的优先级（默认为Ordered.LOWEST_PRECEDENCE - 300）
ant.cache.bean-processor.decorate-cache-interceptor-order=2147483347
# 选填：强制@Cacheable(sync=true)处理器的优先级（默认为Ordered.LOWEST_PRECEDENCE - 200）
ant.cache.bean-processor.force-sync-order=2147483447
# 选填：装饰事务管理器BeanPostProcessor的优先级（默认为Ordered.LOWEST_PRECEDENCE - 100）
ant.cache.bean-processor.decorate-transaction-manager-order=2147483547
```

## 2. 使用Cache
Cache提供的各种能力对使用方来说几乎是透明的，使用方无需感知到Cache的存在，按照常规的使用spring-cache来操作缓存即可。
具体如下：
* 本地缓存能力、缓存防击穿能力、缓存防穿透能力、缓存防雪崩能力、缓存热key防御能力：对使用方来说是透明化的支持，使用方无需感知到Cache的存在。
* 缓存高性能强一致能力、缓存防脏读能力：1、对于被缓存的对象是数据库中的数据，且数据库事务是通过spring-transaction来管理的场景（即95%以上的场景），对使用方来说是透明化的支持，使用方无需感知到Cache的存在。注意：在修改数据库中数据和缓存时，需先通过spring-transaction开启事务（通过@Transactional注解或PlatformTransactionManager开启事务），才能保证缓存强一致（即使没有本Cache，为了程序正确性，在修改数据库中数据时你也需要开启事务）。2、对于被缓存的对象不是数据库中的数据（文件或网络中的数据），或事务不是通过spring-transaction来管理的场景，则修改数据和缓存时需使用CacheTemplate才能保证缓存强一致和缓存防脏读。

Cache支持和兼容spring-cache的绝大部分能力，你可以直接使用spring-cache的注解和接口来透明的使用本Cache。 当然你也可以使用本Cache的接口和CacheTemplate来使用缓存。

> 注意：使用本Cache时无需担心spring-transaction管理的事务是否存在嵌套的情况（比如：一个service的@Transactional方法调用另一个service的@Transactional方法），本Cache都能很好的工作。并且本Cache具备对各个未提交事务的相互隔离能力，不会出现缓存脏读的情况。总而言之，使用本Cache让你可以透明化的具有缓存和数据库强一致的能力。

### 2.1 通过spring-cache使用
使用本Cache和自己使用spring-cache并无区别，按照常规的使用spring-cache来操作缓存即可，本Cache提供的各种能力对使用方来说是透明化的支持。

#### 2.1.1 通过spring-cache的缓存注解使用（推荐）
```java
// 数据库操作Dao
@org.springframework.stereotype.Repository
public class UserDao {
    // 查询用户（有缓存则从缓存获取，无缓存则从数据库获取并设置缓存）
    @org.springframework.cache.annotation.Cacheable(cacheNames = "user", key = "#id")
    public User find(long id) {
        // TODO 从数据库查询用户数据
    }

    // 新增用户（向数据库插入数据，并设置缓存）
    @org.springframework.cache.annotation.CachePut(cacheNames = "user", key = "#user.id")
    // @CacheEvict(cacheNames = "user", key = "#user.id")    // 也可以删除缓存
    public User insert(User user) {
        // TODO 向数据库插入用户数据
    }

    // 更新用户（向数据库修改数据，并设置缓存）
    @org.springframework.cache.annotation.CachePut(cacheNames = "user", key = "#user.id")
    // @CacheEvict(cacheNames = "user", key = "#user.id")    // 也可以删除缓存
    public User update(User user) {
        // TODO 向数据库更新用户数据
    }

    // 删除用户（向数据库删除数据，并删除缓存）
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "user", key = "#id")
    public void delete(long id) {
        // TODO 从数据库删除用户数据
    }
}

// 业务操作Service
@org.springframework.stereotype.Service
public class UserService {
    @Autowired
    private UserDao userDao;

    // 查询用户
    public User find(long id) {
        return userDao.find(id);
    }

    // 新增用户
    @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确性（有数据修改），二是为了保证缓存强一致
    public void insert(User user) {
        userDao.insert(user);
    }

    // 更新用户
    @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确性（有数据修改），二是为了保证缓存强一致
    public void update(User user) {
        userDao.update(user);
    }

    // 删除用户
    @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确性（有数据修改），二是为了保证缓存强一致
    public void delete(long id) {
        userDao.delete(id);
    }
}
```
> 注意：
> 1. 本Cache不支持clear操作，所以@CacheEvict的allEntries属性不能设置为true
> 2. @Cacheable的sync属性已默认强制设置为true，所以cacheNames参数只能配置一个cacheName，配置多个会报类似这样错误：java.lang.IllegalStateException: @Cacheable(sync=true) only allows a single cache on 'Builder[public abstract demo.dal.App demo.dal.AppDao.findByAppId(java.lang.String)] caches=[app, app2] | key='#p0' | keyGenerator='' | cacheManager='' | cacheResolver='' | condition='' | unless='' | sync='true''

#### 2.1.2 通过spring-cache的缓存接口使用
```java
// 业务操作Service
@Service
public class UserService {
    // 获取spring-cache的缓存接口
    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    // 查询用户（有缓存则从缓存获取，无缓存则从数据库获取并设置缓存）
    public User find(long id) {
        Cache cache = cacheManager.getCache("user");
        return cache.get(id, () -> {    // 对于读场景（有缓存则从缓存获取，无缓存则从数据库获取并设置缓存），需要使用本方法来获取数据，才能保证缓存的强一致（原因在注意事项中有说明）
            // TODO 从数据库查询用户数据
        });
    }

    // 新增用户（向数据库插入数据，并设置缓存）
    @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确性（有数据修改），二是为了保证缓存强一致
    public void insert(User user) {
        // TODO 向数据库插入用户数据

        // 设置缓存
        Cache cache = cacheManager.getCache("user");
        cache.put(user.getId(), user);
        // cache.evict(user.getId());   // 也可以删除缓存
    }

    // 更新用户（向数据库修改数据，并设置缓存）
    @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确性（有数据修改），二是为了保证缓存强一致
    public void update(User user) {
        // TODO 向数据库更新用户数据

        // 设置缓存
        Cache cache = cacheManager.getCache("user");
        cache.put(user.getId(), user);
        // cache.evict(user.getId());   // 也可以删除缓存
    }

    // 删除用户（向数据库删除数据，并删除缓存）
    @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确性（有数据修改），二是为了保证缓存强一致
    public void delete(long id) {
        // TODO 从数据库删除用户数据

        // 删除缓存
        Cache cache = cacheManager.getCache("user");
        cache.evict(id);
    }
}
```
> 注意：
> 1. 本Cache不支持clear操作，所以不能调用Cache.clear()方法
> 2. 为了保证缓存的强一致性，对于读场景（有缓存则从缓存获取，无缓存则从数据库获取并设置缓存），应该通过Cache.get(java.lang.Object, java.util.concurrent.Callable<T>)来获取数据，就像上面的查询用户方法一样。不能通过先调用Cache.get(java.lang.Object)获取缓存，自己判断缓存不存在再从数据库获取数据，最后调用Cache.put(java.lang.Object, java.lang.Object)方法设置缓存。否则的话可能会导致缓存不一致。因为Cache.get(java.lang.Object, java.util.concurrent.Callable)是原子性操作，而后面这种方式分散成了几个步骤后是非原子性操作。

### 2.2 通过本Cache的接口和CacheTemplate使用

#### 2.2.1 通过本Cache的接口使用
本Cache的接口和spring-cache接口的名称、方法、作用都很相似
```java
// 业务操作Service
@Service
public class UserService {
    // 获取spring-cache的缓存接口
    @Autowired
    private org.antframework.cache.CacheManager cacheManager;

    // 查询用户（有缓存则从缓存获取，无缓存则从数据库获取并设置缓存）
    public User find(long id) {
        Cache cache = cacheManager.getCache("user");
        return cache.get(id, User.class, () -> {    // 对于读场景（有缓存则从缓存获取，无缓存则从数据库获取并设置缓存），需要使用本方法来获取数据，才能保证缓存的强一致（原因在注意事项中有说明）
            // TODO 从数据库查询用户数据
        });
    }

    // 新增用户（向数据库插入数据，并设置缓存）
    @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确性（有数据修改），二是为了保证缓存强一致
    public void insert(User user) {
        // TODO 向数据库插入用户数据

        // 设置缓存
        Cache cache = cacheManager.getCache("user");
        cache.put(user.getId(), user);
        // cache.evict(user.getId());   // 也可以删除缓存
    }

    // 更新用户（向数据库修改数据，并设置缓存）
    @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确性（有数据修改），二是为了保证缓存强一致
    public void update(User user) {
        // TODO 向数据库更新用户数据

        // 设置缓存
        Cache cache = cacheManager.getCache("user");
        cache.put(user.getId(), user);
        // cache.evict(user.getId());   // 也可以删除缓存
    }

    // 删除用户（向数据库删除数据，并删除缓存）
    @org.springframework.transaction.annotation.Transactional  // 开启事务一是为了程序正确性（有数据修改），二是为了保证缓存强一致
    public void delete(long id) {
        // TODO 从数据库删除用户数据

        // 删除缓存
        Cache cache = cacheManager.getCache("user");
        cache.remove(id);
    }
}
```
> 注意：
> 1. 本Cache不支持clear操作，所以Cache没有clear方法
> 2. 为了保证缓存的强一致性，对于读场景（有缓存则从缓存获取，无缓存则从数据库获取并设置缓存），应该通过Cache.get(java.lang.Object, java.lang.Class<T>, java.util.concurrent.Callable<T>)来获取数据，就像上面的查询用户方法一样。不能通过先调用Cache.get(java.lang.Object, java.lang.Class<T>)获取缓存，自己判断缓存不存在再从数据库获取数据，最后调用Cache.put(java.lang.Object, java.lang.Object)方法设置缓存。否则的话可能会导致缓存不一致。因为Cache.get(java.lang.Object, java.lang.Class<T>, java.util.concurrent.Callable<T>)是原子性操作，而后面这种方式分散成了几个步骤后是非原子性操作。

#### 2.2.2 通过CacheTemplate使用
对于被缓存的对象不是数据库中的数据（文件或网络中的数据），或事务不是通过spring-transaction来管理的场景，则修改数据和缓存时需使用CacheTemplate才能保证缓存强一致。
```java
// 业务操作Service
@Service
public class UserService {
    // 获取spring-cache的缓存接口
    @Autowired
    private org.antframework.cache.CacheManager cacheManager;
    // 获取CacheTemplate
    @Autowired
    private CacheTemplate cacheTemplate;

    // 查询数据（有缓存则从缓存获取，无缓存则从底层数据获取并设置缓存）
    public MyData getData(long dataId) {
        Cache cache = cacheManager.getCache("mydata");
        return cache.get(dataId, MyData.class, () -> {    // 对于读场景（有缓存则从缓存获取，无缓存则从底层数据获取并设置缓存），需要使用本方法来获取数据，才能保证缓存的强一致（原因在注意事项中有说明）
            // TODO 从数据库查询用户数据
        });
    }

    // 修改底层数据和缓存
    public void updateData(long dataId, MyData data) {
        // 一致性执行（保证缓存与底层数据强一致）
        cacheTemplate.consistentDo(cacheManager -> {
            // 缓存回调（修改缓存相关操作在这个回调里编写）

            Cache cache = cacheManager.getCache("mydata");
            cache.put(dataId, data);
            // 其他缓存修改操作。。。
        }, () -> {
            // 数据回调（修改底层数据相关操作在这个回调里编写）

            // TODO 修改底层数据（比如修改文件、修改网络数据等）
            // 其他底层数据修改操作。。。
        });
    }
}
```
> 注意：
> 1. 为了保证缓存的强一致性，对于读场景（有缓存则从缓存获取，无缓存则从底层数据获取并设置缓存），应该通过Cache.get(java.lang.Object, java.lang.Class<T>, java.util.concurrent.Callable<T>)来获取数据，就像上面的查询用户方法一样。不能通过先调用Cache.get(java.lang.Object, java.lang.Class<T>)获取缓存，自己判断缓存不存在再从底层数据获取，最后调用Cache.put(java.lang.Object, java.lang.Object)方法设置缓存。否则的话可能会导致缓存不一致。因为Cache.get(java.lang.Object, java.lang.Class<T>, java.util.concurrent.Callable<T>)是原子性操作，而后面这种方式分散成了几个步骤后是非原子性操作。

## 3. Cache统计
Cache默认提供统计最近24小时统计粒度为1分钟的缓存查询统计，包括：查询耗时、本地缓存命中和未命中次数、远程缓存命中和未命中次数、底层数据加载耗时、缓存功效等。使用方可通过org.antframework.cache.statistic.CounterManager获取Cache统计数据。

通过如下方式获取统计结果：
```java
@Autowired
private CounterManager counterManager;

// 获取所有缓存过去2小时的统计数据（默认最多可以统计过去24小时）
public Map<String, Counter.Statistic> count() {
    Map<String, Counter.Statistic> result = new HashMap<>();

    long endTime = System.currentTimeMillis();
    long startTime = endTime - 2 * 60 * 60 * 1000;

    for (String name : counterManager.getNames()) {
        Counter counter = counterManager.get(name);
        Counter.Statistic statistic = counter.count(startTime, endTime);
        result.put(name, statistic);
    }

    return result;
}
```
统计结果示例如下：
```json
{
  "app": {                              // app为被统计缓存的cacheName
    "load": {                           // 加载底层数据的统计
      "hits": 1,                        // 命中次数
      "averageHitTimeCost": 62,         // 平均的命中耗时（单位：毫秒；-1表示无法计算）
      "misses": 0,                      // 未命中次数
      "averageMissTimeCost": -1         // 平均的未命中耗时（单位：毫秒；-1表示无法计算）
    },
    "orderedNameStorages": {            // 缓存统计
      "0-Local-Caffeine": {             // 本地缓存统计
        "hits": 39,                     // 命中次数
        "averageHitTimeCost": 0,        // 平均的命中耗时（单位：毫秒；-1表示无法计算）
        "misses": 2,                    // 未命中次数
        "averageMissTimeCost": 0        // 平均的未命中耗时（单位：毫秒；-1表示无法计算）
      },
      "1-Remote-Redis": {               // 远程缓存统计
        "hits": 1,                      // 命中次数
        "averageHitTimeCost": 6,        // 平均的命中耗时（单位：毫秒；-1表示无法计算）
        "misses": 1,                    // 未命中次数
        "averageMissTimeCost": 21       // 平均的未命中耗时（单位：毫秒；-1表示无法计算）
      }
    },
    "efficacy": 0.035011801730920535    // 缓存功效（实际读数据耗时/无缓存时读数据耗时；-1表示无法计算；值越小功效越强，比如：0.1表示因为缓存的存在读数据耗时缩短到原来的10%）
  }
}
```

## 4. 扩展性
Cache本身提供极其灵活的扩展：
* 如果想自己实现缓存序列化，可实现org.antframework.cache.serialize.SerializerManager接口并放入Spring容器（默认使用Hessian作为序列化器）
* 如果想自己实现缓存数据存储，可实现org.antframework.cache.storage.StorageManager接口并放入Spring容器（默认使用Caffeine作为本地缓存，Redis作为远程缓存）(采用缓存一致性方案4才有此扩展，缓存一致性方案5无此扩展)
* 如果想自己实现缓存统计，可实现org.antframework.cache.statistic.CounterManager接口并放入Spring容器（默认使用本地内存暂存缓存统计信息）
* 如果想自己实现缓存加锁，可实现org.antframework.cache.lock.LockerManager接口并放入Spring容器（默认采用基于Redis的分布式读写锁加锁器）(采用缓存一致性方案4才有此扩展，缓存一致性方案5无此扩展)

更多扩展能力可自行查看org.antframework.cache.boot.CacheAutoConfiguration、org.antframework.cache.boot.configuration.CacheManagerConfiguration、org.antframework.cache.boot.configuration.ConsistencyV5CacheManagerConfiguration
