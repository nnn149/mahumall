# mahumall

马户商城


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

### Elasticsearch

#### Docker安装

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

### 路径

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



##### redisson

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
