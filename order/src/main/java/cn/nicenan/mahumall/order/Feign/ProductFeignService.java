package cn.nicenan.mahumall.order.Feign;

import cn.nicenan.mahumall.common.utils.R;
import cn.nicenan.mahumall.order.vo.SpuInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("mahumall-product")
public interface ProductFeignService {

    @GetMapping("/product/spuinfo/skuId/{id}")
    R<SpuInfoVo> getSkuInfoBySkuId(@PathVariable("id") Long skuId);
}
