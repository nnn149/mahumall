package cn.nicenan.mahumall.product.feign;

import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.product.vo.SeckillInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "mahumall-seckill")
public interface SeckillFeignService {

    @GetMapping("/sku/seckill/{skuId}")
    R<SeckillInfoVo> getSkuSeckillInfo(@PathVariable("skuId") Long skuId);
}
