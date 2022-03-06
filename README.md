# 马户商土成


[nacos配置中心](coupon/src/main/java/cn/nicenan/mahumall/coupon/controller/CouponController.java)

### nacos注册

1.配置文件内给应用起名字和设置nacos server地址
2.Application内开启访问注册与发现功能 @EnableDiscoveryClient

[gateway](gateway/src/main/java/cn/nicenan/mahumall/gateway/GatewayApplication.java)

[geteway例子](gateway/src/main/resources/application.yml)

### feign

```text
     *   1)、让所有请求过网关；
     *          1、@FeignClient("mahumall-gateway")：mahumall-gateway所在的机器发请求
     *          2、/api/product/skuinfo/info/{skuId}
     *   2）、直接让后台指定服务处理
     *          1、@FeignClient("mahumall-product")
     *          2、/product/skuinfo/info/{skuId}
```
Feign调用流程
1. 构造请求数据，转成json
2. 发送请求
3. 执行请求重试机制。默认关闭
[feign远程调用](member/src/main/java/cn/nicenan/mahumall/member/feign/CouponFeignService.java)

[JSR303 校验](product/src/main/java/cn/nicenan/mahumall/product/entity/BrandEntity.java)

[api文档](https://easydoc.net/s/78237135/ZUqEdvA4/hKJTcbfd)

### 业务逻辑

#### 商品系统平台属性

规格参数和属性分组有关联 (pms_attr_attrgroup_relation)和 销售属性公用代码，都是商品属性(pms_attr)
规格参数是指商品详情页的参数：比如有(属性分组) 主体:上市月份，品牌 基本信息： 长度,宽度,厚度 屏幕：材质类型,尺寸
属性分组有类别
销售属性((笛卡尔积)组合成sku?)用于搜索，没有再分组，比如商品编号，毛重，系统
参考jd

#### 库存采购

先生成采购单，再添加采购需求
由人工或者系统低库存自动预警生成
采购需求合并成采购单(人工合并，系统定时合并)
分配给采购人员
通知供应商或者自主采购
采购单入库，添加库存

[p83 发布商品流程](https://www.bilibili.com/video/BV1np4y1C7Yf?p=83&spm_id_from=pageDriver) 

mysql默认隔离级别是可重复读，调试的时候改成 未提交读 Read uncommitted 方便查询
`SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;`

## Elasticsearch

### Docker安装

```bash
mkdir -p /root/elasticsearch/config
mkdir -p /root/elasticsearch/data
mkdir -p /root/elasticsearch/plugins
mkdir -p /root/elasticsearch/logs
echo "http.host: 0.0.0.0">>/root/elasticsearch/config/elasticsearch.yml
chmod -R 777 /root/elasticsearch/

docker run -d -p 9200:9200 -p 9300:9300 \
--name elasticsearch \
-e "discovery.type=single-node" \
-e ES_JAVA_OPTS="-Xms128m -Xmx256m" \
-v /root/elasticsearch/config/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml \
-v /root/elasticsearch/data:/usr/share/elasticsearch/data \
-v /root/elasticsearch/plugins:/usr/share/elasticsearch/plugins \
-v /root/elasticsearch/logs:/usr/share/elasticsearch/logs \
docker.elastic.co/elasticsearch/elasticsearch:7.17.0

# http://192.168.2.211:9200/

docker run -d --name kib01-test -p 5601:5601 -e "ELASTICSEARCH_HOSTS=http://192.168.2.211:9200" docker.elastic.co/kibana/kibana:7.17.0
```

7.4.2版本测试数据:`https://github.com/elastic/elasticsearch/blob/v7.4.2/docs/src/test/resources/accounts.json`

`POST /bank/account/_bulk`

http://192.168.2.211:5601/app/dev_tools#/console



基础结构 Index Type Document(json)
采用倒排索引，分词存储

查询对象语言 QueryDSL

java代码内最好用常量来使用index，方便以后数据迁移的时候

### query 查询

should增加评分不改变结果

bool复合查询，需要全文匹配(分词器)条件写到bool的must的match里面， must里面的会有评分。不需要评分的写到bool的filter的term。更快一点

term：查询某个字段里含有某个关键词的文档，terms：查询某个字段里含有多个关键词的文档

嵌入式的查询需要外面加一层nested指定path 

sort排序

from size 分页

highlight高亮

```json
"highlight": {
    "fields": {
      "skuTitle": {}
    },
    "pre_tags": "<b style='color:red'>",
    "post_tags": "</b>"
  }
```

###  aggs聚合分析

聚合里面可以子聚合，用上一次的数据。嵌入式聚合也得用嵌入式的

```json
#聚合分析
GET product2/_search
{"query":{"match_all":{}},"aggs":{"brand_agg":{"terms":{"field":"brandId","size":10},"aggs":{"brand_name_agg":{"terms":{"field":"brandName","size":10}},"brand_img_agg":{"terms":{"field":"brandImg","size":10}}}},"catalog_agg":{"terms":{"field":"catalogId","size":10},"aggs":{"catalog_name_agg":{"terms":{"field":"catalogName","size":10}}}},"attr_agg":{"nested":{"path":"attrs"},"aggs":{"attr_id_agg":{"terms":{"field":"attrs.attrId","size":10},"aggs":{"attr_name_agg":{"terms":{"field":"attrs.attrName","size":10}},"attr_value_agg":{"terms":{"field":"attrs.attrValue","size":10}}}}}}}}
```



### 改映射 mapping

需要用数据迁移。

新建一个index

```json
POST _reindex
{
  "source": {
    "index": "product"
  },
  "dest": {
    "index": "product2"
  }
}
```



## Nginx



### 域名指向虚拟机

p130 修改系统Hosts 指定域名到虚拟机IP，使用域名访问虚拟机web服务

可能域名无法访问，看虚拟机内网卡是否获取了多个ip，hosts设置其他ip。关闭本机代理 ,刷新dns `ipconfig -flushdns` `chrome://net-internals/#sockets`

关闭 apache2 `/etc/init.d/apache2 stop` or `update-rc.d -f apache2 remove`

### 流程

1. Nginx的http块指定上游地址 upstream mahumall {}
2. 反代地址`proxy_pass http://mahumall;`
3. Nginx添加Host信息 `proxy_set_header Host $host;`
4. 网关配置文件在最后加上根据Host断言到商品服务



### 动静分离

静态资源都放到Nginx内，规则 /static/** 所有的请求都由nginx直接返回

1. 静态资源放到Nginx的html/static目录
2. 修改Nginx配置 `location /static/`内的 `root /usr/share/nginx/html;`
3. 页面的请求路径都加上static路径

## 性能优化

### 压力测试

#### JMeter



https://jmeter.apache.org/download_jmeter.cgi

1. 添加测试计划
2. 添加线程组
3. 添加取样器-http请求
4. 添加监听器的前三个

### 性能监控

#### VisualVM

https://visualvm.github.io/download.html

`"visualvm\etc\visualvm.conf"`内配置`visualvm_jdkhome`

安装插件Visual GC

## 缓存

业务逻辑无法优化（不要循环查表），需要经常访问且变动较少的查询（商品数据）（**多读少写**）。**及时性**（物流信息），**数据一致性**（商品分类）要求不高的数据

### 缓存流程

1. 请求
2. 读取缓存中数据
   1. 命中：返回结果
3. 没命中：查询数据库
4. 数据放入缓存
5. 返回结果



### 缓存失效

#### 缓存穿透

同时100W请求进来，同时判断**不存在缓存**，就会同时100W的并发落到数据库。 

查询一个缓存不可能存在的数据，比如不存在的id，那么缓存就会失效，去查数据库。

解决：将查询结果null获标志位也放入缓存，加入短暂过期时间。（布隆过滤器）

#### 缓存雪崩

设置缓存的key采用了**相同的过期时间**，导致缓存在同一时刻失效，请求全部到数据库。

解决：原有失效时间加一个随机值，比如0-5分钟。

#### 缓存击穿

设置了过期时间的key（高频热点数据），在过期的时候一瞬间来大量请求，导致所有查询落到数据库。

解决：加锁，大量并发只让一个人查询，其他人等待，查到后放入缓存并释放锁，其他人获取到锁，先查缓存。



### Redis

引入pom,添加redis服务器配置

使用Springboot自动配置好的StringRedisTemplate操作Redis



### 锁

#### 本地锁

查询缓存-查数据库-放入缓存 整个加锁成原子操作 同步代码块：`synchronized (this) {}`。锁针对当前实例，spring 的bean都是单例模式，所以单机模式下可以锁住。

分布式情况下还是会查询多次数据库。

#### 分布式锁

基本原理：所有服务都去同一个地方占坑。

##### redis实现

使用` set key value NX ` or `setnx key value` (NX:Not eXists) 当 key不存在时插入;原子操作，只有第一个能插入，代表加锁成功。

加锁成功:执行业务代码，删除key(锁)

加锁失败:就递归（栈容易溢出）（或循环）调用自己(`sleep 100ms`)重试（自旋）

**加锁问题**：业务代码异常，或者业务代码中服务断电，导致锁没有释放，死锁。

解决：~~上锁后立马设置锁的自动过期时间~~ 可能在设置过期时间前崩溃。需要在设置锁的同时设置过期时间发给redis，原子操作。

**删锁问题**：业务1时间超时，锁自动释放了，业务2抢占加锁，业务2此时释放锁就释放的是业务2的锁，业务3也又会进来。根本原因是应为有了过期时间

加锁->业务->删锁 不是一个原子操作了。

解决：~~加锁的时候value值放入自己的uuid，删锁的时候先拿到value值。等于自己的uuid再删锁。~~可能在请求uuid值的时候锁还没过期，网络返回uuid的时候过期了，redis自动删除了锁，程序再删除可能删除别人的锁。 查值和删锁必须是一个原子操作：删锁时让Redis执行lua脚本，脚本内取值判断再删除。

https://redis.io/topics/distlock

```lua
if redis.call("get",KEYS[1]) == ARGV[1] then
    return redis.call("del",KEYS[1])
else
    return 0
end
```

**自动续期**:业务没有在锁的过期时间内完成，需要续期，



##### Redisson分布式锁框架

[文档](https://github.com/redisson/redisson/wiki/Table-of-Content)

注意加锁粒度(锁的名字)

###### 可重入锁

方法A加锁1，A调用方法B，方法B内又对锁1加锁，是可重入锁：B直接拿锁1用，最后A释放锁。如果是不可重入锁，会造成死锁。

> 实现原理实现是通过为每个锁关联一个请求计数器和一个占有它的线程。当计数为0时，认为锁是未被占有的；线程请求一个未被占有的锁时，JVM将记录锁的占有者，并且将请求计数器置为1 。
>
> 如果同一个线程再次请求这个锁，计数器将递增；
>
> 每次占用线程退出同步块，计数器值将递减。直到计数器为0,锁被释放。

redisson默认是可重入锁，实现了java的juc内的接口



`getLock(name)`获取一把锁，name一样就是同一个锁。

`lock()`加锁，阻塞式等待，拿不到锁就一直等待。

redisson实现了锁的**自动续期**（没有自己设置解锁时间：执行看门狗脚本，执行一个定时任务（看门狗时间/3）间隔续期到30s），如果业务超长，运行期间会自动续上默认30s。业务完成就不会给当前锁续期，不手动解锁，默认会在30s后释放锁。

如果lock时设置了自动解锁时间，锁到时间不会自动续期。可能导致锁自动释放进来其他的线程。自动解锁时间必须大于业务执行时间

`unLock()`解锁(放在finally块)

###### 公平锁

`getFairlock(name)` 按顺序获取锁



###### 读写锁

可重用读写锁

一定能读到最新数据

`RReadWriteLock rwlock = redisson.getReadWriteLock(name)` 

`rwlock.readLock().lock()`读锁：共享锁 ；业务读加读锁

`rwlock.writeLock().lock()`写锁：排他锁；业务修改数据加写锁

A正在修改数据，B想要读取最新数据，必须等到写锁释放。并发读互不影响，并发写排队。写锁存在，读锁等待，写锁等待。写锁不存在，读锁加锁等于没加。



###### 信号量

`getSemaphore(name)`同一个名字就是同一个信号量

`acquire()`获取一个值（信号量）信号量为0时阻塞；占一个位置  -1.

`release()`释放一个值(信号量) +1

分布式限流：信号量总量10000，服务进来先获取一个信号量 -1,说明有空闲，服务完成释放信号量+1

带try的都不阻塞，返回布尔值。



###### 闭锁

多线程调度中所有任务完成才完成。

`RCountDownLatch cd = redisson.getCountDownLatch(name)`获得锁

`cd.trySetCount(5)`等待5个完成

`cd.await()`等待闭锁都完成

`cd.countDown()`计数减一



### 缓存数据一致性

#### 双写模式

改完数据库改缓存

问题：可能出现脏数据，A写数据库后还没改缓存前，B也写数据库再改缓存，A最后改缓存。缓存内不是最新的数据，产生脏数据。

解决：

1. 写数据库和写缓存 整个加锁
2. 最新数据不敏感，设置缓存过期时间即可，过期后查一份新的

#### 失效模式

改完数据库删除缓存，等待下一次查询主动更新

问题：A写db删缓存，B写db,C读缓存读db，B删缓存，C更新缓存。最终C可能把缓存更新为A写的值，最新的确是B的值。也有脏数据问题。

解决：加读写锁

#### 总结

1. 用户维度数据，并发几率小，加过期时间即可，主动更新缓存即可。
2. 菜单，商品介绍等基础数据，可以容忍大程度缓存不一致，可以用canal订阅binlog的方式
3. 缓存数据+过期时间足够解决大部分业务
4. 通过加锁保证并发，写写按顺序排队，读读不管，适用读写锁。

cannal模拟一个mysql 从客户端，接受主mysql的binlog来解析操作，可以自动同步到redis



### Spring Cache

主要有 `CacheManager 和 Cache`两个接口。CacheManager 有很多种类比如Redis的实现，CacheManager 管理多个Cache，每个Cache定义了对缓存的增删改查。

引入pom，可以看下都自动配置了什么 `CacheAutoConfiguration`->`RedisCacheAutoConfiguration` 自动配好了CacheManager 。

自定义配置：[MyCacheConfig.java](product/src/main/java/cn/nicenan/mahumall/product/config/MyCacheConfig.java)

#### 注解

@Cacheable  triggers cache population： 触发将数据保存到缓存的操作
@CacheEvict  triggers cache eviction：  触发将数据从缓存删除的操作
@CachePut  updates the cache without interfering with the method execution：不影响方法执行更新缓存
@Caching   regroups multiple cache operations to be applied on a method：组合上面多个操作
@CacheConfig  shares some common cache-related settings at class-level：在类级别，共享缓存配置

1. Application或Configuration 使用 @EnableCaching 开启缓存
2. 使用注解开启缓存@Cacheable
   1. 代表当前方法的结果需要缓存，如果缓存中有，则方法不调用。如果缓存中没有，会调用方法，最后将结果放入缓存。
   2. 缓存的value值，默认使用jdk序列化机制
   3. 可以指定key的生成规则(spel表达式)
   4. 默认ttl时间-1,永不过期
   5. 需要指定放到哪个名字的缓存(缓存的分区，推荐按业务类型分)
   6. 指定sync=true以同步模式运行，防止缓存击穿。本地锁，不是分布式锁
3. 使用注解删除缓存@CacheEvict 
   1. 指定value和key删除某个分区下key的缓存
   2. 指定value和allEntries = true删除分区下全部缓存
   3. @Caching 组合删除多个key的缓存

对**数据有修改**可以使用

双写模式：更新操作有返回值加上@Cacheable或@CachePut更新缓存

失效模式：更新操作无返回值加上@CacheEvict删除缓存



## 异步&线程池

初始化线程的四种方式

1. 继承Thread ->  new xxx().start()
2. 实现Runnable接口->   new Thread(new xxx()).start()
3. 实现Callable接口+FutureTask（可拿到返回结果，可处理异常）
   1. new Thread(new FuterTask<String>(new Callable01())).start()   xxxFuter.get()阻塞等待线程完成获取返回值 
4. **线程池**
   1. 给线程池直接提交任务
   2. 业务代码里面只用线程池，上面三种都不用
   3. 可以控制资源，性能稳定
   4. 系统里有一两个线程池（核心业务，非核心业务（灵活关闭线程池）），每个异步任务提交给线程池用



### 线程池

### #七大参数

1. **corePoolSize**: 核心线程数。刚创建好线程池的时候是没有可用线程的，懒加载。核心线程数，线程会一直存活
2. **maximumPoolSize**: 最大线程数量，总线程数（包括核心线程）。控制资源，队列满后开新线程，会被自动释放
3. keepAliveTime: 非核心空闲线程存活时间。时间到后 (maximumPoolSize-corePoolSize) 这些线程释放
   1. 如果设置了allowCoreThreadTimeOut =true 核心线程也会超时关闭
4. unit: 存活时间单位
5. **BlockingQueue** <Runnable> workQueue:阻塞队列。如果线程超过核心线程数，会**先将任务放到队列中**
   1. 线程池创建线程需要获取mainlock这个全局锁，影响并发效率，阻塞队列可以很好的缓冲。
   2. 阻塞队列大小默认int最大值，最高根据业务自己设置
6. threadFactory: 线程的创建工程。一般默认，可以自定义线程名字
7. handler: 当线程数到达最大线程并且队列满了，按照我们指定的拒绝策略拒绝执行任务
   1. 默认丢弃策略
   2. 如果不想抛弃，可以使用CallerRunsPolicy 同步执行

> 一个线程池 core 7； max 20 ，queue：50，100 并发进来怎么分配的； 先有 7 个能直接得到执行，接下来 50 个进入队列排队，在多开 13 个继续执行。现在 70 个 被安排上了。剩下 30 个默认拒绝策



#### 4 种线程池

1. newCachedThreadPool
   1. 核心数量为0，可以缓存的线程池，可灵活回收所有空闲线程，若无可回收，则新建线程。
2. newFixedThreadPool
   1. 固定核心数量大小，都不可回收
3. newScheduledThreadPool
   1. 做定时任务的线程池
4. newSingleThreadExecutor
   1. 核心数量0 单线程化的线程池，它只会用唯一的工作线程来执行任务，保证所有任务 按照指定顺序(FIFO, LIFO, 优先级)执行。

#### CompletableFuture 异步编排

开启异步：`CompletableFuture xxFuture = CompletableFuture.runXXX(lambda,threadPool).thenXXX...`

等待完成`xxFuture .allof(xxF,xxF2...).get()` 处理异常`.exceptionally`()

#### ThreadLocal

里面是map，线程id为key

同一个线程共享数据，每个请求进来，tomcat开一个线程处理：

拦截器->controller->service->dao

## 认证

### OAuth2

client： csdn，各种论坛等第三方应用

resource owner ：用户本人

Authorization Server： QQ服务器

resource Server : QQ服务器

用户在qq网站输入账号密码，返回一个授权码，通过授权码返回受保护信息

code换access_token（一段时间多次获取不会变化），只能换一次

### session一致性

tomcat配置共享session

hash一致性，hash请求ip，固定分配的服务器

后端统一存储session,session设置到顶级域名, 使用 spring session。

### 单点登录

一处登录，处处可用，共享session不可行，应为域名可能不一致。

1. 中央认证服务器
2. 其他系统登录去中央认证服务器，登陆成功返回原系统
3. 只要有一个系统登录，其他系统都登录

核心原理就是第一次登录成功后cookie保存uuid在认证服务器的域名下面，uuid存到redis中。每个其它系统登录的时候跳转到认证服务器的域名进行登录，再重定向到其它系统，带上用户信息，便可以单点登录



## RabbitMQ

异步处理：如发送注册邮件，注册短信等。存到消息队列处理。

应用解耦：

流量控制：所有流量存到队列，后端根据能力消费

点对点式：一个发送，只有一个接收，但是可以多个监听，消息被读取后移出队列

发布订阅式：发送消息到主题，多个接收者订阅这个主题，消息到达所有接收者同时收到消息

JMS只有java能用，AMQP网络栈级协议，跨平台

RabbitTemplate发送消息，@RabbitListener 监听消息代理发布的消息

### 核心概念

1. Publisher生产者
2. Message 头(route-key)+体(数据)
   1. Routing Key：路由关键字，exchange根据这个关键字进行消息投递。
3. channel：消息通道
4. Broker：简单来说就是消息队列服务器实体。
   1. vhost：虚拟主机，一个broker里可以开设多个vhost （根据路径），用作不同用户的权限分离。
      1. Exchange：消息交换机，它指定消息按什么规则，路由到哪个队列。
      2. Binding：绑定，它的作用就是把exchange和queue按照路由规则绑定起来。
      3. Queue：消息队列载体，每个消息都会被投入到一个或多个队列。一般都是不排他
5. channel：消息通道，在一个客户端只会建立一条连接（长连接）里，一条连接可建立多个channel（多路复用），每个channel代表一个会话任务。
6. consumer：消息消费者，就是接受消息的程序。

### springboot整合

#### AmqpAdmin

创建删除交换机队列绑定



## 坑

Feign调用是泛型消失，对象变成LinkedHashMap。使用jackson反序列化为对象时，可能远程和本地的对象属性不一致，设置`objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);`忽略不存在的属性。[R.java](common/src/main/java/cn/nicenan/mahumall/common/utils/R.java)

