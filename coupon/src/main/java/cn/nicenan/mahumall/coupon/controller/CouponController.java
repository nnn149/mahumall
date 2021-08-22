package cn.nicenan.mahumall.coupon.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.nicenan.mahumall.coupon.entity.CouponEntity;
import cn.nicenan.mahumall.coupon.service.CouponService;
import cn.nicenan.mahumall.common.utils.PageUtils;
import cn.nicenan.mahumall.common.utils.R;

/**
 * 使用Nacos作为配置中心统一管理配置
 * 1. 引入依赖  https://github.com/alibaba/spring-cloud-alibaba/blob/master/spring-cloud-alibaba-examples/nacos-example/nacos-config-example/readme-zh.md
 * 2. 创建bootstrap.properties文件，这是springboot的文件，配置应用名字和Nacos Server地址
 * 3. 进入Nacos Server的配置中心，新建配置。Data ID一定是 xxx.properties。xxx是应用名字。默认规则 应用名.properties
 * 4. 添加任意配置
 * 5. 动态获取配置，使用两个注解@RefreshScope和  @Value("${配置项名字}")。优先使用配置中心内的值

 细节
 1. 命名空间  配置隔离。默认public(保留空间)。默认新增的配置都在public空间内。bootstrap.properties里面设置spring.cloud.nacos.config.namespace
    可以设置开发，测试，生产命名空间。推荐：也可以每一个微服务之间项目隔离配置，每个微服务创建自己的命名空间(直接用微服务的名字)，只加载自己命名空间的配置，利用分组切换环境。
 2. 配置集  所有配置的集合
 3. 配置集id  类似配置的文件名，就是Data ID。 应用名字.properties
 4. 配置分组  所有的配置集都输入:DEFAULT_GROUP组  。组可以自己随便输。
 5. 加载多个配置文件。
         多配置文件
         spring.cloud.nacos.config.extension-configs[0].data-id="xxx.yml"
         spring.cloud.nacos.config.extension-configs[0].group="dev"
         spring.cloud.nacos.config.extension-configs[0].refresh=true

 推荐用法：
 1. 每个微服务创建自己的命名空间，创建Data ID，使用配置分组区分环境(dev,test,prod)。
 2. 在bootstrap.properties指定命名空间和分组
     #命名空间
     #spring.cloud.nacos.config.namespace=xxx
     #配置分组
     #spring.cloud.nacos.config.group=hhhh
 3. 以前springboot任何方式从配置文件中获取值都能用（@Value，@ConfigurationProperties等），优先使用配置中心的值。
/**
 * 优惠券信息
 *
 * @author Nannan
 * @email 1041836312@qq.com
 * @date 2021-08-21 21:50:24
 */
@RefreshScope
@RestController
@RequestMapping("coupon/coupon")
public class CouponController {
    @Autowired
    private CouponService couponService;

    @Value("${test.user.name}")
    private String name;
    @Value("${test.user.age}")
    private Integer age;


    @RequestMapping("/test")
    public R test() {
        return R.ok().put("name", name).put("age", age);
    }

    @RequestMapping("/member/list")
    public R memberCoupons() {
        CouponEntity couponEntity = new CouponEntity();
        couponEntity.setCouponName("满99减20");
        return R.ok().put("coupons", List.of(couponEntity));
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("coupon:coupon:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = couponService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("coupon:coupon:info")
    public R info(@PathVariable("id") Long id) {
        CouponEntity coupon = couponService.getById(id);

        return R.ok().put("coupon", coupon);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("coupon:coupon:save")
    public R save(@RequestBody CouponEntity coupon) {
        couponService.save(coupon);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("coupon:coupon:update")
    public R update(@RequestBody CouponEntity coupon) {
        couponService.updateById(coupon);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("coupon:coupon:delete")
    public R delete(@RequestBody Long[] ids) {
        couponService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
