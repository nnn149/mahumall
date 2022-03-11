package cn.nicenan.mahumall.seckill.feign;

import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.seckill.vo.SeckillSessionsWithSkus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("mahumall-coupon")
public interface CouponFeignService {

    @GetMapping("/coupon/seckillsession/lates3DaySession")
    R<List<SeckillSessionsWithSkus>> getLate3DaySession();
}
