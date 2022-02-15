# mahumall
马户商城


[nacos配置中心](coupon/src/main/java/cn/nicenan/mahumall/coupon/controller/CouponController.java)
##### nacos注册
1.配置文件内给应用起名字和设置nacos server地址
2.Application内开启访问注册与发现功能 @EnableDiscoveryClient

[gateway](gateway/src/main/java/cn/nicenan/mahumall/gateway/GatewayApplication.java)

[geteway例子](gateway/src/main/resources/application.yml)

[feign远程调用](member/src/main/java/cn/nicenan/mahumall/member/feign/CouponFeignService.java)

[JSR303 校验](product/src/main/java/cn/nicenan/mahumall/product/entity/BrandEntity.java)

[api文档](https://easydoc.net/s/78237135/ZUqEdvA4/hKJTcbfd)

##### 商品系统平台属性
规格参数和属性分组有关联 (pms_attr_attrgroup_relation)和 销售属性公用代码，都是商品属性(pms_attr)
规格参数是指商品详情页的参数：比如有(属性分组) 主体:上市月份，品牌 基本信息： 长度,宽度,厚度 屏幕：材质类型,尺寸
属性分组有类别
销售属性((笛卡尔积)组合成sku?)用于搜索，没有再分组，比如商品编号，毛重，系统
参考jd

[p83 发布商品流程](https://www.bilibili.com/video/BV1np4y1C7Yf?p=83&spm_id_from=pageDriver) 

mysql默认隔离级别是可重复读，调试的时候改成 未提交读 Read uncommitted 方便查询
`SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;`
