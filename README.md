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
2. 发送请求(被调用的服务有拦截器验证的需要放行)
3. 执行请求重试机制。默认关闭
[feign远程调用](member/src/main/java/cn/nicenan/mahumall/member/feign/CouponFeignService.java)

[JSR303 校验](product/src/main/java/cn/nicenan/mahumall/product/entity/BrandEntity.java)

[api文档](https://easydoc.net/s/78237135/ZUqEdvA4/hKJTcbfd)



feign在远程调用之前要构造请求，调用拦截器（默认没有拦截器）。默认构造的request对象没有header。可以对添加自己的请求拦截器进行增强。[配置](order/src/main/java/cn/nicenan/mahumall/order/config/MyFeignConfig.java)

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



数据的查询=信息流、资金付款退款=资金流、物流

订单中心：用户信息，订单信息，商品信息，物流信息，支付信息，促销信息

订单状态:

1. 待付款：需要库存锁定，支付超时自动取消订单
2. 已付款/代发货：仓库调拨，配货，分拣，出库等
3. 待收货/已发货：同步物流信息
4. 已完成:后续支付侧结算，订单若有问题进入售后
5. 已取消：自动取消或自动取消
6. 售后

#### 锁库存

按照下单的收货地址，找就近仓库，锁定库存

微服务情况下，创建订单，然后远程调用锁库存，若使用异常(远程调用失败抛出异常)回滚事务，可能出现**假异**常情况：

比如创建订单然后远程调用锁库存，锁库存成功，但是由于网络或其他原因，远程调用报错出一样，创建订单回滚，实际上库存已经被锁定。

或者远程锁库存完毕后其他的操作出现异常，订单回滚，但是锁库存并不会回滚。

使用分布式事务，使用延时队列（推荐）

## 事务

### 本地事务

事务基本性质：原子性，一致性，隔离性，持久性。

本地事务就是所有操作在一个数据库连接。

#### 事务隔离级别：

1. 读未提交：一个事务可以读到其他事务未提交的数据，出现脏读
2. 读提交：一个事务可以读取另一个已提交的事务，多次读会造成不一样的结果(同一事务内开始select可能和第二次select结果不一样，数据中途被另一个事务改变)。不可重复读。mssql，oracle默认
3. 可重复度：mysql默认。同一事务中，select的结果是事务开始的状态。出现幻读(A先select出N条数据，B插入M条数据，A再select就变成N+M条数据)，InnoDB通过next-key locks 行锁避免，
4. 序列化：给读操作加读共享锁，避免脏读，不可重复读和幻读

不可重复读和幻读比较：
两者有些相似，但是前者针对的是update或delete，后者针对的insert。

#### 事务的传播行为

事务A()调用事务B和事务C：传播行为决定 bc要不要和a共用一个事务

1. PROPAGATION_REQUIRED:如果当前没有事务，就创建一个新事物，存在事务就加入该事务，最常用。A的设置传播到B，B异常AB都回滚。
2. PROPAGATION_REQUIRES_NEW:创建新事物，无论当前存不存在事务。C异常后C回滚，AB不会滚。

springboot事务设置失效（超时多少秒）：

springboot中应为是用代理对象，同一个service内调用其他事务方法等于没有通过代理，spring的事务注解不生效，等于都是跟A一个事务。

解决：

1. 使用代理对象来调用事务方法，引入aop组件`spring-boot-starter-aop`。开启aspectj动态代理(不需要接口)而不是jdk动态代理。本类互调用使用代理对象`AopContext.currentProxy()`
2. 貌似spring可以注入自身？

### 分布式事务

分布式系统经常出现的异常
机器宕机、网络异常、消息丢失、消息乱序、数据错误、不可靠的 TCP、存储数据丢失..

#### CAP定理

在一个分布式系统中

1. C一致性：所有节点下的数据的备份在同一时刻有同样的值
2. A可用性：部分节点故障可继续使用 
3. P分区容错性（一定要满足）：分布系统在多个子网络，两个自网络之间通信可能失败

这三个要素最多同时实现两个，不能三者兼顾。最终CP或AP

一致性算法：raft、paxos

raft：领导选举，日志复制  [动画](http://thesecretlivesofdata.com/raft/) [动画2](https://raft.github.io/).。无法保证可用性，可能选不出新领导

棉铃的问题：实际业务需要保证服务可用性达99.999%，即抛弃C，保证A和P

##### BASE理论

保证AP的情况下永远无法保证强一致，改为弱一致性，**最终一致性**

保证系统基本可用：可以舍弃响应时间和部分功能

软状态：系统存在中间状态如同步中

最终一致性：所有数据副本在一段时间后最终达到一致状态。

#### 分布式事务几种解决方案

2PC 二阶提交模式，XA Transactions。数据库原生支持简单，性能差。或3PC

柔性事务：

**TCC** 事务补偿型方案 : 自己编写try、confirm、cancel对应的代码

**高并发使用以下两种**：

最大努力通知型方案: 按规律进行通知(MQ)，不保证数据一定能通知成功，提供查询接口核对。类似第三方支付的异步回调。

可靠消息+最终一致性方案(异步确保型)：失败发消息，收到失败消息回滚。

#### Seata

p288 分布式事务解决方案(2PC模式)低并发用，比如后台管理系统简单场景

1. TC (Transaction Coordinator) - 事务协调者
   1. 全局管理
   2. 维护全局和分支事务的状态，驱动全局事务提交或回滚。
2. TM (Transaction Manager) - 事务管理器
   1. 大事物
   2. 定义全局事务的范围：开始全局事务、提交或回滚全局事务。
3. RM (Resource Manager) - 资源管理器
   1. 小事务
   2. 管理分支事务处理的资源，与TC交谈以注册分支事务和报告分支事务的状态，并驱动分支事务提交或回滚。

AT模式：

1. 每个数据库创建undo_log表
2. 安装seata server 跟依赖中seata-all版本一致
   1. register.conf 配置注册中心，配置中心
   2. file.conf可以搬去配置中心，也可以用文件。可以配置事务日志存在文件还是数据
3. 所有用到分布式事务的微服务使用seata数据源代理
4. 每个服务导入file.conf并更改vgroup_xx应用名字x  （新版本取消？）
5. @GlobalTransaction 在业务方法上开启全局事务

#### 高并发下解决方案

订单出现错误发消息给库存服务，库存服务自己解锁库存

库存服务自动解锁：

### 幂等性

[p275](https://www.bilibili.com/video/BV1np4y1C7Yf?p=275)

提交多次和提交一次结果相同，防止重复提交

用户多次点击按钮、用户页面回退、微服务互相调用(调用减库存没成功重试可能应为网络)、其他情况

#### 解决方案

1. Token机制：验证码  。开始生成token放到页面，页面提交带上token。服务端收到token后先删除。获取token+对比token+删除token 整个是原子操作。  token存在redis中，使用lua脚本完成获取对比删除。
2. 数据库悲观锁：一般伴随事物一起使用，数据锁定时间长。 
   1. for update仅适用于InnoDB，且必须在事务块(BEGIN/COMMIT)中才能生效。在进行事务操作时，通过“for update”语句，**MySQL会对查询结果集中每行数据都添加排他锁**，其他线程对该记录的更新与删除操作都会阻塞。
3. 数据库乐观锁：采用version字段。当读取数据时，将version字段的值一同读出，数据每更新一次，对此version值加一。当我们提交更新的时候，判断数据库表对应记录的当前版本信息与第一次取出来的version值进行比对，如果数据库表当前版本号与第一次取出来的version值相等，则予以更新，否则认为是过期数据。`update t_goods set count = count -1 , version = version + 1 where good_id=2 and version = 1`
4. 业务层分布式锁
5.   数据库唯一约束：分库分表场景下，路由需要保证相同请求落在同一数据库的同一数据表中。表不能是自增主键，需要在业务中生成全局唯一的主键。
6. redis set 防重：计算数据的md5存入redis的set，若md5存在就不处理 
7. 数据库防重表：订单号插入去重表作唯一索引。去重表和业务表需要在同一数据库，保证同一事物。
8. 全局请求唯一ID：调用接口时成成一个唯一id，redis保存到去重集合中。~~nginx设置每个请求的唯一id~~（可以做链路追踪）`proxy_set_header X-Request-Id $request_id;`

数据库设置订单号唯一索引

[p264 订单](https://www.bilibili.com/video/BV1np4y1C7Yf?p=264&spm_id_from=pageDriver)

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

#### java锁原理

[java锁](https://www.bilibili.com/video/BV1xT4y1A7kA)

自旋：cas轮询，线程不断循环查看目标对象的锁有没有释放，释放了就获取。长时间自旋(有自旋次数默认10)=浪费cpu有适应性自旋。

每个java对象有一把锁，在对象头中，锁中记录了当前对象被哪个线程占用。

java线程实际上是对操作系统线程的映射(1:1,早期1:多，某个线程阻塞会导致多个线程同时等待)，挂起或唤醒需要切换操作系统内核态，某些情况下切换时间甚至超过线程执行任务的时间。synchronized 会对程序性能造成严重的影响,java6后优化

Java6引入：

1. 无锁：竞争情况下通过CAS(Compare and swap),操作系统中一条指令实现（CPU都有实现）。
2. 偏向锁：
3. 轻量级锁
4. 重量级锁



乐观锁：版本号机制和CAS算法实现。观锁适用于多读的应用类型，这样可以提高吞吐量。在Java中`java.util.concurrent.atomic`包下面的原子变量类就是使用了乐观锁的一种实现方式**CAS**实现的。

悲观锁：写多情况下使用。

AQS.CAS.JUC面试

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

每个微服务有自己的交换机（Topic），每个交换机下又有很多队列 

**用途**

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
      3. Queue：消息队列载体，每个消息都会被投入到一个或多个队列。一般都是不排他（只对首次声明它的连接可见。会在其连接断开的时候自动删除。）
5. channel：消息通道，在一个客户端只会建立一条连接（长连接）里，一条连接可建立多个channel（多路复用），每个channel代表一个会话任务。
6. consumer：消息消费者，就是接受消息的程序。

### 延时队列

实现定时任务，场景：下订单30分钟后未支付自动关闭，锁库存(保存锁库存信息到表)40分钟后检查订单不存在或取消解锁库存(根据锁库存表回复库存)

spring的schedule定时任务轮询消耗系统内存，增加了数据库压力，存在大的时间误差。

使用rabbitmq的队列ttl和死信Exchange结合。

消息的ttl就是消息的存活时间Time to Live，惰性检查需要排队。后面的消息要等前面的消息过期

队列ttl就是队列没有消费者连接时的保留时间。

一个消息在满足如下条件，会进入**死信路由**

1. 消息被拒收，reject 并且 requeue为false
2. ttl到了
3. 队列长度满了，排在前面的(佬)消息会被丢弃或进入死信路由

队列设置x-message-ttl 过期时间,x-dead-letter-exchange 设置死信路由，routing-key 路由键。不能有消费者监听此队列





### springboot整合



 AmqpAdmin 创建删除交换机队列绑定

@RabbitListener

1. 放在方法或类上，参数一是Message，参数二是实体类，参数三是channel
2. 同一个消息只有一个客户端能收到
3. 当前方法处理完了释放了才会接受下一个消息
4. @RabbitHandler 在有@RabbitListener类内的方法上，根据不同的参数类型，选择不同的方法接受

创建Bean来创建队列、交换机、绑定等。[配置](order/src/main/java/cn/nicenan/mahumall/order/config/MyMqConfig.java)

#### 可靠性

开启事务会大幅降低性能，所以使用

消息确认机制 开启 `spring.rabbitmq.publisher-confirm-type=correlated`,设置回调

1. publisher 发送到Broker时返回 confirmCallback 可靠抵达。集群模式下需要所有broker都收到
2. publisher Exchange未投递到queue时 returnCallback
3. consumer ack机制

发送数据时设置唯一id，存到数据库。定时扫描数据库，没有送达的消息重发。



消费端确认：保证每个消息被正确消费，才可以删除这个消息

默认是自动确认，只要客户端接收到消息，就会自动确认，服务端删除消息

开启手动确认`spring.rabbitmq.listener.simple.acknowledge-mode=manual`

unacked的消息没被确认后会变回ready状态

long deliveryTag = messsage.getMessageProperties().getDeliveryTag()一个channel内自增的

channel.basicAck(deliveryTag,false) 确认消息。false非批量模式  确认可以try catch起来

channel.basicNack()和channel.basicReject()拒绝消息

## 坑

Feign调用是泛型消失，对象变成LinkedHashMap。使用jackson反序列化为对象时，可能远程和本地的对象属性不一致，设置`objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);`忽略不存在的属性。[R.java](common/src/main/java/cn/nicenan/mahumall/common/utils/R.java)

