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
