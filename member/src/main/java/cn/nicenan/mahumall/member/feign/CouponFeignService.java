package cn.nicenan.mahumall.member.feign;

import cn.nicenan.mahumall.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 远程调用
 * 1.引入open-feign
 * 2.编写接口（feign包），告诉SpringCloud这个接口需要调用远程服务
 * 3.接口加上注解@FeignClient("mahumall-coupon")  mahumall-coupon是服务的名字
 * 4.复制方法签名，补全RequestMapping整个路径
 * 5.开启远程调用功能，主类注解@EnableFeignClients(basePackages = "cn.nicenan.mahumall.member.feign") 自动扫描
 * 6.MemberController写一个测试请求 public R test();
 * 7.SpringCloud新版本需要在Common中排除Ribbon依赖，微服务模块添加loadbalancer依赖 https://blog.csdn.net/weixin_43556636/article/details/110653989
 */

@FeignClient("mahumall-coupon")
public interface CouponFeignService {

    @RequestMapping("/coupon/coupon/member/list")
    public R memberCoupons();
}
