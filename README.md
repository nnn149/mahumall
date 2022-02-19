# mahumall
马户商城


[nacos配置中心](coupon/src/main/java/cn/nicenan/mahumall/coupon/controller/CouponController.java)
##### nacos注册
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

##### 商品系统平台属性
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
