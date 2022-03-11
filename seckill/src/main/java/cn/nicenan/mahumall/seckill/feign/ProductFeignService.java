package cn.nicenan.mahumall.seckill.feign;

import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.seckill.vo.SkuInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("mahumall-product")
public interface ProductFeignService {

    @RequestMapping("/product/skuinfo/info/{skuId}")
    R<SkuInfoVo> skuInfo(@PathVariable("skuId") Long skuId);
}
